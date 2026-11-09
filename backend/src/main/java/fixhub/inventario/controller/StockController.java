package fixhub.inventario.controller;

import fixhub.inventario.dto.AjustarStockRequest;
import fixhub.inventario.dto.ConsumoRepuestoResponse;
import fixhub.inventario.dto.MovimientoStockResponse;
import fixhub.inventario.dto.RegistrarConsumoRepuestoRequest;
import fixhub.inventario.dto.RegistrarEntradaStockRequest;
import fixhub.inventario.service.StockService;
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
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/repuestos/{repuestoId}/stock/entradas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MovimientoStockResponse> registrarEntrada(
        @PathVariable Long repuestoId,
        @Valid @RequestBody RegistrarEntradaStockRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            stockService.registrarEntrada(
                repuestoId,
                request,
                usuarioId(jwt)
            )
        );
    }

    @PatchMapping("/repuestos/{repuestoId}/stock")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MovimientoStockResponse> ajustarStock(
        @PathVariable Long repuestoId,
        @Valid @RequestBody AjustarStockRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
            stockService.ajustarStock(
                repuestoId,
                request,
                usuarioId(jwt)
            )
        );
    }

    @GetMapping("/inventario/movimientos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<MovimientoStockResponse>>
        listarMovimientos(
            @AuthenticationPrincipal Jwt jwt
        ) {
        return ResponseEntity.ok(
            stockService.listarMovimientos(usuarioId(jwt))
        );
    }

    @GetMapping("/repuestos/{repuestoId}/movimientos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<MovimientoStockResponse>>
        listarMovimientosPorRepuesto(
            @PathVariable Long repuestoId,
            @AuthenticationPrincipal Jwt jwt
        ) {
        return ResponseEntity.ok(
            stockService.listarMovimientosPorRepuesto(
                repuestoId,
                usuarioId(jwt)
            )
        );
    }

    @PostMapping("/ordenes/{ordenId}/consumos")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<ConsumoRepuestoResponse> registrarConsumo(
        @PathVariable Long ordenId,
        @Valid @RequestBody RegistrarConsumoRepuestoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            stockService.registrarConsumo(
                ordenId,
                request,
                usuarioId(jwt)
            )
        );
    }

    @GetMapping("/ordenes/{ordenId}/consumos")
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')"
    )
    public ResponseEntity<List<ConsumoRepuestoResponse>>
        listarConsumosPorOrden(
            @PathVariable Long ordenId,
            @AuthenticationPrincipal Jwt jwt
        ) {
        return ResponseEntity.ok(
            stockService.listarConsumosPorOrden(
                ordenId,
                usuarioId(jwt)
            )
        );
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
