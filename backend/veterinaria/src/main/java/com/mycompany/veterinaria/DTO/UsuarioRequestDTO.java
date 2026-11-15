package com.mycompany.veterinaria.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/** Datos que envía el cliente para crear o editar un usuario (nunca se expone el hash). */
public class UsuarioRequestDTO {

    private Long idUsuario;
    private String username;
    private String nombre;
    private String email;
    private Boolean estado;
    @JsonAlias("passwordHash")      // compatibilidad con el formulario web anterior
    private String password;
    private List<String> roles;

    // Solo para el registro público (POST /api/registro)
    private String rolSolicitado;          // "VETERINARIO" o "ADMIN"
    private String tipoDocumento;          // datos del perfil de veterinario
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String telefono;

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getEstado() { return estado; }
    public void setEstado(Boolean estado) { this.estado = estado; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public String getRolSolicitado() { return rolSolicitado; }
    public void setRolSolicitado(String rolSolicitado) { this.rolSolicitado = rolSolicitado; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
