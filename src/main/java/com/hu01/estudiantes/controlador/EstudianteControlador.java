package com.hu01.estudiantes.controlador;

import com.hu01.estudiantes.dto.EstudianteRequest;
import com.hu01.estudiantes.dto.EstudianteResponse;
import com.hu01.estudiantes.excepcion.ErrorRespuesta;
import com.hu01.estudiantes.servicio.EstudianteServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/estudiantes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Estudiantes", description = "CRUD de estudiantes del colegio")
public class EstudianteControlador {

    private final EstudianteServicio servicio;

    public EstudianteControlador(EstudianteServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Crear un estudiante")
    @ApiResponse(responseCode = "201", description = "Estudiante creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o JSON ilegible",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<EstudianteResponse> crear(@Valid @RequestBody EstudianteRequest solicitud) {
        EstudianteResponse creado = servicio.crear(solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar todos los estudiantes")
    @ApiResponse(responseCode = "200", description = "Listado de estudiantes (puede estar vacío)")
    public List<EstudianteResponse> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar un estudiante por id")
    @ApiResponse(responseCode = "200", description = "Estudiante encontrado")
    @ApiResponse(responseCode = "404", description = "No existe un estudiante con ese id",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public EstudianteResponse buscarPorId(@PathVariable String id) {
        return servicio.buscarPorId(id);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Actualizar un estudiante")
    @ApiResponse(responseCode = "200", description = "Estudiante actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o JSON ilegible",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe un estudiante con ese id",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El correo pertenece a otro estudiante",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public EstudianteResponse actualizar(
            @PathVariable String id, @Valid @RequestBody EstudianteRequest solicitud) {
        return servicio.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un estudiante")
    @ApiResponse(responseCode = "204", description = "Estudiante eliminado")
    @ApiResponse(responseCode = "404", description = "No existe un estudiante con ese id",
            content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
