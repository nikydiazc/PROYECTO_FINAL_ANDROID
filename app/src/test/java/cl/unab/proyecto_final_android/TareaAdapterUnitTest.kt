package cl.unab.proyecto_final_android

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaAdapterUnitTest {

    private fun formatTimestamp(timestamp: Timestamp?): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return if (timestamp != null) {
            dateFormat.format(Date(timestamp.toDate().time))
        } else {
            "—"
        }
    }

    @Test
    fun formatTimestamp_timestampNulo_debeRetornarGuion() {
        assertEquals("—", formatTimestamp(null))
    }
}