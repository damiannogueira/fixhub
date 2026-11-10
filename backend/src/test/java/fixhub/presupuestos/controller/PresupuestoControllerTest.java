package fixhub.presupuestos.controller;

import fixhub.inventario.exception.RepuestoNoEncontradoException;
import fixhub.ordenes.exception.OrdenTrabajoNoEncontradaException;
import fixhub.presupuestos.exception.PresupuestoNoEncontradoException;
import fixhub.presupuestos.service.PresupuestoService;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PresupuestoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PresupuestoService presupuestoService;

    @Test
    void tecnicoNoDebeListarTodosLosPresupuestos() throws Exception {
        mockMvc.perform(get("/api/presupuestos").with(conRoles("TECNICO")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(presupuestoService);
    }

    @Test
    void tecnicoDebeUsarListadoFiltradoPorUsuario() throws Exception {
        when(presupuestoService.listarAsignados(30L)).thenReturn(List.of());
        mockMvc.perform(get("/api/presupuestos/me").with(conRoles("TECNICO")))
            .andExpect(status().isOk());
        verify(presupuestoService).listarAsignados(30L);
    }

    @Test
    void tecnicoConRecepcionistaDebeAcumularPermisos() throws Exception {
        when(presupuestoService.listarTodos()).thenReturn(List.of());
        mockMvc.perform(get("/api/presupuestos").with(conRoles("TECNICO", "RECEPCIONISTA")))
            .andExpect(status().isOk());
        verify(presupuestoService).listarTodos();
    }

    @Test
    void presupuestoAusenteDebeResponder404() throws Exception {
        when(presupuestoService.buscarPorOrden(7L, 30L))
            .thenThrow(new PresupuestoNoEncontradoException());
        mockMvc.perform(get("/api/ordenes/7/presupuesto").with(conRoles("RECEPCIONISTA")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void recursosAusentesAlCrearDebenResponder404() throws Exception {
        for (RuntimeException exception : List.of(
            new OrdenTrabajoNoEncontradaException(7L), new RepuestoNoEncontradoException(5L)
        )) {
            org.mockito.Mockito.doThrow(exception).when(presupuestoService).crear(eq(7L), any(), eq(30L));
            mockMvc.perform(post("/api/ordenes/7/presupuesto").with(conRoles("RECEPCIONISTA"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"costoManoObra\":10,\"detalles\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }
    }

    private RequestPostProcessor conRoles(String... roles) {
        return jwt().jwt(token -> token.subject("30").claim("requiereCambioClave", false))
            .authorities(java.util.Arrays.stream(roles)
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toArray(SimpleGrantedAuthority[]::new));
    }
}
