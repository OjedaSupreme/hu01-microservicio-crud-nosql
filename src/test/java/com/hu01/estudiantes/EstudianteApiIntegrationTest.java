package com.hu01.estudiantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hu01.estudiantes.dto.EstudianteRequest;
import com.hu01.estudiantes.dto.EstudianteResponse;
import com.hu01.estudiantes.modelo.Estudiante;
import com.hu01.estudiantes.repositorio.EstudianteRepositorio;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import de.flapdoodle.embed.mongo.config.Net;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Pruebas de integración: recorren el flujo HTTP completo (validación,
 * serialización JSON, servicio, repositorio) contra un MongoDB embebido real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstudianteApiIntegrationTest {

    private static final String RUTA = "/api/estudiantes";
    private static final String ID_INEXISTENTE = "665f1c2ab7e4a93f5c0d1e2f";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EstudianteRepositorio repositorio;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private Net redMongoEmbebido;

    @BeforeEach
    void limpiarColeccion() {
        repositorio.deleteAll();
    }

    // ---------------------------------------------------------------- infraestructura

    @Test
    @DisplayName("El cliente de MongoDB apunta al servidor embebido, no a una instancia externa")
    void elClienteUsaElMongoEmbebido() {
        assertThat(mongoClient.getClusterDescription().getClusterSettings().getHosts())
                .extracting(ServerAddress::getPort)
                .containsExactly(redMongoEmbebido.getPort());
    }

    @Test
    @DisplayName("MongoDB tiene un índice único sobre el correo y rechaza duplicados por sí mismo")
    void mongoTieneIndiceUnicoEnCorreo() {
        assertThat(mongoTemplate.indexOps(Estudiante.class).getIndexInfo())
                .filteredOn(IndexInfo::isUnique)
                .flatExtracting(IndexInfo::getIndexFields)
                .extracting("key")
                .containsExactly("correo");

        repositorio.save(new Estudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu"));
        Estudiante duplicado = new Estudiante("Otra", "Persona", "22223333", "ana.garcia@colegio.edu");

        assertThatThrownBy(() -> repositorio.save(duplicado)).isInstanceOf(DuplicateKeyException.class);
    }

    // ---------------------------------------------------------------- crear

    @Test
    @DisplayName("POST crea el estudiante: 201, cabecera Location y documento persistido con correo en minúsculas")
    void crear_responde201YPersiste() throws Exception {
        MvcResult resultado = mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest("Ana", "García", "+50688887777", "Ana.Garcia@Colegio.EDU"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("García"))
                .andExpect(jsonPath("$.telefono").value("+50688887777"))
                .andExpect(jsonPath("$.correo").value("ana.garcia@colegio.edu"))
                .andReturn();

        EstudianteResponse creado = leer(resultado);
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(RUTA + "/" + creado.id());
        assertThat(repositorio.findById(creado.id())).hasValueSatisfying(persistido -> {
            assertThat(persistido.getNombre()).isEqualTo("Ana");
            assertThat(persistido.getApellido()).isEqualTo("García");
            assertThat(persistido.getTelefono()).isEqualTo("+50688887777");
            assertThat(persistido.getCorreo()).isEqualTo("ana.garcia@colegio.edu");
        });
    }

    @Test
    @DisplayName("POST con varios campos inválidos responde 400 con el detalle de cada campo y no persiste")
    void crear_conCamposInvalidos_responde400ConDetallePorCampo() throws Exception {
        String cuerpo = json(new EstudianteRequest(" ", "A".repeat(61), "12-34", "no-es-un-correo"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("La solicitud contiene datos inválidos"))
                .andExpect(jsonPath("$.path").value(RUTA))
                .andExpect(jsonPath("$.detalles", hasSize(4)))
                .andExpect(jsonPath("$.detalles[*].campo",
                        containsInAnyOrder("nombre", "apellido", "telefono", "correo")))
                .andExpect(jsonPath("$.detalles[*].mensaje", containsInAnyOrder(
                        "El nombre es obligatorio",
                        "El apellido no puede superar los 60 caracteres",
                        "El teléfono debe tener entre 7 y 15 dígitos, con prefijo + opcional",
                        "El correo no tiene un formato válido")));

        assertThat(repositorio.count()).isZero();
    }

    @Test
    @DisplayName("POST sin los campos obligatorios responde 400 e indica cada campo faltante")
    void crear_sinCamposObligatorios_responde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasSize(4)))
                .andExpect(jsonPath("$.detalles[*].mensaje", containsInAnyOrder(
                        "El nombre es obligatorio",
                        "El apellido es obligatorio",
                        "El teléfono es obligatorio",
                        "El correo es obligatorio")));
    }

    @Test
    @DisplayName("POST con JSON mal formado responde 400 con el cuerpo de error uniforme")
    void crear_conJsonInvalido_responde400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": \"Ana\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("El cuerpo de la solicitud no es un JSON válido o no se puede leer"))
                .andExpect(jsonPath("$.path").value(RUTA))
                .andExpect(jsonPath("$.detalles", hasSize(0)));

        assertThat(repositorio.count()).isZero();
    }

    @Test
    @DisplayName("POST con un correo ya registrado (aunque cambie de mayúsculas) responde 409 y no duplica")
    void crear_conCorreoDuplicado_responde409() throws Exception {
        crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");

        mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest("Otra", "Persona", "22223333", "ANA.GARCIA@colegio.edu"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("ana.garcia@colegio.edu")))
                .andExpect(jsonPath("$.path").value(RUTA))
                .andExpect(jsonPath("$.detalles", hasSize(0)));

        assertThat(repositorio.count()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- listar y buscar

    @Test
    @DisplayName("GET lista vacía cuando no hay estudiantes")
    void listar_sinEstudiantes_responde200ConListaVacia() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET lista todos los estudiantes persistidos")
    void listar_responde200ConTodos() throws Exception {
        crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");
        crearEstudiante("Luis", "Mora", "22223333", "luis.mora@colegio.edu");

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].correo",
                        containsInAnyOrder("ana.garcia@colegio.edu", "luis.mora@colegio.edu")))
                .andExpect(jsonPath("$[*].nombre", hasItems("Ana", "Luis")));
    }

    @Test
    @DisplayName("GET por id devuelve 200 con el estudiante")
    void buscarPorId_existente_responde200() throws Exception {
        EstudianteResponse creado = crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");

        mockMvc.perform(get(RUTA + "/{id}", creado.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creado.id()))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("García"))
                .andExpect(jsonPath("$.telefono").value("+50688887777"))
                .andExpect(jsonPath("$.correo").value("ana.garcia@colegio.edu"));
    }

    @Test
    @DisplayName("GET por id inexistente devuelve 404 con el cuerpo de error uniforme")
    void buscarPorId_inexistente_responde404() throws Exception {
        mockMvc.perform(get(RUTA + "/{id}", ID_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString(ID_INEXISTENTE)))
                .andExpect(jsonPath("$.path").value(RUTA + "/" + ID_INEXISTENTE))
                .andExpect(jsonPath("$.detalles", hasSize(0)));
    }

    @Test
    @DisplayName("GET con un id que no tiene formato de ObjectId también devuelve 404")
    void buscarPorId_conIdSinFormatoObjectId_responde404() throws Exception {
        mockMvc.perform(get(RUTA + "/{id}", "no-es-un-object-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------------------------------------------------------------- actualizar

    @Test
    @DisplayName("PUT actualiza todos los campos: 200 y cambios persistidos")
    void actualizar_responde200YPersiste() throws Exception {
        EstudianteResponse creado = crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");

        mockMvc.perform(put(RUTA + "/{id}", creado.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest(
                                "Ana María", "García Solís", "22223333", "Ana.Maria@Colegio.edu"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creado.id()))
                .andExpect(jsonPath("$.nombre").value("Ana María"))
                .andExpect(jsonPath("$.apellido").value("García Solís"))
                .andExpect(jsonPath("$.telefono").value("22223333"))
                .andExpect(jsonPath("$.correo").value("ana.maria@colegio.edu"));

        assertThat(repositorio.count()).isEqualTo(1);
        assertThat(repositorio.findById(creado.id())).hasValueSatisfying(persistido -> {
            assertThat(persistido.getNombre()).isEqualTo("Ana María");
            assertThat(persistido.getCorreo()).isEqualTo("ana.maria@colegio.edu");
        });
    }

    @Test
    @DisplayName("PUT que conserva el correo propio responde 200 (no es un duplicado)")
    void actualizar_conservandoElCorreo_responde200() throws Exception {
        EstudianteResponse creado = crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");

        mockMvc.perform(put(RUTA + "/{id}", creado.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest("Ana", "García", "70001111", "ana.garcia@colegio.edu"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("70001111"))
                .andExpect(jsonPath("$.correo").value("ana.garcia@colegio.edu"));
    }

    @Test
    @DisplayName("PUT sobre un id inexistente responde 404 y no crea nada")
    void actualizar_inexistente_responde404() throws Exception {
        mockMvc.perform(put(RUTA + "/{id}", ID_INEXISTENTE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest("Ana", "García", "+50688887777", "ana.garcia@colegio.edu"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString(ID_INEXISTENTE)))
                .andExpect(jsonPath("$.path").value(RUTA + "/" + ID_INEXISTENTE));

        assertThat(repositorio.count()).isZero();
    }

    @Test
    @DisplayName("PUT con el correo de otro estudiante responde 409 y conserva los datos originales")
    void actualizar_conCorreoDeOtroEstudiante_responde409() throws Exception {
        crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");
        EstudianteResponse luis = crearEstudiante("Luis", "Mora", "22223333", "luis.mora@colegio.edu");

        mockMvc.perform(put(RUTA + "/{id}", luis.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest("Luis", "Mora", "22223333", "Ana.Garcia@colegio.edu"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString("ana.garcia@colegio.edu")));

        assertThat(repositorio.findById(luis.id()))
                .hasValueSatisfying(persistido -> assertThat(persistido.getCorreo()).isEqualTo("luis.mora@colegio.edu"));
    }

    @Test
    @DisplayName("PUT con datos inválidos responde 400 con detalle por campo")
    void actualizar_conDatosInvalidos_responde400() throws Exception {
        EstudianteResponse creado = crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");

        mockMvc.perform(put(RUTA + "/{id}", creado.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest("Ana", "García", "telefono", "ana.garcia@colegio.edu"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                .andExpect(jsonPath("$.detalles[0].campo").value("telefono"));
    }

    // ---------------------------------------------------------------- eliminar

    @Test
    @DisplayName("DELETE elimina el estudiante: 204 sin cuerpo y el documento desaparece")
    void eliminar_responde204YBorra() throws Exception {
        EstudianteResponse creado = crearEstudiante("Ana", "García", "+50688887777", "ana.garcia@colegio.edu");

        mockMvc.perform(delete(RUTA + "/{id}", creado.id()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(repositorio.existsById(creado.id())).isFalse();
        mockMvc.perform(get(RUTA + "/{id}", creado.id())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE sobre un id inexistente responde 404")
    void eliminar_inexistente_responde404() throws Exception {
        mockMvc.perform(delete(RUTA + "/{id}", ID_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString(ID_INEXISTENTE)))
                .andExpect(jsonPath("$.path").value(RUTA + "/" + ID_INEXISTENTE));
    }

    // ---------------------------------------------------------------- errores del framework

    @Test
    @DisplayName("Un método HTTP no soportado responde 405 con el cuerpo de error uniforme")
    void metodoNoSoportado_responde405() throws Exception {
        mockMvc.perform(patch(RUTA))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists(HttpHeaders.ALLOW))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("El método HTTP no está permitido para este recurso"))
                .andExpect(jsonPath("$.path").value(RUTA));
    }

    @Test
    @DisplayName("Un cuerpo que no es JSON responde 415 con el cuerpo de error uniforme")
    void tipoDeContenidoNoSoportado_responde415() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.TEXT_PLAIN).content("nombre=Ana"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.path").value(RUTA));
    }

    @Test
    @DisplayName("Una ruta que no existe responde 404 con el cuerpo de error uniforme")
    void rutaInexistente_responde404() throws Exception {
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("El recurso solicitado no existe"))
                .andExpect(jsonPath("$.path").value("/api/ruta-que-no-existe"));
    }

    // ---------------------------------------------------------------- documentación

    @Test
    @DisplayName("La especificación OpenAPI describe los endpoints de estudiantes")
    void openApi_describeLosEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de Estudiantes - HU-01"))
                .andExpect(jsonPath("$.paths['/api/estudiantes'].post").exists())
                .andExpect(jsonPath("$.paths['/api/estudiantes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/estudiantes/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/estudiantes/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/estudiantes/{id}'].delete").exists());
    }

    @Test
    @DisplayName("Swagger UI está disponible en /swagger-ui/index.html")
    void swaggerUi_estaDisponible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("swagger-ui")));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/swagger-ui/index.html")));
    }

    // ---------------------------------------------------------------- utilidades

    private String json(EstudianteRequest solicitud) {
        return objectMapper.writeValueAsString(solicitud);
    }

    private EstudianteResponse leer(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(cuerpo, EstudianteResponse.class);
    }

    /** Crea un estudiante a través de la propia API y devuelve la respuesta ya deserializada. */
    private EstudianteResponse crearEstudiante(String nombre, String apellido, String telefono, String correo)
            throws Exception {
        MvcResult resultado = mockMvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new EstudianteRequest(nombre, apellido, telefono, correo))))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado);
    }
}
