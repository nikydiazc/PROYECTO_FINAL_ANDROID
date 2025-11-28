package cl.unab.proyecto_final_android

import cl.unab.proyecto_final_android.util.ColorStatus
import cl.unab.proyecto_final_android.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorStatusTest {

    @Test
    fun getColorResource_pendiente_devuelveDrawablePendiente() {
        val result = ColorStatus.getColorResource("Pendiente")
        assertEquals(R.drawable.bg_estado_pendiente, result)
    }

    @Test
    fun getColorResource_asignada_devuelveDrawableAsignada() {
        val result = ColorStatus.getColorResource("Asignada")
        assertEquals(R.drawable.bg_estado_asignada, result)
    }

    @Test
    fun getColorResource_realizada_devuelveDrawableRealizada() {
        val result = ColorStatus.getColorResource("Realizada")
        assertEquals(R.drawable.bg_estado_realizada, result)
    }

    @Test
    fun getColorResource_rechazada_devuelveDrawableRechazada() {
        val result = ColorStatus.getColorResource("Rechazada")
        assertEquals(R.drawable.bg_estado_rechazada, result)
    }

    @Test
    fun getColorResource_estadoDesconocido_devuelveDrawableDefault() {
        val result = ColorStatus.getColorResource("OtraCosa")
        assertEquals(R.drawable.bg_estado_default, result)
    }

    @Test
    fun getColorResource_esCaseInsensitive() {
        val result1 = ColorStatus.getColorResource("pendiente")
        val result2 = ColorStatus.getColorResource("PENDIENTE")
        val result3 = ColorStatus.getColorResource("PeNdIeNtE")

        assertEquals(R.drawable.bg_estado_pendiente, result1)
        assertEquals(R.drawable.bg_estado_pendiente, result2)
        assertEquals(R.drawable.bg_estado_pendiente, result3)
    }
}
