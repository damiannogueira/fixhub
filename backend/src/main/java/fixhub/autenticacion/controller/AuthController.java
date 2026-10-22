package fixhub.autenticacion.controller;

import fixhub.autenticacion.dto.LoginRequest;
import fixhub.autenticacion.dto.LoginResponse;
import fixhub.autenticacion.service.AutenticacionService;
import fixhub.autenticacion.dto.CambiarClaveRequest;
import fixhub.autenticacion.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacionService autenticacionService;
    private final UsuarioService usuarioService;

    public AuthController(
        AutenticacionService autenticacionService,
        UsuarioService usuarioService
    ) {
        this.autenticacionService = autenticacionService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
            autenticacionService.login(request)
        );
    }

    @PostMapping("/cambiar-clave")
    public ResponseEntity<Void> cambiarClave(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CambiarClaveRequest request
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());

        usuarioService.cambiarClave(usuarioId, request);

        return ResponseEntity.noContent().build();
    }
}
