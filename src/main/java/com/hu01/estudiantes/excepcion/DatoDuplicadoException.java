package com.hu01.estudiantes.excepcion;

/**
 * Excepción de dominio: se intentó registrar un dato que debe ser único
 * (por ejemplo, el correo de un estudiante). Se traduce a HTTP 409.
 */
public class DatoDuplicadoException extends RuntimeException {

    public DatoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
