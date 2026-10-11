package cl.siga.msdocentes.model.mapper;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.msdocentes.model.entity.Certificado;
import org.mapstruct.*;

import java.util.List;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CertificadoMapper {
  // Mapea un DTO de solicitud a una entidad de certificado.
  Certificado toEntity(CertificadoRequestDTO dto);

  // Mapea un DTO de modificación a una entidad de certificado.
  void updateEntityFromDto(CertificadoRequestDTO dto, @MappingTarget Certificado certificado);

  // Mapea una entidad de certificado a un DTO de respuesta.
  CertificadoResponseDTO toResponseDto(Certificado certificado);

  // Mapea una lista de entidades de certificado a una lista de DTOs de respuesta.
  List<CertificadoResponseDTO> toResponseDtoList(List<Certificado> certificados);
}
