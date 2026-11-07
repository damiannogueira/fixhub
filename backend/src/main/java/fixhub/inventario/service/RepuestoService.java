package fixhub.inventario.service;

import fixhub.inventario.dto.ActualizarRepuestoRequest;
import fixhub.inventario.dto.CrearRepuestoRequest;
import fixhub.inventario.dto.RepuestoResponse;
import fixhub.inventario.entity.Repuesto;
import fixhub.inventario.exception.CodigoRepuestoDuplicadoException;
import fixhub.inventario.exception.RepuestoNoEncontradoException;
import fixhub.inventario.repository.RepuestoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class RepuestoService {

    private final RepuestoRepository repuestoRepository;

    public RepuestoService(RepuestoRepository repuestoRepository) {
        this.repuestoRepository = repuestoRepository;
    }

    public RepuestoResponse crear(CrearRepuestoRequest request) {
        String codigo = normalizarCodigo(request.codigo());

        if (repuestoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new CodigoRepuestoDuplicadoException(codigo);
        }

        Repuesto repuesto = new Repuesto(
            codigo,
            request.nombre().trim(),
            normalizarTextoOpcional(request.descripcion()),
            request.stockMinimo(),
            request.precioCosto(),
            request.precioVenta()
        );

        return convertirAResponse(repuestoRepository.save(repuesto));
    }

    @Transactional(readOnly = true)
    public List<RepuestoResponse> listar(String nombre) {
        List<Repuesto> repuestos;

        if (nombre == null || nombre.isBlank()) {
            repuestos = repuestoRepository.findAllByActivoTrueOrderByNombreAsc();
        } else {
            repuestos =
                repuestoRepository
                    .findByNombreContainingIgnoreCaseAndActivoTrueOrderByNombreAsc(
                        nombre.trim()
                    );
        }

        return repuestos.stream()
            .map(this::convertirAResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public RepuestoResponse buscarPorId(Long id) {
        return convertirAResponse(buscarEntidadActiva(id));
    }

    public RepuestoResponse actualizar(
        Long id,
        ActualizarRepuestoRequest request
    ) {
        Repuesto repuesto = buscarEntidadActiva(id);
        String codigo = normalizarCodigo(request.codigo());

        if (repuestoRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new CodigoRepuestoDuplicadoException(codigo);
        }

        repuesto.setCodigo(codigo);
        repuesto.setNombre(request.nombre().trim());
        repuesto.setDescripcion(
            normalizarTextoOpcional(request.descripcion())
        );
        repuesto.setStockMinimo(request.stockMinimo());
        repuesto.setPrecioCosto(request.precioCosto());
        repuesto.setPrecioVenta(request.precioVenta());

        return convertirAResponse(repuestoRepository.save(repuesto));
    }

    public void desactivar(Long id) {
        Repuesto repuesto = buscarEntidadActiva(id);
        repuesto.setActivo(false);
        repuestoRepository.save(repuesto);
    }

    @Transactional(readOnly = true)
    public List<RepuestoResponse> listarConStockBajo() {
        return repuestoRepository.findRepuestosConStockBajo().stream()
            .map(this::convertirAResponse)
            .toList();
    }

    private Repuesto buscarEntidadActiva(Long id) {
        return repuestoRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new RepuestoNoEncontradoException(id));
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTextoOpcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }

    private RepuestoResponse convertirAResponse(Repuesto repuesto) {
        return new RepuestoResponse(
            repuesto.getId(),
            repuesto.getCodigo(),
            repuesto.getNombre(),
            repuesto.getDescripcion(),
            repuesto.getStockActual(),
            repuesto.getStockMinimo(),
            repuesto.getPrecioCosto(),
            repuesto.getPrecioVenta(),
            repuesto.getActivo(),
            repuesto.getFechaCreacion(),
            repuesto.getFechaModificacion()
        );
    }
}
