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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
class UsuarioRolesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    @BeforeEach
    void prepararRoles() {
        for (NombreRol nombre : NombreRol.values()) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre));
            }
        }
    }

    static List<Set<NombreRol>> combinacionesProhibidas() {
        return List.of(
            Set.of(NombreRol.CLIENTE, NombreRol.ADMINISTRADOR),
            Set.of(NombreRol.CLIENTE, NombreRol.RECEPCIONISTA),
            Set.of(NombreRol.CLIENTE, NombreRol.TECNICO),
            Set.of(NombreRol.CLIENTE, NombreRol.ADMINISTRADOR, NombreRol.RECEPCIONISTA),
            Set.of(NombreRol.CLIENTE, NombreRol.ADMINISTRADOR, NombreRol.TECNICO),
            Set.of(NombreRol.CLIENTE, NombreRol.RECEPCIONISTA, NombreRol.TECNICO),
            Set.of(NombreRol.values())
        );
    }

    @ParameterizedTest
    @MethodSource("combinacionesProhibidas")
    void crearYEditarDebenRechazarCombinacionesSinModificarDatos(Set<NombreRol> roles) throws Exception {
        long cantidad = usuarioRepository.count();
        String rolesJson = roles.stream().map(rol -> "\"" + rol.name() + "\"")
            .collect(Collectors.joining(",", "[", "]"));
        String cuerpo = """
            {"email":"roles-prueba@fixhub.com","claveTemporal":"Temporal123","roles":%s}
            """.formatted(rolesJson);

        mockMvc.perform(post("/api/usuarios")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Solicitud inválida"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("El rol CLIENTE no puede combinarse con roles internos"));
        assertThat(usuarioRepository.count()).isEqualTo(cantidad);

        Usuario usuario = guardarUsuario(NombreRol.CLIENTE);
        mockMvc.perform(put("/api/usuarios/{id}", usuario.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        Usuario guardado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(guardado.getEmail()).isEqualTo("cuenta-roles@fixhub.com");
        assertThat(guardado.getPasswordHash()).isEqualTo("hash-prueba");
        assertThat(guardado.getRoles()).extracting(Rol::getNombre).containsExactly(NombreRol.CLIENTE);
    }

    @ParameterizedTest
    @EnumSource(value = NombreRol.class, names = "CLIENTE", mode = EnumSource.Mode.EXCLUDE)
    void asignacionIndividualDebeRechazarMezclaEnAmbosSentidos(NombreRol interno) throws Exception {
        Usuario cliente = guardarUsuario(NombreRol.CLIENTE);
        mockMvc.perform(put("/api/usuarios/{id}/roles/{rol}", cliente.getId(), interno)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        assertThat(cliente.getRoles()).extracting(Rol::getNombre).containsExactly(NombreRol.CLIENTE);

        cliente.getRoles().clear();
        cliente.agregarRol(rolRepository.findByNombre(interno).orElseThrow());
        usuarioRepository.saveAndFlush(cliente);
        mockMvc.perform(put("/api/usuarios/{id}/roles/CLIENTE", cliente.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))))
            .andExpect(status().isBadRequest());
        assertThat(cliente.getRoles()).extracting(Rol::getNombre).containsExactly(interno);
    }

    @ParameterizedTest
    @EnumSource(value = NombreRol.class, names = "CLIENTE", mode = EnumSource.Mode.EXCLUDE)
    void recepcionNoDebeVincularUnaCuentaInterna(NombreRol interno) throws Exception {
        Usuario usuario = guardarUsuario(interno);
        long clientes = clienteRepository.count();
        mockMvc.perform(post("/api/clientes/con-usuario")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId":%d,"nombre":"Ana","apellido":"Prueba"}
                    """.formatted(usuario.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(clienteRepository.count()).isEqualTo(clientes);
        assertThat(clienteRepository.existsByUsuarioId(usuario.getId())).isFalse();
        assertThat(usuario.getRoles()).extracting(Rol::getNombre).containsExactly(interno);
    }

    @Test
    void recepcionDebeReutilizarCuentaClienteSinCrearOtra() throws Exception {
        Usuario usuario = guardarUsuario(NombreRol.CLIENTE);
        long usuarios = usuarioRepository.count();
        mockMvc.perform(post("/api/clientes/con-usuario")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RECEPCIONISTA")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId":%d,"nombre":"Ana","apellido":"Prueba"}
                    """.formatted(usuario.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuarioId").value(usuario.getId()));
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
        assertThat(clienteRepository.existsByUsuarioId(usuario.getId())).isTrue();
        assertThat(usuario.getRoles()).extracting(Rol::getNombre).containsExactly(NombreRol.CLIENTE);
    }

    private Usuario guardarUsuario(NombreRol nombre) {
        Usuario usuario = new Usuario("hash-prueba", "cuenta-roles@fixhub.com", null);
        usuario.agregarRol(rolRepository.findByNombre(nombre).orElseThrow());
        return usuarioRepository.saveAndFlush(usuario);
    }
}
