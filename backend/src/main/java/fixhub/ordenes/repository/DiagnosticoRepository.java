package fixhub.ordenes.repository;

import fixhub.ordenes.entity.Diagnostico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiagnosticoRepository
    extends JpaRepository<Diagnostico, Long> {

    Optional<Diagnostico> findByOrdenId(Long ordenId);

    boolean existsByOrdenId(Long ordenId);
}
