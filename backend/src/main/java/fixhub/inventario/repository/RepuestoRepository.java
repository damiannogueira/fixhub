package fixhub.inventario.repository;

import fixhub.inventario.entity.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    Optional<Repuesto> findByIdAndActivoTrue(Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<Repuesto> findAllByActivoTrueOrderByNombreAsc();

    List<Repuesto> findByNombreContainingIgnoreCaseAndActivoTrueOrderByNombreAsc(
        String nombre
    );

    @Query("""
        SELECT r
        FROM Repuesto r
        WHERE r.activo = true
            AND r.stockActual <= r.stockMinimo
        ORDER BY r.stockActual ASC, r.nombre ASC
        """)
    List<Repuesto> findRepuestosConStockBajo();
}
