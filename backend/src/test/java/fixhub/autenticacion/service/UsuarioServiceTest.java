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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
