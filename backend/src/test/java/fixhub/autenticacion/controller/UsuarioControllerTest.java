package fixhub.autenticacion.controller;

import fixhub.autenticacion.dto.UsuarioResponse;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.exception.UsuarioNoEncontradoException;
import fixhub.autenticacion.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void administradorDebePoderListarUsuarios() throws Exception {
        UsuarioResponse usuario = crearUsuarioResponse();

        when(usuarioService.listar())
            .thenReturn(List.of(usuario));

        mockMvc.perform(
                get("/api/usuarios")
                    .with(jwt()
                        .jwt(token -> token
                            .subject("1")
                            .claim(
                                "requiereCambioClave",
                                false
                            )
                        )
                        .authorities(
                            new SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(42))
            .andExpect(
                jsonPath("$[0].email")
                    .value("usuario@fixhub.com")
            )
            .andExpect(
                jsonPath("$[0].roles[0]")
                    .value("TECNICO")
            );

        verify(usuarioService).listar();
    }

    @Test
    void administradorDebePoderBuscarUsuarioPorId()
        throws Exception {

        UsuarioResponse usuario = crearUsuarioResponse();

        when(usuarioService.buscarPorId(42L))
            .thenReturn(usuario);

        mockMvc.perform(
                get("/api/usuarios/42")
                    .with(jwt()
                        .jwt(token -> token
                            .subject("1")
                            .claim(
                                "requiereCambioClave",
                                false
                            )
                        )
                        .authorities(
                            new SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"
                            )
                        )
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(
                jsonPath("$.email")
                    .value("usuario@fixhub.com")
            );

        verify(usuarioService).buscarPorId(42L);
    }

    @Test
    void usuarioSinRolAdministradorNoDebeListarUsuarios()
        throws Exception {

        mockMvc.perform(
                get("/api/usuarios")
                    .with(jwt()
                        .jwt(token -> token
                            .subject("2")
                            .claim(
                                "requiereCambioClave",
                                false
                            )
                        )
                        .authorities(
                            new SimpleGrantedAuthority(
                                "ROLE_TECNICO"
                            )
                        )
                    )
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void usuarioNoAutenticadoNoDebeListarUsuarios()
        throws Exception {

        mockMvc.perform(get("/api/usuarios"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void debeResponderNotFoundSiElUsuarioNoExiste()
        throws Exception {

        when(usuarioService.buscarPorId(99L))
            .thenThrow(
                new UsuarioNoEncontradoException(99L)
            );

        mockMvc.perform(
                get("/api/usuarios/99")
                    .with(jwt()
                        .jwt(token -> token
                            .subject("1")
                            .claim(
                                "requiereCambioClave",
                                false
                            )
                        )
                        .authorities(
                            new SimpleGrantedAuthority(
                                "ROLE_ADMINISTRADOR"
                            )
                        )
                    )
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.title")
                    .value("Usuario no encontrado")
            );

        verify(usuarioService).buscarPorId(99L);
    }

    private UsuarioResponse crearUsuarioResponse() {
        return new UsuarioResponse(
            42L,
            "usuario@fixhub.com",
            "+5493415551234",
            true,
            false,
            LocalDateTime.of(2026, 10, 20, 12, 0),
            Set.of(NombreRol.TECNICO)
        );
    }
}
