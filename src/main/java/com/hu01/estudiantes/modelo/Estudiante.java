package com.hu01.estudiantes.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB que representa a un estudiante del colegio.
 *
 * <p>El correo se guarda siempre normalizado (sin espacios y en minúsculas) y
 * tiene un índice único: es la última barrera contra duplicados cuando dos
 * peticiones concurrentes superan la comprobación previa del servicio.</p>
 */
@Document(collection = "estudiantes")
public class Estudiante {

    @Id
    private String id;

    private String nombre;

    private String apellido;

    private String telefono;

    @Indexed(unique = true)
    private String correo;

    public Estudiante() {
        // Requerido por Spring Data para materializar el documento.
    }

    public Estudiante(String nombre, String apellido, String telefono, String correo) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.correo = correo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
