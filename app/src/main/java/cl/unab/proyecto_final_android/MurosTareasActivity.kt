package cl.unab.proyecto_final_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MurosTareasActivity : AppCompatActivity() {

    private lateinit var rvTareas: RecyclerView
    private lateinit var adapter: TareaAdapter
    private val listaTareas = mutableListOf<Tarea>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muro_tareas)

        rvTareas = findViewById(R.id.rvTareas)

        rvTareas.layoutManager = LinearLayoutManager(this)
        adapter = TareaAdapter(listaTareas) { tarea ->
            // Acción cuando se pulsa "Responder"
            // Por ejemplo abrir otra actividad con detalles de la tarea
        }
        rvTareas.adapter = adapter

        cargarTareas()  // Método para cargar tareas (temporal o desde DB)
    }

    private fun cargarTareas() {
        // Ejemplo de datos de prueba
        listaTareas.add(
            Tarea(
                descripcion = "Basurero lleno en zona C",
                ubicacion = "Frente a Ripley",
                piso = "Piso 1",
                fechaCreacion = "17/11/2025 14:35",
                estado = "Pendiente",
                imagenResId = R.drawable.camera_icon
            )
        )
        // ... añade más tareas
        adapter.notifyDataSetChanged()
    }
}
