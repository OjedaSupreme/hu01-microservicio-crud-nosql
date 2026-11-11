# HU-01 · Microservicio CRUD de estudiantes con MongoDB

Microservicio RESTful que administra los **estudiantes** de un colegio sobre una base de datos NoSQL (MongoDB).
Incluye excepciones de dominio, un manejador global de errores con respuesta JSON uniforme, pruebas unitarias
(Mockito) y de integración (MongoDB embebido), cobertura con JaCoCo y análisis estático con SonarQube.

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data MongoDB, Validation) |
| Base de datos | MongoDB 7 |
| Documentación | springdoc-openapi 3.0.0 (Swagger UI) |
| Pruebas | JUnit 5, Mockito, MockMvc, MongoDB embebido (flapdoodle) |
| Calidad | JaCoCo 0.8.14 (mínimo 80 % de líneas y de ramas), SonarQube Community |

## Requisitos

- **JDK 21** (`java -version`).
- **MongoDB** accesible en `localhost:27017`, o bien **Docker Desktop** para levantarlo con `compose.yaml`.
- No hace falta instalar Maven: el proyecto trae el wrapper (`mvnw.cmd` en Windows, `./mvnw` en Linux/macOS).
- Para las pruebas no se necesita ni Docker ni MongoDB: usan un MongoDB embebido que se descarga solo la primera
  vez (requiere conexión a internet en esa primera ejecución).

## Puesta en marcha

### 1. Levantar MongoDB con Docker Compose

```powershell
docker compose up -d mongo
```

MongoDB queda publicado en el puerto `27017`, sin usuario ni contraseña (uso local).

> Si ya tienes un MongoDB instalado ocupando el puerto 27017, puedes usarlo directamente y saltarte este paso,
> o publicar el contenedor en otro puerto:
>
> ```powershell
> $env:MONGO_PORT = "27018"
> docker compose up -d mongo
> $env:MONGODB_URI = "mongodb://localhost:27018/estudiantes"
> ```

### 2. Ejecutar el microservicio

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda en <http://localhost:8080>.

### Configuración

La conexión se lee de variables de entorno, con valores por defecto para desarrollo local. **Ninguna contraseña se
guarda en el repositorio**: si tu MongoDB exige credenciales, inclúyelas en `MONGODB_URI`.

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/estudiantes` | Cadena de conexión a MongoDB |
| `SERVER_PORT` | `8080` | Puerto HTTP del microservicio |

```powershell
# Ejemplo con credenciales (solo en tu sesión, nunca en un archivo versionado)
$env:MONGODB_URI = "mongodb://usuario:clave@localhost:27017/estudiantes?authSource=admin"
.\mvnw.cmd spring-boot:run
```

> En Spring Boot 4 la propiedad de conexión se llama `spring.mongodb.uri` (en Boot 3 era
> `spring.data.mongodb.uri`). Está definida en `src/main/resources/application.properties`.

## Documentación interactiva (Swagger)

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- Especificación OpenAPI: <http://localhost:8080/v3/api-docs>

## API

Ruta base: `/api/estudiantes`

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/estudiantes` | Crea un estudiante | `201 Created`, `400`, `409` |
| `GET` | `/api/estudiantes` | Lista todos los estudiantes | `200 OK` |
| `GET` | `/api/estudiantes/{id}` | Busca un estudiante por id | `200 OK`, `404` |
| `PUT` | `/api/estudiantes/{id}` | Actualiza un estudiante | `200 OK`, `400`, `404`, `409` |
| `DELETE` | `/api/estudiantes/{id}` | Elimina un estudiante | `204 No Content`, `404` |

### Estudiante

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | String | ObjectId generado por MongoDB (solo en respuestas) |
| `nombre` | String | Obligatorio, máximo 60 caracteres |
| `apellido` | String | Obligatorio, máximo 60 caracteres |
| `telefono` | String | Obligatorio, de 7 a 15 dígitos con prefijo `+` opcional (`^\+?\d{7,15}$`) |
| `correo` | String | Obligatorio, formato de correo, máximo 120 caracteres, **único**. Se guarda en minúsculas |

### Ejemplos

Crear un estudiante (guarda antes el cuerpo en un archivo `estudiante.json` con codificación UTF-8; así se evitan
los problemas de comillas de cada terminal):

```powershell
curl.exe -i -X POST http://localhost:8080/api/estudiantes -H "Content-Type: application/json" --data-binary "@estudiante.json"
```

