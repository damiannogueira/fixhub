package fixhub.autenticacion.security;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixhubUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public FixhubUserDetailsService(
        UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new UsernameNotFoundException(
                "Credenciales inválidas"
            );
        }

        String identificadorNormalizado = identificador.trim();

        Usuario usuario = usuarioRepository
            .findByEmailIgnoreCaseOrTelefono(
                identificadorNormalizado,
                identificadorNormalizado
            )
            .orElseThrow(
                () -> new UsernameNotFoundException(
                    "Credenciales inválidas"
                )
            );

        return new UsuarioPrincipal(usuario);
    }
}
