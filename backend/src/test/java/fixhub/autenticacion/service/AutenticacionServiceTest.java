package fixhub.autenticacion.service;

import fixhub.autenticacion.dto.LoginRequest;
import fixhub.autenticacion.dto.LoginResponse;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.security.JwtService;
import fixhub.autenticacion.security.UsuarioPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private Authentication authentication;

    @Mock
    private UsuarioPrincipal principal;

    @InjectMocks
    private AutenticacionService autenticacionService;

    @Test
    void debeAutenticarYGenerarToken() {
        LoginRequest request = new LoginRequest(
            " usuario@fixhub.com ",
            "ClaveTemporal123"
        );

        when(
            authenticationManager.authenticate(
                any(Authentication.class)
            )
        ).thenReturn(authentication);

        when(authentication.getPrincipal())
            .thenReturn(principal);

        when(principal.getId()).thenReturn(42L);

        when(principal.getUsername())
            .thenReturn("usuario@fixhub.com");

        when(principal.isRequiereCambioClave())
            .thenReturn(true);

        doReturn(
            List.of(
                new SimpleGrantedAuthority(
                    "ROLE_ADMINISTRADOR"
                )
            )
        ).when(principal).getAuthorities();

        when(jwtService.generarToken(principal))
            .thenReturn("jwt-generado");

        when(jwtService.getExpirationSeconds())
            .thenReturn(3600L);

        LoginResponse response =
            autenticacionService.login(request);

        assertThat(response.usuarioId()).isEqualTo(42L);

        assertThat(response.identificador())
            .isEqualTo("usuario@fixhub.com");

        assertThat(response.token())
            .isEqualTo("jwt-generado");

        assertThat(response.tipo()).isEqualTo("Bearer");

        assertThat(response.expiraEnSegundos())
            .isEqualTo(3600L);

        assertThat(response.requiereCambioClave()).isTrue();

        assertThat(response.roles())
            .containsExactly(NombreRol.ADMINISTRADOR);

        ArgumentCaptor<Authentication> captor =
            ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationManager)
            .authenticate(captor.capture());

        assertThat(captor.getValue().getPrincipal())
            .isEqualTo("usuario@fixhub.com");

        assertThat(captor.getValue().getCredentials())
            .isEqualTo("ClaveTemporal123");

        verify(jwtService).generarToken(principal);
    }

    @Test
    void noDebeGenerarTokenConCredencialesInvalidas() {
        LoginRequest request = new LoginRequest(
            "usuario@fixhub.com",
            "ClaveIncorrecta"
        );

        when(
            authenticationManager.authenticate(
                any(Authentication.class)
            )
        ).thenThrow(
            new BadCredentialsException(
                "Credenciales inválidas"
            )
        );

        assertThatThrownBy(
            () -> autenticacionService.login(request)
        )
            .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never())
            .generarToken(any(UsuarioPrincipal.class));
    }
}
