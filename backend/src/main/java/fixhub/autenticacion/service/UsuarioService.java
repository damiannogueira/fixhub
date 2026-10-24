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
import java.util.List;
import fixhub.autenticacion.exception.UsuarioSinRolesException;

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

    @Transactional(readOnly = true)
    public Usuario buscarEntidadPorId(Long id) {
        return buscarUsuarioPorId(id);
    }

    @Transactional
    public Usuario crearCuentaCliente(
        String email,
        String telefono,
        String claveTemporal
    ) {
        String emailNormalizado = normalizarEmail(email);
        String telefonoNormalizado = normalizarContacto(telefono);

        validarIdentificadoresDisponibles(
            emailNormalizado,
            telefonoNormalizado
        );

        Usuario usuario = new Usuario(
            passwordEncoder.encode(claveTemporal),
            emailNormalizado,
            telefonoNormalizado
        );
        // El rol se asigna desde el backend para que esta operación
        // solo cree cuentas Cliente.
        usuario.agregarRol(buscarRol(NombreRol.CLIENTE));

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario asegurarRolCliente(Usuario usuario) {
        if (!usuario.tieneRol(NombreRol.CLIENTE)) {
            usuario.agregarRol(buscarRol(NombreRol.CLIENTE));
            return usuarioRepository.save(usuario);
        }

        return usuario;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByIdAsc().stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);

        return convertirAResponse(usuario);
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void activar(Long id) {
        Usuario usuario = buscarUsuarioPorId(id);

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponse asignarRol(
        Long usuarioId,
        NombreRol nombreRol
    ) {
        Usuario usuario = buscarUsuarioPorId(usuarioId);

        if (!usuario.tieneRol(nombreRol)) {
            Rol rol = buscarRol(nombreRol);

            usuario.agregarRol(rol);
            usuarioRepository.save(usuario);
        }

        return convertirAResponse(usuario);
    }

    @Transactional
    public UsuarioResponse quitarRol(
        Long usuarioId,
        NombreRol nombreRol
    ) {
        Usuario usuario = buscarUsuarioPorId(usuarioId);

        Rol rolAsignado = usuario.getRoles().stream()
            .filter(rol -> rol.getNombre() == nombreRol)
            .findFirst()
            .orElse(null);

        if (rolAsignado == null) {
            return convertirAResponse(usuario);
        }

        if (usuario.cantidadRoles() == 1) {
            throw new UsuarioSinRolesException(usuarioId);
        }

        usuario.quitarRol(rolAsignado);
        usuarioRepository.save(usuario);

        return convertirAResponse(usuario);
    }

    @Transactional
    public void cambiarClave(
        Long usuarioId,
        CambiarClaveRequest request
    ) {
        Usuario usuario = buscarUsuarioPorId(usuarioId);

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

    private Usuario buscarUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(
                () -> new UsuarioNoEncontradoException(id)
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
