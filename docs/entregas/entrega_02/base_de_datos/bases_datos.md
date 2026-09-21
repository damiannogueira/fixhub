# FixHub — Diseño de la Base de Datos

Este documento presenta el diseño de la base de datos relacional de FixHub como parte de la Segunda Entrega del Trabajo Final Integrador.
El modelado se realizó a partir de los requerimientos, las reglas de negocio y los módulos documentados para el proyecto y consolidados durante la Segunda Entrega.

## 1. Diagrama Entidad-Relación

A continuación se presenta el diagrama DER del sistema. Las relaciones indican la cardinalidad entre las entidades principales.

```mermaid
erDiagram
    USUARIOS ||--o{ USUARIOS_ROLES : "tiene"
    ROLES ||--o{ USUARIOS_ROLES : "asignado a"

    USUARIOS ||--o| CLIENTES : "accede como"
    CLIENTES ||--o{ EQUIPOS : "posee"

    EQUIPOS ||--o{ ORDENES_TRABAJO : "reparado en"
    USUARIOS o|--o{ ORDENES_TRABAJO : "responsable tecnico"

    ESTADOS_ORDEN ||--o{ ORDENES_TRABAJO : "define estado"

    ORDENES_TRABAJO ||--o| DIAGNOSTICOS : "tiene"
    USUARIOS ||--o{ DIAGNOSTICOS : "registra"
    ORDENES_TRABAJO ||--o{ REGISTROS_TECNICOS : "documenta tareas"
    USUARIOS ||--o{ REGISTROS_TECNICOS : "realiza"
    ORDENES_TRABAJO ||--o| PRESUPUESTOS : "genera"
    USUARIOS ||--o{ PRESUPUESTOS : "crea"
    USUARIOS o|--o{ PRESUPUESTOS : "publica"
    USUARIOS o|--o{ PRESUPUESTOS : "decide"
    PRESUPUESTOS ||--o{ DETALLES_PRESUPUESTO : "incluye"
    REPUESTOS ||--o{ DETALLES_PRESUPUESTO : "presupuestado en"

    ORDENES_TRABAJO ||--o{ CONSUMOS_REPUESTO : "registra consumo"
    REPUESTOS ||--o{ CONSUMOS_REPUESTO : "consumido en"
    USUARIOS ||--o{ CONSUMOS_REPUESTO : "registra"

    REPUESTOS ||--o{ MOVIMIENTOS_STOCK : "registra movimiento"
    USUARIOS ||--o{ MOVIMIENTOS_STOCK : "registra"
    CONSUMOS_REPUESTO o|--|| MOVIMIENTOS_STOCK : "genera"

    ORDENES_TRABAJO ||--o{ HISTORIAL_ORDENES : "registra cambios"
    USUARIOS ||--o{ HISTORIAL_ORDENES : "realiza cambio"
    ESTADOS_ORDEN o|--o{ HISTORIAL_ORDENES : "estado anterior"
    ESTADOS_ORDEN o|--o{ HISTORIAL_ORDENES : "estado nuevo"

    USUARIOS {
        int id PK
        varchar password_hash
        varchar email
        varchar telefono
        boolean activo
        boolean requiere_cambio_clave
        datetime fecha_creacion
    }

    ROLES {
        int id PK
        varchar nombre
    }

    USUARIOS_ROLES {
        int usuario_id PK,FK
        int rol_id PK,FK
    }

    CLIENTES {
        int id PK
        int usuario_id FK
        varchar nombre
        varchar apellido
        varchar dni
        varchar direccion
    }

    EQUIPOS {
        int id PK
        int cliente_id FK
        varchar tipo
        varchar marca
        varchar modelo
        varchar numero_serie
        text observaciones
    }

    ESTADOS_ORDEN {
        int id PK
        varchar nombre
        boolean es_estado_final
    }

    ORDENES_TRABAJO {
        int id PK
        int equipo_id FK
        int tecnico_id FK
        int estado_id FK
        text problema_inicial
        text motivo_cancelacion
        datetime fecha_ingreso
        datetime fecha_entrega
    }

    DIAGNOSTICOS {
        int id PK
        int orden_id FK
        int usuario_id FK
        text descripcion_falla
        text tareas_requeridas
        datetime fecha
    }

    REGISTROS_TECNICOS {
        int id PK
        int orden_id FK
        int usuario_id FK
        text tarea_realizada
        text observacion
        datetime fecha
    }

    PRESUPUESTOS {
        int id PK
        int orden_id FK
        int creado_por FK
        int publicado_por FK
        int decidido_por FK
        decimal costo_mano_obra
        decimal total
        varchar estado
        datetime fecha_creacion
        datetime fecha_publicacion
        datetime fecha_decision
    }

    REPUESTOS {
        int id PK
        varchar codigo
        varchar nombre
        text descripcion
        decimal precio_costo
        decimal precio_venta
        int stock_actual
        int stock_minimo
    }

    DETALLES_PRESUPUESTO {
        int id PK
        int presupuesto_id FK
        int repuesto_id FK
        int cantidad
        decimal precio_unitario
    }

    CONSUMOS_REPUESTO {
        int id PK
        int orden_id FK
        int repuesto_id FK
        int usuario_id FK
        int cantidad
        decimal precio_unitario
        datetime fecha
    }

    MOVIMIENTOS_STOCK {
        int id PK
        int repuesto_id FK
        int usuario_id FK
        int consumo_id FK,UK
        varchar tipo
        int cantidad
        varchar motivo
        varchar referencia
        datetime fecha
    }

    HISTORIAL_ORDENES {
        int id PK
        int orden_id FK
        int usuario_id FK
        varchar tipo_evento
        int estado_anterior FK
        int estado_nuevo FK
        text comentario
        boolean visible_cliente
        datetime fecha
    }
```

