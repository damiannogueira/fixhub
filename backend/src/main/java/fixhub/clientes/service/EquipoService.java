package fixhub.clientes.service;

import fixhub.clientes.dto.ActualizarEquipoRequest;
import fixhub.clientes.dto.CrearEquipoRequest;
import fixhub.clientes.dto.EquipoResponse;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import fixhub.clientes.exception.EquipoNoEncontradoException;
import fixhub.clientes.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final ClienteService clienteService;

    public EquipoService(
        EquipoRepository equipoRepository,
        ClienteService clienteService
    ) {
        this.equipoRepository = equipoRepository;
        this.clienteService = clienteService;
    }

    @Transactional
    public EquipoResponse crear(
        Long clienteId,
        CrearEquipoRequest request
    ) {
        Cliente cliente = clienteService.buscarEntidadPorId(clienteId);
        Equipo equipo = new Equipo(
            cliente,
            request.tipo().trim(),
            normalizarTextoOpcional(request.marca()),
            normalizarTextoOpcional(request.modelo()),
            normalizarTextoOpcional(request.numeroSerie()),
            normalizarTextoOpcional(request.observaciones())
        );

        return convertirAResponse(equipoRepository.save(equipo));
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listarPorCliente(Long clienteId) {
        clienteService.buscarEntidadPorId(clienteId);

        return equipoRepository
            .findAllByClienteIdOrderByIdAsc(clienteId)
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse buscarPorId(Long id) {
        return convertirAResponse(buscarEntidadPorId(id));
    }

    @Transactional
    public EquipoResponse actualizar(
        Long id,
        ActualizarEquipoRequest request
    ) {
        Equipo equipo = buscarEntidadPorId(id);
        equipo.setTipo(request.tipo().trim());
        equipo.setMarca(normalizarTextoOpcional(request.marca()));
        equipo.setModelo(normalizarTextoOpcional(request.modelo()));
        equipo.setNumeroSerie(
            normalizarTextoOpcional(request.numeroSerie())
        );
        equipo.setObservaciones(
            normalizarTextoOpcional(request.observaciones())
        );

        return convertirAResponse(equipoRepository.save(equipo));
    }

    @Transactional(readOnly = true)
    public List<EquipoResponse> listarPropios(Long usuarioId) {
        Cliente cliente = clienteService.buscarEntidadPorUsuarioId(usuarioId);

        return equipoRepository
            .findAllByClienteIdOrderByIdAsc(cliente.getId())
            .stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public EquipoResponse buscarPropio(Long id, Long usuarioId) {
        // Se busca por equipo y usuario para no permitir ni revelar
        // equipos de otros clientes.
        Equipo equipo = equipoRepository
            .findByIdAndClienteUsuarioId(id, usuarioId)
            .orElseThrow(() -> new EquipoNoEncontradoException(id));

        return convertirAResponse(equipo);
    }

    @Transactional(readOnly = true)
    public Equipo buscarEntidadPorId(Long id) {
        return equipoRepository.findById(id)
            .orElseThrow(() -> new EquipoNoEncontradoException(id));
    }

    private EquipoResponse convertirAResponse(Equipo equipo) {
        return new EquipoResponse(
            equipo.getId(),
            equipo.getCliente().getId(),
            equipo.getTipo(),
            equipo.getMarca(),
            equipo.getModelo(),
            equipo.getNumeroSerie(),
            equipo.getObservaciones()
        );
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}
