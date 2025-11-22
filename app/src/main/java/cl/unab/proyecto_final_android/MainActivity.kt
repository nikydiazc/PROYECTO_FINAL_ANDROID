package cl.unab.proyecto_final_android

import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var rviewTareasDia: RecyclerView
    private lateinit var btnFiltrar: Button
    private lateinit var tvBienvenida: TextView
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // Obtener nombre del usuario y establecer bienvenida
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        uid?.let {
            firestore.collection("users").document(it).get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombre") ?: ""
                    tvBienvenida.text = "Bienvenido/a $nombre"
                }
        }

        // Configurar RecyclerView (layout manager, adapter)
        rviewTareasDia.layoutManager = LinearLayoutManager(this)
        // adapter se definirá luego

        // Escuchar selección de día en el calendario
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            // Mes se cuenta desde 0 en CalendarView => +1
            val selectedDate = String.format("%04d-%02d-%02d", year, month+1, dayOfMonth)
            cargarTareasParaFecha(selectedDate)
        }

        // Botón de filtros
        btnFiltrar.setOnClickListener {
            // Mostrar diálogo de filtros (turno, zona, estado)
            mostrarDialogoFiltros()
        }

        // Opcional: cargar tareas del día actual al inicio
        val today = Calendar.getInstance()
        val todayStr = String.format("%04d-%02d-%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH)+1, today.get(Calendar.DAY_OF_MONTH))
        cargarTareasParaFecha(todayStr)
    }

    private fun cargarTareasParaFecha(fecha: String) {
        // Suponiendo que tienes un campo fechaProgramada o bien haces consulta por 'horarioCreacion'
        // Convertir fecha a timestamps de inicio y fin del día
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = sdf.parse(fecha)
        startDate?.let { date ->
            val startTs = Timestamp(date)
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            val endTs = Timestamp(cal.time)
            firestore.collection("tasks")
                .whereGreaterThanOrEqualTo("horarioCreacion", startTs)
                .whereLessThanOrEqualTo("horarioCreacion", endTs)
                .get()
                .addOnSuccessListener { query ->
                    val tareas = query.documents.map { doc -> /* mapear a modelo */ }
                    // pasar tareas al adapter del RecyclerView
                }
        }
    }

    private fun mostrarDialogoFiltros() {
        // Implementar UI simple para elegir turno, zona, estado
        // Luego volver a llamar cargarTareasParaFecha con filtros aplicados
    }
}
