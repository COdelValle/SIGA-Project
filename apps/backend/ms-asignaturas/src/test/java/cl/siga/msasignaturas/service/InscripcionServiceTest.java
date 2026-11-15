package cl.siga.msasignaturas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.inscripcion.ActualizarEstadoInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.client.EstudianteClient;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.InscripcionMapper;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;

class InscripcionServiceTest {

    private InscripcionRepository inscripcionRepository;
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    private InscripcionMapper inscripcionMapper;
    private EstudianteClient estudianteClient;
    private InscripcionService service;

    @BeforeEach
    void setUp() {
        inscripcionRepository = mock(InscripcionRepository.class);
        cursoAsignaturaRepository = mock(CursoAsignaturaRepository.class);
        inscripcionMapper = mock(InscripcionMapper.class);
        estudianteClient = mock(EstudianteClient.class);
        service = new InscripcionService(
                inscripcionRepository,
                cursoAsignaturaRepository,
                inscripcionMapper,
                estudianteClient);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private CursoAsignatura dictacion(Long id, CaracterAsignatura caracter, Integer cupo) {
        CursoAsignatura curso = new CursoAsignatura();
        curso.setId(id);
        curso.setCaracter(caracter);
        curso.setCupoMaximo(cupo);
        curso.setActive(true);
        return curso;
    }

    @Test
    void rechazaInscripcionEnAsignaturaObligatoria() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(1L))
                .thenReturn(Optional.of(dictacion(1L, CaracterAsignatura.OBLIGATORIA, null)));