## 2. Diccionario de Datos

### 2.1. Módulo de Autenticación, Usuarios y Roles

#### Tabla `usuarios`
Representa las cuentas de acceso al sistema. Un usuario puede tener uno o varios roles y debe contar con al menos un medio de contacto válido: correo electrónico o número de celular.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del usuario. |
| password_hash | VARCHAR | Contraseña almacenada de forma segura (hash). |
| email | VARCHAR | Correo electrónico, puede usarse como identificador de acceso. |
| telefono | VARCHAR | Número de celular, puede usarse como identificador de acceso. |
| activo | BOOLEAN | Indica si la cuenta está habilitada. |
| requiere_cambio_clave | BOOLEAN | Indica si debe cambiar la credencial temporal en el primer ingreso. |
| fecha_creacion | DATETIME | Fecha y hora de creación de la cuenta. |

#### Tabla `roles`
Catálogo de roles disponibles en el sistema.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del rol. |
| nombre | VARCHAR | Nombre del rol (ADMINISTRADOR, RECEPCIONISTA, TECNICO, CLIENTE). |

#### Tabla `usuarios_roles`
Tabla intermedia que resuelve la relación muchos a muchos entre usuarios y roles.

| Campo | Tipo | Descripción |
|---|---|---|
| usuario_id | INT (PK, FK) | Referencia al usuario. |
| rol_id | INT (PK, FK) | Referencia al rol asignado. |

### 2.2. Módulo de Clientes y Equipos

#### Tabla `clientes`
Registro de las personas que llevan equipos al taller. Cada cliente tiene asociada una única cuenta de acceso.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del cliente. |
| usuario_id | INT (FK, UNIQUE) | Cuenta de acceso vinculada al cliente. |
| nombre | VARCHAR | Nombre del cliente. |
| apellido | VARCHAR | Apellido del cliente. |
| dni | VARCHAR | Documento de identidad. |
| direccion | VARCHAR | Domicilio del cliente. |

#### Tabla `equipos`
Equipos que ingresan al taller, asociados a un cliente.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del equipo. |
| cliente_id | INT (FK) | Cliente propietario del equipo. |
| tipo | VARCHAR | Tipo de equipo (celular, notebook, electrodoméstico, etc.). |
| marca | VARCHAR | Marca del equipo. |
| modelo | VARCHAR | Modelo del equipo. |
| numero_serie | VARCHAR | Número de serie o identificador único del equipo. |
| observaciones | TEXT | Observaciones adicionales sobre el equipo. |

### 2.3. Módulo de Órdenes de Trabajo

#### Tabla `estados_orden`
Catálogo de estados posibles de una orden de trabajo.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del estado. |
| nombre | VARCHAR | Nombre del estado (RECIBIDA, EN_DIAGNOSTICO, PENDIENTE_APROBACION, etc.). |
| es_estado_final | BOOLEAN | Indica si es un estado final (ENTREGADA, CANCELADA). |

