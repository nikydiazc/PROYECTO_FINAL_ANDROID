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

    val estado: String = "Pendiente",
    val fechaCreacion: Timestamp? = null,
    val fechaRespuesta: Timestamp? = null,
    val creadaPor: String = "",
    val asignadaA: String = "",
    val comentarioRespuesta: String = ""
)
