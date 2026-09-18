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
- Permitir la creación controlada de cuentas de acceso con rol Cliente durante el registro de un cliente.
- Controlar el acceso a las funcionalidades según los roles asignados.

**Dependencias:**
- No depende funcionalmente de otro módulo para cumplir su responsabilidad principal.
- Los demás módulos dependen de este módulo para identificar al usuario autenticado y determinar las acciones que tiene permitidas.

**Reglas relevantes:**
- Una cuenta podrá tener uno o varios roles simultáneamente.
- Cuando un usuario tenga varios roles, sus permisos se acumularán según los roles asignados.
- La administración general de usuarios y la asignación de roles Administrador, Recepcionista y Técnico estarán reservadas al Administrador.
- Durante el registro de un cliente sin cuenta, el Recepcionista deberá crearle una cuenta de acceso únicamente con rol Cliente.
- Si el cliente ya posee una cuenta de acceso, se reutilizará la existente para futuras órdenes y equipos.
- La cuenta con rol Cliente deberá quedar vinculada exclusivamente a un único registro de Cliente para limitar el acceso a su propia información, equipos, órdenes y presupuestos. Podrá tener otros roles, pero no representar a otro Cliente.
- El cliente deberá proporcionar al menos un medio de contacto válido: correo electrónico o número de celular.
- El correo electrónico o el número de celular registrado podrán utilizarse como identificador de acceso.
- Para el primer ingreso se utilizará una credencial temporal que deberá ser reemplazada por una contraseña definida por el cliente.
- El número de orden no se utilizará como contraseña.
- El MVP no incluirá un registro público y autónomo de usuarios.
- La integración automática con WhatsApp Business permanecerá fuera del alcance del MVP.

**Alcance y límites:**
Incluye la autenticación, administración de usuarios, roles, permisos y control de acceso. También contempla la creación controlada de cuentas con rol Cliente durante el proceso de recepción. No administra la información propia de clientes, equipos, órdenes de trabajo, presupuestos o inventario; únicamente gestiona la identidad de acceso y determina quién puede utilizar esas funcionalidades.

### 4.2. Clientes y equipos

**Objetivo:** Gestionar la información de los clientes y de los equipos asociados que ingresan al servicio técnico.

**Actores:**
- Administrador.
- Recepcionista.
- Cliente, únicamente para consultar información propia cuando corresponda.

**Responsabilidades principales:**
- Registrar, editar y consultar clientes.
- Registrar los datos de contacto necesarios del cliente.
- Registrar y consultar equipos asociados a cada cliente.
- Mantener la relación entre cliente y equipo.
- Mantener la vinculación entre el cliente y su cuenta de acceso con rol Cliente.
- Permitir consultar el historial de reparaciones asociado a un equipo o cliente a partir de las órdenes registradas.

**Dependencias:**
- Depende de Autenticación, usuarios y roles para controlar el acceso según permisos.
- Utiliza la cuenta de acceso gestionada por Autenticación, usuarios y roles para vincular al usuario autenticado con el cliente correspondiente.
- Se relaciona con Órdenes de trabajo, ya que toda orden debe estar asociada a un cliente y a un equipo.
- Utiliza la información histórica de las órdenes para consultar reparaciones anteriores.

**Reglas relevantes:**
- Un cliente podrá tener uno o varios equipos asociados.
- Cada equipo deberá estar asociado a un único cliente.
- El cliente solo podrá consultar su propia información, sus equipos y su historial de reparaciones.
- El técnico solo podrá consultar la información necesaria de los equipos vinculados a sus órdenes asignadas.
- Durante el registro de un cliente nuevo se deberá disponer de al menos un medio de contacto válido: correo electrónico o número de celular.
- Cada cliente tendrá asociada una única cuenta de acceso con rol Cliente.
- Si el cliente ya se encuentra registrado y posee una cuenta de acceso, se reutilizará esa misma cuenta para sus nuevos equipos y órdenes.
- La cuenta vinculada permitirá identificar qué información, equipos, órdenes y presupuestos pertenecen al cliente autenticado.
- No se creará una cuenta nueva por cada equipo u orden de trabajo.