#### Tabla `ordenes_trabajo`
Entidad central del sistema. Representa cada reparación registrada.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único de la orden. |
| equipo_id | INT (FK) | Equipo sobre el que se realiza la reparación. A través del equipo se identifica al cliente propietario. |
| tecnico_id | INT (FK) | Técnico responsable. Puede ser nulo mientras la orden esté en RECIBIDA. |
| estado_id | INT (FK) | Estado actual de la orden. |
| problema_inicial | TEXT | Descripción del problema informado por el cliente al recibir el equipo. |
| motivo_cancelacion | TEXT | Motivo registrado en caso de cancelación. |
| fecha_ingreso | DATETIME | Fecha y hora de recepción del equipo. |
| fecha_entrega | DATETIME | Fecha y hora de entrega al cliente. |

#### Tabla `diagnosticos`
Registro del diagnóstico técnico de una orden. Cada orden tiene como máximo un diagnóstico.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del diagnóstico. |
| orden_id | INT (FK, UNIQUE) | Orden asociada. |
| usuario_id | INT (FK) | Técnico que realizó o registró el diagnóstico. |
| descripcion_falla | TEXT | Descripción de la falla detectada. |
| tareas_requeridas | TEXT | Tareas necesarias para la reparación. |
| fecha | DATETIME | Fecha del diagnóstico. |

#### Tabla `registros_tecnicos`
Registro de las tareas realizadas y las observaciones técnicas durante la reparación. Una orden puede tener múltiples registros técnicos.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del registro técnico. |
| orden_id | INT (FK) | Orden sobre la que se realizó la tarea. |
| usuario_id | INT (FK) | Técnico que realizó el registro. |
| tarea_realizada | TEXT | Descripción de la tarea efectivamente realizada. |
| observacion | TEXT | Observación técnica asociada. |
| fecha | DATETIME | Fecha y hora del registro. |

#### Tabla `historial_ordenes`
Registro de los cambios relevantes en el ciclo de vida de una orden para mantener trazabilidad.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del registro. |
| orden_id | INT (FK) | Orden afectada. |
| usuario_id | INT (FK) | Usuario que realizó el cambio. |
| tipo_evento | VARCHAR | Tipo de evento registrado, como cambio de estado o asignación de técnico. |
| estado_anterior | INT (FK) | Estado previo, opcional cuando el evento no es un cambio de estado. |
| estado_nuevo | INT (FK) | Estado resultante, opcional cuando el evento no es un cambio de estado. |
| comentario | TEXT | Comentario o detalle del evento. |
| visible_cliente | BOOLEAN | Indica si el evento puede mostrarse al cliente. |
| fecha | DATETIME | Fecha y hora del evento. |

### 2.4. Módulo de Presupuestos y Aprobación

#### Tabla `presupuestos`
Presupuesto asociado a una orden. Cada orden que requiera presupuesto tendrá como máximo uno.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del presupuesto. |
| orden_id | INT (FK, UNIQUE) | Orden asociada. |
| creado_por | INT (FK) | Usuario que creó el presupuesto. |
| publicado_por | INT (FK) | Usuario que publicó el presupuesto, opcional hasta su publicación. |
| decidido_por | INT (FK) | Cuenta de usuario vinculada al Cliente correspondiente que tomó la decisión final, opcional hasta la decisión. |
| costo_mano_obra | DECIMAL | Importe global de mano de obra. |
| total | DECIMAL | Total calculado (mano de obra + repuestos). |
| estado | VARCHAR | Estado del presupuesto (BORRADOR, PUBLICADO, APROBADO, RECHAZADO). |
| fecha_creacion | DATETIME | Fecha de creación. |
| fecha_publicacion | DATETIME | Fecha de publicación al cliente, opcional hasta ese momento. |
| fecha_decision | DATETIME | Fecha en que el cliente aprueba o rechaza, opcional hasta la decisión. |

#### Tabla `detalles_presupuesto`
Repuestos incluidos en un presupuesto. Representan una estimación y **no modifican el stock real**.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del detalle. |
| presupuesto_id | INT (FK) | Presupuesto al que pertenece. |
| repuesto_id | INT (FK) | Repuesto presupuestado. |
| cantidad | INT | Cantidad estimada. |
| precio_unitario | DECIMAL | Precio unitario al momento del presupuesto. |

### 2.5. Módulo de Inventario y Stock

