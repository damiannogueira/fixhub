package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.ordenes.dto.ActualizarDiagnosticoRequest;
import fixhub.ordenes.dto.CrearDiagnosticoRequest;
import fixhub.ordenes.dto.DiagnosticoResponse;
import fixhub.ordenes.dto.DiagnosticoClienteResponse;
import fixhub.ordenes.entity.Diagnostico;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.exception.DiagnosticoNoEncontradoException;
import fixhub.ordenes.exception.DiagnosticoYaRegistradoException;
import fixhub.ordenes.exception.EstadoOrdenInvalidoParaDiagnosticoException;
import fixhub.ordenes.repository.DiagnosticoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DiagnosticoService {

    private final DiagnosticoRepository diagnosticoRepository;
    private final OrdenTrabajoService ordenTrabajoService;
    private final UsuarioService usuarioService;

    public DiagnosticoService(
        DiagnosticoRepository diagnosticoRepository,
        OrdenTrabajoService ordenTrabajoService,
        UsuarioService usuarioService
    ) {
        this.diagnosticoRepository = diagnosticoRepository;
        this.ordenTrabajoService = ordenTrabajoService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public DiagnosticoResponse crear(
        Long ordenId,
        CrearDiagnosticoRequest request,
        Long actorId
    ) {
        OrdenTrabajo orden =
            ordenTrabajoService.buscarEntidadPorId(ordenId);

        Usuario actor =
            usuarioService.buscarEntidadPorId(actorId);

        validarGestionTecnica(orden, actor);

        if (diagnosticoRepository.existsByOrdenId(ordenId)) {
            throw new DiagnosticoYaRegistradoException(ordenId);
        }

        Diagnostico diagnostico = diagnosticoRepository.save(
            new Diagnostico(
                orden,
                actor,
                request.descripcionFalla().trim(),
                request.tareasRequeridas().trim(),
                LocalDateTime.now()
            )
        );

        return convertirAResponse(diagnostico);
    }

    @Transactional
    public DiagnosticoResponse actualizar(
        Long ordenId,
        ActualizarDiagnosticoRequest request,
        Long actorId
    ) {
        OrdenTrabajo orden =
            ordenTrabajoService.buscarEntidadPorId(ordenId);

        Usuario actor =
            usuarioService.buscarEntidadPorId(actorId);

        validarGestionTecnica(orden, actor);

        Diagnostico diagnostico = diagnosticoRepository
            .findByOrdenId(ordenId)
            .orElseThrow(
                () -> new DiagnosticoNoEncontradoException(ordenId)
            );

        diagnostico.setUsuario(actor);
        diagnostico.setDescripcionFalla(
            request.descripcionFalla().trim()
        );
        diagnostico.setTareasRequeridas(
            request.tareasRequeridas().trim()
        );
        diagnostico.setFecha(LocalDateTime.now());

        Diagnostico diagnosticoGuardado =
            diagnosticoRepository.save(diagnostico);

        return convertirAResponse(diagnosticoGuardado);
    }

    @Transactional(readOnly = true)
    public DiagnosticoResponse buscarPorOrden(Long ordenId) {
        ordenTrabajoService.buscarEntidadPorId(ordenId);

        Diagnostico diagnostico = diagnosticoRepository
            .findByOrdenId(ordenId)
            .orElseThrow(
                () -> new DiagnosticoNoEncontradoException(ordenId)
            );

        return convertirAResponse(diagnostico);
    }

    @Transactional(readOnly = true)
    public DiagnosticoResponse buscarAsignado(
        Long ordenId,
        Long tecnicoId
    ) {
        ordenTrabajoService.buscarAsignada(
            ordenId,
            tecnicoId
        );

        return buscarPorOrden(ordenId);
    }

    @Transactional(readOnly = true)
    public DiagnosticoClienteResponse buscarPropioCliente(
        Long ordenId,
        Long usuarioId
    ) {
        ordenTrabajoService.buscarPropiaCliente(
            ordenId,
            usuarioId
        );

        Diagnostico diagnostico = diagnosticoRepository
            .findByOrdenId(ordenId)
            .orElseThrow(
                () -> new DiagnosticoNoEncontradoException(ordenId)
            );

        return convertirAResponseCliente(diagnostico);
    }

    private void validarGestionTecnica(
        OrdenTrabajo orden,
        Usuario actor
    ) {
        Usuario tecnicoAsignado = orden.getTecnico();

        if (
            tecnicoAsignado == null
                || !actor.tieneRol(NombreRol.TECNICO)
                || !tecnicoAsignado.getId().equals(actor.getId())
        ) {
            throw new AccessDeniedException(
                "El diagnóstico debe gestionarlo el Técnico asignado"
            );
        }

        NombreEstadoOrden estado =
            orden.getEstado().getNombre();

        if (estado != NombreEstadoOrden.EN_DIAGNOSTICO) {
            throw new EstadoOrdenInvalidoParaDiagnosticoException(
                estado
            );
        }
    }

    private DiagnosticoResponse convertirAResponse(
        Diagnostico diagnostico
    ) {
        return new DiagnosticoResponse(
            diagnostico.getId(),
            diagnostico.getOrden().getId(),
            diagnostico.getUsuario().getId(),
            diagnostico.getDescripcionFalla(),
            diagnostico.getTareasRequeridas(),
            diagnostico.getFecha()
        );
    }

    private DiagnosticoClienteResponse convertirAResponseCliente(
        Diagnostico diagnostico
    ) {
        return new DiagnosticoClienteResponse(
            diagnostico.getId(),
            diagnostico.getOrden().getId(),
            diagnostico.getDescripcionFalla(),
            diagnostico.getTareasRequeridas(),
            diagnostico.getFecha()
        );
    }
}
