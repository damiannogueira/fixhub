package fixhub.autenticacion.config;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicializadorRolesTest {

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private InicializadorRoles inicializadorRoles;

    @Test
    void debeCrearTodosLosRolesCuandoNoExisten() {
        inicializadorRoles.run(null);

        ArgumentCaptor<Rol> captor =
            ArgumentCaptor.forClass(Rol.class);

        verify(rolRepository, times(4))
            .save(captor.capture());

        List<NombreRol> rolesCreados =
            captor.getAllValues().stream()
                .map(Rol::getNombre)
                .toList();

        assertThat(rolesCreados)
            .containsExactlyInAnyOrder(
                NombreRol.ADMINISTRADOR,
                NombreRol.RECEPCIONISTA,
                NombreRol.TECNICO,
                NombreRol.CLIENTE
            );
    }

    @Test
    void noDebeCrearRolesQueYaExisten() {
        when(
            rolRepository.existsByNombre(
                any(NombreRol.class)
            )
        ).thenReturn(true);

        inicializadorRoles.run(null);

        verify(rolRepository, never())
            .save(any(Rol.class));
    }
}
