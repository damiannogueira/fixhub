package fixhub.autenticacion.security;

import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String identificador;
    private final String passwordHash;
    private final boolean activo;
    private final boolean requiereCambioClave;
    private final Set<GrantedAuthority> authorities;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.identificador = obtenerIdentificador(usuario);
        this.passwordHash = usuario.getPasswordHash();
        this.activo = usuario.isActivo();
        this.requiereCambioClave =
            usuario.isRequiereCambioClave();

        this.authorities = usuario.getRoles().stream()
            .map(Rol::getNombre)
            .map(nombre -> "ROLE_" + nombre.name())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String obtenerIdentificador(Usuario usuario) {
        if (usuario.getEmail() != null) {
            return usuario.getEmail();
        }

        return usuario.getTelefono();
    }

    public Long getId() {
        return id;
    }

    public boolean isRequiereCambioClave() {
        return requiereCambioClave;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return identificador;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