**Alcance y límites:**
Incluye la administración de clientes, sus datos de contacto, equipos, la relación entre ambos y la vinculación del cliente con su cuenta de acceso, junto con la consulta de reparaciones anteriores. La autenticación, las credenciales y los permisos de acceso continúan siendo responsabilidad del módulo Autenticación, usuarios y roles. No gestiona el ciclo operativo de una reparación, presupuestos ni inventario.

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

## 5. Dependencias entre módulos

Los cinco módulos mantienen las siguientes relaciones principales:

- **Autenticación, usuarios y roles** es utilizado transversalmente para identificar usuarios y controlar permisos.
- **Clientes y equipos** proporciona la información necesaria para asociar cada orden con el cliente y equipo correspondientes.
- **Órdenes de trabajo** representa el núcleo del proceso de reparación y se relaciona con Presupuestos y aprobación e Inventario y stock.
- **Presupuestos y aprobación** depende de una orden existente y puede consultar información de repuestos para elaborar el presupuesto.
- **Inventario y stock** registra los consumos reales producidos durante las reparaciones y mantiene las existencias correspondientes.

Las dependencias no implican duplicar responsabilidades. Cada módulo conserva la responsabilidad sobre su propio proceso y comparte únicamente la información necesaria con los demás.

## 6. Cobertura del MVP

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

## 7. Paquetes previstos del sistema

A partir de los módulos funcionales definidos, se establece una organización inicial de los paquetes que se desarrollarán durante la implementación de FixHub. Esta estructura busca mantener agrupadas las responsabilidades relacionadas con cada área funcional del sistema y facilitar la organización del código.

La definición presentada en esta etapa establece los paquetes principales previstos, sin detallar todavía las clases, controladores, servicios, repositorios, entidades o DTO que se implementarán posteriormente.

### 7.1. Paquetes principales del backend

Para el backend se prevé una organización por funcionalidad, manteniendo una correspondencia directa con los módulos definidos previamente.

Los paquetes funcionales principales serán:

- `auth`: autenticación, usuarios, roles, permisos y control de acceso.
- `clientes`: gestión de clientes, datos de contacto, equipos asociados y vinculación con la cuenta de acceso.
- `ordenes`: órdenes de trabajo, asignación de técnicos, diagnóstico, tareas, estados e historial.
- `presupuestos`: creación, cálculo, consulta y aprobación o rechazo de presupuestos.
- `inventario`: repuestos, existencias, entradas, ajustes, consumos y movimientos de stock.

Esta organización permite mantener agrupados los componentes relacionados con una misma responsabilidad funcional y evita separar el proyecto únicamente por tipo de clase.

### 7.2. Organización interna prevista del backend

Dentro de cada paquete funcional, la implementación podrá organizarse en componentes específicos según la responsabilidad de cada elemento.

De forma general, podrán utilizarse componentes como:

- Controladores para recibir y responder las solicitudes de la API.
- Servicios para concentrar la lógica de negocio.
- Repositorios para acceder y persistir la información.
- Entidades para representar los datos del dominio.
- DTO para intercambiar información entre la API y los clientes del sistema.

La definición concreta de clases y componentes se realizará durante la etapa de implementación, manteniendo la separación de responsabilidades establecida en los módulos funcionales.

### 7.3. Organización prevista del frontend

El frontend se organizará siguiendo las áreas funcionales definidas para el sistema, agrupando las interfaces y la lógica de presentación según las funcionalidades que representan.

Las principales áreas previstas serán:

- Autenticación y acceso.
- Clientes y equipos.
- Órdenes de trabajo.
- Presupuestos.
- Inventario.
- Panel e indicadores.

La estructura interna del frontend se definirá durante la implementación según las necesidades de las vistas, componentes y servicios utilizados. Esta organización buscará mantener una separación clara entre las distintas funcionalidades y facilitar el mantenimiento de la aplicación.
