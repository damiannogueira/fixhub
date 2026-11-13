package fixhub.autenticacion.service;

import fixhub.autenticacion.dto.CrearUsuarioRequest;
import fixhub.autenticacion.dto.ActualizarUsuarioRequest;
import fixhub.clientes.repository.ClienteRepository;
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
import fixhub.autenticacion.exception.UsuarioNoEsTecnicoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    static List<String> clavesNumericas() {
        return List.of("12345678", "0".repeat(72), "１２３４５６７８");
    }

    @ParameterizedTest
    @MethodSource("clavesNumericas")
    void debeRechazarClavesNumericasSinModificarDatos(String clave) {
        assertThatThrownBy(() -> usuarioService.crear(new CrearUsuarioRequest(
            "interno@fixhub.com", null, clave, Set.of(NombreRol.TECNICO))))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuarioService.crearCuentaCliente(
            "cliente@fixhub.com", null, clave))
            .isInstanceOf(IllegalArgumentException.class);

        Usuario usuario = new Usuario("hash-anterior", "original@fixhub.com", null);
        Rol rol = new Rol(NombreRol.TECNICO);
        usuario.agregarRol(rol);
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre(NombreRol.TECNICO)).thenReturn(Optional.of(rol));
        assertThatThrownBy(() -> usuarioService.actualizar(42L, new ActualizarUsuarioRequest(
            "cambiado@fixhub.com", null, clave, Set.of(NombreRol.TECNICO), false)))
            .isInstanceOf(IllegalArgumentException.class);

        when(passwordEncoder.matches("Temporal123", "hash-anterior")).thenReturn(true);
        assertThatThrownBy(() -> usuarioService.cambiarClave(42L,
            new CambiarClaveRequest("Temporal123", clave, clave)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-anterior");
        assertThat(usuario.isRequiereCambioClave()).isTrue();
        assertThat(usuario.getEmail()).isEqualTo("original@fixhub.com");
        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getRoles()).containsExactly(rol);
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noDebeAceptarLaMismaClaveNiDesactivarElCambioObligatorio() {
        Usuario usuario = new Usuario("hash-anterior", "cliente@fixhub.com", null);
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Temporal123", "hash-anterior")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cambiarClave(42L,
            new CambiarClaveRequest("Temporal123", "Temporal123", "Temporal123")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("La nueva contraseña debe ser diferente de la actual");

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-anterior");
        assertThat(usuario.isRequiereCambioClave()).isTrue();
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    static List<Set<NombreRol>> combinacionesInternas() {
        return List.of(
            Set.of(NombreRol.ADMINISTRADOR, NombreRol.RECEPCIONISTA),
            Set.of(NombreRol.ADMINISTRADOR, NombreRol.TECNICO),
            Set.of(NombreRol.RECEPCIONISTA, NombreRol.TECNICO),
            Set.of(NombreRol.ADMINISTRADOR, NombreRol.RECEPCIONISTA, NombreRol.TECNICO)
        );
    }

    @ParameterizedTest
    @MethodSource("combinacionesInternas")
    void debeCrearYActualizarConRolesInternosAcumulados(Set<NombreRol> roles) {
        roles.forEach(rol -> when(rolRepository.findByNombre(rol))
            .thenReturn(Optional.of(new Rol(rol))));
        when(passwordEncoder.encode("Temporal123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(usuarioService.crear(new CrearUsuarioRequest(
            "interno@fixhub.com", null, "Temporal123", roles)).roles())
            .containsExactlyInAnyOrderElementsOf(roles);

        Usuario usuario = new Usuario("hash", "interno@fixhub.com", null);
        usuario.agregarRol(new Rol(NombreRol.TECNICO));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuario));
        assertThat(usuarioService.actualizar(42L, new ActualizarUsuarioRequest(
            "interno@fixhub.com", null, null, roles, true)).roles())
            .containsExactlyInAnyOrderElementsOf(roles);
    }

    @ParameterizedTest
    @EnumSource(value = NombreRol.class, names = "CLIENTE", mode = EnumSource.Mode.EXCLUDE)
    void debeRechazarClienteConCadaRolInternoSinModificarDatos(NombreRol interno) {
        Set<NombreRol> incompatibles = Set.of(NombreRol.CLIENTE, interno);
        assertThatThrownBy(() -> usuarioService.crear(new CrearUsuarioRequest(
            "cliente@fixhub.com", null, "Temporal123", incompatibles)))
            .isInstanceOf(IllegalArgumentException.class);

        Usuario cliente = new Usuario("hash", "cliente@fixhub.com", null);
        cliente.agregarRol(new Rol(NombreRol.CLIENTE));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(cliente));
        assertThatThrownBy(() -> usuarioService.actualizar(42L, new ActualizarUsuarioRequest(
            "cambiado@fixhub.com", null, "OtraClave123", incompatibles, false)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuarioService.asignarRol(42L, interno))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(cliente.getEmail()).isEqualTo("cliente@fixhub.com");
        assertThat(cliente.getPasswordHash()).isEqualTo("hash");
        assertThat(cliente.isActivo()).isTrue();
        assertThat(cliente.getRoles()).extracting(Rol::getNombre).containsExactly(NombreRol.CLIENTE);

        Usuario usuarioInterno = new Usuario("hash", "interno@fixhub.com", null);
        usuarioInterno.agregarRol(new Rol(interno));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuarioInterno));
        assertThatThrownBy(() -> usuarioService.asignarRol(42L, NombreRol.CLIENTE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usuarioService.asegurarRolCliente(usuarioInterno))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuarioInterno.getRoles()).extracting(Rol::getNombre).containsExactly(interno);
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizarNoDebeQuitarClienteDeCuentaVinculada() {
        Usuario usuario = new Usuario("hash", "cliente@fixhub.com", null);
        usuario.agregarRol(new Rol(NombreRol.CLIENTE));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.existsByUsuarioId(42L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(42L,
            new ActualizarUsuarioRequest("cliente@fixhub.com", null, null,
                Set.of(NombreRol.TECNICO), true)))
            .isInstanceOf(IllegalStateException.class);

        assertThat(usuario.tieneRol(NombreRol.CLIENTE)).isTrue();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarDebeRechazarRolInternoConCliente() {
        Usuario usuario = new Usuario("hash", "cliente@fixhub.com", null);
        usuario.agregarRol(new Rol(NombreRol.CLIENTE));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> usuarioService.actualizar(42L, new ActualizarUsuarioRequest(
            "otro@fixhub.com", null, null, Set.of(NombreRol.CLIENTE, NombreRol.TECNICO), true)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(usuario.getRoles()).extracting(Rol::getNombre)
            .containsExactly(NombreRol.CLIENTE);
        assertThat(usuario.getEmail()).isEqualTo("cliente@fixhub.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void quitarRolTampocoDebeQuitarClienteVinculado() {
        Usuario usuario = new Usuario("hash", "cliente@fixhub.com", null);
        usuario.agregarRol(new Rol(NombreRol.CLIENTE));
        usuario.agregarRol(new Rol(NombreRol.TECNICO));
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.existsByUsuarioId(42L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.quitarRol(42L, NombreRol.CLIENTE))
            .isInstanceOf(IllegalStateException.class);
        assertThat(usuario.tieneRol(NombreRol.CLIENTE)).isTrue();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void debeBuscarUsuarioConRolTecnico() {
        Usuario tecnico = new Usuario("hash", "tecnico@fixhub.com", null);
        tecnico.agregarRol(new Rol(NombreRol.TECNICO));
        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(tecnico));

        Usuario resultado = usuarioService.buscarTecnicoPorId(42L);

        assertThat(resultado).isSameAs(tecnico);
    }

    @Test
    void debeRechazarUsuarioQueNoEsTecnico() {
        Usuario cliente = new Usuario("hash", "cliente@fixhub.com", null);
        cliente.agregarRol(new Rol(NombreRol.CLIENTE));
        when(usuarioRepository.findById(42L))
            .thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> usuarioService.buscarTecnicoPorId(42L))
            .isInstanceOf(UsuarioNoEsTecnicoException.class);
    }

    @Test
    void debeCrearCuentaClienteConRolImpuesto() {
        Rol rolCliente = new Rol(NombreRol.CLIENTE);
        when(passwordEncoder.encode("Temporal123"))
            .thenReturn("hash-cliente");
        when(rolRepository.findByNombre(NombreRol.CLIENTE))
            .thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = usuarioService.crearCuentaCliente(
            " CLIENTE@FIXHUB.COM ",
            " +5493415551234 ",
            "Temporal123"
        );

        assertThat(usuario.getEmail()).isEqualTo("cliente@fixhub.com");
        assertThat(usuario.getTelefono()).isEqualTo("+5493415551234");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash-cliente");
        assertThat(usuario.isRequiereCambioClave()).isTrue();
        assertThat(usuario.getRoles())
            .extracting(Rol::getNombre)
            .containsExactly(NombreRol.CLIENTE);
    }

    @Test
    void debeRechazarClienteEnCuentaInterna() {
        Usuario usuario = new Usuario("hash", "admin@fixhub.com", null);
        usuario.agregarRol(new Rol(NombreRol.ADMINISTRADOR));
        assertThatThrownBy(() -> usuarioService.asegurarRolCliente(usuario))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(usuario.getRoles())
            .extracting(Rol::getNombre)
            .containsExactly(NombreRol.ADMINISTRADOR);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noDebeDuplicarRolCliente() {
        Usuario usuario = new Usuario("hash", "cliente@fixhub.com", null);
        usuario.agregarRol(new Rol(NombreRol.CLIENTE));

        usuarioService.asegurarRolCliente(usuario);

        assertThat(usuario.getRoles()).hasSize(1);
        verify(usuarioRepository, never()).save(usuario);
    }

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
            new Rol(NombreRol.RECEPCIONISTA)
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
                NombreRol.RECEPCIONISTA,
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
            new Rol(NombreRol.RECEPCIONISTA)
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
            .containsExactly(NombreRol.RECEPCIONISTA);

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
