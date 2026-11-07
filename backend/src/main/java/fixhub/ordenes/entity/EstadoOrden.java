package fixhub.ordenes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estados_orden")
public class EstadoOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 40)
    private NombreEstadoOrden nombre;

    @Column(name = "es_estado_final", nullable = false)
    private boolean estadoFinal;

    protected EstadoOrden() {
    }

    public EstadoOrden(NombreEstadoOrden nombre, boolean estadoFinal) {
        this.nombre = nombre;
        this.estadoFinal = estadoFinal;
    }

    public Long getId() {
        return id;
    }

    public NombreEstadoOrden getNombre() {
        return nombre;
    }

    public boolean isEstadoFinal() {
        return estadoFinal;
    }
}
