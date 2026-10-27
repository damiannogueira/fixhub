package fixhub.clientes.controller;

import fixhub.clientes.dto.ActualizarClienteRequest;
import fixhub.clientes.dto.ClienteResponse;
import fixhub.clientes.dto.CrearClienteConCuentaRequest;
import fixhub.clientes.dto.CrearClienteConUsuarioRequest;
import fixhub.clientes.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping("/con-usuario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ClienteResponse> crearConUsuario(
        @Valid @RequestBody CrearClienteConUsuarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(clienteService.crearConUsuario(request));
    }

    @PostMapping("/con-cuenta")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ClienteResponse> crearConCuenta(
        @Valid @RequestBody CrearClienteConCuentaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(clienteService.crearConCuenta(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<ClienteResponse>> listar() {
        return ResponseEntity.ok(clienteService.listar());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClienteResponse> buscarPropio(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            clienteService.buscarPropio(Long.valueOf(jwt.getSubject()))
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ClienteResponse> buscarPorId(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ClienteResponse> actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarClienteRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }
}
