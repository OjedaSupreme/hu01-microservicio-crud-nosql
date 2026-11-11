package com.hu01.estudiantes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hu01.estudiantes.dto.EstudianteRequest;
import com.hu01.estudiantes.dto.EstudianteResponse;
import com.hu01.estudiantes.excepcion.DatoDuplicadoException;
import com.hu01.estudiantes.excepcion.RecursoNoEncontradoException;
import com.hu01.estudiantes.modelo.Estudiante;
import com.hu01.estudiantes.repositorio.EstudianteRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias del servicio: el repositorio es un mock de Mockito y no se
 * levanta el contexto de Spring.
 */
@ExtendWith(MockitoExtension.class)
class EstudianteServicioTest {

    private static final String ID = "665f1c2ab7e4a93f5c0d1e2f";
    private static final String CORREO = "ana.garcia@colegio.edu";

    @Mock
    private EstudianteRepositorio repositorio;

    @InjectMocks
    private EstudianteServicio servicio;

    @Captor
    private ArgumentCaptor<Estudiante> estudianteGuardado;

    @Test
    @DisplayName("crear: guarda el estudiante con el correo normalizado y devuelve su id")
    void crear_guardaEstudianteNormalizado() {
        EstudianteRequest solicitud =
                new EstudianteRequest("  Ana ", " García ", " +50688887777 ", "  Ana.Garcia@Colegio.EDU ");
        when(repositorio.existsByCorreo(CORREO)).thenReturn(false);
        when(repositorio.save(any(Estudiante.class))).thenAnswer(invocacion -> {
            Estudiante recibido = invocacion.getArgument(0);
            recibido.setId(ID);
            return recibido;
        });

        EstudianteResponse respuesta = servicio.crear(solicitud);

        assertThat(respuesta).isEqualTo(new EstudianteResponse(ID, "Ana", "García", "+50688887777", CORREO));
        verify(repositorio).save(estudianteGuardado.capture());
        assertThat(estudianteGuardado.getValue().getCorreo()).isEqualTo(CORREO);
        assertThat(estudianteGuardado.getValue().getNombre()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("crear: rechaza un correo ya registrado sin guardar nada")
    void crear_conCorreoDuplicado_lanzaDatoDuplicado() {
        EstudianteRequest solicitud = new EstudianteRequest("Ana", "García", "+50688887777", "ANA.GARCIA@colegio.edu");
        when(repositorio.existsByCorreo(CORREO)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(solicitud))
                .isInstanceOf(DatoDuplicadoException.class)
                .hasMessageContaining(CORREO);

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("listar: devuelve todos los estudiantes convertidos a respuesta")
    void listar_devuelveTodos() {
        when(repositorio.findAll()).thenReturn(List.of(
                estudiante(ID, "Ana", "García", CORREO),
                estudiante("665f1c2ab7e4a93f5c0d1e30", "Luis", "Mora", "luis.mora@colegio.edu")));

        List<EstudianteResponse> respuesta = servicio.listar();

        assertThat(respuesta)
                .extracting(EstudianteResponse::id, EstudianteResponse::correo)
                .containsExactly(
                        tuple(ID, CORREO),
                        tuple("665f1c2ab7e4a93f5c0d1e30", "luis.mora@colegio.edu"));
    }

    @Test
    @DisplayName("listar: devuelve una lista vacía cuando no hay estudiantes")
    void listar_sinEstudiantes_devuelveListaVacia() {
        when(repositorio.findAll()).thenReturn(List.of());

        assertThat(servicio.listar()).isEmpty();
    }

    @Test
    @DisplayName("buscarPorId: devuelve el estudiante existente")
    void buscarPorId_existente() {
        when(repositorio.findById(ID)).thenReturn(Optional.of(estudiante(ID, "Ana", "García", CORREO)));

        EstudianteResponse respuesta = servicio.buscarPorId(ID);

        assertThat(respuesta).isEqualTo(new EstudianteResponse(ID, "Ana", "García", "+50688887777", CORREO));
    }

    @Test
    @DisplayName("buscarPorId: lanza RecursoNoEncontradoException si el id no existe")
    void buscarPorId_inexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorId(ID))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ID);
    }

    @Test
    @DisplayName("actualizar: aplica los cambios, incluido un correo nuevo que está libre")
    void actualizar_conCorreoNuevoLibre() {
        EstudianteRequest solicitud =
                new EstudianteRequest("Ana María", "García Solís", "22223333", "Ana.Maria@Colegio.edu");
        when(repositorio.findById(ID)).thenReturn(Optional.of(estudiante(ID, "Ana", "García", CORREO)));
        when(repositorio.existsByCorreo("ana.maria@colegio.edu")).thenReturn(false);
        when(repositorio.save(any(Estudiante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EstudianteResponse respuesta = servicio.actualizar(ID, solicitud);

        assertThat(respuesta).isEqualTo(
                new EstudianteResponse(ID, "Ana María", "García Solís", "22223333", "ana.maria@colegio.edu"));
        verify(repositorio).save(estudianteGuardado.capture());
        assertThat(estudianteGuardado.getValue().getId()).isEqualTo(ID);
    }

    @Test
    @DisplayName("actualizar: conservar el correo propio no se trata como duplicado")
    void actualizar_conservandoCorreo_noConsultaDuplicados() {
        EstudianteRequest solicitud = new EstudianteRequest("Ana María", "García", "22223333", CORREO);
        when(repositorio.findById(ID)).thenReturn(Optional.of(estudiante(ID, "Ana", "García", CORREO)));
        when(repositorio.save(any(Estudiante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EstudianteResponse respuesta = servicio.actualizar(ID, solicitud);

        assertThat(respuesta.nombre()).isEqualTo("Ana María");
        assertThat(respuesta.correo()).isEqualTo(CORREO);
        verify(repositorio, never()).existsByCorreo(anyString());
    }

    @Test
    @DisplayName("actualizar: rechaza el correo que ya pertenece a otro estudiante")
    void actualizar_conCorreoDeOtroEstudiante_lanzaDatoDuplicado() {
        EstudianteRequest solicitud = new EstudianteRequest("Ana", "García", "+50688887777", "luis.mora@colegio.edu");
        when(repositorio.findById(ID)).thenReturn(Optional.of(estudiante(ID, "Ana", "García", CORREO)));
        when(repositorio.existsByCorreo("luis.mora@colegio.edu")).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(ID, solicitud))
                .isInstanceOf(DatoDuplicadoException.class)
                .hasMessageContaining("luis.mora@colegio.edu");

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: lanza RecursoNoEncontradoException si el id no existe")
    void actualizar_inexistente_lanzaRecursoNoEncontrado() {
        EstudianteRequest solicitud = new EstudianteRequest("Ana", "García", "+50688887777", CORREO);
        when(repositorio.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(ID, solicitud))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ID);

        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("eliminar: borra el estudiante existente")
    void eliminar_existente() {
        when(repositorio.existsById(ID)).thenReturn(true);

        servicio.eliminar(ID);

        verify(repositorio).deleteById(ID);
    }

    @Test
    @DisplayName("eliminar: lanza RecursoNoEncontradoException si el id no existe")
    void eliminar_inexistente_lanzaRecursoNoEncontrado() {
        when(repositorio.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(ID))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining(ID);

        verify(repositorio, never()).deleteById(anyString());
    }

    private static Estudiante estudiante(String id, String nombre, String apellido, String correo) {
        Estudiante estudiante = new Estudiante(nombre, apellido, "+50688887777", correo);
        estudiante.setId(id);
        return estudiante;
    }
}
