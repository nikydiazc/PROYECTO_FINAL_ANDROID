package cl.unab.proyecto_final_android

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Tarea(
    @DocumentId val id: String = "",

    val descripcion: String? = null,
    val ubicacion: String? = null,
    val piso: String? = null,
    val estado: String = "Pendiente",

    // URLs
    val fotoAntesUrl: String? = null,
    val fotoDespuesUrl: String? = null,

    // Fechas
    val fechaCreacion: Timestamp = Timestamp.now(),
    val fechaRespuesta: Timestamp? = null,

    // Usuarios
    val creadaPor: String? = null,
    val asignadaA: String? = null
)