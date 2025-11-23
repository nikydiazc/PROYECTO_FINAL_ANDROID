package cl.unab.proyecto_final_android

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MurosTareasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuroTareasBinding
    private lateinit var firestore: FirebaseFirestore

    private lateinit var adapter: TareaAdapter

    private var rolUsuario: String = LoginActivity.ROL_REALIZAR

    // Estado actual del muro: "Pendiente" o "Realizada"
    private var estadoActual: String = "Pendiente"

    // Listas para filtros
    private val listaOriginal = mutableListOf<Tarea>()
    private val listaFiltrada = mutableListOf<Tarea>()

    // Filtro de texto (busca en descripción o ubicación)
    private var textoFiltro: String = ""

    // Filtro por piso
    private var pisoFiltro: String = "Todos"

    private var listenerTareas: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMuroTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_REALIZAR

        configurarRecyclerView()
        configurarSpinnerPiso()
        configurarEventos()
        configurarSwipeSiAdmin()

        cargarTareasDesdeFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerTareas?.remove()
    }

    private fun configurarRecyclerView() {
        adapter = TareaAdapter(
            tareas = listaFiltrada,
            rolUsuario = rolUsuario,
            onResponderClick = { tarea ->
                // Aquí más adelante implementamos flujo de respuesta (foto después, comentario, etc.)
                Toast.makeText(
                    this,
                    "Responder tarea ${tarea.id} (pendiente implementar)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        binding.rvTareas.apply {
            layoutManager = LinearLayoutManager(this@MurosTareasActivity)
            adapter = this@MurosTareasActivity.adapter
        }
    }

    private fun configurarSpinnerPiso() {
        val pisos = mutableListOf("Todos")
        for (piso in 6 downTo -6) {
            pisos.add(piso.toString())
        }

        val adapterPisos = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            pisos
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spFiltroPiso.adapter = adapterPisos
    }

    private fun configurarEventos() {
        // Botones de estado (Pendientes / Realizadas)
        binding.btnTareasPendientes.setOnClickListener {
            estadoActual = "Pendiente"
            cargarTareasDesdeFirestore()
        }

        binding.btnTareasRealizadas.setOnClickListener {
            estadoActual = "Realizada"
            cargarTareasDesdeFirestore()
        }

        // Filtro por piso
        binding.spFiltroPiso.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                pisoFiltro = parent?.getItemAtPosition(position)?.toString() ?: "Todos"
                aplicarFiltrosLocales()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nada
            }
        }

        // Búsqueda por descripción O ubicación (un solo campo)
        binding.etBuscarDescripcionOUbicacion.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textoFiltro = s?.toString()?.trim() ?: ""
                aplicarFiltrosLocales()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) { }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) { }
        })
    }

    private fun configurarSwipeSiAdmin() {
        if (rolUsuario != LoginActivity.ROL_ADMIN) return

        val callback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val tarea = listaFiltrada.getOrNull(position) ?: return

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // Rechazar tarea
                        rechazarTarea(tarea)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // Asignar a usuario genérico realizar_tarea (después podemos cambiar a supervisores específicos)
                        asignarTareaAGenerico(tarea)
                    }
                }

                // Volver a dibujar el ítem mientras Firestore se actualiza
                adapter.notifyItemChanged(position)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.rvTareas)
    }

    private fun rechazarTarea(tarea: Tarea) {
        if (tarea.id.isEmpty()) return

        val docRef = firestore.collection("tareas").document(tarea.id)
        docRef.update(
            mapOf(
                "estado" to "Rechazada",
                "fechaRespuesta" to Timestamp.now()
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Tarea rechazada", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al rechazar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun asignarTareaAGenerico(tarea: Tarea) {
        if (tarea.id.isEmpty()) return

        val docRef = firestore.collection("tareas").document(tarea.id)
        docRef.update(
            mapOf(
                "asignadaA" to "realizar_tarea",
                "estado" to "Pendiente" // Podrías cambiar a "En Proceso" si quieres
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Tarea asignada a realizar_tarea", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al asignar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarTareasDesdeFirestore() {
        // Detener listener anterior si existía
        listenerTareas?.remove()

        var query = firestore.collection("tareas")
            .whereEqualTo("estado", estadoActual)

        // Si quieres aplicar filtro de piso desde Firestore, podrías descomentar esto,
        // pero ojo, estaría duplicado con el filtro local:
        if (pisoFiltro != "Todos") {
            query = query.whereEqualTo("piso", pisoFiltro)
        }

        listenerTareas = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(
                    this,
                    "Error al cargar tareas: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            val documentos = snapshot?.documents ?: emptyList()
            listaOriginal.clear()

            for (doc in documentos) {
                val tarea = doc.toObject(Tarea::class.java)
                if (tarea != null) {
                    listaOriginal.add(tarea)
                }
            }

            aplicarFiltrosLocales()
        }
    }

    private fun aplicarFiltrosLocales() {
        val filtroLower = textoFiltro.lowercase()

        val filtradas = listaOriginal.filter { tarea ->
            val coincideTexto = if (filtroLower.isEmpty()) {
                true
            } else {
                tarea.descripcion.lowercase().contains(filtroLower) ||
                        tarea.ubicacion.lowercase().contains(filtroLower)
            }

            val coincidePiso = if (pisoFiltro == "Todos") {
                true
            } else {
                tarea.piso == pisoFiltro
            }

            coincideTexto && coincidePiso
        }

        listaFiltrada.clear()
        listaFiltrada.addAll(filtradas)
        adapter.actualizarLista(listaFiltrada.toList())
    }
}
