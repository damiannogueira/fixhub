package fixhub.autenticacion.repository;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(NombreRol nombre);
}
