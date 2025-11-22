package cl.unab.proyecto_final_android

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.app.AlertDialog
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth


class MurosTareasActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private lateinit var rvTareas: RecyclerView
    private lateinit var adapter: TareaAdapter
    private val listaTareas = mutableListOf<Tarea>()

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var tareaSeleccionada: Tarea? = null
    private var respuestaImageUri: Uri? = null

    // Solo galería para foto de respuesta
    private val pickRespuestaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            respuestaImageUri = it
            subirRespuestaAFirebase()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muro_tareas)

        rvTareas = findViewById(R.id.rvTareas)
        rvTareas.layoutManager = LinearLayoutManager(this)

        adapter = TareaAdapter(listaTareas) { tarea ->
            onResponderClick(tarea)
        }
        rvTareas.adapter = adapter

        cargarTareas()

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_muro_tareas

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    startActivity(Intent(this, CrearTareaActivity::class.java))
                    true
                }

                R.id.nav_muro_tareas -> {
                    // Ya estás en el muro
                    true
                }

                R.id.nav_usuario -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

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
                            // Guardar el id del documento
                            listaTareas.add(tarea.copy(id = doc.id))
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
    private fun onResponderClick(tarea: Tarea) {
        tareaSeleccionada = tarea

        // Solo galería por ahora
        val options = arrayOf("Galería")
        AlertDialog.Builder(this)
            .setTitle("Foto de respuesta")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickRespuestaLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun subirRespuestaAFirebase() {
        val tarea = tareaSeleccionada ?: return
        val uri = respuestaImageUri ?: return

        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("respuestas/${tarea.id}_${UUID.randomUUID()}.jpg")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val urlRespuesta = downloadUri.toString()

                        db.collection("tareas")
                            .document(tarea.id)
                            .update(
                                mapOf(
                                    "respuestaUrl" to urlRespuesta,
                                    "estado" to "Completada",
                                    "fechaRespuesta" to Timestamp.now()
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Respuesta registrada",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Error al actualizar tarea: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al obtener URL de respuesta: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al subir foto de respuesta: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
