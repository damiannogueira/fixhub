package fixhub.inventario.entity;

import fixhub.autenticacion.entity.Usuario;
import fixhub.ordenes.entity.OrdenTrabajo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consumos_repuesto")
public class ConsumoRepuesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected ConsumoRepuesto() {}

    public ConsumoRepuesto(OrdenTrabajo orden, Repuesto repuesto, Usuario usuario,
                           Integer cantidad, BigDecimal precioUnitario, LocalDateTime fecha) {
        this.orden = orden;
        this.repuesto = repuesto;
        this.usuario = usuario;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public OrdenTrabajo getOrden() { return orden; }
    public Repuesto getRepuesto() { return repuesto; }
    public Usuario getUsuario() { return usuario; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public LocalDateTime getFecha() { return fecha; }
}
