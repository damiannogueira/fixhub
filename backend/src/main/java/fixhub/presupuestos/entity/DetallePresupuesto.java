package fixhub.presupuestos.entity;

import fixhub.inventario.entity.Repuesto;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalles_presupuesto")
public class DetallePresupuesto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presupuesto_id", nullable = false)
    private Presupuesto presupuesto;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    protected DetallePresupuesto() {}
    public DetallePresupuesto(Presupuesto presupuesto, Repuesto repuesto, Integer cantidad, BigDecimal precioUnitario) {
        this.presupuesto = presupuesto; this.repuesto = repuesto;
        this.cantidad = cantidad; this.precioUnitario = precioUnitario;
    }
    public Long getId() { return id; }
    public Repuesto getRepuesto() { return repuesto; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
