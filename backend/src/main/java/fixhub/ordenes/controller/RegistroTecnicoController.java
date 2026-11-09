package fixhub.ordenes.controller;

import fixhub.ordenes.dto.CrearRegistroTecnicoRequest;
import fixhub.ordenes.dto.RegistroTecnicoClienteResponse;
import fixhub.ordenes.dto.RegistroTecnicoResponse;
import fixhub.ordenes.service.RegistroTecnicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RegistroTecnicoController {

    private final RegistroTecnicoService
        registroTecnicoService;

    public RegistroTecnicoController(
        RegistroTecnicoService registroTecnicoService
    ) {
        this.registroTecnicoService =
            registroTecnicoService;
    }

    @PostMapping(
        "/ordenes/{id}/registros-tecnicos"
    )
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<RegistroTecnicoResponse> crear(
        @PathVariable Long id,
        @Valid
        @RequestBody
        CrearRegistroTecnicoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            registroTecnicoService.crear(
                id,
                request,
                usuarioId(jwt)
            )
        );
    }

    @GetMapping(
        "/ordenes/{id}/registros-tecnicos"
    )
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')"
    )
    public ResponseEntity<List<RegistroTecnicoResponse>>
        listar(
            @PathVariable Long id
        ) {
        return ResponseEntity.ok(
            registroTecnicoService.listarPorOrden(id)
        );
    }

    @GetMapping(
        "/ordenes/me/{id}/registros-tecnicos"
    )
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<List<RegistroTecnicoResponse>>
        listarAsignados(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
        ) {
        return ResponseEntity.ok(
            registroTecnicoService.listarAsignados(
                id,
                usuarioId(jwt)
            )
        );
    }

    @GetMapping(
        "/clientes/me/ordenes/{id}/registros-tecnicos"
    )
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<RegistroTecnicoClienteResponse>>
        listarPropiosCliente(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
        ) {
        return ResponseEntity.ok(
            registroTecnicoService.listarPropiosCliente(
                id,
                usuarioId(jwt)
            )
        );
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
