package fixhub.clientes.service;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.repository.RolRepository;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.dto.CrearClienteConCuentaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ClienteTransactionTest {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Test
    void fallaDeClienteNoDebeDejarCuentaHuerfana() {
        rolRepository.save(new Rol(NombreRol.CLIENTE));
        long usuariosIniciales = usuarioRepository.count();

        assertThatThrownBy(() -> clienteService.crearConCuenta(
            new CrearClienteConCuentaRequest(
                "rollback@fixhub.com",
                null,
                "Temporal123",
                "Cuenta",
                "Cliente",
                "1".repeat(300),
                null
            )
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(usuarioRepository.count()).isEqualTo(usuariosIniciales);
    }
}
