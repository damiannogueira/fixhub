package fixhub.presupuestos.controller;

import fixhub.presupuestos.dto.*;
import fixhub.presupuestos.service.PresupuestoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PresupuestoController {
    private final PresupuestoService service;
    public PresupuestoController(PresupuestoService service) { this.service = service; }
    @PostMapping("/ordenes/{ordenId}/presupuesto")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')")
    public PresupuestoResponse crear(@PathVariable Long ordenId, @Valid @RequestBody CrearPresupuestoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.crear(ordenId, request, usuarioId(jwt));
    }
    @GetMapping("/ordenes/{ordenId}/presupuesto")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO', 'CLIENTE')")
    public PresupuestoResponse buscar(@PathVariable Long ordenId, @AuthenticationPrincipal Jwt jwt) { return service.buscarPorOrden(ordenId, usuarioId(jwt)); }
    @GetMapping("/presupuestos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')")
    public List<PresupuestoResponse> todos() { return service.listarTodos(); }
    @GetMapping("/presupuestos/me")
    @PreAuthorize("hasRole('TECNICO')")
    public List<PresupuestoResponse> asignados(@AuthenticationPrincipal Jwt jwt) {
        return service.listarAsignados(usuarioId(jwt));
    }
    @PutMapping("/presupuestos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'TECNICO')")
    public PresupuestoResponse editar(@PathVariable Long id, @Valid @RequestBody CrearPresupuestoRequest request, @AuthenticationPrincipal Jwt jwt) { return service.editar(id, request, usuarioId(jwt)); }
    @PostMapping("/presupuestos/{id}/publicar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    public PresupuestoResponse publicar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) { return service.publicar(id, usuarioId(jwt)); }
    @GetMapping("/clientes/me/presupuestos")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<PresupuestoResponse> propios(@AuthenticationPrincipal Jwt jwt) { return service.listarPropios(usuarioId(jwt)); }
    @PostMapping("/presupuestos/{id}/decision")
    @PreAuthorize("hasRole('CLIENTE')")
    public PresupuestoResponse decidir(@PathVariable Long id, @Valid @RequestBody DecisionPresupuestoRequest request, @AuthenticationPrincipal Jwt jwt) { return service.decidir(id, request, usuarioId(jwt)); }
    private Long usuarioId(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
