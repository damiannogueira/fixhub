package fixhub.inventario.repository;

import fixhub.inventario.entity.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    Optional<Repuesto> findByIdAndActivoTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Repuesto r WHERE r.id = :id AND r.activo = true")
    Optional<Repuesto> findActiveForUpdate(@Param("id") Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<Repuesto> findAllByActivoTrueOrderByNombreAsc();

    // Catálogo completo (activos e inactivos) para el Administrador.
    List<Repuesto> findAllByOrderByActivoDescNombreAsc();

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