#### Tabla `repuestos`
Catálogo de repuestos. El stock se representa como un atributo de la entidad, sin una tabla independiente.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del repuesto. |
| codigo | VARCHAR | Código interno del repuesto. |
| nombre | VARCHAR | Nombre del repuesto. |
| descripcion | TEXT | Descripción detallada. |
| precio_costo | DECIMAL | Precio de compra. |
| precio_venta | DECIMAL | Precio de venta sugerido. |
| stock_actual | INT | Cantidad disponible en el momento. |
| stock_minimo | INT | Stock mínimo para alertas. |

#### Tabla `consumos_repuesto`
Registro de los repuestos realmente consumidos durante una reparación. Se registra como concepto independiente del presupuesto.

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del consumo. |
| orden_id | INT (FK) | Orden en la que se consumió el repuesto. |
| repuesto_id | INT (FK) | Repuesto consumido. |
| usuario_id | INT (FK) | Usuario que registró el consumo real. |
| cantidad | INT | Cantidad consumida. |
| precio_unitario | DECIMAL | Precio unitario al momento del consumo. |
| fecha | DATETIME | Fecha del consumo. |

#### Tabla `movimientos_stock`
Trazabilidad de todas las variaciones de stock (entradas, ajustes, consumos).

| Campo | Tipo | Descripción |
|---|---|---|
| id | INT (PK) | Identificador único del movimiento. |
| repuesto_id | INT (FK) | Repuesto afectado. |
| usuario_id | INT (FK) | Usuario que registró el movimiento. |
| consumo_id | INT (FK, UNIQUE) | Consumo real relacionado, opcional para entradas y ajustes. |
| tipo | VARCHAR | Tipo de movimiento (ENTRADA, AJUSTE, CONSUMO). |
| cantidad | INT | Cantidad del movimiento (positiva o negativa según el tipo). |
| motivo | VARCHAR | Motivo o descripción del movimiento. |
| referencia | VARCHAR | Referencia general al documento relacionado; no sustituye la FK del consumo real. |
| fecha | DATETIME | Fecha y hora del movimiento. |

## 3. Justificación del modelo de datos

El modelado de la base de datos de FixHub se realizó respetando los requerimientos, las reglas de negocio y los módulos documentados para el proyecto y consolidados durante la Segunda Entrega. A continuación se explican las decisiones de diseño más relevantes.

### 3.1. Elección de un modelo relacional (MySQL)

Se optó por una base de datos relacional porque el sistema requiere:

- **Integridad referencial fuerte**: las entidades están fuertemente relacionadas (cliente → equipo → orden → diagnóstico → presupuesto → consumos).
- **Transacciones ACID**: operaciones críticas como el consumo de repuestos y la actualización de stock deben ejecutarse de forma atómica para evitar inconsistencias.
- **Consultas complejas**: el dashboard y los reportes requieren agregaciones sobre múltiples tablas.
- **Trazabilidad**: el historial de órdenes y los movimientos de stock necesitan relaciones bien definidas.

Estas características hacen que un modelo relacional sea más adecuado que uno documental para el dominio del problema.

### 3.2. Relación entre usuarios, roles y clientes

- Se definió una **relación muchos a muchos** entre `usuarios` y `roles` mediante la tabla intermedia `usuarios_roles`, ya que una misma cuenta puede tener varios roles simultáneamente (por ejemplo, un propietario que es Administrador y Técnico a la vez) y sus permisos se acumulan.
- La relación entre `clientes` y `usuarios` es **uno a uno** (`usuario_id` con restricción `UNIQUE`), respetando la regla de negocio que indica que cada cliente tiene asociada una única cuenta de acceso y que esa cuenta no puede representar a más de un cliente.

### 3.3. Orden de trabajo como entidad central

La tabla `ordenes_trabajo` concentra las referencias al equipo, al técnico responsable y al estado, ya que es el núcleo del proceso de reparación. El cliente se obtiene a través de la relación orden → equipo → cliente. Esto evita guardar en la orden dos referencias que podrían quedar inconsistentes entre sí.

El campo `tecnico_id` es opcional mientras la orden permanece en `RECIBIDA`, porque puede crearse sin técnico. Antes de pasar a `EN_DIAGNOSTICO` debe tener uno asignado. De esta manera, cada orden tiene como máximo un técnico responsable y un técnico puede estar a cargo de varias órdenes.

