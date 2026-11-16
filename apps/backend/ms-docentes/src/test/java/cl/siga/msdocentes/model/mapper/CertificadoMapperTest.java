package cl.siga.msdocentes.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.msdocentes.model.entity.Certificado;

class CertificadoMapperTest {

    private final CertificadoMapper mapper = new CertificadoMapperImpl();

    private Certificado certificado() {
        return Certificado.builder()
                .id(1L)
                .nombre("Titulo")
                .institucionRealizacion("Instituto")
                .fechaTitulacion(LocalDate.of(2019, 1, 1))
                .build();
    }

    @Test
    void toEntityMapeaLosCampos() {
        Certificado entity = mapper.toEntity(new CertificadoRequestDTO(
                "Magister", "Universidad", LocalDate.of(2021, 2, 3)));

        assertEquals("Magister", entity.getNombre());
        assertEquals("Universidad", entity.getInstitucionRealizacion());
        assertEquals(LocalDate.of(2021, 2, 3), entity.getFechaTitulacion());
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDtoIgnoraNulosYActualizaValores() {
        Certificado entity = certificado();
        mapper.updateEntityFromDto(new CertificadoRequestDTO(null, null, null), entity);
        assertEquals("Titulo", entity.getNombre());

        mapper.updateEntityFromDto(new CertificadoRequestDTO(
                "Magister", "Universidad", LocalDate.of(2021, 2, 3)), entity);
        assertEquals("Magister", entity.getNombre());
        assertEquals("Universidad", entity.getInstitucionRealizacion());
        assertEquals(LocalDate.of(2021, 2, 3), entity.getFechaTitulacion());

        mapper.updateEntityFromDto(null, entity);
        assertEquals("Magister", entity.getNombre());
    }

    @Test
    void toResponseDtoMapeaLosCampos() {
        CertificadoResponseDTO dto = mapper.toResponseDto(certificado());

        assertEquals(1L, dto.id());
        assertEquals("Titulo", dto.nombre());
        assertEquals("Instituto", dto.institucionRealizacion());
        assertEquals(LocalDate.of(2019, 1, 1), dto.fechaTitulacion());
        assertNull(mapper.toResponseDto(null));
    }

    @Test
    void toResponseDtoListMapeaLaLista() {
        assertEquals(1, mapper.toResponseDtoList(List.of(certificado())).size());
        assertNull(mapper.toResponseDtoList(null));
    }
}
