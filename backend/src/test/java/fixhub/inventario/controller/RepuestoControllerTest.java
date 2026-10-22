package fixhub.inventario.controller;

import fixhub.inventario.dto.ActualizarRepuestoRequest;
import fixhub.inventario.dto.CrearRepuestoRequest;
import fixhub.inventario.dto.RepuestoResponse;
import fixhub.inventario.service.RepuestoService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RepuestoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepuestoService repuestoService;

    @ParameterizedTest
    @ValueSource(strings = {
        "ADMINISTRADOR",
        "RECEPCIONISTA",
        "TECNICO"
    })
    void rolesAutorizadosDebenConsultarRepuestos(
        String rol
    ) throws Exception {
        when(repuestoService.listar(null))
            .thenReturn(List.of());

        mockMvc.perform(
                get("/api/repuestos")
                    .with(jwtConRol(rol))
            )
            .andExpect(status().isOk());

        verify(repuestoService).listar(null);
    }

    @Test
    void clienteNoDebeConsultarRepuestos() throws Exception {
        mockMvc.perform(
                get("/api/repuestos")
                    .with(jwtConRol("CLIENTE"))
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(repuestoService);
    }

    @Test
    void usuarioNoAutenticadoNoDebeConsultarRepuestos()
        throws Exception {

        mockMvc.perform(get("/api/repuestos"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(repuestoService);
    }

    @Test
    void administradorDebeCrearRepuesto() throws Exception {
        when(
            repuestoService.crear(
                any(CrearRepuestoRequest.class)
            )
        ).thenReturn(crearResponse());

        mockMvc.perform(
                post("/api/repuestos")
                    .with(jwtConRol("ADMINISTRADOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoRepuesto())
            )
            .andExpect(status().isCreated());

        verify(repuestoService).crear(
            any(CrearRepuestoRequest.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RECEPCIONISTA",
        "TECNICO",
        "CLIENTE"
    })
    void rolesNoAdministradoresNoDebenCrearRepuestos(
        String rol
    ) throws Exception {
        mockMvc.perform(
                post("/api/repuestos")
                    .with(jwtConRol(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoRepuesto())
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(repuestoService);
    }

    @Test
    void administradorDebeActualizarRepuesto()
        throws Exception {

        when(
            repuestoService.actualizar(
                eq(1L),
                any(ActualizarRepuestoRequest.class)
            )
        ).thenReturn(crearResponse());

        mockMvc.perform(
                put("/api/repuestos/1")
                    .with(jwtConRol("ADMINISTRADOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoRepuesto())
            )
            .andExpect(status().isOk());

        verify(repuestoService).actualizar(
            eq(1L),
            any(ActualizarRepuestoRequest.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RECEPCIONISTA",
        "TECNICO",
        "CLIENTE"
    })
    void rolesNoAdministradoresNoDebenActualizarRepuestos(
        String rol
    ) throws Exception {
        mockMvc.perform(
                put("/api/repuestos/1")
                    .with(jwtConRol(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpoRepuesto())
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(repuestoService);
    }

    @Test
    void administradorDebeDesactivarRepuesto()
        throws Exception {

        mockMvc.perform(
                delete("/api/repuestos/1")
                    .with(jwtConRol("ADMINISTRADOR"))
            )
            .andExpect(status().isNoContent());

        verify(repuestoService).desactivar(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RECEPCIONISTA",
        "TECNICO",
        "CLIENTE"
    })
    void rolesNoAdministradoresNoDebenDesactivarRepuestos(
        String rol
    ) throws Exception {
        mockMvc.perform(
                delete("/api/repuestos/1")
                    .with(jwtConRol(rol))
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(repuestoService);
    }

    private RequestPostProcessor jwtConRol(String rol) {
        return jwt()
            .jwt(token -> token
                .subject("42")
                .claim("requiereCambioClave", false)
            )
            .authorities(
                new SimpleGrantedAuthority("ROLE_" + rol)
            );
    }

    private String cuerpoRepuesto() {
        return """
            {
                "codigo": "REP-001",
                "nombre": "Pantalla",
                "descripcion": "Pantalla compatible",
                "stockMinimo": 2,
                "precioCosto": 80000.00,
                "precioVenta": 125000.00
            }
            """;
    }

    private RepuestoResponse crearResponse() {
        return new RepuestoResponse(
            1L,
            "REP-001",
            "Pantalla",
            "Pantalla compatible",
            0,
            2,
            new BigDecimal("80000.00"),
            new BigDecimal("125000.00"),
            true,
            LocalDateTime.of(2026, 10, 22, 8, 0),
            null
        );
    }
}
