# FixHub — Módulos funcionales

## 1. Introducción

Este documento presenta los módulos funcionales definidos para FixHub como parte de la Segunda Entrega del Trabajo Final Integrador.

La organización de los módulos se realizó a partir del alcance y del MVP aprobados en la Primera Entrega, buscando separar las responsabilidades principales del sistema y evitar duplicaciones entre funcionalidades.

Se definieron cinco módulos funcionales principales. Algunas funcionalidades, como diagnóstico, historial y trazabilidad, seguimiento del cliente y dashboard, se integran dentro de los módulos correspondientes o funcionan de manera transversal.

## 2. Módulos funcionales

Los módulos definidos para FixHub son:

1. Autenticación, usuarios y roles.
2. Clientes y equipos.
3. Órdenes de trabajo.
4. Presupuestos y aprobación.
5. Inventario y stock.

## 3. Funcionalidades integradas y transversales

Las siguientes funcionalidades forman parte del MVP, pero no se consideran módulos funcionales independientes:

- **Diagnóstico:** se integra dentro del módulo de Órdenes de trabajo.
- **Historial y trazabilidad:** se distribuye entre Órdenes de trabajo y Clientes y equipos.
- **Seguimiento del cliente:** utiliza funcionalidades de Órdenes de trabajo, Presupuestos y aprobación, y Autenticación, usuarios y roles.
- **Dashboard e indicadores básicos:** presenta información obtenida principalmente de Órdenes de trabajo e Inventario y stock.

## 4. Definición de los módulos

### 4.1. Autenticación, usuarios y roles

**Objetivo:** Gestionar el acceso de los usuarios al sistema y controlar qué funcionalidades pueden utilizar según sus roles y permisos.

**Actores:**
- Administrador.
- Recepcionista.
- Técnico.
- Cliente.

**Responsabilidades principales:**
- Gestionar la autenticación e inicio de sesión.
- Administrar usuarios.
- Administrar roles y permisos.
- Controlar el acceso a las funcionalidades según los roles asignados.

**Dependencias:**
- No depende funcionalmente de otro módulo para cumplir su responsabilidad principal.
- Los demás módulos dependen de este módulo para identificar al usuario autenticado y determinar las acciones que tiene permitidas.

**Reglas relevantes:**
- Una cuenta podrá tener uno o varios roles simultáneamente.
- Cuando un usuario tenga varios roles, sus permisos se acumularán según los roles asignados.
- La creación y administración de usuarios y la asignación de roles estarán reservadas al Administrador.
- El MVP no incluirá un registro público y autónomo de usuarios.

**Alcance y límites:**
Incluye la autenticación, administración de usuarios, roles, permisos y control de acceso. No administra la información propia de clientes, equipos, órdenes de trabajo, presupuestos o inventario; únicamente determina quién puede acceder a esas funcionalidades.

### 4.2. Clientes y equipos

**Objetivo:** Gestionar la información de los clientes y de los equipos asociados que ingresan al servicio técnico.

**Actores:**
- Administrador.
- Recepcionista.
- Cliente, únicamente para consultar información propia cuando corresponda.

**Responsabilidades principales:**
- Registrar, editar y consultar clientes.
- Registrar y consultar equipos asociados a cada cliente.
- Mantener la relación entre cliente y equipo.
- Permitir consultar el historial de reparaciones asociado a un equipo o cliente a partir de las órdenes registradas.

**Dependencias:**
- Depende de Autenticación, usuarios y roles para controlar el acceso según permisos.
- Se relaciona con Órdenes de trabajo, ya que toda orden debe estar asociada a un cliente y a un equipo.
- Utiliza la información histórica de las órdenes para consultar reparaciones anteriores.

**Reglas relevantes:**
- Un cliente podrá tener uno o varios equipos asociados.
- Cada equipo deberá estar asociado a un único cliente.
- El cliente solo podrá consultar su propia información, sus equipos y su historial de reparaciones.
- El técnico solo podrá consultar la información necesaria de los equipos vinculados a sus órdenes asignadas.

**Alcance y límites:**
Incluye la administración de clientes, equipos y la relación entre ambos, junto con la consulta de reparaciones anteriores. No gestiona el ciclo operativo de una reparación, presupuestos ni inventario.

### 4.3. Órdenes de trabajo

**Objetivo:** Gestionar el ciclo operativo completo de cada reparación, desde la recepción del equipo hasta su entrega al cliente.

