# FixHub

Sistema Integral de Gestión para Talleres y Servicios Técnicos.

## Descripción

FixHub es una aplicación web orientada a talleres y servicios técnicos pequeños y medianos que busca centralizar y organizar el ciclo completo de una reparación, desde la recepción del equipo hasta su entrega al cliente.

La propuesta surge a partir de una problemática real observada en un servicio técnico, donde diferentes procesos de gestión se realizan mediante herramientas y canales no integrados, como planillas de cálculo, WhatsApp, llamadas telefónicas y registros manuales.

A partir de esta situación, el equipo identificó una problemática que puede presentarse también en otros tipos de servicios técnicos, como reparación de celulares, electrónica, computación, electrodomésticos y otros rubros.

## Objetivo

Desarrollar el MVP de FixHub, un Sistema Integral de Gestión para Talleres y Servicios Técnicos que permita administrar el ciclo completo de reparaciones, centralizando la información y automatizando tareas manuales.

## Requerimientos funcionales

FixHub deberá permitir:

- Autenticar a los usuarios y controlar el acceso a las funcionalidades según los roles y permisos asignados.
- Administrar usuarios y roles del sistema de acuerdo con los permisos asignados.
- Registrar, editar y consultar clientes.
- Permitir al Recepcionista crear de forma controlada una cuenta de acceso con rol Cliente durante el registro de un cliente que todavía no posea una.
- Reutilizar la cuenta de acceso existente cuando un cliente ya registrado incorpore nuevos equipos u órdenes de trabajo.
- Vincular cada Cliente con una única cuenta de acceso para limitar el acceso a su propia información, equipos, órdenes y presupuestos.
- Permitir que el cliente utilice su correo electrónico o número de celular como identificador de acceso.
- Permitir el primer ingreso mediante una credencial temporal que posteriormente deberá ser reemplazada por una contraseña definida por el cliente.
- Permitir al cliente autenticado acceder únicamente a las funcionalidades y a la información correspondientes a su propio registro.
- Registrar y consultar los equipos asociados a cada cliente.
- Crear órdenes de trabajo vinculadas a un cliente y a un equipo.
- Registrar el problema informado al momento de recibir el equipo.
- Asignar un técnico responsable a cada orden de trabajo.
- Registrar y actualizar el diagnóstico correspondiente a una reparación.
- Registrar tareas y observaciones técnicas realizadas durante el proceso.
- Gestionar los estados de las órdenes de trabajo durante todo el ciclo de reparación.
- Generar presupuestos que incluyan mano de obra y repuestos.
- Permitir que el cliente consulte, apruebe o rechace sus propios presupuestos.
- Impedir el inicio de una reparación cuando exista un presupuesto requerido que todavía no haya sido aprobado.
- Registrar y consultar repuestos disponibles.
- Mantener actualizado el stock a partir de entradas, ajustes y consumos reales.
- Registrar los movimientos necesarios para conservar la trazabilidad de las variaciones de stock.
- Mantener un historial de los cambios relevantes realizados sobre las órdenes de trabajo.
- Permitir consultar las reparaciones anteriores asociadas a un cliente o a un equipo.
- Permitir que el cliente consulte el estado y la información habilitada de sus propias reparaciones.
- Mostrar indicadores básicos de gestión utilizando información de las órdenes de trabajo y del inventario.
- Proporcionar una interfaz web adaptable a distintos tamaños de pantalla.

## Reglas de negocio

Las siguientes reglas definen el comportamiento esperado del sistema dentro del alcance del MVP y deberán respetarse durante la implementación.

### Usuarios, roles y acceso de clientes

- Una cuenta de usuario podrá tener uno o varios roles simultáneamente. Cuando posea más de un rol, acumulará los permisos correspondientes a cada uno.
- Cada Cliente deberá tener asociada una única cuenta de acceso.
- Si durante el registro de un cliente todavía no existe una cuenta asociada, el Recepcionista deberá crear una cuenta únicamente con rol Cliente.
- Si el cliente ya posee una cuenta de acceso, deberá reutilizarse para sus futuros equipos y órdenes de trabajo.
- La cuenta con rol Cliente quedará vinculada exclusivamente a un único registro de Cliente para determinar qué información, equipos, órdenes y presupuestos puede consultar.
- Una cuenta podrá poseer otros roles además de Cliente cuando corresponda, pero no podrá representar a más de un Cliente.
- El cliente deberá disponer de al menos un medio de contacto válido: correo electrónico o número de celular.
- El correo electrónico o el número de celular podrán utilizarse como identificador de acceso.
- El primer ingreso se realizará mediante una credencial temporal que deberá ser reemplazada por una contraseña definida por el cliente.
- El número de orden de trabajo no podrá utilizarse como contraseña.
- No se permitirá el registro público y autónomo de usuarios dentro del MVP.
- El Recepcionista no podrá crear usuarios con roles Administrador, Recepcionista o Técnico ni administrar roles.
- La integración automática con WhatsApp Business continuará fuera del alcance del MVP.

