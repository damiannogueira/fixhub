package fixhub.ordenes.controller;

import fixhub.ordenes.dto.CrearDiagnosticoRequest;
import fixhub.ordenes.dto.DiagnosticoClienteResponse;
import fixhub.ordenes.dto.DiagnosticoResponse;
import fixhub.ordenes.service.DiagnosticoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosticoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiagnosticoService diagnosticoService;

    @Test
    void tecnicoDebeCrearDiagnosticoConSuIdentidad()
        throws Exception {

        when(
            diagnosticoService.crear(
                eq(10L),
                any(CrearDiagnosticoRequest.class),
                eq(42L)
            )
        ).thenReturn(respuestaInterna());

        mockMvc.perform(
            post("/api/ordenes/10/diagnostico")
                .with(jwtTecnico(42L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "descripcionFalla": "Falla eléctrica",
                        "tareasRequeridas": "Reemplazar conector"
                    }
                    """)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.ordenId").value(10))
            .andExpect(jsonPath("$.usuarioId").value(42))
            .andExpect(
                jsonPath("$.descripcionFalla")
                    .value("Falla eléctrica")
            );

        verify(diagnosticoService).crear(
            eq(10L),
            any(CrearDiagnosticoRequest.class),
            eq(42L)
        );
    }

    @Test
    void administradorNoDebeCrearDiagnostico()
        throws Exception {

        mockMvc.perform(
            post("/api/ordenes/10/diagnostico")
                .with(jwtAdministrador(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "descripcionFalla": "Falla eléctrica",
                        "tareasRequeridas": "Reemplazar conector"
                    }
                    """)
        )
            .andExpect(status().isForbidden());

        verifyNoInteractions(diagnosticoService);
    }

    @Test
    void administradorDebeConsultarDiagnostico()
        throws Exception {

        when(diagnosticoService.buscarPorOrden(10L))
            .thenReturn(respuestaInterna());

        mockMvc.perform(
            get("/api/ordenes/10/diagnostico")
                .with(jwtAdministrador(1L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.ordenId").value(10));

        verify(diagnosticoService).buscarPorOrden(10L);
    }

    @Test
    void tecnicoDebeConsultarSoloRutaAsignada()
        throws Exception {

        when(diagnosticoService.buscarAsignado(10L, 42L))
            .thenReturn(respuestaInterna());

        mockMvc.perform(
            get("/api/ordenes/me/10/diagnostico")
                .with(jwtTecnico(42L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordenId").value(10));

        verify(diagnosticoService)
            .buscarAsignado(10L, 42L);
    }

    @Test
    void clienteDebeConsultarRespuestaLimitada()
        throws Exception {

        when(
            diagnosticoService.buscarPropioCliente(
                10L,
                77L
            )
        ).thenReturn(respuestaCliente());

        mockMvc.perform(
            get("/api/clientes/me/ordenes/10/diagnostico")
                .with(jwtCliente(77L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.ordenId").value(10))
            .andExpect(
                jsonPath("$.descripcionFalla")
                    .value("Falla eléctrica")
            )
            .andExpect(
                jsonPath("$.usuarioId").doesNotExist()
            );

        verify(diagnosticoService)
            .buscarPropioCliente(10L, 77L);
    }

    @Test
    void datosInvalidosDebenDevolverBadRequest()
        throws Exception {

        mockMvc.perform(
            post("/api/ordenes/10/diagnostico")
                .with(jwtTecnico(42L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "descripcionFalla": " ",
                        "tareasRequeridas": ""
                    }
                    """)
        )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(diagnosticoService);
    }

    @Test
    void usuarioNoAutenticadoNoDebeAcceder()
        throws Exception {

        mockMvc.perform(
            get("/api/ordenes/10/diagnostico")
        )
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(diagnosticoService);
    }

    private DiagnosticoResponse respuestaInterna() {
        return new DiagnosticoResponse(
            100L,
            10L,
            42L,
            "Falla eléctrica",
            "Reemplazar conector",
            LocalDateTime.of(2026, 11, 6, 12, 0)
        );
    }

    private DiagnosticoClienteResponse respuestaCliente() {
        return new DiagnosticoClienteResponse(
            100L,
            10L,
            "Falla eléctrica",
            "Reemplazar conector",
            LocalDateTime.of(2026, 11, 6, 12, 0)
        );
    }

    private org.springframework.test.web.servlet.request
        .RequestPostProcessor jwtTecnico(Long id) {

        return jwt()
            .jwt(token -> token.subject(id.toString()))
            .authorities(
                new SimpleGrantedAuthority("ROLE_TECNICO")
            );
    }

    private org.springframework.test.web.servlet.request
        .RequestPostProcessor jwtAdministrador(Long id) {

        return jwt()
            .jwt(token -> token.subject(id.toString()))
            .authorities(
                new SimpleGrantedAuthority(
                    "ROLE_ADMINISTRADOR"
                )
            );
    }

    private org.springframework.test.web.servlet.request
        .RequestPostProcessor jwtCliente(Long id) {

        return jwt()
            .jwt(token -> token.subject(id.toString()))
            .authorities(
                new SimpleGrantedAuthority("ROLE_CLIENTE")
            );
    }
}