**Actores:**
- Administrador.
- Recepcionista.
- Técnico.
- Cliente, únicamente para consultar el estado y la información visible de sus propias reparaciones.

**Responsabilidades principales:**
- Crear órdenes de trabajo asociadas a un cliente y a un equipo.
- Registrar el problema informado al momento de la recepción.
- Asignar un técnico responsable.
- Gestionar los cambios de estado de la orden.
- Registrar y actualizar el diagnóstico.
- Registrar observaciones técnicas y tareas realizadas.
- Registrar los cambios importantes producidos durante el ciclo de la orden.
- Permitir consultar el estado actual de la reparación.
- Mantener la información necesaria para consultar reparaciones anteriores.

**Dependencias:**
- Depende de Autenticación, usuarios y roles para controlar permisos.
- Depende de Clientes y equipos porque toda orden debe estar asociada a un cliente y a un equipo.
- Se relaciona con Presupuestos y aprobación cuando la reparación requiere un presupuesto.
- Se relaciona con Inventario y stock cuando se utilizan repuestos durante la reparación.

**Reglas relevantes:**
- Cada orden tendrá un único técnico responsable.
- La orden podrá crearse sin técnico responsable mientras permanezca en `RECIBIDA`, pero deberá tener un técnico asignado antes de pasar a `EN_DIAGNOSTICO`.
- Cada orden podrá tener como máximo un diagnóstico.
- El técnico solamente podrá realizar acciones técnicas sobre las órdenes que tenga asignadas.
- La orden deberá respetar el flujo de estados definido.
- Si una orden requiere presupuesto, no podrá avanzar a reparación hasta que este haya sido aprobado por el cliente.
- Si no requiere presupuesto, podrá avanzar desde `EN_DIAGNOSTICO` hacia `EN_REPARACION`.
- La cancelación estará permitida únicamente antes de iniciar la reparación.
- Los cambios importantes deberán quedar registrados para conservar la trazabilidad.
- El cliente solamente podrá consultar la información visible correspondiente a sus propias reparaciones.

**Alcance y límites:**
Incluye el ciclo operativo de la reparación, la asignación del técnico, estados, diagnóstico, observaciones, tareas realizadas y trazabilidad de la orden. No realiza el cálculo ni la aprobación de presupuestos y tampoco administra directamente las existencias de repuestos.

### 4.4. Presupuestos y aprobación

**Objetivo:** Gestionar la elaboración, cálculo, consulta y resolución de los presupuestos asociados a una orden de trabajo.

**Actores:**
- Administrador.
- Recepcionista.
- Técnico, para aportar información técnica y trabajar sobre presupuestos de sus órdenes asignadas.
- Cliente, para consultar y aprobar o rechazar sus propios presupuestos.

**Responsabilidades principales:**
- Generar presupuestos asociados a una orden de trabajo.
- Incluir mano de obra y repuestos.
- Calcular automáticamente el total.
- Permitir la consulta del presupuesto por parte del cliente.
- Registrar la aprobación o rechazo del cliente.
- Mantener vinculada la decisión del presupuesto con el avance de la orden.

**Dependencias:**
- Depende de Autenticación, usuarios y roles para controlar permisos y acceso.
- Depende de Órdenes de trabajo porque todo presupuesto debe estar asociado a una orden.
- Se relaciona con Inventario y stock para consultar los repuestos que pueden incluirse en un presupuesto.

**Reglas relevantes:**
- Cada orden que requiera presupuesto podrá tener como máximo un presupuesto.
- El presupuesto podrá modificarse mientras se encuentre pendiente de la decisión del cliente.
- Si el cliente rechaza un presupuesto requerido, la orden no podrá continuar hacia reparación y pasará a `CANCELADA`.
- Una vez aprobado o rechazado, quedará cerrado y no se manejarán versiones independientes dentro del MVP.
- El total se calculará a partir de la mano de obra y los repuestos incluidos.
- La aprobación o rechazo deberá quedar registrado.
- Solamente el cliente correspondiente podrá aprobar o rechazar su presupuesto.
- Si una orden requiere presupuesto, no podrá avanzar a reparación hasta que este haya sido aprobado.
- Incluir un repuesto en un presupuesto no modificará el stock real.

**Alcance y límites:**
Incluye la creación, cálculo, consulta, publicación y aprobación o rechazo de presupuestos asociados a una orden. No administra el stock real de repuestos ni el ciclo completo de estados de la orden, aunque el resultado del presupuesto condiciona su avance hacia reparación.

