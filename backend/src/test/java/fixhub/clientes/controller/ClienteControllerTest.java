package fixhub.clientes.controller;

import fixhub.clientes.dto.ClienteResponse;
import fixhub.clientes.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "RECEPCIONISTA"})
    void rolesAdministrativosDebenListarClientes(String rol)
        throws Exception {
        when(clienteService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/clientes").with(jwtConRol(rol, 42L)))
            .andExpect(status().isOk());

        verify(clienteService).listar();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "RECEPCIONISTA"})
    void rolesAdministrativosDebenCrearCuentaCliente(String rol)
        throws Exception {
        when(clienteService.crearConCuenta(any()))
            .thenReturn(response());

        mockMvc.perform(
                post("/api/clientes/con-cuenta")
                    .with(jwtConRol(rol, 42L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoConCuenta())
            )
            .andExpect(status().isCreated());

        verify(clienteService).crearConCuenta(any());
    }

    @Test
    void clienteDebeConsultarSuPropioRegistro() throws Exception {
        when(clienteService.buscarPropio(42L)).thenReturn(response());

        mockMvc.perform(
                get("/api/clientes/me")
                    .with(jwtConRol("CLIENTE", 42L))
            )
            .andExpect(status().isOk());

        verify(clienteService).buscarPropio(42L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "TECNICO"})
    void rolesNoAdministrativosNoDebenListarClientes(String rol)
        throws Exception {
        mockMvc.perform(get("/api/clientes").with(jwtConRol(rol, 42L)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(clienteService);
    }

    @Test
    void usuarioNoAutenticadoNoDebeAcceder() throws Exception {
        mockMvc.perform(get("/api/clientes"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(clienteService);
    }

    private RequestPostProcessor jwtConRol(String rol, Long usuarioId) {
        return jwt()
            .jwt(token -> token
                .subject(usuarioId.toString())
                .claim("requiereCambioClave", false)
            )
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private String cuerpoConCuenta() {
        return """
            {
                "email": "cliente@fixhub.com",
                "claveTemporal": "Temporal123",
                "nombre": "Ana",
                "apellido": "Pérez"
            }
            """;
    }

    private ClienteResponse response() {
        return new ClienteResponse(
            1L, "Ana", "Pérez", null, null,
            42L, "cliente@fixhub.com", null
        );
    }
}
