package fixhub.autenticacion.repository;

import fixhub.autenticacion.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByTelefono(String telefono);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTelefono(String telefono);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByEmailIgnoreCaseOrTelefono(
        String email,
        String telefono
    );
}
