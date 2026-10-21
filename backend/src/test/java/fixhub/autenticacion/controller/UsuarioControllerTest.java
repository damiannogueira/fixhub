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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import fixhub.autenticacion.exception.UsuarioSinRolesException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

    @Test
    void administradorDebePoderDesactivarUsuario()
        throws Exception {

        mockMvc.perform(
                patch("/api/usuarios/42/desactivar")
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
            .andExpect(status().isNoContent());

        verify(usuarioService).desactivar(42L);
    }

    @Test
    void administradorDebePoderActivarUsuario()
        throws Exception {

        mockMvc.perform(
                patch("/api/usuarios/42/activar")
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
            .andExpect(status().isNoContent());

        verify(usuarioService).activar(42L);
    }

    @Test
    void usuarioSinRolAdministradorNoDebeDesactivarUsuarios()
        throws Exception {

        mockMvc.perform(
                patch("/api/usuarios/42/desactivar")
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
    void debeResponderNotFoundAlDesactivarUsuarioInexistente()
        throws Exception {

        org.mockito.Mockito.doThrow(
            new UsuarioNoEncontradoException(99L)
        )
            .when(usuarioService)
            .desactivar(99L);

        mockMvc.perform(
                patch("/api/usuarios/99/desactivar")
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

        verify(usuarioService).desactivar(99L);
    }

    @Test
    void administradorDebePoderAsignarRol()
        throws Exception {

        UsuarioResponse response = crearUsuarioResponse();

        when(
            usuarioService.asignarRol(
                42L,
                NombreRol.TECNICO
            )
        ).thenReturn(response);

        mockMvc.perform(
                put("/api/usuarios/42/roles/TECNICO")
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
                jsonPath("$.roles[0]")
                    .value("TECNICO")
            );

        verify(usuarioService).asignarRol(
            42L,
            NombreRol.TECNICO
        );
    }

    @Test
    void administradorDebePoderQuitarRol()
        throws Exception {

        UsuarioResponse response = crearUsuarioResponse();

        when(
            usuarioService.quitarRol(
                42L,
                NombreRol.CLIENTE
            )
        ).thenReturn(response);

        mockMvc.perform(
                delete("/api/usuarios/42/roles/CLIENTE")
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
                jsonPath("$.roles[0]")
                    .value("TECNICO")
            );

        verify(usuarioService).quitarRol(
            42L,
            NombreRol.CLIENTE
        );
    }

    @Test
    void usuarioSinRolAdministradorNoDebeAsignarRoles()
        throws Exception {

        mockMvc.perform(
                put("/api/usuarios/42/roles/TECNICO")
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
    void debeResponderConflictAlQuitarElUltimoRol()
        throws Exception {

        when(
            usuarioService.quitarRol(
                42L,
                NombreRol.CLIENTE
            )
        ).thenThrow(
            new UsuarioSinRolesException(42L)
        );

        mockMvc.perform(
                delete("/api/usuarios/42/roles/CLIENTE")
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
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.title").value(
                    "El usuario debe conservar al menos un rol"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "No se puede quitar el último rol "
                        + "del usuario con id 42"
                )
            );

        verify(usuarioService).quitarRol(
            42L,
            NombreRol.CLIENTE
        );
    }
}
