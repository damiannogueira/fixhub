package fixhub.autenticacion.security;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixhubUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private FixhubUserDetailsService userDetailsService;

    @Test
    void debeBuscarUsuarioNormalizandoElIdentificador() {
        Usuario usuario = new Usuario(
            "$2a$10$hashDePrueba",
            "usuario@fixhub.com",
            null
        );

        when(
            usuarioRepository.findByEmailIgnoreCaseOrTelefono(
                "USUARIO@FIXHUB.COM",
                "USUARIO@FIXHUB.COM"
            )
        ).thenReturn(Optional.of(usuario));

        UserDetails resultado =
            userDetailsService.loadUserByUsername(
                " USUARIO@FIXHUB.COM "
            );

        assertThat(resultado)
            .isInstanceOf(UsuarioPrincipal.class);

        assertThat(resultado.getUsername())
            .isEqualTo("usuario@fixhub.com");

        verify(usuarioRepository)
            .findByEmailIgnoreCaseOrTelefono(
                "USUARIO@FIXHUB.COM",
                "USUARIO@FIXHUB.COM"
            );
    }

    @Test
    void debeLanzarExcepcionSiElUsuarioNoExiste() {
        when(
            usuarioRepository.findByEmailIgnoreCaseOrTelefono(
                "inexistente@fixhub.com",
                "inexistente@fixhub.com"
            )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> userDetailsService.loadUserByUsername(
                "inexistente@fixhub.com"
            )
        )
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Credenciales inválidas");
    }

    @Test
    void debeRechazarIdentificadorVacioSinConsultarRepositorio() {
        assertThatThrownBy(
            () -> userDetailsService.loadUserByUsername("   ")
        )
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Credenciales inválidas");

        verifyNoInteractions(usuarioRepository);
    }
}