### 4.5. Inventario y stock

**Objetivo:** Gestionar los repuestos disponibles y mantener actualizado el stock utilizado durante las reparaciones.

**Actores:**
- Administrador.
- Recepcionista, únicamente para consultar catálogo y disponibilidad cuando corresponda.
- Técnico, para consultar repuestos y registrar consumos en sus órdenes asignadas. Un usuario con rol Administrador solo podrá registrar consumos si también posee el rol Técnico y tiene asignada la orden correspondiente.

**Responsabilidades principales:**
- Registrar, editar y consultar repuestos.
- Controlar las existencias disponibles.
- Registrar entradas y ajustes de stock.
- Registrar el consumo real de repuestos asociado a una orden de trabajo.
- Actualizar las existencias cuando se utilizan repuestos.
- Mantener la trazabilidad de los movimientos de stock.
- Proporcionar información sobre stock bajo para los indicadores del dashboard.

**Dependencias:**
- Depende de Autenticación, usuarios y roles para controlar permisos.
- Se relaciona con Órdenes de trabajo porque el consumo de repuestos ocurre dentro de una reparación.
- Se relaciona con Presupuestos y aprobación porque los repuestos pueden formar parte de un presupuesto.

**Reglas relevantes:**
- El stock se representará como un atributo de `Repuesto`.
- No se utilizará una entidad `Stock` independiente dentro del MVP.
- Los cambios de existencias deberán conservar trazabilidad mediante movimientos de stock.
- El registro de un consumo y la actualización de las existencias deberán realizarse de manera consistente.
- Un consumo no podrá generar existencias negativas.
- Solamente el Administrador podrá registrar repuestos, entradas y ajustes manuales.
- El Técnico podrá registrar consumos únicamente en sus órdenes asignadas.
- El Recepcionista podrá consultar disponibilidad, pero no modificar existencias.
- Incluir un repuesto en un presupuesto no se considerará un consumo y no modificará el stock.
- Los repuestos presupuestados y los realmente consumidos se registrarán como conceptos diferentes.

**Alcance y límites:**
Incluye la gestión de repuestos, existencias, consumos y movimientos de stock. No administra presupuestos ni el ciclo operativo de las órdenes, aunque se relaciona con ambos cuando se presupuestan o utilizan repuestos.

## 5. Flujo funcional de las órdenes de trabajo

Para el MVP, una orden de trabajo podrá utilizar los siguientes estados:

- `RECIBIDA`
- `EN_DIAGNOSTICO`
- `PENDIENTE_APROBACION`
- `APROBADA`
- `EN_REPARACION`
- `FINALIZADA`
- `LISTA_PARA_RETIRAR`
- `ENTREGADA`
- `CANCELADA`

La asignación de un técnico responsable no se considera un estado de la orden, sino una acción realizada sobre ella que deberá conservarse en la trazabilidad correspondiente.

Los estados propios del presupuesto se administran dentro del módulo Presupuestos y aprobación y no se duplican como estados de la orden.

### 5.1. Transiciones permitidas

El flujo normal permite las siguientes transiciones:

- `RECIBIDA` → `EN_DIAGNOSTICO`
- `EN_DIAGNOSTICO` → `PENDIENTE_APROBACION`, cuando la reparación requiere presupuesto.
- `EN_DIAGNOSTICO` → `EN_REPARACION`, cuando la reparación no requiere presupuesto.
- `PENDIENTE_APROBACION` → `APROBADA`, cuando el cliente aprueba el presupuesto.
- `PENDIENTE_APROBACION` → `CANCELADA`, cuando el cliente rechace el presupuesto.
- `APROBADA` → `EN_REPARACION`
- `EN_REPARACION` → `FINALIZADA`
- `FINALIZADA` → `LISTA_PARA_RETIRAR`
- `LISTA_PARA_RETIRAR` → `ENTREGADA`

La orden podrá pasar a `CANCELADA` desde `RECIBIDA`, `EN_DIAGNOSTICO`, `PENDIENTE_APROBACION` o `APROBADA`.

Una vez iniciada la reparación, no se permitirá una cancelación normal dentro del alcance del MVP.

Toda cancelación deberá registrar el usuario responsable, la fecha y el motivo correspondiente.

`ENTREGADA` y `CANCELADA` se consideran estados finales.

## 6. Permisos por rol