Cuerpo de la solicitud (`POST` y `PUT`):

```json
{
  "nombre": "Ana",
  "apellido": "García",
  "telefono": "+50688887777",
  "correo": "Ana.Garcia@Colegio.edu"
}
```

Respuesta `201 Created` (incluye la cabecera `Location: /api/estudiantes/{id}`):

```json
{
  "id": "6af4feefe425ed1c9108495c",
  "nombre": "Ana",
  "apellido": "García",
  "telefono": "+50688887777",
  "correo": "ana.garcia@colegio.edu"
}
```

Otras operaciones:

```powershell
curl.exe http://localhost:8080/api/estudiantes
curl.exe http://localhost:8080/api/estudiantes/6af4feefe425ed1c9108495c
curl.exe -X PUT http://localhost:8080/api/estudiantes/6af4feefe425ed1c9108495c -H "Content-Type: application/json" --data-binary "@estudiante.json"
curl.exe -X DELETE http://localhost:8080/api/estudiantes/6af4feefe425ed1c9108495c
```

## Errores

Todas las respuestas de error comparten el mismo cuerpo JSON:

| Campo | Descripción |
|---|---|
| `timestamp` | Momento del error en UTC (ISO-8601) |
| `status` | Código de estado HTTP |
| `error` | Nombre estándar del estado HTTP |
| `message` | Explicación del error |
| `path` | Ruta de la solicitud |
| `detalles` | Lista de `{ "campo", "mensaje" }`; vacía cuando no aplica |

| Código | Cuándo ocurre | Origen |
|---|---|---|
| `400 Bad Request` | Algún campo no cumple las validaciones | `MethodArgumentNotValidException` |
| `400 Bad Request` | El cuerpo no es un JSON legible | `HttpMessageNotReadableException` |
| `404 Not Found` | No existe un estudiante con ese id | `RecursoNoEncontradoException` |
| `409 Conflict` | El correo ya pertenece a otro estudiante (al crear o al actualizar) | `DatoDuplicadoException` |
| `409 Conflict` | MongoDB rechaza el índice único (dos peticiones simultáneas) | `DuplicateKeyException` |
| `405` / `415` / `404` | Método, tipo de contenido o ruta no admitidos | Errores estándar de Spring MVC |
| `500 Internal Server Error` | Fallo inesperado. El detalle queda en el log del servidor; el cliente nunca recibe la traza | `Exception` |

Validación (`400`):

```json
{
  "timestamp": "2026-11-11T23:49:03.494753700Z",
  "status": 400,
  "error": "Bad Request",
  "message": "La solicitud contiene datos inválidos",
  "path": "/api/estudiantes",
  "detalles": [
    { "campo": "correo", "mensaje": "El correo no tiene un formato válido" },
    { "campo": "nombre", "mensaje": "El nombre es obligatorio" },
    { "campo": "telefono", "mensaje": "El teléfono debe tener entre 7 y 15 dígitos, con prefijo + opcional" }
  ]
}
```

Recurso inexistente (`404`):

```json
{
  "timestamp": "2026-11-11T23:49:03.629659Z",
  "status": 404,
  "error": "Not Found",
  "message": "No existe un estudiante con el id '665f1c2ab7e4a93f5c0d1e2f'",
  "path": "/api/estudiantes/665f1c2ab7e4a93f5c0d1e2f",
  "detalles": []
}
```

Correo duplicado (`409`):

```json
{
  "timestamp": "2026-11-11T23:49:03.452233800Z",
  "status": 409,
  "error": "Conflict",
  "message": "Ya existe un estudiante registrado con el correo 'ana.garcia@colegio.edu'",
  "path": "/api/estudiantes",
  "detalles": []
}
```

## Pruebas

```powershell
.\mvnw.cmd clean verify
```

Ese comando compila, ejecuta todas las pruebas, genera el reporte de cobertura y **falla si la cobertura baja del
80 %** de líneas o de ramas. Para ejecutar solo las pruebas: `.\mvnw.cmd test`.

| Clase | Tipo | Qué comprueba |
|---|---|---|
| `EstudianteServicioTest` | Unitaria (Mockito, sin Spring) | Reglas de negocio: éxito y error de crear, listar, buscar, actualizar y eliminar |
| `EstudianteApiIntegrationTest` | Integración (`@SpringBootTest` + MockMvc) | Flujo HTTP completo contra MongoDB embebido: códigos, JSON, validaciones, persistencia, índice único y Swagger |
| `ManejadorGlobalExcepcionesTest` | Capa web (`@WebMvcTest`) | Respuesta 500 sin traza y 409 cuando MongoDB rechaza una clave duplicada |

