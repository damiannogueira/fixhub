package fixhub.ordenes.repository;

import fixhub.ordenes.entity.RegistroTecnico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroTecnicoRepository
    extends JpaRepository<RegistroTecnico, Long> {

    List<RegistroTecnico>
        findAllByOrdenIdOrderByFechaAscIdAsc(Long ordenId);
}
