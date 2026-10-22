package fixhub.autenticacion.config;

import fixhub.autenticacion.dto.CrearUsuarioRequest;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.autenticacion.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadorAdministradorTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioService usuarioService;

    @Test
    void debeCrearAdministradorInicialSiNoExiste() {
        InicializadorAdministrador inicializador =
            crearInicializador(
                " ADMIN@FIXHUB.COM ",
                "ClaveSegura123"
            );

        when(
            usuarioRepository.findByEmailIgnoreCase(
                "admin@fixhub.com"
            )
        ).thenReturn(Optional.empty());

        inicializador.run(null);

        ArgumentCaptor<CrearUsuarioRequest> captor =
            ArgumentCaptor.forClass(
                CrearUsuarioRequest.class
            );

        verify(usuarioService).crear(captor.capture());

        CrearUsuarioRequest request = captor.getValue();

        assertThat(request.email())
            .isEqualTo("admin@fixhub.com");

        assertThat(request.telefono()).isNull();

        assertThat(request.claveTemporal())
            .isEqualTo("ClaveSegura123");

        assertThat(request.roles())
            .containsExactly(NombreRol.ADMINISTRADOR);
    }

    @Test
    void noDebeCrearNuevamenteAdministradorExistente() {
        Usuario administrador = new Usuario(
            "$2a$10$hashAdministrador",
            "admin@fixhub.com",
            null
        );

        administrador.agregarRol(
            new Rol(NombreRol.ADMINISTRADOR)
        );

        InicializadorAdministrador inicializador =
            crearInicializador(
                "admin@fixhub.com",
                "ClaveSegura123"
            );

        when(
            usuarioRepository.findByEmailIgnoreCase(
                "admin@fixhub.com"
            )
        ).thenReturn(Optional.of(administrador));

        inicializador.run(null);

        verifyNoInteractions(usuarioService);
    }

    @Test
    void debeRechazarEmailQuePerteneceANoAdministrador() {
        Usuario tecnico = new Usuario(
            "$2a$10$hashTecnico",
            "admin@fixhub.com",
            null
        );

        tecnico.agregarRol(
            new Rol(NombreRol.TECNICO)
        );

        InicializadorAdministrador inicializador =
            crearInicializador(
                "admin@fixhub.com",
                "ClaveSegura123"
            );

        when(
            usuarioRepository.findByEmailIgnoreCase(
                "admin@fixhub.com"
            )
        ).thenReturn(Optional.of(tecnico));

        assertThatThrownBy(
            () -> inicializador.run(null)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(
                "ya pertenece a otro usuario"
            );

        verifyNoInteractions(usuarioService);
    }

    private InicializadorAdministrador crearInicializador(
        String email,
        String password
    ) {
        return new InicializadorAdministrador(
            usuarioRepository,
            usuarioService,
            email,
            password
        );
    }
}
