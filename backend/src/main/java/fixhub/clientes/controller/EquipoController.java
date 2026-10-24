package fixhub.clientes.controller;

import fixhub.clientes.dto.ActualizarEquipoRequest;
import fixhub.clientes.dto.CrearEquipoRequest;
import fixhub.clientes.dto.EquipoResponse;
import fixhub.clientes.service.EquipoService;
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

import java.util.List;

@RestController
@RequestMapping("/api")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @PostMapping("/clientes/{clienteId}/equipos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EquipoResponse> crear(
        @PathVariable Long clienteId,
        @Valid @RequestBody CrearEquipoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(equipoService.crear(clienteId, request));
    }

    @GetMapping("/clientes/{clienteId}/equipos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<EquipoResponse>> listarPorCliente(
        @PathVariable Long clienteId
    ) {
        return ResponseEntity.ok(
            equipoService.listarPorCliente(clienteId)
        );
    }

    @GetMapping("/equipos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EquipoResponse> buscarPorId(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(equipoService.buscarPorId(id));
    }

    @PutMapping("/equipos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<EquipoResponse> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarEquipoRequest request
    ) {
        return ResponseEntity.ok(equipoService.actualizar(id, request));
    }

    @GetMapping("/clientes/me/equipos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<EquipoResponse>> listarPropios(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            equipoService.listarPropios(Long.valueOf(jwt.getSubject()))
        );
    }

    @GetMapping("/clientes/me/equipos/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<EquipoResponse> buscarPropio(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            equipoService.buscarPropio(
                id,
                Long.valueOf(jwt.getSubject())
            )
        );
    }
}
