package fixhub.autenticacion.security;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioPrincipalTest {

    @Test
    void debeConvertirUsuarioEnPrincipalConSusRoles() {
        Usuario usuario = new Usuario(
            "$2a$10$hashDePrueba",
            "usuario@fixhub.com",
            null
        );

        usuario.agregarRol(
            new Rol(NombreRol.ADMINISTRADOR)
        );

        usuario.agregarRol(
            new Rol(NombreRol.TECNICO)
        );

        UsuarioPrincipal principal =
            new UsuarioPrincipal(usuario);

        assertThat(principal.getUsername())
            .isEqualTo("usuario@fixhub.com");

        assertThat(principal.getPassword())
            .isEqualTo("$2a$10$hashDePrueba");

        assertThat(principal.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder(
                "ROLE_ADMINISTRADOR",
                "ROLE_TECNICO"
            );

        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isRequiereCambioClave()).isTrue();
    }

    @Test
    void debeUsarTelefonoCuandoElUsuarioNoTieneEmail() {
        Usuario usuario = new Usuario(
            "$2a$10$hashDePrueba",
            null,
            "+5493415551234"
        );

        UsuarioPrincipal principal =
            new UsuarioPrincipal(usuario);

        assertThat(principal.getUsername())
            .isEqualTo("+5493415551234");
    }

    @Test
    void debeReflejarUsuarioInactivoYClaveYaCambiada() {
        Usuario usuario = new Usuario(
            "$2a$10$hashDePrueba",
            "usuario@fixhub.com",
            null
        );

        usuario.setActivo(false);
        usuario.setRequiereCambioClave(false);

        UsuarioPrincipal principal =
            new UsuarioPrincipal(usuario);

        assertThat(principal.isEnabled()).isFalse();

        assertThat(principal.isRequiereCambioClave())
            .isFalse();
    }
}
