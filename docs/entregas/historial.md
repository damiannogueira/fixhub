# Historial del Trabajo Final Integrador

Este documento resume de forma breve los principales cambios realizados durante el desarrollo del Trabajo Final Integrador.

## Propuesta inicial — MyPapyro

- Se presentó inicialmente **MyPapyro** como propuesta para el Trabajo Final Integrador.
- La propuesta no fue aprobada por el comité para continuar con el desarrollo del TFI.
- A partir de esa devolución, el equipo decidió cambiar la propuesta y presentar un nuevo proyecto.

## Primera Entrega — FixHub

- Se presentó **FixHub** como nueva propuesta del Trabajo Final Integrador.
- Se definieron la problemática, el objetivo, el alcance inicial, el MVP y las tecnologías previstas.
- La propuesta fue aprobada por la tutora sin observaciones adicionales.

## Segunda Entrega

- Se definieron y documentaron los cinco módulos funcionales del sistema.
- Se documentaron los requerimientos, reglas de negocio, roles, permisos y flujo de las órdenes de trabajo.
- Se realizó el diseño relacional de la base de datos con su DER, diccionario de datos y justificación.
- A partir de la revisión de la tutora se incorporó el diagrama de estados de las órdenes de trabajo y se creó este historial de seguimiento.

## Revisión posterior — Política de roles del MVP

- En la Segunda Entrega se aprobó que una cuenta pudiera combinar Cliente con roles internos.
- Posteriormente, los tres integrantes acordaron que Cliente será exclusivo y no podrá combinarse con Administrador, Recepcionista ni Técnico.
- Administrador, Recepcionista y Técnico podrán combinarse libremente y sus permisos se acumularán.
- La vinculación de una cuenta a un Cliente deberá respetar esta exclusividad y conservar el acceso únicamente a sus datos y acciones habilitadas.
- La decisión no modifica los cinco módulos ni las relaciones del DER. Su aplicación requiere validación del backend y coherencia del frontend y la documentación.
