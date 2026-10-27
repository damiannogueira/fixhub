package fixhub.clientes.repository;

import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.entity.Cliente;
import fixhub.clientes.entity.Equipo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EquipoRepositoryTest {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void debeListarEquiposDelClienteYVerificarPropiedad() {
        Cliente primero = guardarCliente("primero@fixhub.com", "Ana");
        Cliente segundo = guardarCliente("segundo@fixhub.com", "Juan");
        Equipo notebook = new Equipo(
            primero, "Notebook", "Dell", "XPS", null, null
        );
        Equipo celular = new Equipo(
            primero, "Celular", "Samsung", "S24", null, null
        );
        Equipo ajeno = new Equipo(
            segundo, "Tablet", "Apple", "iPad", null, null
        );
        equipoRepository.saveAllAndFlush(
            List.of(notebook, celular, ajeno)
        );

        assertThat(
            equipoRepository.findAllByClienteIdOrderByIdAsc(
                primero.getId()
            )
        ).containsExactly(notebook, celular);
        assertThat(
            equipoRepository.findByIdAndClienteUsuarioId(
                notebook.getId(),
                primero.getUsuario().getId()
            )
        ).contains(notebook);
        assertThat(
            equipoRepository.findByIdAndClienteUsuarioId(
                ajeno.getId(),
                primero.getUsuario().getId()
            )
        ).isEmpty();
    }

    private Cliente guardarCliente(String email, String nombre) {
        Usuario usuario = usuarioRepository.saveAndFlush(
            new Usuario("hash", email, null)
        );
        return clienteRepository.saveAndFlush(
            new Cliente(usuario, nombre, "Cliente", null, null)
        );
    }
}
