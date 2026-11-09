package fixhub.inventario.controller;

import fixhub.inventario.dto.ActualizarRepuestoRequest;
import fixhub.inventario.dto.CrearRepuestoRequest;
import fixhub.inventario.dto.RepuestoResponse;
import fixhub.inventario.dto.AjusteStockRequest;
import fixhub.inventario.dto.MovimientoStockRequest;
import fixhub.inventario.dto.MovimientoStockResponse;
import fixhub.inventario.service.RepuestoService;
import fixhub.inventario.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/api/repuestos")
public class RepuestoController {

    private final RepuestoService repuestoService;
    private final InventarioService inventarioService;

    public RepuestoController(RepuestoService repuestoService, InventarioService inventarioService) {
        this.repuestoService = repuestoService;
        this.inventarioService = inventarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RepuestoResponse crear(
        @Valid @RequestBody CrearRepuestoRequest request
    ) {
        return repuestoService.crear(request);
    }

    @GetMapping
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')"
    )
    public List<RepuestoResponse> listar(
        @RequestParam(required = false) String nombre
    ) {
        return repuestoService.listar(nombre);
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')"
    )
    public List<RepuestoResponse> listarConStockBajo() {
        return repuestoService.listarConStockBajo();
    }

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')"
    )
    public RepuestoResponse buscarPorId(
        @PathVariable Long id
    ) {
        return repuestoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public RepuestoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarRepuestoRequest request
    ) {
        return repuestoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void desactivar(@PathVariable Long id) {
        repuestoService.desactivar(id);
    }

    @PostMapping("/{id}/entradas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MovimientoStockResponse entrada(
        @PathVariable Long id,
        @Valid @RequestBody MovimientoStockRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return inventarioService.entrada(id, request, usuarioId(jwt));
    }

    @PostMapping("/{id}/ajustes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public MovimientoStockResponse ajuste(
        @PathVariable Long id,
        @Valid @RequestBody AjusteStockRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return inventarioService.ajuste(id, request, usuarioId(jwt));
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<MovimientoStockResponse> movimientos(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return inventarioService.movimientos(id, usuarioId(jwt));
    }

    private Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
