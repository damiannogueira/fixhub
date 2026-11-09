package fixhub.inventario.controller;

import fixhub.inventario.dto.ConsumoRepuestoResponse;
import fixhub.inventario.dto.CrearConsumoRequest;
import fixhub.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ordenes")
public class ConsumoRepuestoController {
    private final InventarioService inventarioService;

    public ConsumoRepuestoController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/{ordenId}/consumos")
    @PreAuthorize("hasRole('TECNICO')")
    public ConsumoRepuestoResponse consumir(
        @PathVariable Long ordenId,
        @Valid @RequestBody CrearConsumoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return inventarioService.consumir(ordenId, request, usuarioId(jwt));
    }

    @GetMapping("/{ordenId}/consumos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')")
    public List<ConsumoRepuestoResponse> listar(
        @PathVariable Long ordenId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return inventarioService.consumosPorOrden(ordenId, usuarioId(jwt));
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
