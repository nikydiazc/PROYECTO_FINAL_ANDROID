📱 Aplicación Android – Gestión de Solicitudes de Limpieza
🧹 Descripción General

Esta aplicación Android permite gestionar solicitudes de limpieza en tiempo real, pensada para operaciones internas en instalaciones como centros comerciales.
El sistema facilita la creación de solicitudes, asignación a supervisores y la actualización de tareas con evidencia fotográfica de antes y después.

La app está diseñada para tres tipos principales de usuarios:

Crear Tarea: Puede generar solicitudes con descripción, ubicación, piso y foto inicial.

Administrador: Puede asignar tareas, editar, eliminar, ver listas completas y responder tareas si es necesario.

Realizar Tarea / Supervisores: Ven tareas asignadas o pendientes y pueden subir evidencia fotográfica de la realización.

🔐 Roles de Usuario
crear_tarea:	Crea solicitudes de limpieza. No puede ver, editar ni eliminar tareas.

administrador: Puede ver todas las tareas (pendientes, asignadas, realizadas), asignar supervisores, rechazar, editar y eliminar tareas, además de responder con evidencia.

realizar_tarea / supervisores: Pueden ver tareas asignadas, responder con foto y comentario, marcar como realizadas. No pueden editar ni eliminar.

Lo usuarios de prueba son:
• Usuario1 - Crear tareas:
- email: crear_tarea@miapp.com
- contraseña: Creartarea01

• Usuario2 - Administrador:
- email: administrador@miapp.com
- contraseña: Administrador02

• Usuario3 - Realizar tareas (genérico, sin asignación)
- email: realizar_tarea@miapp.com
- contraseña: Realizartarea03

Algunos usuarios de supervisores: 
- email: delfina.cabello@miapp.com/contraseña: delfina.cabello
- email: rodrigo.reyes@miapp.com/contraseña: rodrigo.reyes
- email: maria.caruajulca@miapp.com/contraseña: maria.caruajulca
- email: john.vilchez@miapp.com/ contraseña: john.vilchez

  ✨ Funcionalidades Principales
✔ Crear Solicitudes

Los usuarios pueden ingresar:
Descripción
Ubicación (texto libre)
Piso (selector desplegable desde -6 a 6)
Fotografía de evidencia inicial

✔ Panel de Tareas (Muro)
Se muestran tres vistas:

🟥 Tareas Pendientes
Solo aparecen tareas sin asignación
• Admin puede deslizar:
Izquierda: Rechazar (eliminar)
Derecha: Asignar a supervisor

🟧 Tareas Asignadas
Filtrable por:
Supervisor asignado
Piso
Texto (descripción o ubicación)
Supervisores pueden responder con foto

🟩 Tareas Realizadas
Muestra evidencia “ANTES y DESPUÉS”
Solo Admin puede eliminar

✔ Subida de Fotografías
Cámara nativa
Permite elegir desde galería (solo para crear, para la respuesta es solo con fotografia desde la cámara)
Evidencia de respuesta obligatoria
Se almacena en Firebase Storage

🛠 Tecnologías Utilizadas

Kotlin
Firebase Firestore → almacenamiento de tareas
Firebase Storage → fotos antes/después
Firebase Authentication → creación de usuarios autorizados para acceder
Camera Intent → captura de evidencia
RecyclerView + CardView
Glide → carga de imágenes







