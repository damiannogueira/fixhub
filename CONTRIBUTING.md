# Contribución a FixHub

## Flujo de trabajo

1. Nunca trabajar directamente sobre `main`.
2. Antes de comenzar una tarea, actualizar la rama `main` local.
3. Crear una rama individual para cada tarea.
4. Usar uno de los siguientes prefijos según corresponda:
   - `feat/`
   - `fix/`
   - `docs/`
   - `chore/`
   - `refactor/`
   - `test/`
5. Escribir los nombres de las ramas en minúsculas y separar las palabras con guiones. Por ejemplo: `docs/preparar-segunda-entrega`.
6. Realizar commits siguiendo Conventional Commits y redactar sus mensajes en español.
7. Revisar el código fuente real y el diff antes de crear cada commit.
8. Subir la rama correspondiente y abrir un Pull Request.
9. Damián, como propietario del repositorio, revisa y gestiona los Pull Requests.
10. Integrar los cambios únicamente después de su revisión.
11. Después de la integración, actualizar la rama `main` local antes de comenzar otra tarea.

## Responsabilidad del equipo

Las decisiones de análisis, diseño, arquitectura e implementación son responsabilidad de los integrantes del equipo.

Cada integrante debe comprender y poder explicar los cambios que incorpora al proyecto.

## Herramientas de apoyo

Las herramientas de inteligencia artificial, cuando se utilicen, se limitan a funciones de tutoría y revisión, de acuerdo con las pautas establecidas para el Trabajo Final Integrador.

## Revisión de cambios

Antes de aceptar un cambio se debe:

- Revisar el código fuente o documentación modificada.
- Revisar el diff correspondiente.
- Verificar que el cambio respete el alcance acordado.
- Confirmar que no se hayan incorporado archivos ajenos a la tarea.

## Archivos excluidos

No incluir secretos, credenciales, configuraciones locales, dependencias ni resultados de compilación o construcción.

## Formato de archivos

Cada integrante debe configurar su editor para respetar las reglas definidas en `.editorconfig` y `.gitattributes`.

Los archivos deben conservar la codificación, indentación y finales de línea establecidos por el repositorio, independientemente del sistema operativo utilizado.

Antes de incorporar cambios, se debe verificar que no se hayan introducido modificaciones de formato o finales de línea ajenas a la tarea.

## Compatibilidad

Los cambios deben mantener la compatibilidad del repositorio con Windows y macOS.
