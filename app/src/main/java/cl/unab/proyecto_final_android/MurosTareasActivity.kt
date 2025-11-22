package cl.unab.proyecto_final_android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MurosTareasActivity : AppCompatActivity() {

    private lateinit var rvTareas: RecyclerView
    private lateinit var adapter: TareaAdapter
    private val listaTareas = mutableListOf<Tarea>()

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muro_tareas)

        rvTareas = findViewById(R.id.rvTareas)
        rvTareas.layoutManager = LinearLayoutManager(this)

        adapter = TareaAdapter(listaTareas)
        rvTareas.adapter = adapter

        cargarTareas()
    }

    private fun cargarTareas() {
        db.collection("tareas")
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error al cargar tareas: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    listaTareas.clear()
                    for (doc in snapshot.documents) {
                        val tarea = doc.toObject(Tarea::class.java)
                        if (tarea != null) {
                            // Guardamos el id del documento también por si luego quieres actualizar/borrar
                            listaTareas.add(tarea.copy(id = doc.id))
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}
