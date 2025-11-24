package cl.unab.proyecto_final_android.ui.muro

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class TareaAdapterTest {

    // Simulación de la función formatDate del Adapter (si la hiciste pública/package-private)
    private fun formatTimestamp(timestamp: Timestamp?): String {
        // Simula la lógica de formateo de fecha que tienes en el Adapter
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return if (timestamp != null) {
            dateFormat.format(Date(timestamp.toDate().time))
        } else {
            "—"
        }
    }
    @Test
    fun formatTimestamp_conTimestampValido_debeFormatearCorrectamente() {

        val date = Date(1732420200000L)
        val timestamp = Timestamp(date)

        val expected = "24/11/2025 09:30"
        val actual = formatTimestamp(timestamp)


        assertEquals(expected, actual)
    }
    @Test
    fun formatTimestamp_conTimestampNulo_debeRetornarGuion() {
        val expected = "—"
        val actual = formatTimestamp(null)
        assertEquals(expected, actual)
    }
}