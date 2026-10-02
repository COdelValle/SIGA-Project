package cl.siga.msdocentes.service;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msdocentes.model.entity.Certificado;
import cl.siga.msdocentes.model.entity.Docente;
import cl.siga.msdocentes.model.mapper.CertificadoMapper;
import cl.siga.msdocentes.repository.CertificadoRepository;
import cl.siga.msdocentes.repository.DocenteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class CertificadoService {
  private final CertificadoRepository certificadoRepository;
  private final DocenteRepository docenteRepository;
  private final CertificadoMapper mapper;

  @Transactional(readOnly = true)
  public List<CertificadoResponseDTO> getCertificadosByDocenteId(Long docenteId) {
    if (!docenteRepository.existsByIdAndActivoTrue(docenteId)) {
      throw new ResourceNotFoundException("Docente con ID " + docenteId + " no encontrado.");
    }
    return mapper.toResponseDtoList(certificadoRepository.findByDocenteId(docenteId));
  }

  @Transactional
  public CertificadoResponseDTO addCertificado(Long docenteId, @Valid CertificadoRequestDTO request) {
    Docente docente = docenteRepository.findByIdAndActivoTrue(docenteId)
      .orElseThrow(() -> new ResourceNotFoundException("Docente con ID " + docenteId + " no encontrado."));

    Certificado certificado = mapper.toEntity(request);
    certificado.setDocente(docente);

    return mapper.toResponseDto(certificadoRepository.save(certificado));
  }

  @Transactional
  public void deleteCertificado(Long docenteId, Long certificadoId) {
    Certificado certificado = certificadoRepository.findById(certificadoId)
      .orElseThrow(() -> new ResourceNotFoundException("Certificado con ID " + certificadoId + " no encontrado."));

    if (!certificado.getDocente().getId().equals(docenteId)) {
      throw new ResourceNotFoundException("El certificado no pertenece al docente indicado.");
    }

    long totalCertificados = certificadoRepository.countByDocenteId(docenteId);
    if (totalCertificados <= 1) {
      throw new BusinessException("No se puede eliminar. El docente debe mantener al menos un certificado registrado.");
    }

    certificadoRepository.delete(certificado);
  }
}
