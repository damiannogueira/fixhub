package fixhub.autenticacion.repository;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;

import java.util.List;

@SpringBootTest
@Transactional
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void debeBuscarUsuarioPorEmailSinDistinguirMayusculas() {
        Usuario usuario = crearUsuario(
            "usuario@fixhub.com",
            null
        );

        usuarioRepository.saveAndFlush(usuario);

        var resultado =
            usuarioRepository.findByEmailIgnoreCase("USUARIO@FIXHUB.COM");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEmail())
            .isEqualTo("usuario@fixhub.com");
    }

    @Test
    void debeBuscarUsuarioPorTelefono() {
        Usuario usuario = crearUsuario(
            null,
            "+5493415551234"
        );

        usuarioRepository.saveAndFlush(usuario);

        var resultado =
            usuarioRepository.findByTelefono("+5493415551234");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTelefono())
            .isEqualTo("+5493415551234");
    }

    @Test
    void debeBuscarPorIdentificadorYCargarRoles() {
        Rol rolCliente = rolRepository.saveAndFlush(
            new Rol(NombreRol.CLIENTE)
        );

        Usuario usuario = crearUsuario(
            "cliente@fixhub.com",
            "+5493415555678"
        );

        usuario.agregarRol(rolCliente);
        usuarioRepository.saveAndFlush(usuario);

        var resultado =
            usuarioRepository.findByEmailIgnoreCaseOrTelefono(
                "CLIENTE@FIXHUB.COM",
                "CLIENTE@FIXHUB.COM"
            );

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getRoles())
            .extracting(Rol::getNombre)
            .containsExactly(NombreRol.CLIENTE);
    }

    @Test
    void debeDetectarEmailYTelefonoExistentes() {
        Usuario usuario = crearUsuario(
            "existente@fixhub.com",
            "+5493415559999"
        );

        usuarioRepository.saveAndFlush(usuario);

        assertThat(
            usuarioRepository.existsByEmailIgnoreCase(
                "EXISTENTE@FIXHUB.COM"
            )
        ).isTrue();

        assertThat(
            usuarioRepository.existsByTelefono("+5493415559999")
        ).isTrue();
    }

    private Usuario crearUsuario(String email, String telefono) {
        return new Usuario(
            "$2a$10$hashDePrueba",
            email,
            telefono
        );
    }

    @Test
    void debeListarUsuariosActivosEInactivosOrdenadosPorId() {
        Rol rolAdministrador = rolRepository.saveAndFlush(
            new Rol(NombreRol.ADMINISTRADOR)
        );

        Rol rolTecnico = rolRepository.saveAndFlush(
            new Rol(NombreRol.TECNICO)
        );

        Usuario administrador = crearUsuario(
            "admin@fixhub.com",
            null
        );

        administrador.agregarRol(rolAdministrador);

        Usuario tecnico = crearUsuario(
            "tecnico@fixhub.com",
            null
        );

        tecnico.agregarRol(rolTecnico);
        tecnico.setActivo(false);

        usuarioRepository.saveAndFlush(administrador);
        usuarioRepository.saveAndFlush(tecnico);

        entityManager.clear();

        List<Usuario> resultado =
            usuarioRepository.findAllByOrderByIdAsc();

        assertThat(resultado)
            .extracting(Usuario::getEmail)
            .containsExactly(
                "admin@fixhub.com",
                "tecnico@fixhub.com"
            );

        assertThat(resultado)
            .extracting(Usuario::isActivo)
            .containsExactly(true, false);

        assertThat(resultado)
            .allSatisfy(usuario ->
                assertThat(Hibernate.isInitialized(usuario.getRoles()))
                    .isTrue()
            );
    }

    @Test
    void debeBuscarUsuarioPorIdYCargarSusRoles() {
        Rol rolCliente = rolRepository.saveAndFlush(
            new Rol(NombreRol.CLIENTE)
        );

        Usuario usuario = crearUsuario(
            "cliente@fixhub.com",
            null
        );

        usuario.agregarRol(rolCliente);

        Usuario guardado =
            usuarioRepository.saveAndFlush(usuario);

        Long usuarioId = guardado.getId();

        entityManager.clear();

        var resultado = usuarioRepository.findById(usuarioId);

        assertThat(resultado).isPresent();

        assertThat(
            Hibernate.isInitialized(resultado.get().getRoles())
        ).isTrue();

        assertThat(resultado.get().getRoles())
            .extracting(Rol::getNombre)
            .containsExactly(NombreRol.CLIENTE);
    }
}
