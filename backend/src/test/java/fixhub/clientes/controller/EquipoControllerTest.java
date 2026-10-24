package fixhub.clientes.controller;

import fixhub.clientes.dto.EquipoResponse;
import fixhub.clientes.service.EquipoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EquipoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipoService equipoService;

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "RECEPCIONISTA"})
    void rolesAdministrativosDebenCrearEquipo(String rol)
        throws Exception {
        when(equipoService.crear(eq(1L), any())).thenReturn(response());

        mockMvc.perform(
                post("/api/clientes/1/equipos")
                    .with(jwtConRol(rol, 42L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tipo\":\"Notebook\"}")
            )
            .andExpect(status().isCreated());

        verify(equipoService).crear(eq(1L), any());
    }

    @Test
    void clienteDebeListarYSolicitarEquiposPropios() throws Exception {
        when(equipoService.listarPropios(42L)).thenReturn(List.of());
        when(equipoService.buscarPropio(5L, 42L)).thenReturn(response());

        mockMvc.perform(
                get("/api/clientes/me/equipos")
                    .with(jwtConRol("CLIENTE", 42L))
            )
            .andExpect(status().isOk());
        mockMvc.perform(
                get("/api/clientes/me/equipos/5")
                    .with(jwtConRol("CLIENTE", 42L))
            )
            .andExpect(status().isOk());

        verify(equipoService).listarPropios(42L);
        verify(equipoService).buscarPropio(5L, 42L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "TECNICO"})
    void rolesSinGestionNoDebenCrearEquipos(String rol)
        throws Exception {
        mockMvc.perform(
                post("/api/clientes/1/equipos")
                    .with(jwtConRol(rol, 42L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tipo\":\"Notebook\"}")
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(equipoService);
    }

    @Test
    void tecnicoNoDebeConsultarEquiposEnGeneral() throws Exception {
        mockMvc.perform(
                get("/api/equipos/5")
                    .with(jwtConRol("TECNICO", 42L))
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(equipoService);
    }

    @Test
    void usuarioNoAutenticadoNoDebeAcceder() throws Exception {
        mockMvc.perform(get("/api/equipos/5"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(equipoService);
    }

    private RequestPostProcessor jwtConRol(String rol, Long usuarioId) {
        return jwt()
            .jwt(token -> token
                .subject(usuarioId.toString())
                .claim("requiereCambioClave", false)
            )
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private EquipoResponse response() {
        return new EquipoResponse(
            5L, 1L, "Notebook", "Dell", "XPS", null, null
        );
    }
}