        assertThrows(BusinessException.class,
                () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 1L)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void rechazaInscripcionSinCupos() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
                .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.ELECTIVA, 1)));
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(1);

        assertThrows(BusinessException.class,
                () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L)));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void registraInscripcionCuandoHayCupo() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
                .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.ELECTIVA, 2)));
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L)).thenReturn(Optional.empty());
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);

        Inscripcion nueva = new Inscripcion();
        when(inscripcionMapper.toEntity(any())).thenReturn(nueva);
        when(inscripcionRepository.save(nueva)).thenReturn(nueva);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, nueva.getEstado());
        assertEquals(5L, nueva.getCursoAsignatura().getId());
        verify(inscripcionRepository).save(nueva);
    }

    @Test
    void reactivaInscripcionCancelada() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
                .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.OPTATIVA, 2)));
        Inscripcion cancelada = new Inscripcion();
        cancelada.setId(9L);
        cancelada.setEstado(EstadoInscripcion.CANCELADO);
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L)).thenReturn(Optional.of(cancelada));
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);
        when(inscripcionRepository.save(cancelada)).thenReturn(cancelada);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, cancelada.getEstado());
    }

    @Test
    void buscarInscripcionesAplicaSpecsYMapeaLaPagina() {
        Pageable pageable = PageRequest.of(0, 10);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(1L);
        InscripcionResponseDTO dto = new InscripcionResponseDTO(
            1L, 1L, 5L, EstadoInscripcion.ACTIVO, null);
        when(inscripcionRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(inscripcion), pageable, 1));
        when(inscripcionMapper.toDto(inscripcion)).thenReturn(dto);

        var pagina = service.buscarInscripciones(1L, 5L, List.of(EstadoInscripcion.ACTIVO), pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(dto, pagina.getContent().get(0));
    }

    @Test
    void obtenerPorIdDevuelveLaInscripcionMapeada() {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(1L);
        InscripcionResponseDTO dto = new InscripcionResponseDTO(
            1L, 1L, 5L, EstadoInscripcion.ACTIVO, null);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionMapper.toDto(inscripcion)).thenReturn(dto);

        assertEquals(dto, service.obtenerPorId(1L));
    }

    @Test
    void obtenerPorIdFallaCuandoNoExiste() {
        when(inscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
            () -> service.obtenerPorId(99L));

        assertTrue(error.getMessage().contains("99"));
    }

    @Test
    void rechazaReinscripcionConEstadoQueOcupaCupo() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
            .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.ELECTIVA, 10)));
        Inscripcion activa = new Inscripcion();
        activa.setEstado(EstadoInscripcion.ACTIVO);
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(1L, 5L))
            .thenReturn(Optional.of(activa));

        assertThrows(BusinessException.class,
            () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(1L, 5L)));
    }

    @Test
    void actualizarEstadoQueOcupaCupoValidaDisponibilidad() {
        Inscripcion cancelada = new Inscripcion();
        cancelada.setId(9L);
        cancelada.setEstado(EstadoInscripcion.CANCELADO);
        CursoAsignatura curso = dictacion(5L, CaracterAsignatura.ELECTIVA, 2);
        cancelada.setCursoAsignatura(curso);
        when(inscripcionRepository.findById(9L)).thenReturn(Optional.of(cancelada));
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.of(curso));
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(5L), anyList())).thenReturn(0);
        when(inscripcionRepository.save(cancelada)).thenReturn(cancelada);

        service.actualizarEstado(9L, new ActualizarEstadoInscripcionRequestDTO(EstadoInscripcion.PRE_INSCRITO));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, cancelada.getEstado());
    }

    @Test
    void actualizarEstadoQueOcupaCupoFallaSiLaDictacionEstaInactiva() {
        Inscripcion cancelada = new Inscripcion();
        cancelada.setId(9L);
        cancelada.setEstado(EstadoInscripcion.CANCELADO);
        cancelada.setCursoAsignatura(dictacion(5L, CaracterAsignatura.ELECTIVA, 2));
        when(inscripcionRepository.findById(9L)).thenReturn(Optional.of(cancelada));
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.actualizarEstado(9L,
                new ActualizarEstadoInscripcionRequestDTO(EstadoInscripcion.ACTIVO)));
    }

    @Test
    void actualizarEstadoQueLiberaCupoNoRevalidaDisponibilidad() {
        Inscripcion activa = new Inscripcion();
        activa.setId(9L);
        activa.setEstado(EstadoInscripcion.ACTIVO);
        when(inscripcionRepository.findById(9L)).thenReturn(Optional.of(activa));
        when(inscripcionRepository.save(activa)).thenReturn(activa);

        service.actualizarEstado(9L, new ActualizarEstadoInscripcionRequestDTO(EstadoInscripcion.CANCELADO));

        assertEquals(EstadoInscripcion.CANCELADO, activa.getEstado());
        verify(cursoAsignaturaRepository, never()).findByIdAndActiveTrueForUpdate(any());
    }

    @Test
    void actualizarEstadoFallaCuandoLaInscripcionNoExiste() {
        when(inscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.actualizarEstado(99L,
                new ActualizarEstadoInscripcionRequestDTO(EstadoInscripcion.ACTIVO)));
    }

    @Test
    void adminNoPuedeInscribirAUnEstudianteInexistente() {
        autenticar("oid-admin", "ADMIN", null);
        when(estudianteClient.existsById(10L)).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
            () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(10L, 5L)));

        assertTrue(error.getMessage().contains("no existe"));
        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void adminPuedeInscribirAUnEstudianteExistente() {
        autenticar("oid-admin", "ADMIN", null);
        when(estudianteClient.existsById(10L)).thenReturn(true);
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(10L, 5L)));
    }

    @Test
    void usuarioSinRolEstudianteNoSeValidaContraElCliente() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(10L, 5L)));

        verify(estudianteClient, never()).existsById(any());
    }

    @Test
    void estudianteSinOidEnElTokenNoPuedeInscribirse() {
        autenticar(null, "ESTUDIANTE", null);

        assertThrows(AccessDeniedException.class,
            () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(10L, 5L)));
    }

    @Test
    void estudianteSoloPuedeInscribirseASiMismo() {
        autenticar("oid-camila", "ESTUDIANTE", null);
        when(estudianteClient.getEstudianteByIdUsuario("oid-camila")).thenReturn(new EstudianteResponseDTO(
            11L, "oid-camila", "24112345-6", "CAMILA", null, "SOTO", null,
            null, null, "REGISTRADO", null));

        AccessDeniedException error = assertThrows(AccessDeniedException.class,
            () -> service.registrarInscripcion(new RegistrarInscripcionRequestDTO(10L, 5L)));

        assertTrue(error.getMessage().contains("ti mismo"));
    }

    @Test
    void estudiantePuedeInscribirseASiMismo() {
        autenticar("oid-camila", "ESTUDIANTE", null);
        when(estudianteClient.getEstudianteByIdUsuario("oid-camila")).thenReturn(new EstudianteResponseDTO(
            10L, "oid-camila", "24112345-6", "CAMILA", null, "SOTO", null,
            null, null, "REGISTRADO", null));
        when(cursoAsignaturaRepository.findByIdAndActiveTrueForUpdate(5L))
            .thenReturn(Optional.of(dictacion(5L, CaracterAsignatura.ELECTIVA, null)));
        when(inscripcionRepository.findByIdAlumnoAndCursoAsignaturaId(10L, 5L)).thenReturn(Optional.empty());
        Inscripcion nueva = new Inscripcion();
        when(inscripcionMapper.toEntity(any())).thenReturn(nueva);
        when(inscripcionRepository.save(nueva)).thenReturn(nueva);

        service.registrarInscripcion(new RegistrarInscripcionRequestDTO(10L, 5L));

        assertEquals(EstadoInscripcion.PRE_INSCRITO, nueva.getEstado());
    }

    private static void autenticar(String oid, String rol, String nombre) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none");
        if (oid != null) {
            builder.claim("oid", oid);
        } else {
            builder.claim("sub", "sin-oid");
        }
        if (nombre != null) {
            builder.claim("name", nombre);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            builder.build(), List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
