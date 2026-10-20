package fixhub.autenticacion.controller;

import fixhub.autenticacion.dto.LoginRequest;
import fixhub.autenticacion.dto.LoginResponse;
import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.service.AutenticacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticacionService autenticacionService;

    @Test
    void debePermitirLoginSinEstarAutenticado() throws Exception {
        LoginResponse response = new LoginResponse(
            42L,
            "usuario@fixhub.com",
            "jwt-generado",
            "Bearer",
            3600L,
            true,
            Set.of(NombreRol.ADMINISTRADOR)
        );

        when(
            autenticacionService.login(any(LoginRequest.class))
        ).thenReturn(response);

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identificador": "usuario@fixhub.com",
                      "clave": "ClaveTemporal123"
                    }
                    """)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.usuarioId").value(42)
            )
            .andExpect(
                jsonPath("$.identificador")
                    .value("usuario@fixhub.com")
            )
            .andExpect(
                jsonPath("$.token").value("jwt-generado")
            )
            .andExpect(
                jsonPath("$.tipo").value("Bearer")
            )
            .andExpect(
                jsonPath("$.expiraEnSegundos").value(3600)
            )
            .andExpect(
                jsonPath("$.requiereCambioClave").value(true)
            )
            .andExpect(
                jsonPath("$.roles[0]")
                    .value("ADMINISTRADOR")
            );
    }

    @Test
    void debeResponderUnauthorizedConCredencialesInvalidas()
        throws Exception {

        when(
            autenticacionService.login(any(LoginRequest.class))
        ).thenThrow(
            new BadCredentialsException(
                "Credenciales inválidas"
            )
        );

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identificador": "usuario@fixhub.com",
                      "clave": "ClaveIncorrecta"
                    }
                    """)
        )
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.title")
                    .value("Credenciales inválidas")
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "El identificador o la contraseña "
                        + "son incorrectos"
                )
            );
    }

    @Test
    void debeRechazarSolicitudDeLoginInvalida()
        throws Exception {

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "identificador": "",
                      "clave": ""
                    }
                    """)
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    void debeProtegerLosDemasEndpointsSinToken()
        throws Exception {

        mockMvc.perform(get("/api/repuestos"))
            .andExpect(status().isUnauthorized());
    }
}
