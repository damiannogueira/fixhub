package fixhub.ordenes.repository;

import fixhub.ordenes.entity.HistorialOrden;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialOrdenRepository
    extends JpaRepository<HistorialOrden, Long> {
}
