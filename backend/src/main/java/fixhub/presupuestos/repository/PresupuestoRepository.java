package fixhub.presupuestos.repository;

import fixhub.presupuestos.entity.Presupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {
    Optional<Presupuesto> findByOrdenId(Long ordenId);
    List<Presupuesto> findAllByOrderByFechaCreacionDesc();
    List<Presupuesto> findAllByOrdenEquipoClienteUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
}
