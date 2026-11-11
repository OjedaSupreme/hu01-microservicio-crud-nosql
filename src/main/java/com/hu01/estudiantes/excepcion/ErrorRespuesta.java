package com.hu01.estudiantes.excepcion;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Cuerpo JSON uniforme que la API devuelve ante cualquier error.
 */
@Schema(description = "Formato uniforme de las respuestas de error")
public record ErrorRespuesta(

        @Schema(description = "Momento del error en UTC (ISO-8601)", example = "2026-11-11T23:30:00Z")
        Instant timestamp,

        @Schema(description = "Código de estado HTTP", example = "404")
        int status,

        @Schema(description = "Nombre estándar del estado HTTP", example = "Not Found")
        String error,

        @Schema(description = "Explicación del error", example = "No existe un estudiante con el id 'abc'")
        String message,

        @Schema(description = "Ruta de la solicitud", example = "/api/estudiantes/abc")
        String path,

        @Schema(description = "Detalle por campo; lista vacía cuando no aplica")
        List<DetalleError> detalles) {
}
