package fixhub.ordenes.controller;

import fixhub.ordenes.dto.CrearRegistroTecnicoRequest;
import fixhub.ordenes.dto.RegistroTecnicoResponse;
import fixhub.ordenes.service.RegistroTecnicoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

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
class RegistroTecnicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroTecnicoService registroTecnicoService;

    @Test
    void tecnicoDebeCrearRegistroConSuIdentidad()
        throws Exception {

        when(
            registroTecnicoService.crear(
                eq(10L),
                any(CrearRegistroTecnicoRequest.class),
                eq(42L)
            )
        ).thenReturn(respuesta());

        mockMvc.perform(
            post("/api/ordenes/10/registros-tecnicos")
                .with(jwtTecnico(42L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tareaRealizada": "Reemplazar conector",
                        "observacion": "El equipo encendió"
                    }
                    """)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.ordenId").value(10))
            .andExpect(jsonPath("$.usuarioId").value(42))
            .andExpect(
                jsonPath("$.tareaRealizada")
                    .value("Reemplazar conector")
            );

        verify(registroTecnicoService).crear(
            eq(10L),
            any(CrearRegistroTecnicoRequest.class),
            eq(42L)
        );
    }

    @Test
    void administradorNoDebeCrearRegistro()
        throws Exception {

        mockMvc.perform(
            post("/api/ordenes/10/registros-tecnicos")
                .with(jwtAdministrador(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tareaRealizada": "Reemplazar conector",
                        "observacion": null
                    }
                    """)
        )
            .andExpect(status().isForbidden());

        verifyNoInteractions(registroTecnicoService);
    }

    @Test
    void administradorDebeConsultarRegistros()
        throws Exception {

        when(
            registroTecnicoService.listarPorOrden(10L)
        ).thenReturn(List.of(respuesta()));

        mockMvc.perform(
            get("/api/ordenes/10/registros-tecnicos")
                .with(jwtAdministrador(1L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(100))
            .andExpect(
                jsonPath("$[0].tareaRealizada")
                    .value("Reemplazar conector")
            );

        verify(registroTecnicoService)
            .listarPorOrden(10L);
    }

    @Test
    void tecnicoNoDebeUsarConsultaAdministrativa()
        throws Exception {

        mockMvc.perform(
            get("/api/ordenes/10/registros-tecnicos")
                .with(jwtTecnico(42L))
        )
            .andExpect(status().isForbidden());

        verifyNoInteractions(registroTecnicoService);
    }

    @Test
    void tecnicoDebeConsultarSoloRutaAsignada()
        throws Exception {

        when(
            registroTecnicoService.listarAsignados(
                10L,
                42L
            )
        ).thenReturn(List.of(respuesta()));

        mockMvc.perform(
            get("/api/ordenes/me/10/registros-tecnicos")
                .with(jwtTecnico(42L))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ordenId").value(10));

        verify(registroTecnicoService)
            .listarAsignados(10L, 42L);
    }

    @Test
    void tareaVaciaDebeDevolverBadRequest()
        throws Exception {

        mockMvc.perform(
            post("/api/ordenes/10/registros-tecnicos")
                .with(jwtTecnico(42L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "tareaRealizada": " ",
                        "observacion": "Observación"
                    }
                    """)
        )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registroTecnicoService);
    }

    @Test
    void usuarioNoAutenticadoNoDebeAcceder()
        throws Exception {

        mockMvc.perform(
            get("/api/ordenes/10/registros-tecnicos")
        )
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(registroTecnicoService);
    }

    private RegistroTecnicoResponse respuesta() {
        return new RegistroTecnicoResponse(
            100L,
            10L,
            42L,
            "Reemplazar conector",
            "El equipo encendió",
            LocalDateTime.of(2026, 11, 6, 13, 0)
        );
    }

    private RequestPostProcessor jwtTecnico(Long id) {
        return jwt()
            .jwt(token -> token.subject(id.toString()))
            .authorities(
                new SimpleGrantedAuthority("ROLE_TECNICO")
            );
    }

    private RequestPostProcessor jwtAdministrador(Long id) {
        return jwt()
            .jwt(token -> token.subject(id.toString()))
            .authorities(
                new SimpleGrantedAuthority(
                    "ROLE_ADMINISTRADOR"
                )
            );
    }
}
