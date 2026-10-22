package fixhub.autenticacion.service;

import fixhub.autenticacion.dto.LoginRequest;
import fixhub.autenticacion.dto.LoginResponse;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.security.JwtService;
import fixhub.autenticacion.security.UsuarioPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AutenticacionService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AutenticacionService(
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String identificador = request.identificador().trim();

        Authentication authentication =
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    identificador,
                    request.clave()
                )
            );

        UsuarioPrincipal principal =
            obtenerUsuarioPrincipal(authentication);

        String token = jwtService.generarToken(principal);

        Set<NombreRol> roles = principal.getAuthorities().stream()
            .map(this::convertirRol)
            .collect(Collectors.toUnmodifiableSet());

        return new LoginResponse(
            principal.getId(),
            principal.getUsername(),
            token,
            "Bearer",
            jwtService.getExpirationSeconds(),
            principal.isRequiereCambioClave(),
            roles
        );
    }

    private UsuarioPrincipal obtenerUsuarioPrincipal(
        Authentication authentication
    ) {
        if (
            authentication.getPrincipal()
                instanceof UsuarioPrincipal principal
        ) {
            return principal;
        }

        throw new IllegalStateException(
            "El principal autenticado no es válido"
        );
    }

    private NombreRol convertirRol(
        GrantedAuthority authority
    ) {
        String nombre = authority.getAuthority();

        if (!nombre.startsWith("ROLE_")) {
            throw new IllegalStateException(
                "La autoridad no representa un rol: " + nombre
            );
        }

        return NombreRol.valueOf(
            nombre.substring("ROLE_".length())
        );
    }
}
