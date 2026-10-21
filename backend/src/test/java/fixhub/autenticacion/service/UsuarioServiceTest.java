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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import fixhub.autenticacion.exception.UsuarioSinRolesException;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void debeCrearUsuarioNormalizandoDatosYCifrandoClave() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
            " USUARIO@FIXHUB.COM ",
            " +5493415551234 ",
            "ClaveTemporal123",
            Set.of(NombreRol.CLIENTE)
        );

        Rol rolCliente = new Rol(NombreRol.CLIENTE);

        when(
            usuarioRepository.existsByEmailIgnoreCase(
                "usuario@fixhub.com"
            )
        ).thenReturn(false);

        when(
            usuarioRepository.existsByTelefono("+5493415551234")
        ).thenReturn(false);

        when(rolRepository.findByNombre(NombreRol.CLIENTE))
            .thenReturn(Optional.of(rolCliente));

        when(passwordEncoder.encode("ClaveTemporal123"))
            .thenReturn("$2a$10$hashGenerado");

        when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse response = usuarioService.crear(request);

        assertThat(response.email())
            .isEqualTo("usuario@fixhub.com");

        assertThat(response.telefono())
            .isEqualTo("+5493415551234");

        assertThat(response.activo()).isTrue();
        assertThat(response.requiereCambioClave()).isTrue();

        assertThat(response.roles())
            .containsExactly(NombreRol.CLIENTE);

        verify(passwordEncoder).encode("ClaveTemporal123");

        ArgumentCaptor<Usuario> captor =
            ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertThat(usuarioGuardado.getPasswordHash())
            .isEqualTo("$2a$10$hashGenerado")
            .isNotEqualTo("ClaveTemporal123");
    }

    @Test
    void noDebeCrearUsuarioConEmailDuplicado() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
            "existente@fixhub.com",
            null,
            "ClaveTemporal123",
            Set.of(NombreRol.CLIENTE)
        );

        when(
            usuarioRepository.existsByEmailIgnoreCase(
                "existente@fixhub.com"
            )
        ).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
            .isInstanceOf(
                IdentificadorUsuarioDuplicadoException.class
            )
            .hasMessageContaining("existente@fixhub.com");

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void noDebeCrearUsuarioConTelefonoDuplicado() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
            null,
            "+5493415551234",
            "ClaveTemporal123",
            Set.of(NombreRol.CLIENTE)
        );

        when(
            usuarioRepository.existsByTelefono("+5493415551234")
        ).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
            .isInstanceOf(
                IdentificadorUsuarioDuplicadoException.class
            )
            .hasMessageContaining("+5493415551234");

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void noDebeCrearUsuarioConRolInexistente() {
        CrearUsuarioRequest request = new CrearUsuarioRequest(
            "usuario@fixhub.com",
            null,
            "ClaveTemporal123",
            Set.of(NombreRol.TECNICO)
        );

        when(
            usuarioRepository.existsByEmailIgnoreCase(
                "usuario@fixhub.com"
            )
        ).thenReturn(false);

        when(rolRepository.findByNombre(NombreRol.TECNICO))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.crear(request))
            .isInstanceOf(RolNoEncontradoException.class)
            .hasMessageContaining("TECNICO");

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void debeListarUsuariosConSusRoles() {
        Usuario administrador = new Usuario(
            "$2a$10$hashAdministrador",
            "admin@fixhub.com",
            null
        );

        administrador.agregarRol(
            new Rol(NombreRol.ADMINISTRADOR)
        );

        Usuario tecnico = new Usuario(
            "$2a$10$hashTecnico",
            "tecnico@fixhub.com",
            "+5493415551234"
        );

        tecnico.agregarRol(
            new Rol(NombreRol.TECNICO)
        );

        when(usuarioRepository.findAllByOrderByIdAsc())
            .thenReturn(List.of(administrador, tecnico));

        List<UsuarioResponse> resultado =
            usuarioService.listar();

        assertThat(resultado).hasSize(2);

        assertThat(resultado)
            .extracting(UsuarioResponse::email)
            .containsExactly(
                "admin@fixhub.com",
                "tecnico@fixhub.com"
            );

        assertThat(resultado.get(0).roles())
            .containsExactly(NombreRol.ADMINISTRADOR);

        assertThat(resultado.get(1).roles())
            .containsExactly(NombreRol.TECNICO);
    }

    @Test
    void debeBuscarUsuarioPorIdConSusRoles() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            "+5493415551234"
        );

        usuario.agregarRol(
            new Rol(NombreRol.CLIENTE)
        );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        UsuarioResponse resultado =
            usuarioService.buscarPorId(42L);

        assertThat(resultado.email())
            .isEqualTo("usuario@fixhub.com");

        assertThat(resultado.telefono())
            .isEqualTo("+5493415551234");

        assertThat(resultado.activo()).isTrue();

        assertThat(resultado.roles())
            .containsExactly(NombreRol.CLIENTE);
    }

    @Test
    void debeLanzarExcepcionAlBuscarUsuarioInexistente() {
        when(usuarioRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> usuarioService.buscarPorId(99L)
        )
            .isInstanceOf(UsuarioNoEncontradoException.class)
            .hasMessageContaining("99");
    }

    @Test
    void debeDesactivarUsuario() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        usuarioService.desactivar(42L);

        assertThat(usuario.isActivo()).isFalse();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void debeActivarUsuario() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        usuario.setActivo(false);

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        usuarioService.activar(42L);

        assertThat(usuario.isActivo()).isTrue();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void noDebeDesactivarUsuarioInexistente() {
        when(usuarioRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> usuarioService.desactivar(99L)
        )
            .isInstanceOf(UsuarioNoEncontradoException.class)
            .hasMessageContaining("99");

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    @Test
    void noDebeActivarUsuarioInexistente() {
        when(usuarioRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> usuarioService.activar(99L)
        )
            .isInstanceOf(UsuarioNoEncontradoException.class)
            .hasMessageContaining("99");

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    @Test
    void debeCambiarClaveYDesactivarCambioObligatorio() {
        Usuario usuario = new Usuario(
            "$2a$10$hashAnterior",
            "usuario@fixhub.com",
            null
        );

        CambiarClaveRequest request =
            new CambiarClaveRequest(
                "ClaveTemporal123",
                "NuevaClave456",
                "NuevaClave456"
            );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        when(
            passwordEncoder.matches(
                "ClaveTemporal123",
                "$2a$10$hashAnterior"
            )
        ).thenReturn(true);

        when(passwordEncoder.encode("NuevaClave456"))
            .thenReturn("$2a$10$hashNuevo");

        usuarioService.cambiarClave(42L, request);

        assertThat(usuario.getPasswordHash())
            .isEqualTo("$2a$10$hashNuevo");

        assertThat(usuario.isRequiereCambioClave())
            .isFalse();

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void noDebeCambiarClaveSiLaActualEsIncorrecta() {
        Usuario usuario = new Usuario(
            "$2a$10$hashAnterior",
            "usuario@fixhub.com",
            null
        );

        CambiarClaveRequest request =
            new CambiarClaveRequest(
                "ClaveIncorrecta",
                "NuevaClave456",
                "NuevaClave456"
            );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        when(
            passwordEncoder.matches(
                "ClaveIncorrecta",
                "$2a$10$hashAnterior"
            )
        ).thenReturn(false);

        assertThatThrownBy(
            () -> usuarioService.cambiarClave(42L, request)
        )
            .isInstanceOf(
                ClaveActualIncorrectaException.class
            )
            .hasMessage("La clave actual es incorrecta");

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    @Test
    void noDebeCambiarClaveSiElUsuarioNoExiste() {
        CambiarClaveRequest request =
            new CambiarClaveRequest(
                "ClaveTemporal123",
                "NuevaClave456",
                "NuevaClave456"
            );

        when(usuarioRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> usuarioService.cambiarClave(99L, request)
        )
            .isInstanceOf(
                UsuarioNoEncontradoException.class
            )
            .hasMessageContaining("99");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    @Test
    void debeAsignarRolAUsuario() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        usuario.agregarRol(
            new Rol(NombreRol.CLIENTE)
        );

        Rol rolTecnico = new Rol(NombreRol.TECNICO);

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        when(rolRepository.findByNombre(NombreRol.TECNICO))
            .thenReturn(Optional.of(rolTecnico));

        UsuarioResponse resultado =
            usuarioService.asignarRol(
                42L,
                NombreRol.TECNICO
            );

        assertThat(resultado.roles())
            .containsExactlyInAnyOrder(
                NombreRol.CLIENTE,
                NombreRol.TECNICO
            );

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void noDebeDuplicarRolYaAsignado() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        usuario.agregarRol(
            new Rol(NombreRol.TECNICO)
        );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        UsuarioResponse resultado =
            usuarioService.asignarRol(
                42L,
                NombreRol.TECNICO
            );

        assertThat(resultado.roles())
            .containsExactly(NombreRol.TECNICO);

        verify(rolRepository, never())
            .findByNombre(NombreRol.TECNICO);

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    @Test
    void debeQuitarRolSiElUsuarioTieneMasDeUno() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        usuario.agregarRol(
            new Rol(NombreRol.CLIENTE)
        );

        usuario.agregarRol(
            new Rol(NombreRol.TECNICO)
        );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        UsuarioResponse resultado =
            usuarioService.quitarRol(
                42L,
                NombreRol.TECNICO
            );

        assertThat(resultado.roles())
            .containsExactly(NombreRol.CLIENTE);

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void noDebeQuitarElUltimoRolDelUsuario() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        usuario.agregarRol(
            new Rol(NombreRol.CLIENTE)
        );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        assertThatThrownBy(
            () -> usuarioService.quitarRol(
                42L,
                NombreRol.CLIENTE
            )
        )
            .isInstanceOf(UsuarioSinRolesException.class)
            .hasMessageContaining("42");

        assertThat(usuario.tieneRol(NombreRol.CLIENTE))
            .isTrue();

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    @Test
    void quitarRolNoAsignadoNoDebeModificarUsuario() {
        Usuario usuario = new Usuario(
            "$2a$10$hashUsuario",
            "usuario@fixhub.com",
            null
        );

        usuario.agregarRol(
            new Rol(NombreRol.CLIENTE)
        );

        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(usuario));

        UsuarioResponse resultado =
            usuarioService.quitarRol(
                42L,
                NombreRol.TECNICO
            );

        assertThat(resultado.roles())
            .containsExactly(NombreRol.CLIENTE);

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }
}
