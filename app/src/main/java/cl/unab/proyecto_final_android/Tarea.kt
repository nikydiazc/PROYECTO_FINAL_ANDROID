package cl.unab.proyecto_final_android

import com.google.firebase.Timestamp

data class Tarea(
    val id: String = "",
    val descripcion: String = "",
    val ubicacion: String = "",
    val piso: String = "",
    val imagenUrl: String = "",
    val estado: String = "",
    val fechaCreacion: Timestamp? = null
)