### Clientes, equipos y órdenes de trabajo

- Toda orden de trabajo deberá estar asociada a un cliente y a un equipo.
- Cada equipo estará asociado a un único cliente, mientras que un cliente podrá tener uno o varios equipos.
- Cada orden de trabajo tendrá un único técnico responsable. La participación de múltiples técnicos en una misma orden queda fuera del alcance del MVP.
- Una orden podrá crearse inicialmente sin técnico responsable mientras permanezca en estado `RECIBIDA`, pero deberá tener uno asignado antes de pasar a `EN_DIAGNOSTICO`.
- El Técnico solamente podrá realizar acciones técnicas sobre las órdenes que tenga asignadas.
- Cada orden podrá tener como máximo un diagnóstico.
- El diagnóstico podrá modificarse cuando corresponda, pero no se manejarán versiones independientes dentro del MVP.
- Los cambios relevantes realizados sobre una orden deberán conservarse para mantener su trazabilidad.
- La cancelación normal de una orden solamente estará permitida antes de iniciar la reparación.
- Las órdenes en estado `ENTREGADA` o `CANCELADA` se considerarán finalizadas dentro del flujo del MVP.

### Presupuestos

- Cada orden que requiera presupuesto podrá tener como máximo un presupuesto.
- El presupuesto podrá modificarse mientras se encuentre pendiente de la decisión del cliente.
- Una vez aprobado o rechazado, el presupuesto quedará cerrado y no se manejarán versiones independientes dentro del MVP.
- El total del presupuesto se calculará a partir de la mano de obra y de los repuestos incluidos.
- Solamente el cliente correspondiente podrá aprobar o rechazar su propio presupuesto.
- Si una reparación requiere presupuesto, no podrá iniciarse hasta que el cliente lo haya aprobado.
- Si el cliente rechaza un presupuesto requerido, la orden no continuará hacia reparación y pasará a estado `CANCELADA`.
- Los repuestos incluidos en un presupuesto representan una estimación y no modificarán el stock real.
- Los repuestos presupuestados y los repuestos realmente consumidos se registrarán como conceptos diferentes.

### Inventario y stock

- El stock se representará como un atributo de `Repuesto`; no se utilizará una entidad `Stock` independiente dentro del MVP.
- Las variaciones de existencias deberán conservar su trazabilidad mediante movimientos de stock.
- El consumo real de un repuesto disminuirá las existencias y deberá registrarse de manera consistente con el movimiento correspondiente.
- No se permitirá registrar un consumo que genere existencias negativas.
- El registro de consumos reales corresponderá al rol Técnico sobre sus órdenes asignadas.
- Un usuario con rol Administrador solamente podrá registrar consumos si también posee el rol Técnico y tiene asignada la orden correspondiente.
- El Administrador podrá registrar repuestos, entradas y ajustes de stock.
- El Recepcionista podrá consultar la disponibilidad de repuestos, pero no modificar las existencias.
- Incluir un repuesto en un presupuesto no se considerará un consumo y no modificará el stock real.

## Módulos funcionales

Para organizar las responsabilidades principales del sistema, FixHub se divide en cinco módulos funcionales:

1. Autenticación, usuarios y roles.
2. Clientes y equipos.
3. Órdenes de trabajo.
4. Presupuestos y aprobación.
5. Inventario y stock.

El diagnóstico, el historial y la trazabilidad, el seguimiento del cliente y el dashboard se integran dentro de estos módulos o funcionan de manera transversal, por lo que no se consideran módulos independientes.

La definición detallada de responsabilidades, dependencias, reglas y permisos se encuentra en la [Definición de módulos de la Segunda Entrega](docs/entregas/entrega_02/modulos.md).

## Roles y permisos

FixHub contempla cuatro roles principales dentro del MVP: Administrador, Recepcionista, Técnico y Cliente.

