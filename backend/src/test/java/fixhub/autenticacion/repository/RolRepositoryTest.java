package fixhub.autenticacion.repository;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RolRepositoryTest {

    @Autowired
    private RolRepository rolRepository;

    @Test
    void debeBuscarRolPorNombre() {
        Rol rol = new Rol(NombreRol.CLIENTE);
        rolRepository.saveAndFlush(rol);

        var resultado = rolRepository.findByNombre(NombreRol.CLIENTE);

        assertThat(resultado)
            .isPresent()
            .contains(rol);
    }
}
