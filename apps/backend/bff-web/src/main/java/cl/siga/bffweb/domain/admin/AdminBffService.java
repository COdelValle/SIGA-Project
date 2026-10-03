package cl.siga.bffweb.domain.admin;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.admin.dto.AsignaturaAdminDTO;
import cl.siga.bffweb.domain.admin.dto.UsuarioAdminDTO;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import lombok.RequiredArgsConstructor;

/** Datos de gestion institucional (usuarios y asignaturas) para el portal admin. */
@Service
@RequiredArgsConstructor
public class AdminBffService {
    private static final int PAGE_SIZE = 200;

    private final UsuarioClient usuarioClient;
    private final AsignaturaClient asignaturaClient;

    public List<UsuarioAdminDTO> getUsuarios(String email, Rol rol, StateUsuario state) {
        return contentOf(usuarioClient.searchUsuarios(email, rol, state, PAGE_SIZE)).stream()
            .map(usuario -> new UsuarioAdminDTO(
                usuario.id(),
                usuario.email(),
                usuario.email(),
                usuario.rol() == null ? null : usuario.rol().name(),
                usuario.state() == null ? null : usuario.state().name()))
            .toList();
    }

    public List<AsignaturaAdminDTO> getAsignaturas() {
        return contentOf(asignaturaClient.searchAsignaturas(PAGE_SIZE)).stream()
            .map(asignatura -> new AsignaturaAdminDTO(
                asignatura.id(),
                asignatura.name(),
                asignatura.description(),
                true))
            .toList();
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }
}
