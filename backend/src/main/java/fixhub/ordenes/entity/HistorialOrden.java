package fixhub.ordenes.entity;

import fixhub.autenticacion.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_ordenes")
public class HistorialOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoOrden tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_anterior")
    private EstadoOrden estadoAnterior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_nuevo")
    private EstadoOrden estadoNuevo;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "visible_cliente", nullable = false)
    private boolean visibleCliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected HistorialOrden() {
    }

    public HistorialOrden(
        OrdenTrabajo orden,
        Usuario usuario,
        TipoEventoOrden tipoEvento,
        EstadoOrden estadoAnterior,
        EstadoOrden estadoNuevo,
        String comentario,
        boolean visibleCliente,
        LocalDateTime fecha
    ) {
        this.orden = orden;
        this.usuario = usuario;
        this.tipoEvento = tipoEvento;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.comentario = comentario;
        this.visibleCliente = visibleCliente;
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

    public TipoEventoOrden getTipoEvento() {
        return tipoEvento;
    }

    public EstadoOrden getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoOrden getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getComentario() {
        return comentario;
    }

    public boolean isVisibleCliente() {
        return visibleCliente;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
