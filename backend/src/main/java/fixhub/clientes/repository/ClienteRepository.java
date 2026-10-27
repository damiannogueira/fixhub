package fixhub.clientes.repository;

import fixhub.clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByUsuarioId(Long usuarioId);

    Optional<Cliente> findByUsuarioId(Long usuarioId);

    List<Cliente> findAllByOrderByApellidoAscNombreAscIdAsc();
}
