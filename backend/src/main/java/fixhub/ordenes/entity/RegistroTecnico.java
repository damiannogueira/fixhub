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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_tecnicos")
public class RegistroTecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotBlank
    @Column(
        name = "tarea_realizada",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String tareaRealizada;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected RegistroTecnico() {
    }

    public RegistroTecnico(
        OrdenTrabajo orden,
        Usuario usuario,
        String tareaRealizada,
        String observacion,
        LocalDateTime fecha
    ) {
        this.orden = orden;
        this.usuario = usuario;
        this.tareaRealizada = tareaRealizada;
        this.observacion = observacion;
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

    public String getTareaRealizada() {
        return tareaRealizada;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
