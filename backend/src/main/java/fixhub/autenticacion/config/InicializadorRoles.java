package fixhub.autenticacion.config;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.repository.RolRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
@ConditionalOnProperty(
    name = "app.bootstrap.roles.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class InicializadorRoles implements ApplicationRunner {

    private final RolRepository rolRepository;

    public InicializadorRoles(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (NombreRol nombreRol : NombreRol.values()) {
            crearSiNoExiste(nombreRol);
        }
    }

    private void crearSiNoExiste(NombreRol nombreRol) {
        if (!rolRepository.existsByNombre(nombreRol)) {
            rolRepository.save(
                new Rol(nombreRol)
            );
        }
    }
}
