📱 **PROYECTO FINAL – App de Gestión de Solicitudes de Limpieza**

Autor: Nicole Díaz
Curso: Desarrollo de Aplicaciones Móviles
Tecnologías: Android + Kotlin + Firebase

🧹 Descripción General

Esta aplicación móvil permite gestionar solicitudes de limpieza internas dentro de un centro comercial u operación similar.
El sistema organiza las tareas, permite enviar fotos antes/después y facilita la coordinación entre jefaturas y supervisores, evitando que las solicitudes se pierdan en WhatsApp.

La aplicación considera flujos reales de operación, roles definidos y manejo de evidencia.

👥 Roles de Usuario
🟦 1. Crear Tarea

Crea solicitudes con descripción, ubicación, piso y foto inicial.

No ve ni edita tareas.

🟧 2. Administrador

Ve todas las tareas.

Puede asignar supervisores, editar, eliminar, rechazar y responder tareas.

Acceso completo.

🟩 3. Realizar Tarea / Supervisores

Ven tareas asignadas.

Responden con foto después.

No pueden editar ni eliminar.

🧪 Usuarios de Prueba
👉 Crear tareas

correo: crear_tarea@miapp.com

contraseña: Creartarea01

👉 Administrador

correo: administrador@miapp.com

contraseña: Administrador02

👉 Realizar tareas

correo: realizar_tarea@miapp.com

contraseña: Realizartarea03

👉 Supervisores reales

(usan contraseña = primera parte del correo)

delfina.cabello@miapp.com

rodrigo.reyes@miapp.com

maria.caruajulca@miapp.com

john.vilchez@miapp.com
…y más.

✨ Funcionalidades Principales
✔ Crear Solicitudes

El usuario ingresar:

Descripción

Ubicación

Piso (desde -6 a 6)

Foto de evidencia inicial

Se guarda en Firestore + Storage

✔ Muro de Tareas (Dashboard)

La app tiene un panel dividido en:

🟥 Pendientes

No tienen asignación

Acciones para el Admin:

Swipe izquierda → Rechazar

Swipe derecha → Asignar supervisor

🟧 Asignadas

Filtros dinámicos: supervisor, piso, búsqueda por texto

Supervisores pueden responder con foto

🟩 Realizadas

Muestra evidencia ANTES / DESPUÉS con ViewPager

Solo Admin puede eliminar

✔ Subida de Fotografías

Cámara nativa

Galería solo para crear tareas

Respuestas siempre con cámara (evidencia en tiempo real)

Se almacenan en Firebase Storage

✔ Autenticación y Sesiones

Firebase Authentication

App guarda la sesión con SharedPreferences

Solo se cierra al presionar “Cerrar sesión”