Una misma cuenta podrá tener uno o varios roles simultáneamente. Cuando una cuenta posea más de un rol, acumulará los permisos correspondientes a cada uno.

### Administrador

El Administrador será responsable de la gestión general del sistema. Podrá:

- Administrar usuarios, roles y permisos.
- Crear y gestionar cuentas con roles Administrador, Recepcionista y Técnico.
- Registrar, editar y consultar clientes y equipos.
- Crear órdenes de trabajo, administrar sus datos de recepción y asignar técnicos.
- Consultar todas las órdenes de trabajo.
- Crear, editar y publicar presupuestos, pero no aprobarlos ni rechazarlos en nombre del cliente.
- Registrar, editar y consultar repuestos.
- Gestionar entradas y ajustes de stock.
- Consultar información general e indicadores del sistema.
- Cancelar órdenes cuando las reglas de negocio lo permitan.

Las tareas estrictamente técnicas de diagnóstico, reparación y consumo real de repuestos estarán reservadas al rol Técnico. Un usuario con rol Administrador solamente podrá realizarlas si también posee el rol Técnico y tiene asignada la orden correspondiente.

### Recepcionista

El Recepcionista estará orientado principalmente a la atención del cliente y a la gestión administrativa del proceso de recepción y entrega. Podrá:

- Registrar, editar y consultar clientes y equipos.
- Crear una cuenta de acceso únicamente con rol Cliente cuando registre a un cliente que todavía no posea una, o reutilizar la existente.
- Crear órdenes de trabajo y registrar la información de recepción.
- Asignar o cambiar el técnico responsable.
- Consultar las órdenes de trabajo.
- Crear, editar y publicar presupuestos mientras se encuentren pendientes de la decisión del cliente.
- Consultar el catálogo de repuestos y su disponibilidad.
- Marcar órdenes como listas para retirar.
- Registrar la entrega del equipo.
- Consultar los indicadores operativos que tenga habilitados.

El Recepcionista no podrá administrar roles ni crear cuentas con roles Administrador, Recepcionista o Técnico. Tampoco podrá realizar diagnósticos, reparaciones, modificar existencias ni aprobar o rechazar presupuestos en nombre del cliente.

### Técnico

El Técnico será responsable de las acciones directamente relacionadas con la reparación. Podrá:

- Consultar las órdenes que tenga asignadas.
- Consultar la información necesaria del cliente y del equipo vinculados con esas órdenes.
- Registrar y actualizar el diagnóstico.
- Registrar tareas y observaciones técnicas.
- Iniciar y finalizar la reparación.
- Participar en la elaboración de presupuestos correspondientes a sus órdenes asignadas.
- Consultar repuestos.
- Registrar los consumos reales de repuestos utilizados durante la reparación.

El Técnico no podrá administrar clientes, equipos, usuarios, roles ni existencias generales del inventario.

### Cliente

El Cliente accederá al sistema mediante la cuenta vinculada exclusivamente a su registro. Podrá:

- Iniciar sesión utilizando su cuenta de acceso.
- Consultar únicamente sus propios datos y equipos.
- Consultar el estado y la información habilitada de sus propias reparaciones.
- Consultar sus diagnósticos cuando se encuentren disponibles.
- Consultar sus propios presupuestos.
- Aprobar o rechazar únicamente sus propios presupuestos.
- Consultar su historial de reparaciones cuando corresponda.

El Cliente no podrá acceder a información perteneciente a otros clientes, observaciones internas ni realizar funciones administrativas, técnicas o de inventario.

## Flujo de las órdenes de trabajo

El ciclo de una orden de trabajo representa el proceso de una reparación desde la recepción del equipo hasta su entrega al cliente o su cancelación.

Los estados definidos para el MVP son:

- `RECIBIDA`
- `EN_DIAGNOSTICO`
- `PENDIENTE_APROBACION`
- `APROBADA`
- `EN_REPARACION`
- `FINALIZADA`
- `LISTA_PARA_RETIRAR`
- `ENTREGADA`
- `CANCELADA`

La asignación de un técnico responsable no se considera un estado de la orden. Una orden podrá crearse sin técnico mientras permanezca en `RECIBIDA`, pero deberá tener uno asignado antes de pasar a `EN_DIAGNOSTICO`.

La asignación o cambio del técnico responsable deberá quedar registrada para conservar la trazabilidad de la orden.

Los estados propios del presupuesto se gestionarán de manera independiente y no se duplicarán como estados de la orden de trabajo.

