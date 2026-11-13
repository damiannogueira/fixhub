package fixhub.autenticacion.controller;

import fixhub.autenticacion.entity.NombreRol;
import fixhub.autenticacion.entity.Rol;
import fixhub.autenticacion.entity.Usuario;
import fixhub.autenticacion.repository.RolRepository;
import fixhub.autenticacion.repository.UsuarioRepository;
import fixhub.clientes.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioClaveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararRoles() {
        for (NombreRol nombre : NombreRol.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/usuarios", "/api/clientes/con-cuenta", "/api/clientes/con-roles"})
    void altaConClaveNumericaDebeDevolver400SinCrearDatos(String ruta) throws Exception {
        long usuarios = usuarioRepository.count();
        long clientes = clienteRepository.count();
        String rol = ruta.equals("/api/usuarios") ? "TECNICO" : "CLIENTE";
        exigirError(post(ruta), """
            {"email":"numerica@fixhub.com","claveTemporal":"12345678",
             "roles":["%s"],"nombre":"Ana","apellido":"Prueba"}
            """.formatted(rol), "La contraseña no puede estar compuesta exclusivamente por dígitos");
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
        assertThat(clienteRepository.count()).isEqualTo(clientes);
    }

    @Test
    void editarConClaveNumericaNoDebeModificarLaCuenta() throws Exception {
        Usuario usuario = guardarUsuario();
        usuario.setRequiereCambioClave(false);
        usuarioRepository.saveAndFlush(usuario);
        String hash = usuario.getPasswordHash();
        exigirError(put("/api/usuarios/{id}", usuario.getId()), """
            {"email":"modificado@fixhub.com","claveTemporal":"12345678",
             "roles":["ADMINISTRADOR"],"activo":false}
            """, "La contraseña no puede estar compuesta exclusivamente por dígitos");
        assertThat(usuario.getEmail()).isEqualTo("clave-prueba@fixhub.com");
        assertThat(usuario.getRoles()).extracting(Rol::getNombre).containsExactly(NombreRol.TECNICO);
        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getPasswordHash()).isEqualTo(hash);
        assertThat(usuario.isRequiereCambioClave()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/usuarios", "/api/clientes/con-cuenta", "/api/clientes/con-roles"})
    void altaDebeAceptarClaveConLetrasYNumeros(String ruta) throws Exception {
        String roles = ruta.equals("/api/usuarios") ? ",\"roles\":[\"TECNICO\"]"
            : ruta.endsWith("con-roles") ? ",\"roles\":[\"CLIENTE\"]" : "";
        String cliente = ruta.equals("/api/usuarios") ? "" : ",\"nombre\":\"Ana\",\"apellido\":\"Prueba\"";
        mockMvc.perform(post(ruta)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"alta-clave@fixhub.com","claveTemporal":"Temporal123"%s%s}
                    """.formatted(roles, cliente)))
            .andExpect(status().isCreated());
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase("alta-clave@fixhub.com").orElseThrow();
        assertThat(passwordEncoder.matches("Temporal123", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.isRequiereCambioClave()).isTrue();
    }

    @Test
    void editarDebeAceptarClaveTemporalConLetrasYNumeros() throws Exception {
        Usuario usuario = guardarUsuario();
        usuario.setRequiereCambioClave(false);
        usuarioRepository.saveAndFlush(usuario);
        mockMvc.perform(put("/api/usuarios/{id}", usuario.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"clave-prueba@fixhub.com","claveTemporal":"OtraTemporal456","roles":["TECNICO"]}
                    """))
            .andExpect(status().isOk());
        assertThat(passwordEncoder.matches("OtraTemporal456", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.isRequiereCambioClave()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "Temporal123"})
    void cambioInvalidoDebeConservarHashEIndicador(String nueva) throws Exception {
        Usuario usuario = guardarUsuario();
        String hash = usuario.getPasswordHash();
        mockMvc.perform(post("/api/auth/cambiar-clave")
                .with(jwt().jwt(token -> token.subject(usuario.getId().toString())
                    .claim("requiereCambioClave", true))
                    .authorities(new SimpleGrantedAuthority("ROLE_TECNICO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"claveActual":"Temporal123","claveNueva":"%s","confirmacionClave":"%s"}
                    """.formatted(nueva, nueva)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(nueva.equals("Temporal123")
                ? "La nueva contraseña debe ser diferente de la actual"
                : "La contraseña no puede estar compuesta exclusivamente por dígitos"));
        assertThat(usuario.getPasswordHash()).isEqualTo(hash);
        assertThat(usuario.isRequiereCambioClave()).isTrue();
    }

    @Test
    void cambioValidoDebeActualizarHashYDesactivarCambioObligatorio() throws Exception {
        Usuario usuario = guardarUsuario();
        String hash = usuario.getPasswordHash();
        mockMvc.perform(post("/api/auth/cambiar-clave")
                .with(jwt().jwt(token -> token.subject(usuario.getId().toString())
                    .claim("requiereCambioClave", true))
                    .authorities(new SimpleGrantedAuthority("ROLE_TECNICO")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"claveActual":"Temporal123","claveNueva":"NuevaClave456","confirmacionClave":"NuevaClave456"}
                    """))
            .andExpect(status().isNoContent());
        assertThat(usuario.getPasswordHash()).isNotEqualTo(hash);
        assertThat(passwordEncoder.matches("NuevaClave456", usuario.getPasswordHash())).isTrue();
        assertThat(usuario.isRequiereCambioClave()).isFalse();
    }

    private void exigirError(MockHttpServletRequestBuilder solicitud, String cuerpo, String detalle) throws Exception {
        mockMvc.perform(solicitud
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Solicitud inválida"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value(detalle));
    }

    private Usuario guardarUsuario() {
        Usuario usuario = new Usuario(passwordEncoder.encode("Temporal123"), "clave-prueba@fixhub.com", null);
        usuario.agregarRol(rolRepository.findByNombre(NombreRol.TECNICO).orElseThrow());
        return usuarioRepository.saveAndFlush(usuario);
    }
}
