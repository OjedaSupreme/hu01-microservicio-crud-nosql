package com.hu01.estudiantes.excepcion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hu01.estudiantes.controlador.EstudianteControlador;
import com.hu01.estudiantes.dto.EstudianteRequest;
import com.hu01.estudiantes.servicio.EstudianteServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Prueba de la capa web (sin base de datos) para los fallos que no se pueden
 * provocar de forma determinista contra un MongoDB real: un error inesperado y
 * el rechazo del índice único en una carrera entre dos peticiones.
 */
@WebMvcTest(EstudianteControlador.class)
class ManejadorGlobalExcepcionesTest {

    private static final String DETALLE_INTERNO = "conexion rechazada por el nodo mongo-interno:27017";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EstudianteServicio servicio;

    @Test
    @DisplayName("Un error inesperado responde 500 genérico, sin traza ni detalles internos")
    void errorInesperado_responde500SinFiltrarDetalles() throws Exception {
        when(servicio.listar()).thenThrow(new IllegalStateException(DETALLE_INTERNO));

        mockMvc.perform(get("/api/estudiantes"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Ocurrió un error interno. Inténtelo de nuevo más tarde"))
                .andExpect(jsonPath("$.path").value("/api/estudiantes"))
                .andExpect(jsonPath("$.detalles", hasSize(0)))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString(DETALLE_INTERNO))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at com.hu01"))));
    }

    @Test
    @DisplayName("Si MongoDB rechaza el índice único se responde 409 sin exponer el mensaje del driver")
    void claveDuplicadaDeMongo_responde409() throws Exception {
        when(servicio.crear(any(EstudianteRequest.class))).thenThrow(new DuplicateKeyException(
                "E11000 duplicate key error collection: estudiantes.estudiantes index: correo dup key"));
        String cuerpo = objectMapper.writeValueAsString(
                new EstudianteRequest("Ana", "García", "+50688887777", "ana.garcia@colegio.edu"));

        mockMvc.perform(post("/api/estudiantes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ya existe un estudiante registrado con el correo indicado"))
                .andExpect(jsonPath("$.path").value("/api/estudiantes"))
                .andExpect(jsonPath("$.detalles", hasSize(0)))
                .andExpect(content().string(not(containsString("E11000"))));
    }
}
