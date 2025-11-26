package cl.unab.proyecto_final_android.util

import cl.unab.proyecto_final_android.R // Importa los recursos de tu proyecto

/**
 * Objeto utilitario que mapea el estado de una tarea (String)
 * al recurso drawable (fondo de color/forma) correspondiente.
 */
object ColorStatus {

    /**
     * Retorna el ID del recurso de fondo (Drawable) basado en el estado.
     * @param estado String con el estado de la tarea (ej: "Pendiente").
     * @return ID de recurso Int (R.drawable.bg_estado_pendiente).
     */
    fun getColorResource(estado: String): Int {
        return when (estado.uppercase()) {
            "PENDIENTE" -> R.drawable.bg_estado_pendiente
            "ASIGNADA" -> R.drawable.bg_estado_asignada
            "REALIZADA" -> R.drawable.bg_estado_realizada
            "RECHAZADA" -> R.drawable.bg_estado_rechazada
            else -> R.drawable.bg_estado_default // Asegúrate de tener este recurso
        }
    }
}