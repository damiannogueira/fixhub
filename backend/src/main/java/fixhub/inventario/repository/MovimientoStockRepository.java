package fixhub.inventario.repository;

import fixhub.inventario.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoStockRepository
    extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findAllByOrderByFechaDescIdDesc();

    List<MovimientoStock>
        findAllByRepuestoIdOrderByFechaDescIdDesc(Long repuestoId);
}
