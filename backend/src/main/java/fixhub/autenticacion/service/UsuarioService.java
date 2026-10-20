package fixhub.autenticacion.service;

import fixhub.autenticacion.dto.CrearUsuarioRequest;
import fixhub.autenticacion.dto.UsuarioResponse;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.exception.IdentificadorUsuarioDuplicadoException;
import fixhub.autenticacion.exception.RolNoEncontradoException;
import fixhub.autenticacion.repository.RolRepository;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.autenticacion.dto.CambiarClaveRequest;
import fixhub.autenticacion.exception.ClaveActualIncorrectaException;
import fixhub.autenticacion.exception.UsuarioNoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        RolRepository rolRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        String email = normalizarEmail(request.email());
        String telefono = normalizarContacto(request.telefono());

        validarIdentificadoresDisponibles(email, telefono);

        Set<Rol> roles = buscarRoles(request.roles());

        String passwordHash =
            passwordEncoder.encode(request.claveTemporal());

        Usuario usuario = new Usuario(
            passwordHash,
            email,
            telefono
        );

        roles.forEach(usuario::agregarRol);

        Usuario usuarioGuardado =
            usuarioRepository.save(usuario);

        return convertirAResponse(usuarioGuardado);
    }

    @Transactional
    public void cambiarClave(
        Long usuarioId,
        CambiarClaveRequest request
    ) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(
                () -> new UsuarioNoEncontradoException(usuarioId)
            );

        boolean claveActualValida = passwordEncoder.matches(
            request.claveActual(),
            usuario.getPasswordHash()
        );

        if (!claveActualValida) {
            throw new ClaveActualIncorrectaException();
        }

        String nuevoPasswordHash =
            passwordEncoder.encode(request.claveNueva());

        usuario.setPasswordHash(nuevoPasswordHash);
        usuario.setRequiereCambioClave(false);

        usuarioRepository.save(usuario);
    }

    private void validarIdentificadoresDisponibles(
        String email,
        String telefono
    ) {
        if (
            email != null
                && usuarioRepository.existsByEmailIgnoreCase(email)
        ) {
            throw new IdentificadorUsuarioDuplicadoException(email);
        }

        if (
            telefono != null
                && usuarioRepository.existsByTelefono(telefono)
        ) {
            throw new IdentificadorUsuarioDuplicadoException(telefono);
        }
    }

    private Set<Rol> buscarRoles(Set<NombreRol> nombresRoles) {
        return nombresRoles.stream()
            .map(this::buscarRol)
            .collect(Collectors.toSet());
    }

    private Rol buscarRol(NombreRol nombreRol) {
        return rolRepository.findByNombre(nombreRol)
            .orElseThrow(
                () -> new RolNoEncontradoException(nombreRol)
            );
    }

    private UsuarioResponse convertirAResponse(Usuario usuario) {
        Set<NombreRol> nombresRoles = usuario.getRoles().stream()
            .map(Rol::getNombre)
            .collect(Collectors.toUnmodifiableSet());

        return new UsuarioResponse(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getTelefono(),
            usuario.isActivo(),
            usuario.isRequiereCambioClave(),
            usuario.getFechaCreacion(),
            nombresRoles
        );
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private String normalizarContacto(String contacto) {
        if (contacto == null || contacto.isBlank()) {
            return null;
        }

        return contacto.trim();
    }
}
