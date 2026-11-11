package com.hu01.estudiantes.dto;

import com.hu01.estudiantes.modelo.Estudiante;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representación de un estudiante que la API devuelve al cliente.
 */
@Schema(description = "Estudiante registrado")
public record EstudianteResponse(

        @Schema(description = "Identificador (ObjectId de MongoDB)", example = "665f1c2ab7e4a93f5c0d1e2f")
        String id,

        @Schema(example = "Ana")
        String nombre,

        @Schema(example = "García")
        String apellido,

        @Schema(example = "+50688887777")
        String telefono,

        @Schema(example = "ana.garcia@colegio.edu")
        String correo) {

    public static EstudianteResponse desde(Estudiante estudiante) {
        return new EstudianteResponse(
                estudiante.getId(),
                estudiante.getNombre(),
                estudiante.getApellido(),
                estudiante.getTelefono(),
                estudiante.getCorreo());
    }
}
