package fixhub.ordenes.controller;

import fixhub.ordenes.dto.AsignarTecnicoRequest;
import fixhub.ordenes.dto.CambiarEstadoOrdenRequest;
import fixhub.ordenes.dto.CancelarOrdenRequest;
import fixhub.ordenes.dto.CrearOrdenTrabajoRequest;
import fixhub.ordenes.dto.OrdenClienteResponse;
import fixhub.ordenes.dto.OrdenTrabajoResponse;
import fixhub.ordenes.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrdenTrabajoController {

    private final OrdenTrabajoService ordenTrabajoService;

    public OrdenTrabajoController(
        OrdenTrabajoService ordenTrabajoService
    ) {
        this.ordenTrabajoService = ordenTrabajoService;
    }

    @PostMapping("/ordenes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<OrdenTrabajoResponse> crear(
        @Valid @RequestBody CrearOrdenTrabajoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ordenTrabajoService.crear(request, usuarioId(jwt))
        );
    }

    @GetMapping("/ordenes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<OrdenTrabajoResponse>> listar() {
        return ResponseEntity.ok(ordenTrabajoService.listar());
    }

    @GetMapping("/ordenes/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<OrdenTrabajoResponse> buscarPorId(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(ordenTrabajoService.buscarPorId(id));
    }

    @PatchMapping("/ordenes/{id}/tecnico")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<OrdenTrabajoResponse> asignarTecnico(
        @PathVariable Long id,
        @Valid @RequestBody AsignarTecnicoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.asignarTecnico(
                id,
                request.tecnicoId(),
                usuarioId(jwt)
            )
        );
    }

    @PatchMapping("/ordenes/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'RECEPCIONISTA')")
    public ResponseEntity<OrdenTrabajoResponse> cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody CambiarEstadoOrdenRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.cambiarEstado(
                id,
                request.estado(),
                usuarioId(jwt)
            )
        );
    }

    @PostMapping("/ordenes/{id}/cancelacion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<OrdenTrabajoResponse> cancelar(
        @PathVariable Long id,
        @Valid @RequestBody CancelarOrdenRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.cancelar(
                id,
                request.motivo(),
                usuarioId(jwt)
            )
        );
    }

    @GetMapping("/ordenes/me")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<List<OrdenTrabajoResponse>> listarAsignadas(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.listarAsignadas(usuarioId(jwt))
        );
    }

    @GetMapping("/ordenes/me/{id}")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<OrdenTrabajoResponse> buscarAsignada(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.buscarAsignada(id, usuarioId(jwt))
        );
    }

    @GetMapping("/clientes/me/ordenes")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<OrdenClienteResponse>> listarPropiasCliente(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.listarPropiasCliente(usuarioId(jwt))
        );
    }

    @GetMapping("/clientes/me/ordenes/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<OrdenClienteResponse> buscarPropiaCliente(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            ordenTrabajoService.buscarPropiaCliente(id, usuarioId(jwt))
        );
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
