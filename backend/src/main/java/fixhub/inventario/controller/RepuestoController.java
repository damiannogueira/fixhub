package fixhub.inventario.controller;

import fixhub.inventario.dto.ActualizarRepuestoRequest;
import fixhub.inventario.dto.CrearRepuestoRequest;
import fixhub.inventario.dto.RepuestoResponse;
import fixhub.inventario.service.RepuestoService;
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

import java.util.List;

@RestController
@RequestMapping("/api/repuestos")
public class RepuestoController {

    private final RepuestoService repuestoService;

    public RepuestoController(RepuestoService repuestoService) {
        this.repuestoService = repuestoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RepuestoResponse crear(
        @Valid @RequestBody CrearRepuestoRequest request
    ) {
        return repuestoService.crear(request);
    }

    @GetMapping
    public List<RepuestoResponse> listar(
        @RequestParam(required = false) String nombre
    ) {
        return repuestoService.listar(nombre);
    }

    @GetMapping("/stock-bajo")
    public List<RepuestoResponse> listarConStockBajo() {
        return repuestoService.listarConStockBajo();
    }

    @GetMapping("/{id}")
    public RepuestoResponse buscarPorId(@PathVariable Long id) {
        return repuestoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public RepuestoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarRepuestoRequest request
    ) {
        return repuestoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        repuestoService.desactivar(id);
    }
}