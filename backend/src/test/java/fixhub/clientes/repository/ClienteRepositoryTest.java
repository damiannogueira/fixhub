package fixhub.clientes.repository;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void debeGuardarYBuscarClientePorUsuario() {
        Usuario usuario = guardarUsuario("cliente@fixhub.com");
        Cliente cliente = clienteRepository.saveAndFlush(
            new Cliente(usuario, "Ana", "Pérez", null, null)
        );

        assertThat(clienteRepository.findByUsuarioId(usuario.getId()))
            .contains(cliente);
        assertThat(cliente.getUsuario().getId()).isEqualTo(usuario.getId());
    }

    @Test
    void noDebePermitirDosClientesParaElMismoUsuario() {
        Usuario usuario = guardarUsuario("unico@fixhub.com");
        clienteRepository.saveAndFlush(
            new Cliente(usuario, "Ana", "Pérez", null, null)
        );

        assertThatThrownBy(() -> clienteRepository.saveAndFlush(
            new Cliente(usuario, "Juan", "Pérez", null, null)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Usuario guardarUsuario(String email) {
        return usuarioRepository.saveAndFlush(
            new Usuario("hash", email, null)
        );
    }
}
