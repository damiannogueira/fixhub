package fixhub.autenticacion.security;

import fixhub.autenticacion.dto.LoginRequest;
import fixhub.autenticacion.service.AutenticacionService;
import fixhub.autenticacion.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityErrorResponsesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AutenticacionService autenticacionService;

    @Test
    void solicitudSinJwtDebeDevolverProblem401() throws Exception {
        verificarProblem401(mockMvc.perform(get("/api/usuarios")));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void bearerInvalidoDebeDevolverProblem401() throws Exception {
        verificarProblem401(mockMvc.perform(
            get("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
        ));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void jwtVencidoDebeDevolverProblem401() throws Exception {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("https://fixhub.local")
            .subject("42")
            .issuedAt(ahora.minusSeconds(7200))
            .expiresAt(ahora.minusSeconds(3600))
            .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(),
            claims
        )).getTokenValue();

        verificarProblem401(mockMvc.perform(
            get("/api/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        ));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void usuarioSinPermisoDebeDevolverProblem403() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                .with(jwt().authorities(
                    new SimpleGrantedAuthority("ROLE_CLIENTE")
                )))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.title").value("Acceso denegado"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value(
                "No tiene permisos para realizar esta operación"
            ));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambioObligatorioDebeConservarProblemEspecifico() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                .with(jwt().jwt(token -> token
                    .subject("42")
                    .claim("requiereCambioClave", true)
                ).authorities(
                    new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")
                )))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.title").value("Cambio de clave requerido"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value(
                "Debe cambiar la clave temporal antes de continuar"
            ));

        verifyNoInteractions(usuarioService);
    }

    @Test
    void loginIncorrectoDebeConservarProblemEspecifico() throws Exception {
        when(autenticacionService.login(any(LoginRequest.class)))
            .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "identificador": "usuario@fixhub.com",
                        "clave": "ClaveIncorrecta"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(jsonPath("$.title").value("Credenciales inválidas"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value(
                "El identificador o la contraseña son incorrectos"
            ));
    }

    private void verificarProblem401(ResultActions resultado) throws Exception {
        resultado
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_PROBLEM_JSON
            ))
            .andExpect(header().string(
                HttpHeaders.WWW_AUTHENTICATE,
                startsWith("Bearer")
            ))
            .andExpect(jsonPath("$.title").value("Autenticación requerida"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value(
                "Debe autenticarse con un token válido para acceder a este recurso"
            ));
    }
}
