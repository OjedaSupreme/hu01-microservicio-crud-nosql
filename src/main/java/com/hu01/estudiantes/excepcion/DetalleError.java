package com.hu01.estudiantes.excepcion;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalle puntual de un error, normalmente asociado a un campo de la solicitud.
 */
@Schema(description = "Detalle de un error asociado a un campo de la solicitud")
public record DetalleError(

        @Schema(description = "Campo que originó el error", example = "correo")
        String campo,

        @Schema(description = "Descripción del problema", example = "El correo no tiene un formato válido")
        String mensaje) {
}
