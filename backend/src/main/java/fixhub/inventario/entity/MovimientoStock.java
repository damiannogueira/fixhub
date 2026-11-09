package fixhub.inventario.entity;

import fixhub.autenticacion.entity.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
public class MovimientoStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private Repuesto repuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumo_id", unique = true)
    private ConsumoRepuesto consumo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoStock tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(length = 100)
    private String referencia;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected MovimientoStock() {}

    public MovimientoStock(Repuesto repuesto, Usuario usuario, TipoMovimientoStock tipo,
                           Integer cantidad, String motivo, String referencia,
                           ConsumoRepuesto consumo, LocalDateTime fecha) {
        this.repuesto = repuesto;
        this.usuario = usuario;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.referencia = referencia;
        this.consumo = consumo;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Repuesto getRepuesto() { return repuesto; }
    public Usuario getUsuario() { return usuario; }
    public ConsumoRepuesto getConsumo() { return consumo; }
    public TipoMovimientoStock getTipo() { return tipo; }
    public Integer getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public String getReferencia() { return referencia; }
    public LocalDateTime getFecha() { return fecha; }
}