Las entidades relacionadas se modelaron como tablas separadas vinculadas por clave foránea, lo que permite:

- Mantener el diagnóstico como una entidad **uno a uno opcional** con la orden y registrar mediante `usuario_id` qué técnico lo realizó o registró.
- Registrar múltiples tareas realizadas y observaciones mediante `registros_tecnicos`, sin convertirlas en estados de la orden.
- Manejar el presupuesto como una entidad **uno a uno opcional** cuando la reparación lo requiere.
- Registrar múltiples consumos de repuestos y múltiples eventos de historial por orden.

### 3.4. Estados como catálogo

Los estados de la orden se modelaron en una tabla `estados_orden` en lugar de usar un `ENUM` en la tabla de órdenes. Este catálogo centraliza los estados definidos para el flujo del MVP, permite identificar los estados finales mediante `es_estado_final` y posibilita referenciarlos de manera consistente desde las órdenes y el historial. No se plantea como un conjunto de estados libremente modificable durante la ejecución del sistema.

### 3.5. Stock como atributo de Repuesto

Siguiendo la regla de negocio explícita, el stock se representa como un atributo (`stock_actual`) de la entidad `Repuesto`, sin crear una tabla independiente `Stock`. Esto simplifica el modelo y es suficiente para el alcance del MVP, manteniendo la trazabilidad mediante la tabla `movimientos_stock`.

### 3.6. Separación entre presupuesto y consumo real

Se modelaron entidades independientes para:

- `detalles_presupuesto`: repuestos estimados en un presupuesto, que **no modifican el stock**.
- `consumos_repuesto`: repuestos realmente consumidos durante la reparación, que **sí modifican el stock**.

Esta separación respeta la regla de negocio que indica que "los repuestos presupuestados y los repuestos realmente consumidos se registrarán como conceptos diferentes", y permite comparar lo presupuestado versus lo efectivamente utilizado.

El presupuesto utiliza los estados `BORRADOR`, `PUBLICADO`, `APROBADO` y `RECHAZADO`, separados de los estados de la orden. En `BORRADOR` todavía no fue publicado al cliente y puede editarse. En `PUBLICADO` ya fue presentado al cliente, está pendiente de decisión y puede modificarse mientras continúe pendiente. Al quedar `APROBADO` o `RECHAZADO`, se cierra y ya no puede modificarse. Los campos de usuario y fecha permiten registrar quién lo creó, quién lo publicó y qué cuenta de usuario vinculada al Cliente correspondiente tomó la decisión final. Solamente el cliente correspondiente puede aprobarlo o rechazarlo.

Para el alcance del MVP, `costo_mano_obra` representa un único importe global. No se discriminan tareas de mano de obra con precios individuales.

### 3.7. Trazabilidad mediante historial y movimientos

Se incorporaron tablas específicas para garantizar la trazabilidad:

- `historial_ordenes` registra cambios de estado, asignaciones o cambios de técnico y otros eventos relevantes. Los estados anterior y nuevo son opcionales cuando el evento no corresponde a un cambio de estado. El campo `visible_cliente` diferencia la información que puede consultar el cliente de la información interna.
- `consumos_repuesto.usuario_id` identifica al usuario o técnico que registró el consumo real.
- `movimientos_stock` registra todas las variaciones de inventario. Cada consumo real genera exactamente un movimiento vinculado mediante `consumo_id`, mientras que una entrada o un ajuste pueden no relacionarse con un consumo. El campo `referencia` se mantiene para referencias generales y no sustituye esta clave foránea.

Estas tablas permiten reconstruir los eventos relevantes de una orden y las variaciones de un repuesto.

### 3.8. Coherencia con los módulos funcionales

El modelo respeta la organización en cinco módulos definida en la documentación de módulos:

- **Autenticación, usuarios y roles** → `usuarios`, `roles`, `usuarios_roles`.
- **Clientes y equipos** → `clientes`, `equipos`.
- **Órdenes de trabajo** → `ordenes_trabajo`, `estados_orden`, `diagnosticos`, `registros_tecnicos`, `historial_ordenes`.
- **Presupuestos y aprobación** → `presupuestos`, `detalles_presupuesto`.
- **Inventario y stock** → `repuestos`, `consumos_repuesto`, `movimientos_stock`.

Cada módulo tiene sus tablas bien delimitadas y las relaciones entre ellos se mantienen mediante claves foráneas, sin duplicar responsabilidades.
