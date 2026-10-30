package fixhub.ordenes.controller;

import fixhub.clientes.exception.EquipoNoEncontradoException;
import fixhub.ordenes.dto.ClienteOrdenResponse;
import fixhub.ordenes.dto.EquipoOrdenResponse;
import fixhub.ordenes.dto.OrdenClienteResponse;
import fixhub.ordenes.dto.OrdenTrabajoResponse;
import fixhub.ordenes.entity.NombreEstadoOrden;
import fixhub.ordenes.service.OrdenTrabajoService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrdenTrabajoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdenTrabajoService ordenTrabajoService;

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "RECEPCIONISTA"})
    void rolesAdministrativosDebenCrearYConsultarOrdenes(String rol)
        throws Exception {
        when(ordenTrabajoService.crear(any(), eq(20L)))
            .thenReturn(response());
        when(ordenTrabajoService.listar()).thenReturn(List.of(response()));

        mockMvc.perform(
                post("/api/ordenes")
                    .with(jwtConRol(rol, 20L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "equipoId": 3,
                            "problemaInicial": "No enciende"
                        }
                        """)
            )
            .andExpect(status().isCreated());
        mockMvc.perform(get("/api/ordenes").with(jwtConRol(rol, 20L)))
            .andExpect(status().isOk());

        verify(ordenTrabajoService).crear(any(), eq(20L));
        verify(ordenTrabajoService).listar();
    }

    @Test
    void crearConEquipoInexistenteDebeDevolverNotFound() throws Exception {
        when(ordenTrabajoService.crear(any(), eq(20L)))
            .thenThrow(new EquipoNoEncontradoException(999L));

        mockMvc.perform(
                post("/api/ordenes")
                    .with(jwtConRol("RECEPCIONISTA", 20L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "equipoId": 999,
                            "problemaInicial": "No enciende"
                        }
                        """)
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void clienteNoDebeAccederAListadoAdministrativo() throws Exception {
        mockMvc.perform(
                get("/api/ordenes").with(jwtConRol("CLIENTE", 10L))
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(ordenTrabajoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TECNICO", "RECEPCIONISTA"})
    void rolesPermitidosDebenSolicitarCambioDeEstado(String rol)
        throws Exception {
        when(ordenTrabajoService.cambiarEstado(
            7L,
            NombreEstadoOrden.EN_DIAGNOSTICO,
            30L
        )).thenReturn(response());

        mockMvc.perform(
                patch("/api/ordenes/7/estado")
                    .with(jwtConRol(rol, 30L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"EN_DIAGNOSTICO\"}")
            )
            .andExpect(status().isOk());
    }

    @Test
    void administradorSinOtroRolNoDebeCambiarEstado() throws Exception {
        mockMvc.perform(
                patch("/api/ordenes/7/estado")
                    .with(jwtConRol("ADMINISTRADOR", 40L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"estado\":\"EN_DIAGNOSTICO\"}")
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(ordenTrabajoService);
    }

    @Test
    void soloAdministradorDebeCancelar() throws Exception {
        when(ordenTrabajoService.cancelar(7L, "Cliente desiste", 40L))
            .thenReturn(response());

        mockMvc.perform(
                post("/api/ordenes/7/cancelacion")
                    .with(jwtConRol("ADMINISTRADOR", 40L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"Cliente desiste\"}")
            )
            .andExpect(status().isOk());
        mockMvc.perform(
                post("/api/ordenes/7/cancelacion")
                    .with(jwtConRol("RECEPCIONISTA", 20L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"motivo\":\"Cliente desiste\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void tecnicoDebeConsultarSoloConSuIdentidadDelJwt() throws Exception {
        when(ordenTrabajoService.listarAsignadas(30L))
            .thenReturn(List.of(response()));
        when(ordenTrabajoService.buscarAsignada(7L, 30L))
            .thenReturn(response());

        mockMvc.perform(
                get("/api/ordenes/me").with(jwtConRol("TECNICO", 30L))
            )
            .andExpect(status().isOk());
        mockMvc.perform(
                get("/api/ordenes/me/7")
                    .with(jwtConRol("TECNICO", 30L))
            )
            .andExpect(status().isOk());

        verify(ordenTrabajoService).listarAsignadas(30L);
        verify(ordenTrabajoService).buscarAsignada(7L, 30L);
    }

    @Test
    void clienteDebeConsultarSoloConSuIdentidadDelJwt() throws Exception {
        when(ordenTrabajoService.listarPropiasCliente(10L))
            .thenReturn(List.of(responseCliente()));
        when(ordenTrabajoService.buscarPropiaCliente(7L, 10L))
            .thenReturn(responseCliente());

        mockMvc.perform(
                get("/api/clientes/me/ordenes")
                    .with(jwtConRol("CLIENTE", 10L))
            )
            .andExpect(status().isOk());
        mockMvc.perform(
                get("/api/clientes/me/ordenes/7")
                    .with(jwtConRol("CLIENTE", 10L))
            )
            .andExpect(status().isOk());

        verify(ordenTrabajoService).listarPropiasCliente(10L);
        verify(ordenTrabajoService).buscarPropiaCliente(7L, 10L);
    }

    @Test
    void usuarioNoAutenticadoNoDebeAcceder() throws Exception {
        mockMvc.perform(get("/api/ordenes"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(ordenTrabajoService);
    }

    private RequestPostProcessor jwtConRol(String rol, Long usuarioId) {
        return jwt()
            .jwt(token -> token
                .subject(usuarioId.toString())
                .claim("requiereCambioClave", false)
            )
            .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private OrdenTrabajoResponse response() {
        return new OrdenTrabajoResponse(
            7L,
            equipo(),
            new ClienteOrdenResponse(2L, "Ana", "Pérez"),
            30L,
            NombreEstadoOrden.RECIBIDA,
            "No enciende",
            null,
            LocalDateTime.now(),
            null
        );
    }

    private OrdenClienteResponse responseCliente() {
        return new OrdenClienteResponse(
            7L,
            equipo(),
            NombreEstadoOrden.RECIBIDA,
            "No enciende",
            null,
            LocalDateTime.now(),
            null
        );
    }

    private EquipoOrdenResponse equipo() {
        return new EquipoOrdenResponse(
            3L,
            "Notebook",
            "Dell",
            "XPS",
            "ABC",
            "Sin golpes"
        );
    }
}
