package cl.siga.msdocentes.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import cl.siga.coreshare.dto.docente.ActualizarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msdocentes.model.entity.Certificado;
import cl.siga.msdocentes.model.entity.Docente;

class DocenteMapperTest {

    private DocenteMapper mapper;

    @BeforeEach
    void setUp() {
        DocenteMapperImpl impl = new DocenteMapperImpl();
        ReflectionTestUtils.setField(impl, "certificadoMapper", new CertificadoMapperImpl());
        mapper = impl;
    }

    private RegistrarDocenteRequestDTO request() {
        return new RegistrarDocenteRequestDTO("oid-1", "Ana", "Maria", "Perez", "Soto",
                "21.000.002-K", LocalDate.of(2020, 3, 1), AreaAcademica.MATEMATICAS,
                List.of(new CertificadoRequestDTO("Titulo", "Instituto", LocalDate.of(2019, 1, 1))));
    }

    private Docente docente() {
        Docente docente = Docente.builder()
                .id(9L)
                .idUsuario("oid-1")
                .rut("21000002-K")
                .firstName("Ana")
                .middleName("Maria")
                .firstSurname("Perez")
                .secondSurname("Soto")
                .fechaContratacion(LocalDate.of(2020, 3, 1))
                .area(AreaAcademica.MATEMATICAS)
                .activo(true)
                .certificados(new ArrayList<>(List.of(Certificado.builder()
                        .id(1L).nombre("Titulo").institucionRealizacion("Instituto")
                        .fechaTitulacion(LocalDate.of(2019, 1, 1)).build())))
                .build();
        docente.getCertificados().forEach(certificado -> certificado.setDocente(docente));
        return docente;
    }

    @Test
    void toEntityMapeaTodosLosCamposYCertificados() {
        Docente entity = mapper.toEntity(request());

        assertEquals("oid-1", entity.getIdUsuario());
        assertEquals("21.000.002-K", entity.getRut());
        assertEquals("Ana", entity.getFirstName());
        assertEquals("Maria", entity.getMiddleName());
        assertEquals(LocalDate.of(2020, 3, 1), entity.getFechaContratacion());
        assertEquals(AreaAcademica.MATEMATICAS, entity.getArea());
        assertEquals(1, entity.getCertificados().size());
        assertEquals("Titulo", entity.getCertificados().get(0).getNombre());
    }

    @Test
    void toEntityNuloDevuelveNulo() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDtoIgnoraValoresNulos() {
        Docente entity = docente();
        mapper.updateEntityFromDto(new ActualizarDocenteRequestDTO(
                null, null, null, null, null, null), entity);

        assertEquals("Ana", entity.getFirstName());
        assertEquals(AreaAcademica.MATEMATICAS, entity.getArea());
    }

    @Test
    void updateEntityFromDtoConDtoNuloNoCambiaLaEntidad() {
        Docente entity = docente();
        mapper.updateEntityFromDto(null, entity);

        assertEquals("Ana", entity.getFirstName());
    }

    @Test
    void updateEntityFromDtoActualizaCamposYEnlazaCertificados() {
        Docente entity = docente();
        mapper.updateEntityFromDto(new ActualizarDocenteRequestDTO(
                "Ana Maria", "Josefa", "Perez", "Soto",
                LocalDate.of(2021, 4, 2), AreaAcademica.CIENCIAS), entity);

        assertEquals("Ana Maria", entity.getFirstName());
        assertEquals("Josefa", entity.getMiddleName());
        assertEquals(LocalDate.of(2021, 4, 2), entity.getFechaContratacion());
        assertEquals(AreaAcademica.CIENCIAS, entity.getArea());
        assertSame(entity, entity.getCertificados().get(0).getDocente());
    }

    @Test
    void toResponseDtoMapeaCamposYCertificados() {
        DocenteResponseDTO dto = mapper.toResponseDto(docente());

        assertEquals(9L, dto.id());
        assertEquals("oid-1", dto.idUsuario());
        assertEquals("21000002-K", dto.rut());
        assertEquals(AreaAcademica.MATEMATICAS, dto.area());
        assertEquals(Boolean.TRUE, dto.activo());
        assertEquals(1, dto.certificados().size());
    }

    @Test
    void toResponseDtoNuloDevuelveNulo() {
        assertNull(mapper.toResponseDto(null));
    }

    @Test
    void toResponseDtoListMapeaLaLista() {
        assertEquals(1, mapper.toResponseDtoList(List.of(docente())).size());
        assertNull(mapper.toResponseDtoList(null));
    }
}
