package fixhub.ordenes.repository;

import fixhub.ordenes.entity.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenTrabajoRepository
    extends JpaRepository<OrdenTrabajo, Long> {

    List<OrdenTrabajo> findAllByOrderByFechaIngresoDescIdDesc();

    List<OrdenTrabajo> findAllByTecnicoIdOrderByFechaIngresoDescIdDesc(
        Long tecnicoId
    );

    Optional<OrdenTrabajo> findByIdAndTecnicoId(
        Long id,
        Long tecnicoId
    );

    List<OrdenTrabajo>
        findAllByEquipoClienteUsuarioIdOrderByFechaIngresoDescIdDesc(
            Long usuarioId
        );

    Optional<OrdenTrabajo> findByIdAndEquipoClienteUsuarioId(
        Long id,
        Long usuarioId
    );
}
