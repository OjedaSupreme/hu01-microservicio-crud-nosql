package com.hu01.estudiantes.servicio;

import com.hu01.estudiantes.dto.EstudianteRequest;
import com.hu01.estudiantes.dto.EstudianteResponse;
import com.hu01.estudiantes.excepcion.DatoDuplicadoException;
import com.hu01.estudiantes.excepcion.RecursoNoEncontradoException;
import com.hu01.estudiantes.modelo.Estudiante;
import com.hu01.estudiantes.repositorio.EstudianteRepositorio;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Reglas de negocio del CRUD de estudiantes.
 */
@Service
public class EstudianteServicio {

    private final EstudianteRepositorio repositorio;

    public EstudianteServicio(EstudianteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public EstudianteResponse crear(EstudianteRequest solicitud) {
        String correo = normalizarCorreo(solicitud.correo());
        if (repositorio.existsByCorreo(correo)) {
            throw new DatoDuplicadoException(mensajeCorreoDuplicado(correo));
        }
        Estudiante nuevo = new Estudiante(
                solicitud.nombre().trim(),
                solicitud.apellido().trim(),
                solicitud.telefono().trim(),
                correo);
        return EstudianteResponse.desde(repositorio.save(nuevo));
    }

    public List<EstudianteResponse> listar() {
        return repositorio.findAll().stream()
                .map(EstudianteResponse::desde)
                .toList();
    }

    public EstudianteResponse buscarPorId(String id) {
        return EstudianteResponse.desde(obtenerOFallar(id));
    }

    public EstudianteResponse actualizar(String id, EstudianteRequest solicitud) {
        Estudiante existente = obtenerOFallar(id);
        String correo = normalizarCorreo(solicitud.correo());
        // Solo se valida la unicidad si el correo cambia: conservar el propio no es un duplicado.
        if (!correo.equals(existente.getCorreo()) && repositorio.existsByCorreo(correo)) {
            throw new DatoDuplicadoException(mensajeCorreoDuplicado(correo));
        }
        existente.setNombre(solicitud.nombre().trim());
        existente.setApellido(solicitud.apellido().trim());
        existente.setTelefono(solicitud.telefono().trim());
        existente.setCorreo(correo);
        return EstudianteResponse.desde(repositorio.save(existente));
    }

    public void eliminar(String id) {
        if (!repositorio.existsById(id)) {
            throw new RecursoNoEncontradoException(mensajeNoEncontrado(id));
        }
        repositorio.deleteById(id);
    }

    private Estudiante obtenerOFallar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(mensajeNoEncontrado(id)));
    }

    private static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private static String mensajeNoEncontrado(String id) {
        return "No existe un estudiante con el id '" + id + "'";
    }

    private static String mensajeCorreoDuplicado(String correo) {
        return "Ya existe un estudiante registrado con el correo '" + correo + "'";
    }
}
