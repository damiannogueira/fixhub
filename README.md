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

## Roles del sistema

FixHub contempla los siguientes roles:

- **Administrador:** gestiona usuarios, permisos, inventario, órdenes y métricas del sistema.
- **Recepcionista:** registra clientes y equipos, crea órdenes de trabajo y gestiona la recepción y entrega.
- **Técnico:** consulta las órdenes asignadas, registra diagnósticos, tareas realizadas y repuestos utilizados.
- **Cliente:** consulta el estado de sus reparaciones y puede aprobar o rechazar presupuestos.

En servicios técnicos pequeños, una misma persona puede desempeñar más de una función dentro del negocio.

## Flujo principal de una orden de trabajo

El ciclo principal de una orden contempla los siguientes estados:

`Recibido → En diagnóstico → Pendiente de aprobación → Aprobado → En reparación → Finalizado → Listo para retirar → Entregado`

También se contemplan el rechazo del presupuesto y la cancelación de la orden cuando corresponda.

Una orden que requiera presupuesto no podrá pasar al estado **En reparación** hasta que dicho presupuesto haya sido aprobado por el cliente.

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

## Estado del proyecto

Proyecto en etapa inicial de planificación y diseño correspondiente al Trabajo Final Integrador.

## Instalación y ejecución

Las instrucciones de instalación, configuración y ejecución local se incorporarán a medida que se implemente el frontend, backend y la base de datos.

## Trabajo Final Integrador

Proyecto desarrollado como Trabajo Final Integrador de la Tecnicatura Universitaria en Programación a Distancia.