### Flujo sin presupuesto

Cuando la reparación no requiere aprobación previa mediante presupuesto, el flujo será:

`RECIBIDA → EN_DIAGNOSTICO → EN_REPARACION → FINALIZADA → LISTA_PARA_RETIRAR → ENTREGADA`

### Flujo con presupuesto

Cuando la reparación requiere la aprobación previa de un presupuesto, el flujo será:

`RECIBIDA → EN_DIAGNOSTICO → PENDIENTE_APROBACION → APROBADA → EN_REPARACION → FINALIZADA → LISTA_PARA_RETIRAR → ENTREGADA`

Mientras la orden se encuentre en `PENDIENTE_APROBACION`, la reparación no podrá iniciarse.

Si el cliente aprueba el presupuesto, la orden pasará a `APROBADA` y podrá continuar hacia `EN_REPARACION`.

Si el cliente rechaza un presupuesto requerido, la orden pasará a `CANCELADA` y no continuará hacia reparación.

### Transiciones permitidas

Las transiciones normales serán:

- `RECIBIDA → EN_DIAGNOSTICO`
- `EN_DIAGNOSTICO → PENDIENTE_APROBACION`, cuando se requiera presupuesto.
- `EN_DIAGNOSTICO → EN_REPARACION`, cuando no se requiera presupuesto.
- `PENDIENTE_APROBACION → APROBADA`, cuando el cliente apruebe el presupuesto.
- `PENDIENTE_APROBACION → CANCELADA`, cuando el cliente rechace el presupuesto.
- `APROBADA → EN_REPARACION`
- `EN_REPARACION → FINALIZADA`
- `FINALIZADA → LISTA_PARA_RETIRAR`
- `LISTA_PARA_RETIRAR → ENTREGADA`

La cancelación también podrá realizarse desde:

- `RECIBIDA → CANCELADA`
- `EN_DIAGNOSTICO → CANCELADA`
- `PENDIENTE_APROBACION → CANCELADA`
- `APROBADA → CANCELADA`

Una vez iniciada la reparación no se contemplará una cancelación normal dentro del MVP.

Toda cancelación deberá conservar la información necesaria para su trazabilidad, incluyendo el usuario responsable, la fecha y el motivo.

Los estados `ENTREGADA` y `CANCELADA` se consideran estados finales.

## Tecnologías

### Frontend

- HTML
- CSS
- JavaScript
- TypeScript
- Vite

### Backend

- Java
- Spring Boot
- Maven
- Spring Data JPA
- Hibernate
- Spring Security
- JWT
- API REST

### Base de datos

- MySQL

### Control de versiones

- Git
- GitHub

## Despliegue

Para el despliegue se evaluarán servicios administrados compatibles con las tecnologías seleccionadas.

Alternativas consideradas:

- **Frontend:** Vercel o Netlify.
- **Backend:** Render o Railway.
- **Base de datos:** servicio administrado compatible con MySQL.

La selección definitiva se realizará considerando compatibilidad, disponibilidad y costos al momento del despliegue.

## Alcance

El MVP está orientado a talleres y servicios técnicos pequeños o medianos y busca resolver principalmente la gestión operativa de reparaciones.

Quedan fuera del alcance inicial:

- Pagos online.
- Facturación electrónica AFIP/ARCA.
- Integración con WhatsApp Business.
- Aplicación móvil nativa.
- Geolocalización.
- Inteligencia artificial.
- Gestión de múltiples sucursales.
- Contabilidad completa.
- Compras automáticas a proveedores.
- Marketplace de repuestos.

## Integrantes

**Grupo 186**

- Damián Nogueira
- Gabriel Etchegoyen
- Manuel Galarza 

## Tutor

- Sofia Raia

## Estado del proyecto

Proyecto en etapa de diseño correspondiente al Trabajo Final Integrador de la Tecnicatura Universitaria en Programación a Distancia.

Actualmente se encuentra en desarrollo la documentación de la Segunda Entrega. La definición de los módulos funcionales ya fue consolidada y documentada, mientras continúan el diseño de la base de datos y las restantes tareas de documentación previas al inicio de la implementación.

## Instalación y ejecución

Las instrucciones de instalación, configuración y ejecución local se incorporarán a medida que se implemente el frontend, backend y la base de datos.

## Trabajo Final Integrador

Proyecto desarrollado como Trabajo Final Integrador de la Tecnicatura Universitaria en Programación a Distancia.
