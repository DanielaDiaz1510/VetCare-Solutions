# 🐾 VetCare Solutions

**Una app para llevar la veterinaria en el celular.**

VetCare ayuda a una veterinaria a tener en un solo lugar la información de los dueños, las mascotas, las citas, las historias clínicas y las vacunas. Antes todo estaba disperso; ahora el equipo lo consulta y lo actualiza desde el celular.

Proyecto final de *Desarrollo de Aplicaciones Móviles Nativas* · Ingeniería de Software · Fundación Universitaria Compensar.

![Android](https://img.shields.io/badge/App-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Java](https://img.shields.io/badge/Servidor-Java-ED8B00?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/Base%20de%20datos-MySQL-4479A1?logo=mysql&logoColor=white)

## ✨ ¿Qué se puede hacer?

- **Iniciar sesión y registrarse** como veterinario o como administrador. Las cuentas nuevas las aprueba un administrador.
- **Mascotas:** buscarlas, registrarlas, editarlas, desactivarlas y reactivarlas.
- **Propietarios:** buscarlos, registrarlos y desactivarlos cuando ya no atienden en la clínica.
- **Citas:** agendar, cancelar y atender, sin que dos citas se crucen.
- **Historia clínica:** registrar lo que pasó en cada consulta, con sus tratamientos.
- **Vacunas:** ver el carnet de cada mascota y los refuerzos próximos.
- **Administración:** el administrador gestiona usuarios y veterinarios.
- **Modo oscuro** para trabajar cómodo.

## 👥 Dos tipos de usuario

| | Administrador | Veterinario |
|---|:---:|:---:|
| Mascotas, propietarios, citas, historias y vacunas | ✅ | ✅ |
| Activar o desactivar y eliminar registros | ✅ | ❌ |
| Gestionar usuarios y veterinarios | ✅ | ❌ |
| Aprobar cuentas nuevas | ✅ | ❌ |

## 🧩 ¿Cómo está hecho?

El proyecto tiene tres partes que trabajan juntas:

| Parte | Qué es | Tecnología |
|---|---|---|
| 📱 **App** | Lo que se ve y se toca en el celular | Android con Kotlin |
| ⚙️ **Servidor** | Recibe lo que pide la app, revisa los permisos y guarda los datos | Java y Tomcat |
| 🗄️ **Base de datos** | Donde queda guardada la información | MySQL |

La app nunca toca la base de datos directamente. Siempre pasa por el servidor, que verifica quién es el usuario y qué puede hacer. Las contraseñas se guardan cifradas.

## 🚀 Cómo probarlo

**1. Base de datos:** ejecutar `database/schema.sql` y luego `database/seed.sql` en MySQL.

**2. Servidor:** dentro de `backend/veterinaria`, ejecutar `mvn clean package` y poner el archivo `target/veterinaria.war` en Tomcat.

**3. App:** abrir el proyecto en **Android Studio**, conectar el celular por USB y ejecutar:
```
adb reverse tcp:8080 tcp:8080
```
Después, correr la app desde Android Studio.

**Cuentas de prueba**

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `veterinario` | `vet12345` | Veterinario |

> Son cuentas de demostración.

## 📚 Documentación

| Documento | Para qué sirve |
|---|---|
| [Documento empresarial y requisitos](docs/DOCUMENTO%20EMPRESARIAL_VETCARE%20SOLUTIONS%20S.A.S.pdf) | Qué debe hacer el sistema y sus reglas |
| [Manual de instalación y de usuario](docs/MANUAL_VETCARE%20SOLUTIONS%20S.A.S.pdf) | Cómo instalarlo y usar cada pantalla |
| [Diagramas](docs/DIAGRAMAS_VETCARE%20SOLUTIONS%20S.A.S.pdf) | Cómo está organizado el sistema |
| [Wireframes](docs/WIREFRAMES_VETCARE%20SOLUTIONS%20S.A.S.pdf) | Bocetos de las pantallas |
| [Evidencias](docs/EVIDENCIAS_VETCARE%20SOLUTIONS%20S.A.S.pdf) | La app funcionando en un celular real |

## 📁 Carpetas

```
├── app/                  La aplicación Android
├── backend/veterinaria/  El servidor
├── database/             Creación y datos de ejemplo de la base de datos
└── docs/                 Documentación en PDF
```

## 👩‍💻 Autores

- **Daniela Díaz Falla** · [@DanielaDiaz1510](https://github.com/DanielaDiaz1510)
- **William Fernando Guerrero**

Fundación Universitaria Compensar · Ingeniería de Software · 2026
