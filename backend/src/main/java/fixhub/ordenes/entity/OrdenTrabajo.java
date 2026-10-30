package fixhub.ordenes.entity;

import fixhub.autenticacion.entity.Usuario;
import fixhub.clientes.entity.Equipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_trabajo")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoOrden estado;

    @Column(name = "problema_inicial", nullable = false, columnDefinition = "TEXT")
    private String problemaInicial;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    @Column(name = "fecha_ingreso", nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    protected OrdenTrabajo() {
    }

    public OrdenTrabajo(
        Equipo equipo,
        Usuario tecnico,
        EstadoOrden estado,
        String problemaInicial,
        LocalDateTime fechaIngreso
    ) {
        this.equipo = equipo;
        this.tecnico = tecnico;
        this.estado = estado;
        this.problemaInicial = problemaInicial;
        this.fechaIngreso = fechaIngreso;
    }

    public Long getId() {
        return id;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public void setTecnico(Usuario tecnico) {
        this.tecnico = tecnico;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }

    public String getProblemaInicial() {
        return problemaInicial;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }
}
