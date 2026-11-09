package fixhub.inventario.controller;

import fixhub.inventario.dto.ConsumoRepuestoResponse;
import fixhub.inventario.dto.MovimientoStockResponse;
import fixhub.inventario.dto.RegistrarConsumoRepuestoRequest;
import fixhub.inventario.dto.RegistrarEntradaStockRequest;
import fixhub.inventario.entity.TipoMovimientoStock;
import fixhub.inventario.exception.StockInsuficienteException;
import fixhub.inventario.service.StockService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockService stockService;

    @Test
    void administradorDebeRegistrarEntrada() throws Exception {
        when(
            stockService.registrarEntrada(
                eq(1L),
                any(RegistrarEntradaStockRequest.class),
                eq(42L)
            )
        ).thenReturn(movimientoResponse());

        mockMvc.perform(
                post("/api/repuestos/1/stock/entradas")
                    .with(jwtConRol("ADMINISTRADOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "cantidad": 10,
                            "motivo": "Compra",
                            "referencia": "FACTURA-001"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.tipo").value("ENTRADA")
            )
            .andExpect(
                jsonPath("$.cantidad").value(10)
            );

        verify(stockService).registrarEntrada(
            eq(1L),
            any(RegistrarEntradaStockRequest.class),
            eq(42L)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "RECEPCIONISTA",
        "TECNICO",
        "CLIENTE"
    })
    void rolesNoAdministradoresNoDebenRegistrarEntrada(
        String rol
    ) throws Exception {
        mockMvc.perform(
                post("/api/repuestos/1/stock/entradas")
                    .with(jwtConRol(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "cantidad": 10,
                            "motivo": "Compra"
                        }
                        """)
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(stockService);
    }

    @Test
    void administradorDebeAjustarStock() throws Exception {
        when(
            stockService.ajustarStock(
                eq(1L),
                any(),
                eq(42L)
            )
        ).thenReturn(movimientoAjusteResponse());

        mockMvc.perform(
                patch("/api/repuestos/1/stock")
                    .with(jwtConRol("ADMINISTRADOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "nuevoStock": 7,
                            "motivo": "Conteo físico"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.tipo").value("AJUSTE")
            );
    }

    @Test
    void tecnicoDebeRegistrarConsumoConIdDelJwt()
        throws Exception {
        when(
            stockService.registrarConsumo(
                eq(20L),
                any(RegistrarConsumoRepuestoRequest.class),
                eq(42L)
            )
        ).thenReturn(consumoResponse());

        mockMvc.perform(
                post("/api/ordenes/20/consumos")
                    .with(jwtConRol("TECNICO"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "repuestoId": 1,
                            "cantidad": 2
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.ordenId").value(20)
            )
            .andExpect(
                jsonPath("$.cantidad").value(2)
            );

        verify(stockService).registrarConsumo(
            eq(20L),
            any(RegistrarConsumoRepuestoRequest.class),
            eq(42L)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ADMINISTRADOR",
        "RECEPCIONISTA",
        "CLIENTE"
    })
    void rolesSinTecnicoNoDebenRegistrarConsumo(
        String rol
    ) throws Exception {
        mockMvc.perform(
                post("/api/ordenes/20/consumos")
                    .with(jwtConRol(rol))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "repuestoId": 1,
                            "cantidad": 2
                        }
                        """)
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(stockService);
    }

    @Test
    void administradorDebeConsultarMovimientos()
        throws Exception {
        when(stockService.listarMovimientos(42L))
            .thenReturn(List.of(movimientoResponse()));

        mockMvc.perform(
                get("/api/inventario/movimientos")
                    .with(jwtConRol("ADMINISTRADOR"))
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$[0].tipo").value("ENTRADA")
            );

        verify(stockService).listarMovimientos(42L);
    }

    @Test
    void entradaInvalidaDebeDevolverBadRequest()
        throws Exception {
        mockMvc.perform(
                post("/api/repuestos/1/stock/entradas")
                    .with(jwtConRol("ADMINISTRADOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "cantidad": 0,
                            "motivo": ""
                        }
                        """)
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(stockService);
    }

    @Test
    void stockInsuficienteDebeDevolverConflict()
        throws Exception {
        when(
            stockService.registrarConsumo(
                eq(20L),
                any(RegistrarConsumoRepuestoRequest.class),
                eq(42L)
            )
        ).thenThrow(
            new StockInsuficienteException(1L, 1, 2)
        );

        mockMvc.perform(
                post("/api/ordenes/20/consumos")
                    .with(jwtConRol("TECNICO"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "repuestoId": 1,
                            "cantidad": 2
                        }
                        """)
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.title").value("Stock insuficiente")
            );
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

    private MovimientoStockResponse movimientoResponse() {
        return new MovimientoStockResponse(
            1L,
            1L,
            "REP-001",
            "Pantalla",
            42L,
            null,
            TipoMovimientoStock.ENTRADA,
            10,
            "Compra",
            "FACTURA-001",
            LocalDateTime.of(2026, 11, 9, 18, 0)
        );
    }

    private MovimientoStockResponse movimientoAjusteResponse() {
        return new MovimientoStockResponse(
            2L,
            1L,
            "REP-001",
            "Pantalla",
            42L,
            null,
            TipoMovimientoStock.AJUSTE,
            -3,
            "Conteo físico",
            null,
            LocalDateTime.of(2026, 11, 9, 18, 5)
        );
    }

    private ConsumoRepuestoResponse consumoResponse() {
        return new ConsumoRepuestoResponse(
            1L,
            20L,
            1L,
            "REP-001",
            "Pantalla",
            42L,
            2,
            new BigDecimal("125000.00"),
            LocalDateTime.of(2026, 11, 9, 18, 10)
        );
    }
}
