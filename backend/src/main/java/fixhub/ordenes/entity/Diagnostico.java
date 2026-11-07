package fixhub.ordenes.entity;

import fixhub.autenticacion.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosticos")
public class Diagnostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "orden_id",
        nullable = false,
        unique = true
    )
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotBlank
    @Column(
        name = "descripcion_falla",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String descripcionFalla;

    @NotBlank
    @Column(
        name = "tareas_requeridas",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String tareasRequeridas;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Diagnostico() {
    }

    public Diagnostico(
        OrdenTrabajo orden,
        Usuario usuario,
        String descripcionFalla,
        String tareasRequeridas,
        LocalDateTime fecha
    ) {
        this.orden = orden;
        this.usuario = usuario;
        this.descripcionFalla = descripcionFalla;
        this.tareasRequeridas = tareasRequeridas;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public OrdenTrabajo getOrden() {
        return orden;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getDescripcionFalla() {
        return descripcionFalla;
    }

    public void setDescripcionFalla(String descripcionFalla) {
        this.descripcionFalla = descripcionFalla;
    }

    public String getTareasRequeridas() {
        return tareasRequeridas;
    }

    public void setTareasRequeridas(String tareasRequeridas) {
        this.tareasRequeridas = tareasRequeridas;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
