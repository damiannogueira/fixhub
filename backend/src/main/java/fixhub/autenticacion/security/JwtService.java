package fixhub.autenticacion.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private static final String ISSUER = "https://fixhub.local";

    private final JwtEncoder jwtEncoder;
    private final Duration expiration;

    public JwtService(
        JwtEncoder jwtEncoder,
        @Value("${app.security.jwt.expiration}")
        Duration expiration
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
    }

    public String generarToken(UsuarioPrincipal principal) {
        Instant ahora = Instant.now();
        Instant vencimiento = ahora.plus(expiration);

        List<String> roles = principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(this::quitarPrefijoRol)
            .sorted()
            .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(ahora)
            .expiresAt(vencimiento)
            .subject(principal.getId().toString())
            .claim(
                "identificador",
                principal.getUsername()
            )
            .claim("roles", roles)
            .claim(
                "requiereCambioClave",
                principal.isRequiereCambioClave()
            )
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }

    private String quitarPrefijoRol(String authority) {
        return authority.startsWith("ROLE_")
            ? authority.substring(5)
            : authority;
    }
}
