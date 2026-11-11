package com.hu01.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear o actualizar un estudiante.
 */
@Schema(description = "Datos necesarios para crear o actualizar un estudiante")
public record EstudianteRequest(

        @Schema(description = "Nombre del estudiante", example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar los {max} caracteres")
        String nombre,

        @Schema(description = "Apellido del estudiante", example = "García")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 60, message = "El apellido no puede superar los {max} caracteres")
        String apellido,

        @Schema(description = "Teléfono de contacto: de 7 a 15 dígitos con prefijo + opcional",
                example = "+50688887777")
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+?\\d{7,15}$",
                message = "El teléfono debe tener entre 7 y 15 dígitos, con prefijo + opcional")
        String telefono,

        @Schema(description = "Correo electrónico, único por estudiante", example = "ana.garcia@colegio.edu")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 120, message = "El correo no puede superar los {max} caracteres")
        String correo) {
}
