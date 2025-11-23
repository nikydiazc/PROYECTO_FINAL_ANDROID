package cl.unab.proyecto_final_android

import com.google.firebase.Timestamp

data class Tarea(
    val id: String = "",
    val descripcion: String = "",
    val ubicacion: String = "",
    val piso: String = "",

    // Imagen antes
    val fotoAntesUrl: String = "",

    // Imagen después (respuesta)
    val fotoDespuesUrl: String = "",

    // Estado de la tarea: "Pendiente", "En Proceso", "Realizada", "Rechazada"
    val estado: String = "Pendiente",

    // Fechas
    val fechaCreacion: Timestamp? = null,
    val fechaRespuesta: Timestamp? = null,

    // Quién la creó (usuario o uid)
    val creadaPor: String = "",

    // A quién está asignada (usuario o uid del supervisor / realizar_tarea)
    val asignadaA: String = "",

    // Comentario opcional al responder la tarea
    val comentarioRespuesta: String = ""
)
