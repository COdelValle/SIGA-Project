package cl.siga.msusuariosauth.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.CandidatoUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.InvitacionLoteResponseDTO;
import cl.siga.coreshare.dto.usuario.InvitacionUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.model.mapper.UsuarioMapper;
import cl.siga.msusuariosauth.model.specification.UsuarioSpecifications;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UsuarioService {
    private static final int MAX_INVITACIONES_LOTE = 2000;
    private static final long MAX_CSV_BYTES = 2L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;

    private final InvitacionUsuarioRepository invitacionRepository;

    private final UsuarioMapper mapper;

    private final GraphUserDirectory graphDirectory;

    private final InvitacionVinculacionService vinculacionService;

    private final Validator validator;

    @Transactional (readOnly = true)
    public UsuarioResponseDTO getUsuarioById(String id) {
        return mapper.toResponseDto(usuarioRepository.findById(id)
                .filter(usuario -> usuario.getState() != StateUsuario.INACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado.")));
    }

    /**
     * Usuario autenticado según el JWT (claim {@code oid}). Es la base de
     * {@code GET /api/v1/usuarios/me} que consume el BFF. Si el oid aún no
     * tiene fila, se intenta vincular una invitación por el correo del token;
     * sin invitación se responde 404 (acceso cerrado).
     */
    @Transactional (readOnly = true)
    public UsuarioResponseDTO getCurrentUsuario() {
        String oid = SecurityUtils.getCurrentUserOid()
                .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
        Optional<Usuario> existente = usuarioRepository.findById(oid)
                .filter(usuario -> usuario.getState() != StateUsuario.INACTIVO);
        if (existente.isPresent()) {
            return mapper.toResponseDto(existente.get());
        }
        String email = SecurityUtils.getCurrentUserEmail()
                .map(UsuarioService::normalizarEmail)
                .orElse(null);
        try {
            return vinculacionService.vincular(oid, email)
                    .map(mapper::toResponseDto)
                    .orElseThrow(() -> noRegistrado(oid));
        } catch (DataIntegrityViolationException ex) {
            // Choque de dos primeros logins simultáneos: otra transacción creó la fila.
            log.warn("Vinculación concurrente para {}; se reutiliza la fila existente.", oid);
            return usuarioRepository.findById(oid)
                    .filter(usuario -> usuario.getState() != StateUsuario.INACTIVO)
                    .map(mapper::toResponseDto)
                    .orElseThrow(() -> noRegistrado(oid));
        }
    }

    @Transactional (readOnly = true)
    public Page<UsuarioResponseDTO> searchUsuarios(@Valid @Email String email, Rol rol, StateUsuario state, Pageable pageable) {
        Specification<Usuario> spec = (root, query, cb) -> cb.notEqual(root.get("state"), StateUsuario.INACTIVO);

        if (email != null && !email.isBlank()) {
            spec = spec.and(UsuarioSpecifications.hasEmail(email));
        }
        if (rol != null) {
            spec = spec.and(UsuarioSpecifications.hasRol(rol));
        }
        if (state != null) {
            spec = spec.and(UsuarioSpecifications.hasState(state));
        }

        return usuarioRepository.findAll(spec, pageable).map(mapper::toResponseDto);
    }

    /** Busca una cuenta en Entra ID por correo para pre-registrarla. */
    @Transactional (readOnly = true)
    public CandidatoUsuarioResponseDTO lookupCandidato(@Valid @Email String email) {
        return graphDirectory.findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró en Entra ID una cuenta para el correo " + email + "."));
    }

    /**
     * Alta de un usuario. Si se resuelve el {@code oid} (entregado o vía Graph)
     * se registra ACTIVO; si no, se crea una invitación por correo que se
     * vincula sola en el primer inicio de sesión (state INVITADO en la respuesta).
     */
    @Transactional
    public RegistrarUsuarioResponseDTO saveUsuario(@Valid RegistrarUsuarioRequestDTO request) {
        String emailFormatted = normalizarEmail(request.email());
        String oid = (request.id() != null && !request.id().isBlank())
                ? request.id().trim()
                : resolverOidDesdeGraph(emailFormatted);

        if (oid == null) {
            crearInvitacionIndividual(emailFormatted, request.rol());
            log.info("Invitación registrada para {} (rol {}); se vinculará en el primer login.",
                    emailFormatted, request.rol());
            return new RegistrarUsuarioResponseDTO(null, emailFormatted, request.rol(), StateUsuario.INVITADO);
        }

        if (usuarioRepository.existsById(oid)) {
            throw new BusinessException("El usuario con ID de Azure " + oid + " ya está registrado.");
        }
        if (usuarioRepository.existsByEmail(emailFormatted)) {
            throw new BusinessException("El correo electrónico ya está registrado.");
        }

        Usuario usuario = mapper.toEntity(request);
        usuario.setId(oid);
        usuario.setEmail(emailFormatted);
        usuario.setState(StateUsuario.ACTIVO);

        Usuario saved = usuarioRepository.save(usuario);
        marcarInvitacionVinculada(emailFormatted);
        syncRolesIfEnabledAfterCommit(saved);
        return new RegistrarUsuarioResponseDTO(saved.getId(), saved.getEmail(), saved.getRol(), saved.getState());
    }

    /**
     * Carga masiva de invitaciones. Las filas inválidas o duplicadas no
     * bloquean al resto del lote y se informan en el resumen.
     */
    @Transactional
    public InvitacionLoteResponseDTO invitarLote(List<InvitacionUsuarioRequestDTO> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty()) {
            throw new BusinessException("El lote no contiene invitaciones.");
        }
        if (solicitudes.size() > MAX_INVITACIONES_LOTE) {
            throw new BusinessException(
                    "El lote supera el máximo de " + MAX_INVITACIONES_LOTE + " invitaciones.");
        }

        String invitedBy = SecurityUtils.getCurrentUserOid().orElse(null);
        int creadas = 0;
        int duplicadas = 0;
        List<InvitacionLoteResponseDTO.Invalida> invalidas = new ArrayList<>();
        Set<String> emailsDelLote = new HashSet<>();

        for (InvitacionUsuarioRequestDTO solicitud : solicitudes) {
            String email = solicitud == null ? "" : normalizarEmail(solicitud.email());
            String motivo = motivoInvalidez(solicitud, email);
            if (motivo != null) {
                invalidas.add(new InvitacionLoteResponseDTO.Invalida(email, motivo));
                continue;
            }
            if (!emailsDelLote.add(email)) {
                duplicadas++;
                continue;
            }
            if (usuarioRepository.existsByEmail(email) || invitacionRepository.existsByEmail(email)) {
                duplicadas++;
                continue;
            }
            invitacionRepository.save(InvitacionUsuario.builder()
                    .email(email)
                    .rol(solicitud.rol())
                    .state(StateInvitacion.INVITADO)
                    .invitedBy(invitedBy)
                    .build());
            creadas++;
        }

        log.info("Lote de invitaciones: {} creadas, {} duplicadas, {} inválidas.",
                creadas, duplicadas, invalidas.size());
        return new InvitacionLoteResponseDTO(creadas, duplicadas, invalidas);
    }

    /** Carga masiva desde un CSV con encabezado opcional {@code email,rol}. */
    @Transactional
    public InvitacionLoteResponseDTO invitarLoteCsv(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("El archivo CSV está vacío.");
        }
        if (archivo.getSize() > MAX_CSV_BYTES) {
            throw new BusinessException("El archivo CSV supera el máximo de 2 MB.");
        }
        return invitarLote(leerCsv(archivo));
    }

    @Transactional
    public UsuarioResponseDTO updateUsuario(String id, @Valid ActualizarUsuarioRequestDTO request) {
        Usuario existingUsuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));

        if (request.state() == StateUsuario.INACTIVO
                && SecurityUtils.getCurrentUserOid().map(id::equals).orElse(false)) {
            throw new BusinessException("No puedes desactivar tu propia cuenta.");
        }

        String newEmailFormatted = request.email().trim().toLowerCase();

        // Validar si cambió el correo y si el nuevo correo pertenece a otro usuario
        if (!existingUsuario.getEmail().equalsIgnoreCase(newEmailFormatted)) {
            if (usuarioRepository.existsByEmail(newEmailFormatted)) {
                throw new BusinessException("El correo electrónico " + request.email() + " ya está en uso.");
            }
        }

        Rol previousRol = existingUsuario.getRol();
        StateUsuario previousState = existingUsuario.getState();
        mapper.updateEntityFromDto(request, existingUsuario);
        if (request.rol() != null) {
            existingUsuario.setRol(request.rol());
        }

        Usuario saved = usuarioRepository.save(existingUsuario);
        boolean rolChanged = previousRol != saved.getRol();
        boolean stateChanged = previousState != saved.getState();
        if (rolChanged || stateChanged) {
            if (rolChanged) {
                log.info("Rol del usuario {} cambió de {} a {}; sincronizando con Entra ID.",
                        saved.getId(), previousRol, saved.getRol());
            }
            syncRolesIfEnabledAfterCommit(saved);
        }
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public void deleteUsuario(String id) {
        Usuario existingUsuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));
        existingUsuario.setState(StateUsuario.INACTIVO);
        usuarioRepository.save(existingUsuario);

        revokeRolesAfterCommit(existingUsuario.getId());
    }

    /**
     * Fuerza la reconciliación del rol local hacia Entra ID. Útil para el
     * bootstrap del primer admin o si una sincronización previa falló.
     */
    @Transactional
    public void syncUserRoles(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado."));
        if (!graphDirectory.isEnabled()) {
            throw new BusinessException(
                    "La integración con Microsoft Graph no está configurada (falta AZURE_CLIENT_SECRET).");
        }
        syncRolesIfEnabled(usuario);
    }

    private ResourceNotFoundException noRegistrado(String oid) {
        return new ResourceNotFoundException("Usuario con ID " + oid + " no encontrado.");
    }

    private String resolverOidDesdeGraph(String email) {
        if (!graphDirectory.isEnabled()) {
            return null;
        }
        return graphDirectory.findUserByEmail(email)
                .map(CandidatoUsuarioResponseDTO::oid)
                .orElse(null);
    }

    private void crearInvitacionIndividual(String email, Rol rol) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("El correo electrónico ya está registrado.");
        }
        if (invitacionRepository.existsByEmail(email)) {
            throw new BusinessException("Ya existe una invitación para el correo " + email + ".");
        }
        invitacionRepository.save(InvitacionUsuario.builder()
                .email(email)
                .rol(rol)
                .state(StateInvitacion.INVITADO)
                .invitedBy(SecurityUtils.getCurrentUserOid().orElse(null))
                .build());
    }

    private void marcarInvitacionVinculada(String email) {
        invitacionRepository.findByEmail(email)
                .filter(inv -> inv.getState() == StateInvitacion.INVITADO)
                .ifPresent(inv -> {
                    inv.setState(StateInvitacion.VINCULADA);
                    inv.setBoundAt(LocalDateTime.now());
                    invitacionRepository.save(inv);
                });
    }

    private String motivoInvalidez(InvitacionUsuarioRequestDTO solicitud, String email) {
        if (solicitud == null) {
            return "Fila vacía o con formato inválido.";
        }
        InvitacionUsuarioRequestDTO normalizada = new InvitacionUsuarioRequestDTO(email, solicitud.rol());
        Set<ConstraintViolation<InvitacionUsuarioRequestDTO>> violations = validator.validate(normalizada);
        if (!violations.isEmpty()) {
            return violations.iterator().next().getMessage();
        }
        if (email == null || email.isBlank()) {
            return "El correo electrónico es obligatorio.";
        }
        return null;
    }

    private List<InvitacionUsuarioRequestDTO> leerCsv(MultipartFile archivo) {
        List<InvitacionUsuarioRequestDTO> solicitudes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            boolean encabezado = true;
            while ((linea = reader.readLine()) != null) {
                String limpia = linea.replace("\uFEFF", "").trim();
                if (limpia.isEmpty()) {
                    continue;
                }
                if (encabezado) {
                    encabezado = false;
                    if (limpia.toLowerCase(Locale.ROOT).contains("email")) {
                        continue;
                    }
                }
                String[] partes = limpia.split("[;,]", 2);
                String email = partes.length > 0 ? partes[0].trim() : "";
                String rol = partes.length > 1 ? partes[1].trim() : "";
                solicitudes.add(new InvitacionUsuarioRequestDTO(email, parseRol(rol)));
            }
        } catch (IOException ex) {
            throw new BusinessException("No se pudo leer el archivo CSV: " + ex.getMessage());
        }
        return solicitudes;
    }

    private Rol parseRol(String valor) {
        try {
            return Rol.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Refleja en Entra ID el rol/estado local. La BD es la fuente autoritativa;
     * si Graph no está configurado (sin AZURE_CLIENT_SECRET) se omite sin romper
     * el CRUD.
     */
    private void syncRolesIfEnabled(Usuario usuario) {
        if (!graphDirectory.isEnabled()) {
            log.debug("Graph no configurado: se omite la sincronización de roles para {}", usuario.getId());
            return;
        }
        if (usuario.getState() == StateUsuario.INACTIVO) {
            graphDirectory.revokeRoles(usuario.getId());
        } else {
            graphDirectory.syncRole(usuario.getId(), usuario.getRol());
        }
    }

    /**
     * Ejecuta la sincronizacion con Graph **despues** del commit para no hacer
     * llamadas externas dentro de la transaccion; si Graph falla, se registra y
     * no se revierte la operacion local.
     */
    private void syncRolesIfEnabledAfterCommit(Usuario usuario) {
        if (!graphDirectory.isEnabled()) {
            log.debug("Graph no configurado: se omite la sincronización de roles para {}", usuario.getId());
            return;
        }
        runAfterCommit(() -> syncRolesIfEnabled(usuario),
                "sincronizar roles", usuario.getId());
    }

    private void revokeRolesAfterCommit(String id) {
        if (!graphDirectory.isEnabled()) {
            return;
        }
        runAfterCommit(() -> graphDirectory.revokeRoles(id), "revocar roles", id);
    }

    private void runAfterCommit(Runnable action, String operacion, String id) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException ex) {
                        log.error("Fallo al {} en Entra ID para {}: {}", operacion, id, ex.getMessage());
                    }
                }
            });
        } else {
            action.run();
        }
    }
}
