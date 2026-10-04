package cl.siga.bffweb.domain.admin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.admin.dto.AsignaturaAdminDTO;
import cl.siga.bffweb.domain.admin.dto.UsuarioAdminDTO;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
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
        List<AsignaturaResponseDTO> catalogo = contentOf(asignaturaClient.searchAsignaturas(PAGE_SIZE));
        Map<Long, List<MallaCurricularResponseDTO>> mallaPorAsignatura = asignaturaClient.getMalla(null).stream()
            .collect(Collectors.groupingBy(MallaCurricularResponseDTO::idAsignatura));

        return catalogo.stream()
            .map(asignatura -> new AsignaturaAdminDTO(
                asignatura.id(),
                asignatura.nombre(),
                asignatura.area(),
                asignatura.calificable(),
                nivelesDe(mallaPorAsignatura.getOrDefault(asignatura.id(), List.of())),
                asignatura.activa()))
            .toList();
    }

    /** Filas de la malla curricular, opcionalmente acotadas a un nivel. */
    public List<MallaCurricularResponseDTO> getMalla(Nivel nivel) {
        return asignaturaClient.getMalla(nivel);
    }

    private static List<String> nivelesDe(List<MallaCurricularResponseDTO> filas) {
        return filas.stream()
            .map(MallaCurricularResponseDTO::nivel)
            .distinct()
            .sorted(Comparator.comparingInt(Nivel::ordinal))
            .map(Nivel::getDescripcion)
            .toList();
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }
}
