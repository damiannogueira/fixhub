# FixHub

Sistema Integral de Gestión para Talleres y Servicios Técnicos.

## Descripción

FixHub es una aplicación web orientada a talleres y servicios técnicos pequeños y medianos que busca centralizar y organizar el ciclo completo de una reparación, desde la recepción del equipo hasta su entrega al cliente.

La propuesta surge a partir de una problemática real observada en un servicio técnico, donde diferentes procesos de gestión se realizan mediante herramientas y canales no integrados, como planillas de cálculo, WhatsApp, llamadas telefónicas y registros manuales.

A partir de esta situación, el equipo identificó una problemática que puede presentarse también en otros tipos de servicios técnicos, como reparación de celulares, electrónica, computación, electrodomésticos y otros rubros.

## Objetivo

Desarrollar el MVP de FixHub, un Sistema Integral de Gestión para Talleres y Servicios Técnicos que permita administrar el ciclo completo de reparaciones, centralizando la información y automatizando tareas manuales.

## Funcionalidades principales del MVP

- Autenticación e inicio de sesión con roles.
- Gestión de clientes.
- Gestión de equipos asociados a clientes.
- Gestión de órdenes de trabajo.
- Asignación de técnicos.
- Registro de diagnósticos.
- Gestión de presupuestos.
- Aprobación o rechazo de presupuestos por parte del cliente.
- Gestión de repuestos e inventario.
- Actualización automática del stock al utilizar repuestos.
- Historial de cambios de las órdenes de trabajo.
- Historial de reparaciones por equipo.
- Seguimiento del estado de las reparaciones por parte del cliente.
- Dashboard básico con indicadores de gestión.
- Interfaz web responsive.

## Módulos funcionales

Para organizar las responsabilidades principales del sistema, FixHub se divide en cinco módulos funcionales:

1. Autenticación, usuarios y roles.
2. Clientes y equipos.
3. Órdenes de trabajo.
4. Presupuestos y aprobación.
5. Inventario y stock.

El diagnóstico, el historial y la trazabilidad, el seguimiento del cliente y el dashboard se integran dentro de estos módulos o funcionan de manera transversal, por lo que no se consideran módulos independientes.

La definición detallada de responsabilidades, dependencias, reglas y permisos se encuentra en la [Definición de módulos de la Segunda Entrega](docs/entregas/entrega_02/modulos.md).

## Roles del sistema

FixHub contempla los siguientes roles:

- **Administrador:** gestiona usuarios, permisos, inventario, órdenes y métricas del sistema.
- **Recepcionista:** registra clientes y equipos, crea órdenes de trabajo y gestiona la recepción y entrega.
- **Técnico:** consulta las órdenes asignadas, registra diagnósticos, tareas realizadas y repuestos utilizados.
- **Cliente:** consulta el estado de sus reparaciones y puede aprobar o rechazar presupuestos.

En servicios técnicos pequeños, una misma persona puede desempeñar más de una función dentro del negocio.

Una cuenta de usuario podrá tener uno o varios roles simultáneamente. Cuando un usuario tenga más de un rol, acumulará los permisos correspondientes a cada uno.

## Flujo principal de una orden de trabajo

El ciclo de una orden de trabajo depende de si la reparación requiere o no un presupuesto.

Cuando se requiere presupuesto, el flujo principal es:

`RECIBIDA → EN_DIAGNOSTICO → PENDIENTE_APROBACION → APROBADA → EN_REPARACION → FINALIZADA → LISTA_PARA_RETIRAR → ENTREGADA`

Cuando no se requiere presupuesto, la orden puede avanzar directamente desde `EN_DIAGNOSTICO` hacia `EN_REPARACION`.

Si el cliente rechaza un presupuesto requerido, la orden pasa de `PENDIENTE_APROBACION` a `CANCELADA` y no continúa hacia reparación.

Una orden que requiera presupuesto no podrá pasar a `EN_REPARACION` hasta que dicho presupuesto haya sido aprobado por el cliente. La cancelación normal de una orden solamente se contempla antes de iniciar la reparación.

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
