package com.mycompany.veterinaria.service;

import com.mycompany.veterinaria.DTO.PropietarioDTO;
import com.mycompany.veterinaria.dao.PropietarioDAO;
import com.mycompany.veterinaria.exception.AccesoDenegadoException;
import com.mycompany.veterinaria.exception.NoEncontradoException;
import com.mycompany.veterinaria.exception.ReglaNegocioException;
import com.mycompany.veterinaria.model.entity.Propietario;
import com.mycompany.veterinaria.util.Tx;
import com.mycompany.veterinaria.util.Validador;
import java.util.List;

/** Reglas de negocio de los propietarios (dueños de las mascotas). */
public class PropietarioService {

    private final AuditoriaService auditoria = new AuditoriaService();

    public List<PropietarioDTO> listarTodosDTO() {
        return buscarDTO(null, null, "TODOS");
    }

    /**
     * RF-09: filtra por texto (nombre/apellido), documento y estado.
     * Sin 'estado' se listan solo los ACTIVOS; con estado=TODOS, todos.
     */
    public List<PropietarioDTO> buscarDTO(String q, String documento, String estado) {
        String texto = (q == null || q.isBlank()) ? null : q.trim();
        String doc = (documento == null || documento.isBlank()) ? null : documento.trim();
        String est = (estado == null || estado.isBlank()) ? "ACTIVO"
                : "TODOS".equalsIgnoreCase(estado.trim()) ? null
                : Validador.uno(estado.trim().toUpperCase(), "estado", "ACTIVO", "INACTIVO");
        return Tx.leer(em -> new PropietarioDAO(em).buscar(texto, doc, est).stream().map(this::aDTO).toList());
    }

    /** RF-10: detalle de un propietario. */
    public PropietarioDTO obtenerDTO(Long id) {
        return Tx.leer(em -> aDTO(buscar(em.find(Propietario.class, id))));
    }

    public PropietarioDTO crear(Propietario datos, Long idActor) {
        PropietarioDTO creado = Tx.ejecutar(em -> {
            Propietario p = new Propietario();
            copiar(datos, p);
            if (p.getEstado() == null) {
                p.setEstado("ACTIVO");
            }
            if (new PropietarioDAO(em).existeDocumento(p.getTipoDocumento(), p.getNumeroDocumento(), null)) {
                throw new ReglaNegocioException("Ya existe un propietario con el documento "
                        + p.getTipoDocumento() + " " + p.getNumeroDocumento());
            }
            em.persist(p);
            return aDTO(p);
        });
        auditoria.registrar(idActor, "CREAR", "Propietario", creado.getId(),
                "Creación de propietario: " + creado.getNombres() + " " + creado.getApellidos());
        return creado;
    }

    public PropietarioDTO actualizar(Propietario datos, Long idActor, boolean esAdmin) {
        PropietarioDTO actualizado = Tx.ejecutar(em -> {
            Propietario actual = buscar(datos.getIdPropietario() == null ? null : em.find(Propietario.class, datos.getIdPropietario()));
            String estadoAntes = actual.getEstado();
            copiar(datos, actual);
            // Cambiar el estado (desactivar o reactivar) es solo del administrador, como eliminar
            if (!esAdmin && !java.util.Objects.equals(estadoAntes, actual.getEstado())) {
                throw new AccesoDenegadoException("Solo un administrador puede desactivar o reactivar propietarios");
            }
            // Desactivar (PUT con estado INACTIVO): no puede quedar una mascota activa con dueño inactivo
            if ("INACTIVO".equals(actual.getEstado()) && !"INACTIVO".equals(estadoAntes)) {
                long activas = new PropietarioDAO(em).contarMascotasActivas(actual.getIdPropietario());
                if (activas > 0) {
                    throw new ReglaNegocioException("No se puede desactivar: el propietario tiene " + activas
                            + " mascota(s) activa(s). Desactívelas primero");
                }
            }
            if (new PropietarioDAO(em).existeDocumento(actual.getTipoDocumento(), actual.getNumeroDocumento(), actual.getIdPropietario())) {
                throw new ReglaNegocioException("Ya existe otro propietario con el documento "
                        + actual.getTipoDocumento() + " " + actual.getNumeroDocumento());
            }
            return aDTO(actual);
        });
        auditoria.registrar(idActor, "ACTUALIZAR", "Propietario", actualizado.getId(),
                "Actualización de propietario: " + actualizado.getNombres() + " " + actualizado.getApellidos());
        return actualizado;
    }

    /** Solo se elimina si no tiene mascotas: las mascotas y su historia clínica nunca se borran en cascada. */
    public void eliminar(Long id, Long idActor) {
        String nombre = Tx.ejecutar(em -> {
            Propietario p = buscar(em.find(Propietario.class, id));
            long mascotas = new PropietarioDAO(em).contarMascotas(id);
            if (mascotas > 0) {
                throw new ReglaNegocioException("No se puede eliminar: el propietario tiene " + mascotas
                        + " mascota(s) registrada(s)");
            }
            String n = p.getNombres() + " " + p.getApellidos();
            em.remove(p);
            return n;
        });
        auditoria.registrar(idActor, "ELIMINAR", "Propietario", id, "Eliminación de propietario: " + nombre);
    }

    // ------------------------------------------------------------------ internos

    private Propietario buscar(Propietario p) {
        if (p == null) {
            throw new NoEncontradoException("Propietario no encontrado");
        }
        return p;
    }

    /** Valida los datos recibidos y los copia a la entidad (solo campos permitidos). */
    private void copiar(Propietario origen, Propietario destino) {
        destino.setTipoDocumento(Validador.uno(origen.getTipoDocumento(), "tipoDocumento", "CC", "TI", "CE", "PAS", "NIT"));
        destino.setNumeroDocumento(Validador.documento(origen.getNumeroDocumento()));
        destino.setNombres(Validador.requerido(origen.getNombres(), "nombres", 100));
        destino.setApellidos(Validador.requerido(origen.getApellidos(), "apellidos", 100));
        destino.setTelefono(Validador.telefonoOpcional(origen.getTelefono()));
        destino.setEmail(Validador.emailOpcional(origen.getEmail()));
        destino.setDireccion(Validador.opcional(origen.getDireccion(), "direccion", 200));
        // El estado solo cambia si el cliente lo envía; si no, se conserva (ACTIVO en los nuevos)
        if (origen.getEstado() != null && !origen.getEstado().isBlank()) {
            destino.setEstado(Validador.uno(origen.getEstado().trim().toUpperCase(), "estado", "ACTIVO", "INACTIVO"));
        }
    }

    private PropietarioDTO aDTO(Propietario p) {
        return new PropietarioDTO(p.getIdPropietario(), p.getTipoDocumento(), p.getNumeroDocumento(),
                p.getNombres(), p.getApellidos(), p.getTelefono(), p.getEmail(), p.getDireccion(), p.getEstado());
    }
}
