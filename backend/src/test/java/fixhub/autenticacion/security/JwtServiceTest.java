package fixhub.autenticacion.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.doReturn;
import java.time.temporal.ChronoUnit;

class JwtServiceTest {

    private static final String TEST_SECRET =
        "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void debeGenerarTokenFirmadoConLosDatosDelUsuario() {
        JwtConfig config = new JwtConfig(TEST_SECRET);
        SecretKey secretKey = config.jwtSecretKey();

        JwtService jwtService = new JwtService(
            config.jwtEncoder(secretKey),
            Duration.ofHours(1)
        );

        JwtDecoder jwtDecoder =
            config.jwtDecoder(secretKey);

        UsuarioPrincipal principal =
            mock(UsuarioPrincipal.class);

        when(principal.getId()).thenReturn(42L);

        when(principal.getUsername())
            .thenReturn("usuario@fixhub.com");

        when(principal.isRequiereCambioClave())
            .thenReturn(true);

        doReturn(
            List.of(
                new SimpleGrantedAuthority(
                    "ROLE_ADMINISTRADOR"
                ),
                new SimpleGrantedAuthority(
                    "ROLE_TECNICO"
                )
            )
        ).when(principal).getAuthorities();

        Instant antesDeGenerar = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        String token = jwtService.generarToken(principal);
        Jwt jwt = jwtDecoder.decode(token);

        assertThat(token).isNotBlank();
        assertThat(jwt.getIssuer().toString())
            .isEqualTo("https://fixhub.local");

        assertThat(jwt.getSubject()).isEqualTo("42");

        assertThat(
            jwt.getClaimAsString("identificador")
        ).isEqualTo("usuario@fixhub.com");

        assertThat(
            jwt.getClaimAsStringList("roles")
        ).containsExactly(
            "ADMINISTRADOR",
            "TECNICO"
        );

        assertThat(
            jwt.getClaimAsBoolean("requiereCambioClave")
        ).isTrue();

        assertThat(jwt.getIssuedAt())
            .isAfterOrEqualTo(antesDeGenerar);

        assertThat(jwt.getExpiresAt())
            .isAfter(jwt.getIssuedAt());

        assertThat(jwtService.getExpirationSeconds())
            .isEqualTo(3600);
    }
}
