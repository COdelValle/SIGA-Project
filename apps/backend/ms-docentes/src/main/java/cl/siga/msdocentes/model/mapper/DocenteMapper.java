package cl.siga.msdocentes.model.mapper;

import cl.siga.coreshare.dto.docente.ActualizarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.msdocentes.model.entity.Docente;
import org.mapstruct.*;

import java.util.List;

@Mapper(
  componentModel = "spring",
  uses = { CertificadoMapper.class },
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DocenteMapper {
  // Mapea un DTO de solicitud a una entidad de docente.
  Docente toEntity(RegistrarDocenteRequestDTO requestDto);

  // Mapea un DTO de modificación a una entidad de docente.
  void updateEntityFromDto(ActualizarDocenteRequestDTO dto, @MappingTarget Docente docente);

  // Mapea una entidad de docente a un DTO de respuesta.
  DocenteResponseDTO toResponseDto(Docente docente);

  // Mapea una lista de entidades de docente a una lista de DTOs de respuesta.
  List<DocenteResponseDTO> toResponseDtoList(List<Docente> docentes);

  // Método que se ejecuta después del mapeo para establecer la relación bidireccional entre Docente y Certificado.
  @AfterMapping
  default void linkCertificados(@MappingTarget Docente docente) {
    if (docente.getCertificados() != null) {
      docente.getCertificados().forEach(certificado -> certificado.setDocente(docente));
    }
  }
}
