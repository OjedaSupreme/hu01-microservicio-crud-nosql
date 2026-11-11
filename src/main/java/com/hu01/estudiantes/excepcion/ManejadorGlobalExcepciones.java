package com.hu01.estudiantes.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de la aplicación a respuestas HTTP con un cuerpo JSON
 * uniforme ({@link ErrorRespuesta}), sin exponer detalles internos al cliente.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    private static final String MENSAJE_VALIDACION = "La solicitud contiene datos inválidos";
    private static final String MENSAJE_JSON_ILEGIBLE =
            "El cuerpo de la solicitud no es un JSON válido o no se puede leer";
    private static final String MENSAJE_CLAVE_DUPLICADA =
            "Ya existe un estudiante registrado con el correo indicado";
    private static final String MENSAJE_ERROR_INTERNO =
            "Ocurrió un error interno. Inténtelo de nuevo más tarde";
    private static final String MENSAJE_SOLICITUD_NO_PROCESABLE = "La solicitud no pudo ser procesada";

    /** Mensajes en español para los errores HTTP que resuelve el propio Spring MVC. */
    private static final Map<HttpStatus, String> MENSAJES_HTTP = Map.of(
            HttpStatus.NOT_FOUND, "El recurso solicitado no existe",
            HttpStatus.METHOD_NOT_ALLOWED, "El método HTTP no está permitido para este recurso",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "El tipo de contenido no es compatible; envíe la solicitud como application/json");

    private static final Comparator<DetalleError> ORDEN_DETALLES =
            Comparator.comparing(DetalleError::campo).thenComparing(DetalleError::mensaje);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), solicitud, List.of());
    }

    @ExceptionHandler(DatoDuplicadoException.class)
    public ResponseEntity<ErrorRespuesta> manejarDatoDuplicado(
            DatoDuplicadoException ex, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), solicitud, List.of());
    }

    /**
     * Respaldo del índice único de MongoDB: cubre la carrera entre dos peticiones
     * que superan a la vez la comprobación del servicio. No se reenvía el mensaje
     * del driver porque revela nombres de colección e índice.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorRespuesta> manejarClaveDuplicada(
            DuplicateKeyException ex, HttpServletRequest solicitud) {
        LOG.warn("MongoDB rechazó una clave duplicada en {}: {}", solicitud.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.CONFLICT, MENSAJE_CLAVE_DUPLICADA, solicitud, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest solicitud) {
        List<DetalleError> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new DetalleError(error.getField(), error.getDefaultMessage()))
                .sorted(ORDEN_DETALLES)
                .toList();
        return responder(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION, solicitud, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarJsonIlegible(
            HttpMessageNotReadableException ex, HttpServletRequest solicitud) {
        LOG.debug("Cuerpo ilegible en {}: {}", solicitud.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, MENSAJE_JSON_ILEGIBLE, solicitud, List.of());
    }

    /**
     * Último recurso. Los errores que Spring MVC ya clasifica (405, 415, ruta
     * inexistente...) conservan su código; cualquier otro fallo se registra en el
     * log del servidor y el cliente recibe un 500 genérico, sin traza.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorNoControlado(Exception ex, HttpServletRequest solicitud) {
        if (ex instanceof ErrorResponse errorMvc) {
            HttpStatus estado = HttpStatus.valueOf(errorMvc.getStatusCode().value());
            String mensaje = MENSAJES_HTTP.getOrDefault(estado, MENSAJE_SOLICITUD_NO_PROCESABLE);
            return responder(estado, errorMvc.getHeaders(), mensaje, solicitud, List.of());
        }
        LOG.error("Error no controlado en {} {}", solicitud.getMethod(), solicitud.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_INTERNO, solicitud, List.of());
    }

    private static ResponseEntity<ErrorRespuesta> responder(
            HttpStatus estado, String mensaje, HttpServletRequest solicitud, List<DetalleError> detalles) {
        return responder(estado, HttpHeaders.EMPTY, mensaje, solicitud, detalles);
    }

    private static ResponseEntity<ErrorRespuesta> responder(
            HttpStatus estado,
            HttpHeaders cabeceras,
            String mensaje,
            HttpServletRequest solicitud,
            List<DetalleError> detalles) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                Instant.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                solicitud.getRequestURI(),
                detalles);
        return ResponseEntity.status(estado).headers(cabeceras).body(cuerpo);
    }
}
