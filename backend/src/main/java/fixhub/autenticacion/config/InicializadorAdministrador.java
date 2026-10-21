package fixhub.autenticacion.config;

import fixhub.autenticacion.dto.CrearUsuarioRequest;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.autenticacion.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Component
@Order(2)
@ConditionalOnProperty(
    name = "app.bootstrap.admin.enabled",
    havingValue = "true"
)
public class InicializadorAdministrador
    implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final String email;
    private final String password;

    public InicializadorAdministrador(
        UsuarioRepository usuarioRepository,
        UsuarioService usuarioService,
        @Value("${app.bootstrap.admin.email:}") String email,
        @Value("${app.bootstrap.admin.password:}") String password
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validarConfiguracion();

        String emailNormalizado =
            email.trim().toLowerCase();

        Optional<Usuario> usuarioExistente =
            usuarioRepository.findByEmailIgnoreCase(
                emailNormalizado
            );

        if (usuarioExistente.isPresent()) {
            validarAdministradorExistente(
                usuarioExistente.get()
            );

            return;
        }

        usuarioService.crear(
            new CrearUsuarioRequest(
                emailNormalizado,
                null,
                password,
                Set.of(NombreRol.ADMINISTRADOR)
            )
        );
    }

    private void validarConfiguracion() {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                "Debe configurarse el email "
                    + "del administrador inicial"
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalStateException(
                "La contraseña del administrador inicial "
                    + "debe tener al menos 8 caracteres"
            );
        }
    }

    private void validarAdministradorExistente(
        Usuario usuario
    ) {
        if (!usuario.tieneRol(NombreRol.ADMINISTRADOR)) {
            throw new IllegalStateException(
                "El email configurado para el administrador "
                    + "ya pertenece a otro usuario"
            );
        }
    }
}
