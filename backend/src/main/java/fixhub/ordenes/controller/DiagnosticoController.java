package fixhub.ordenes.controller;

import fixhub.ordenes.dto.ActualizarDiagnosticoRequest;
import fixhub.ordenes.dto.CrearDiagnosticoRequest;
import fixhub.ordenes.dto.DiagnosticoClienteResponse;
import fixhub.ordenes.dto.DiagnosticoResponse;
import fixhub.ordenes.service.DiagnosticoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DiagnosticoController {

    private final DiagnosticoService diagnosticoService;

    public DiagnosticoController(
        DiagnosticoService diagnosticoService
    ) {
        this.diagnosticoService = diagnosticoService;
    }

    @PostMapping("/ordenes/{id}/diagnostico")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<DiagnosticoResponse> crear(
        @PathVariable Long id,
        @Valid @RequestBody CrearDiagnosticoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            diagnosticoService.crear(
                id,
                request,
                usuarioId(jwt)
            )
        );
    }

    @PutMapping("/ordenes/{id}/diagnostico")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<DiagnosticoResponse> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarDiagnosticoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            diagnosticoService.actualizar(
                id,
                request,
                usuarioId(jwt)
            )
        );
    }

    @GetMapping("/ordenes/{id}/diagnostico")
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')"
    )
    public ResponseEntity<DiagnosticoResponse> buscar(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(
            diagnosticoService.buscarPorOrden(id)
        );
    }

    @GetMapping("/ordenes/me/{id}/diagnostico")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<DiagnosticoResponse> buscarAsignado(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            diagnosticoService.buscarAsignado(
                id,
                usuarioId(jwt)
            )
        );
    }

    @GetMapping("/clientes/me/ordenes/{id}/diagnostico")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<DiagnosticoClienteResponse>
        buscarPropioCliente(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
        ) {
        return ResponseEntity.ok(
            diagnosticoService.buscarPropioCliente(
                id,
                usuarioId(jwt)
            )
        );
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
