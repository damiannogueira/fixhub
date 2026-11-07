package fixhub.autenticacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Email
    @Column(unique = true, length = 254)
    private String email;

    @Column(unique = true, length = 30)
    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "requiere_cambio_clave", nullable = false)
    private boolean requiereCambioClave = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuarios_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id"),
        uniqueConstraints = @UniqueConstraint(
            columnNames = {"usuario_id", "rol_id"}
        )
    )
    private Set<Rol> roles = new HashSet<>();

    protected Usuario() {
    }

    public Usuario(String passwordHash, String email, String telefono) {
        this.passwordHash = passwordHash;
        this.email = normalizarEmail(email);
        this.telefono = normalizarContacto(telefono);
    }

    @PrePersist
    private void asignarFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    @AssertTrue(message = "Debe informarse al menos un email o teléfono válido")
    public boolean isMedioContactoValido() {
        boolean emailInformado = email != null && !email.isBlank();
        boolean telefonoInformado = telefono != null && !telefono.isBlank();

        boolean emailValido = email == null || !email.isBlank();
        boolean telefonoValido = telefono == null || !telefono.isBlank();

        return emailValido
            && telefonoValido
            && (emailInformado || telefonoInformado);
    }

    public void agregarRol(Rol rol) {
        roles.add(rol);
    }

    public void quitarRol(Rol rol) {
        roles.remove(rol);
    }

    public boolean tieneRol(NombreRol nombreRol) {
        return roles.stream()
            .anyMatch(rol -> rol.getNombre() == nombreRol);
    }

    public int cantidadRoles() {
        return roles.size();
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    private String normalizarContacto(String contacto) {
        if (contacto == null || contacto.isBlank()) {
            return null;
        }

        return contacto.trim();
    }

    public Long getId() {
        return id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalizarEmail(email);
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = normalizarContacto(telefono);
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isRequiereCambioClave() {
        return requiereCambioClave;
    }

    public void setRequiereCambioClave(boolean requiereCambioClave) {
        this.requiereCambioClave = requiereCambioClave;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Set<Rol> getRoles() {
        return roles;
    }
}