Las pruebas de integración usan el perfil `test` (`src/test/resources/application-test.properties`), donde la
propiedad `de.flapdoodle.mongodb.embedded.version` fija la versión del MongoDB embebido. El binario se descarga la
primera vez (unos minutos) y queda en caché en `~/.embedmongo`. El servidor embebido arranca en un puerto libre, así
que no interfiere con un MongoDB que ya esté en ejecución.

### Cobertura (JaCoCo)

Tras `.\mvnw.cmd clean verify` (o `.\mvnw.cmd test`) el reporte queda en:

- HTML: `target/site/jacoco/index.html`
- XML (el que lee SonarQube): `target/site/jacoco/jacoco.xml`
- CSV: `target/site/jacoco/jacoco.csv`

```powershell
start target\site\jacoco\index.html
```

Los umbrales están en `pom.xml` (`jacoco.cobertura.lineas` y `jacoco.cobertura.ramas`). Solo se excluye del cálculo
la clase de arranque `EstudiantesApplication`.

## Análisis con SonarQube

1. Levanta SonarQube Community y su PostgreSQL:

   ```powershell
   docker compose up -d sonarqube
   ```

   Espera a que <http://localhost:9000/api/system/status> responda `"status":"UP"` (uno o dos minutos).

2. Entra en <http://localhost:9000> con `admin` / `admin` (SonarQube pide cambiar la contraseña en el primer
   acceso) y genera un token en **Mi cuenta → Seguridad**.

3. Ejecuta el análisis pasando el token por variable de entorno:

   ```powershell
   $env:SONAR_TOKEN = "<tu-token>"
   .\mvnw.cmd clean verify sonar:sonar "-Dsonar.qualitygate.wait=true"
   ```

   Con `sonar.qualitygate.wait=true` el comando espera el resultado del Quality Gate y falla si no se supera.

4. Consulta el resultado en <http://localhost:9000/dashboard?id=hu01-microservicio-crud-nosql>.

La configuración del análisis (clave del proyecto, URL del servidor y
`sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`) está en las propiedades de `pom.xml`, que son
las que lee `sonar-maven-plugin`. El archivo `sonar-project.properties` contiene los mismos valores para quien
prefiera usar SonarScanner CLI.

> Las credenciales `sonar` / `sonar` de la base de datos de SonarQube en `compose.yaml` son **solo para desarrollo
> local**. Se pueden cambiar con las variables `SONAR_DB_USER` y `SONAR_DB_PASSWORD` (ver `.env.example`).

Para detener y eliminar los contenedores: `docker compose down` (añade `-v` para borrar también los datos).

## Estructura del proyecto

```text
src/main/java/com/hu01/estudiantes
├── EstudiantesApplication.java        Clase de arranque
├── config/OpenApiConfig.java          Metadatos de Swagger
├── controlador/EstudianteControlador  Endpoints REST
├── dto/                               EstudianteRequest (validaciones) y EstudianteResponse
├── excepcion/                         RecursoNoEncontradoException, DatoDuplicadoException,
│                                      ManejadorGlobalExcepciones, ErrorRespuesta, DetalleError
├── modelo/Estudiante.java             Documento MongoDB (índice único en correo)
├── repositorio/EstudianteRepositorio  Spring Data MongoDB
└── servicio/EstudianteServicio        Reglas de negocio
src/test/java/com/hu01/estudiantes     Pruebas unitarias y de integración
compose.yaml                           MongoDB, SonarQube y PostgreSQL para uso local
sonar-project.properties               Configuración para SonarScanner CLI
docs/reporte-tecnico-hu01.md           Reporte técnico con las evidencias de la HU-01
```

## Decisiones de diseño

- **Unicidad del correo en dos niveles.** El servicio comprueba el correo antes de guardar para devolver un mensaje
  claro, y MongoDB tiene un índice único que cubre el caso de dos peticiones simultáneas; ambos terminan en `409`.
- **Correo normalizado.** Se guarda sin espacios y en minúsculas, de modo que `Ana@Colegio.edu` y `ana@colegio.edu`
  son el mismo correo.
- **Actualizar sin cambiar el correo no es un duplicado.** La unicidad solo se valida cuando el correo cambia.
- **Sin fugas de información.** El manejador global nunca devuelve trazas ni mensajes del driver de MongoDB.
