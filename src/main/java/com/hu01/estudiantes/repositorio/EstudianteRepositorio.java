package com.hu01.estudiantes.repositorio;

import com.hu01.estudiantes.modelo.Estudiante;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Acceso a la colección {@code estudiantes}. Las consultas se derivan del
 * nombre del método, por lo que los valores viajan siempre como parámetros.
 */
public interface EstudianteRepositorio extends MongoRepository<Estudiante, String> {

    boolean existsByCorreo(String correo);
}
