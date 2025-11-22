package cl.unab.proyecto_final_android

data class Tarea(
    val descripcion: String,
    val ubicacion: String,
    val piso: String,
    val fechaCreacion: String,
    val estado: String,
    val imagenResId: Int? = null  // o URL si usas imágenes desde la web
)
