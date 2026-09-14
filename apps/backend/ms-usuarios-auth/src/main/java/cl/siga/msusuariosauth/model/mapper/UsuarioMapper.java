package cl.siga.msusuariosauth.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.msusuariosauth.model.entity.Usuario;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UsuarioMapper {
    // Mapea un DTO de solicitud a una entidad de usuario.
    Usuario toEntity(RegistrarUsuarioRequestDTO requestDto);

    // Mapea un DTO de actualización a una entidad de usuario.
    void updateEntityFromDto(ActualizarUsuarioRequestDTO dto, @MappingTarget Usuario usuario);

    // Mapea una entidad de usuario a un DTO de respuesta.
    UsuarioResponseDTO toResponseDto(Usuario usuario);

    List<UsuarioResponseDTO> toResponseDtoList(List<Usuario> usuarios);
}
