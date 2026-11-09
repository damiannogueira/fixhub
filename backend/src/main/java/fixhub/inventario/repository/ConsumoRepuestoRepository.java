package fixhub.inventario.repository;

import fixhub.inventario.entity.ConsumoRepuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsumoRepuestoRepository extends JpaRepository<ConsumoRepuesto, Long> {
    List<ConsumoRepuesto> findAllByOrdenIdOrderByFechaAscIdAsc(Long ordenId);
}
