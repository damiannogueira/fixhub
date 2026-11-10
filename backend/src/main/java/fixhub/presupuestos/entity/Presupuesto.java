package fixhub.presupuestos.entity;

import fixhub.autenticacion.entity.Usuario;
import fixhub.ordenes.entity.OrdenTrabajo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presupuestos")
public class Presupuesto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publicado_por")
    private Usuario publicadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decidido_por")
    private Usuario decididoPor;

    @Column(name = "costo_mano_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoManoObra;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EstadoPresupuesto estado = EstadoPresupuesto.BORRADOR;
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;
    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePresupuesto> detalles = new ArrayList<>();

    protected Presupuesto() {}

    public Presupuesto(OrdenTrabajo orden, Usuario creadoPor, BigDecimal costoManoObra) {
        this.orden = orden;
        this.creadoPor = creadoPor;
        this.costoManoObra = costoManoObra;
        this.total = costoManoObra;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public OrdenTrabajo getOrden() { return orden; }
    public Usuario getCreadoPor() { return creadoPor; }
    public Usuario getPublicadoPor() { return publicadoPor; }
    public Usuario getDecididoPor() { return decididoPor; }
    public BigDecimal getCostoManoObra() { return costoManoObra; }
    public void setCostoManoObra(BigDecimal value) { costoManoObra = value; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal value) { total = value; }
    public EstadoPresupuesto getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public LocalDateTime getFechaDecision() { return fechaDecision; }
    public List<DetallePresupuesto> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePresupuesto> value) { detalles = value; }
    public void publicar(Usuario actor) { estado = EstadoPresupuesto.PUBLICADO; publicadoPor = actor; fechaPublicacion = LocalDateTime.now(); }
    public void decidir(Usuario actor, EstadoPresupuesto decision) { estado = decision; decididoPor = actor; fechaDecision = LocalDateTime.now(); }
}
