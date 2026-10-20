package fixhub.autenticacion.controller;

import fixhub.autenticacion.dto.LoginRequest;
import fixhub.autenticacion.dto.LoginResponse;
import fixhub.autenticacion.service.AutenticacionService;
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

    public AuthController(
        AutenticacionService autenticacionService
    ) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
            autenticacionService.login(request)
        );
    }
}
