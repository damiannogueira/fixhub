-- Esquema base para una base MySQL vacía, sin crear usuarios ni credenciales.
-- Ejecutar antes de presupuestos.sql e inventario.sql.

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(254) NULL,
    telefono VARCHAR(30) NULL,
    activo BOOLEAN NOT NULL,
    requiere_cambio_clave BOOLEAN NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_telefono UNIQUE (telefono)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL,
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE usuarios_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuarios_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_usuarios_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    apellido VARCHAR(255) NOT NULL,
    dni VARCHAR(255) NULL,
    direccion VARCHAR(255) NULL,
    CONSTRAINT uk_clientes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE equipos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    tipo VARCHAR(255) NOT NULL,
    marca VARCHAR(255) NULL,
    modelo VARCHAR(255) NULL,
    numero_serie VARCHAR(255) NULL,
    observaciones TEXT NULL,
    CONSTRAINT fk_equipos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB;

CREATE TABLE estados_orden (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL,
    es_estado_final BOOLEAN NOT NULL,
    CONSTRAINT uk_estados_orden_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE ordenes_trabajo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    tecnico_id BIGINT NULL,
    estado_id BIGINT NOT NULL,
    problema_inicial TEXT NOT NULL,
    motivo_cancelacion TEXT NULL,
    fecha_ingreso DATETIME(6) NOT NULL,
    fecha_entrega DATETIME(6) NULL,
    CONSTRAINT fk_ordenes_equipo FOREIGN KEY (equipo_id) REFERENCES equipos(id),
    CONSTRAINT fk_ordenes_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuarios(id),
    CONSTRAINT fk_ordenes_estado FOREIGN KEY (estado_id) REFERENCES estados_orden(id)
) ENGINE=InnoDB;

CREATE TABLE diagnosticos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    descripcion_falla TEXT NOT NULL,
    tareas_requeridas TEXT NOT NULL,
    fecha DATETIME(6) NOT NULL,
    CONSTRAINT uk_diagnosticos_orden UNIQUE (orden_id),
    CONSTRAINT fk_diagnosticos_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo(id),
    CONSTRAINT fk_diagnosticos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE registros_tecnicos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tarea_realizada TEXT NOT NULL,
    observacion TEXT NULL,
    fecha DATETIME(6) NOT NULL,
    CONSTRAINT fk_registros_tecnicos_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo(id),
    CONSTRAINT fk_registros_tecnicos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE historial_ordenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    estado_anterior BIGINT NULL,
    estado_nuevo BIGINT NULL,
    comentario TEXT NULL,
    visible_cliente BOOLEAN NOT NULL,
    fecha DATETIME(6) NOT NULL,
    CONSTRAINT fk_historial_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo(id),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_historial_estado_anterior FOREIGN KEY (estado_anterior) REFERENCES estados_orden(id),
    CONSTRAINT fk_historial_estado_nuevo FOREIGN KEY (estado_nuevo) REFERENCES estados_orden(id)
) ENGINE=InnoDB;

CREATE TABLE repuestos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion TEXT NULL,
    precio_costo DECIMAL(12,2) NOT NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    stock_actual INT NOT NULL,
    stock_minimo INT NOT NULL,
    activo BOOLEAN NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_modificacion DATETIME(6) NOT NULL,
    CONSTRAINT uk_repuestos_codigo UNIQUE (codigo),
    CONSTRAINT ck_repuestos_precio_costo CHECK (precio_costo >= 0),
    CONSTRAINT ck_repuestos_precio_venta CHECK (precio_venta >= 0),
    CONSTRAINT ck_repuestos_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT ck_repuestos_stock_minimo CHECK (stock_minimo >= 0)
) ENGINE=InnoDB;
