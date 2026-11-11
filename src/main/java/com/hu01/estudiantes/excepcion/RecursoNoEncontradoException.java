package com.hu01.estudiantes.excepcion;

/**
 * Excepción de dominio: el recurso solicitado no existe. Se traduce a HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
