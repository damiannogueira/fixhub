package fixhub.autenticacion.controller;

import fixhub.autenticacion.dto.UsuarioResponse;
import fixhub.autenticacion.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import fixhub.autenticacion.entity.NombreRol;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import fixhub.autenticacion.dto.CrearUsuarioRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> crear(
        @Valid @RequestBody CrearUsuarioRequest request
    ) {
        UsuarioResponse usuario = usuarioService.crear(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(usuario);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> buscarPorId(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(
            usuarioService.buscarPorId(id)
        );
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivar(
        @PathVariable Long id
    ) {
        usuarioService.desactivar(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> activar(
        @PathVariable Long id
    ) {
        usuarioService.activar(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles/{rol}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> asignarRol(
        @PathVariable Long id,
        @PathVariable NombreRol rol
    ) {
        return ResponseEntity.ok(
            usuarioService.asignarRol(id, rol)
        );
    }

    @DeleteMapping("/{id}/roles/{rol}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioResponse> quitarRol(
        @PathVariable Long id,
        @PathVariable NombreRol rol
    ) {
        return ResponseEntity.ok(
            usuarioService.quitarRol(id, rol)
        );
    }
}
