package fixhub.ordenes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.service.UsuarioService;
import fixhub.ordenes.dto.CrearRegistroTecnicoRequest;
import fixhub.ordenes.dto.RegistroTecnicoResponse;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.entity.OrdenTrabajo;
import fixhub.ordenes.entity.RegistroTecnico;
import fixhub.ordenes.exception.EstadoOrdenInvalidoParaRegistroTecnicoException;
import fixhub.ordenes.repository.RegistroTecnicoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistroTecnicoService {

    private final RegistroTecnicoRepository
        registroTecnicoRepository;
    private final OrdenTrabajoService ordenTrabajoService;
    private final UsuarioService usuarioService;

    public RegistroTecnicoService(
        RegistroTecnicoRepository registroTecnicoRepository,
        OrdenTrabajoService ordenTrabajoService,
        UsuarioService usuarioService
    ) {
        this.registroTecnicoRepository =
            registroTecnicoRepository;
        this.ordenTrabajoService = ordenTrabajoService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public RegistroTecnicoResponse crear(
        Long ordenId,
        CrearRegistroTecnicoRequest request,
        Long actorId
    ) {
        OrdenTrabajo orden =
            ordenTrabajoService.buscarEntidadPorId(ordenId);

        Usuario actor =
            usuarioService.buscarEntidadPorId(actorId);

        validarRegistroTecnico(orden, actor);

        RegistroTecnico registro =
            registroTecnicoRepository.save(
                new RegistroTecnico(
                    orden,
                    actor,
                    request.tareaRealizada().trim(),
                    normalizarObservacion(
                        request.observacion()
                    ),
                    LocalDateTime.now()
                )
            );

        return convertirAResponse(registro);
    }

    @Transactional(readOnly = true)
    public List<RegistroTecnicoResponse> listarPorOrden(
        Long ordenId
    ) {
        ordenTrabajoService.buscarEntidadPorId(ordenId);

        return buscarRegistros(ordenId);
    }

    @Transactional(readOnly = true)
    public List<RegistroTecnicoResponse> listarAsignados(
        Long ordenId,
        Long tecnicoId
    ) {
        ordenTrabajoService.buscarAsignada(
            ordenId,
            tecnicoId
        );

        return buscarRegistros(ordenId);
    }

    private List<RegistroTecnicoResponse> buscarRegistros(
        Long ordenId
    ) {
        return registroTecnicoRepository
            .findAllByOrdenIdOrderByFechaAscIdAsc(ordenId)
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    private void validarRegistroTecnico(
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
                "El registro debe realizarlo el Técnico asignado"
            );
        }

        NombreEstadoOrden estado =
            orden.getEstado().getNombre();

        if (estado != NombreEstadoOrden.EN_REPARACION) {
            throw new
                EstadoOrdenInvalidoParaRegistroTecnicoException(
                    estado
                );
        }
    }

    private String normalizarObservacion(String observacion) {
        if (
            observacion == null
                || observacion.isBlank()
        ) {
            return null;
        }

        return observacion.trim();
    }

    private RegistroTecnicoResponse convertirAResponse(
        RegistroTecnico registro
    ) {
        return new RegistroTecnicoResponse(
            registro.getId(),
            registro.getOrden().getId(),
            registro.getUsuario().getId(),
            registro.getTareaRealizada(),
            registro.getObservacion(),
            registro.getFecha()
        );
    }
}