Los permisos se aplicarán según los roles asignados a cada usuario. Cuando una cuenta posea más de un rol, acumulará los permisos correspondientes a todos ellos.

### Administrador

Podrá:
- Administrar usuarios, roles y permisos.
- Registrar, editar y consultar clientes y equipos.
- Crear órdenes, administrar sus datos de recepción y asignar técnicos.
- Consultar todas las órdenes.
- Gestionar y publicar presupuestos, pero no aprobarlos ni rechazarlos en nombre del cliente.
- Administrar repuestos, entradas y ajustes de stock.
- Consultar información general e indicadores del sistema.

Las tareas estrictamente técnicas de diagnóstico y reparación quedarán reservadas al rol Técnico.

### Recepcionista

Podrá:
- Registrar, editar y consultar clientes y equipos.
- Crear órdenes de trabajo y registrar la información de recepción.
- Asignar o cambiar el técnico responsable.
- Consultar las órdenes.
- Marcar órdenes como listas para retirar y registrar su entrega.
- Crear, editar y publicar presupuestos.
- Consultar el catálogo de repuestos y su disponibilidad.
- Consultar indicadores operativos permitidos.

No podrá realizar diagnóstico, reparación, modificar existencias ni aprobar presupuestos en nombre del cliente.

### Técnico

Podrá:
- Consultar únicamente las órdenes que tenga asignadas.
- Consultar la información necesaria del cliente y del equipo vinculados con esas órdenes.
- Registrar o actualizar el diagnóstico.
- Registrar tareas y observaciones técnicas.
- Iniciar y finalizar la reparación.
- Participar en la elaboración de presupuestos de sus órdenes asignadas.
- Consultar repuestos y registrar consumos reales correspondientes a sus órdenes.

No podrá administrar clientes, equipos, usuarios, roles ni existencias generales del inventario.

### Cliente

Podrá:
- Iniciar sesión y consultar sus propios datos.
- Consultar únicamente sus equipos y reparaciones.
- Consultar el diagnóstico y los avances definidos como visibles.
- Consultar sus propios presupuestos.
- Aprobar o rechazar únicamente sus propios presupuestos.

No tendrá acceso a información administrativa, observaciones internas, inventario ni reparaciones pertenecientes a otros clientes.

## 7. Dependencias entre módulos

Los cinco módulos mantienen las siguientes relaciones principales:

- **Autenticación, usuarios y roles** es utilizado transversalmente para identificar usuarios y controlar permisos.
- **Clientes y equipos** proporciona la información necesaria para asociar cada orden con el cliente y equipo correspondientes.
- **Órdenes de trabajo** representa el núcleo del proceso de reparación y se relaciona con Presupuestos y aprobación e Inventario y stock.
- **Presupuestos y aprobación** depende de una orden existente y puede consultar información de repuestos para elaborar el presupuesto.
- **Inventario y stock** registra los consumos reales producidos durante las reparaciones y mantiene las existencias correspondientes.

Las dependencias no implican duplicar responsabilidades. Cada módulo conserva la responsabilidad sobre su propio proceso y comparte únicamente la información necesaria con los demás.

## 8. Cobertura del MVP

La organización definida permite cubrir las funcionalidades establecidas para el MVP:

- Autenticación, inicio de sesión, roles y permisos → Autenticación, usuarios y roles.
- Gestión de clientes y equipos → Clientes y equipos.
- Creación, asignación, estados y ciclo de las reparaciones → Órdenes de trabajo.
- Diagnósticos y observaciones técnicas → Órdenes de trabajo.
- Generación, cálculo, publicación, aprobación y rechazo de presupuestos → Presupuestos y aprobación.
- Gestión de repuestos, existencias, consumos y movimientos → Inventario y stock.
- Historial de cambios de una orden → Órdenes de trabajo.
- Historial de reparaciones por equipo o cliente → Clientes y equipos junto con Órdenes de trabajo.
- Consulta del estado de una reparación por parte del cliente → Órdenes de trabajo, respetando autenticación y permisos.
- Consulta y decisión sobre presupuestos por parte del cliente → Presupuestos y aprobación, respetando autenticación y permisos.
- Dashboard e indicadores básicos → información obtenida principalmente de Órdenes de trabajo e Inventario y stock.
- Interfaz responsive → característica general de presentación y no módulo funcional independiente.

De esta forma, los cinco módulos definidos cubren el MVP sin necesidad de crear módulos independientes para diagnóstico, historial, seguimiento del cliente o dashboard.
