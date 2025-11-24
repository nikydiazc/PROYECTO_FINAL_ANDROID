package cl.unab.proyecto_final_android

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Tarea(
    @DocumentId val id: String = "",

    // Todos los Strings que pueden faltar o ser null deben ser String?
    val descripcion: String? = null,
    val ubicacion: String? = null,
    val piso: String? = null,
    val estado: String = "Pendiente", // Asumimos que "estado" siempre se inicializa

    // URLs
    val fotoAntesUrl: String? = null,
    val fotoDespuesUrl: String? = null,

    // Fechas
    val fechaCreacion: Timestamp = Timestamp.now(), // Asumimos que existe
    val fechaRespuesta: Timestamp? = null, // Puede ser null

    // Usuarios
    val creadaPor: String? = null,
    val asignadaA: String? = null
)