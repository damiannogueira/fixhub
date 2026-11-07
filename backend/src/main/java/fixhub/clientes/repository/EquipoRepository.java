package fixhub.clientes.repository;

import fixhub.clientes.entity.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findAllByClienteIdOrderByIdAsc(Long clienteId);

    Optional<Equipo> findByIdAndClienteUsuarioId(
        Long equipoId,
        Long usuarioId
    );
}
