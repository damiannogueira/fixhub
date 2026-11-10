CREATE TABLE consumos_repuesto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    repuesto_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT ck_consumo_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_consumo_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo(id),
    CONSTRAINT fk_consumo_repuesto FOREIGN KEY (repuesto_id) REFERENCES repuestos(id),
    CONSTRAINT fk_consumo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE movimientos_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    repuesto_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    consumo_id BIGINT UNIQUE NULL,
    tipo VARCHAR(20) NOT NULL,
    cantidad INT NOT NULL,
    motivo VARCHAR(255) NULL,
    referencia VARCHAR(120) NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT fk_movimiento_repuesto FOREIGN KEY (repuesto_id) REFERENCES repuestos(id),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_movimiento_consumo FOREIGN KEY (consumo_id) REFERENCES consumos_repuesto(id)
) ENGINE=InnoDB;
