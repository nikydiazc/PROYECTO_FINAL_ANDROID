package cl.unab.proyecto_final_android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MuroTareasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuroTareasBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: TareaAdapter
    private var listenerTareas: ListenerRegistration? = null

    // rol y usuario actual
    private var rolUsuario: String = LoginActivity.ROL_REALIZAR
    private var usernameActual: String = ""

    // es admin si el username es "administrador" o "administrador@miapp.com"
    private val esAdmin: Boolean
        get() = usernameActual.equals("administrador", ignoreCase = true) ||
                usernameActual.equals("administrador@miapp.com", ignoreCase = true)

    // listas de tareas
    private val listaOriginal = mutableListOf<Tarea>()
    private val listaFiltrada = mutableListOf<Tarea>()

    // modo del muro: PENDIENTES / REALIZADAS / ASIGNADAS
    private var modoMuro: String = "PENDIENTES"

    // filtros
    private var textoBusqueda: String = ""
    private var pisoSeleccionado: String = "Todos"
    private var filtroSupervisor: String? = null

    data class SupervisorUsuario(
        val nombreVisible: String,
        val username: String
    )

    // supervisores (ajusta username según lo que guardes en Firestore en asignadaA)
    private val listaSupervisores = listOf(
        SupervisorUsuario("Delfina Cabello (Poniente)", "delfina.cabello"),
        SupervisorUsuario("Rodrigo Reyes (Poniente)", "rodrigo.reyes"),
        SupervisorUsuario("Maria Caruajulca (Poniente)", "maria.caruajulca"),
        SupervisorUsuario("Cristian Vergara (Poniente)", "cristian.vergara"),
        SupervisorUsuario("Enrique Mendez (Poniente)", "enrique.mendez"),
        SupervisorUsuario("Norma Marican (Poniente)", "norma.marican"),
        SupervisorUsuario("John Vilchez (Oriente)", "john.vilchez"),
        SupervisorUsuario("Libia Florez (Oriente)", "libia.florez"),
        SupervisorUsuario("Jorge Geisbuhler (Oriente)", "jorge.geisbuhler")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMuroTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_REALIZAR
        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""

        configurarRecyclerView()
        configurarSpinnerPiso()
        configurarSpinnerSupervisor()
        configurarEventos()
        configurarBottomNav()
        configurarSwipeConRol()
        cargarTareasDesdeFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerTareas?.remove()
    }

    // -------------------- UI BÁSICA --------------------

    private fun configurarRecyclerView() {
        adapter = TareaAdapter(
            tareas = listaFiltrada,
            rolUsuario = rolUsuario,
            onResponderClick = { tarea ->
                if (tarea.estado != "Pendiente") {
                    Toast.makeText(
                        this,
                        "Solo se pueden marcar como realizadas las tareas pendientes",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mostrarDialogoMarcarRealizada(tarea)
                }
            }
        )

        binding.rvTareas.apply {
            layoutManager = LinearLayoutManager(this@MuroTareasActivity)
            adapter = this@MuroTareasActivity.adapter
        }
    }

    private fun configurarSpinnerPiso() {
        val pisos = mutableListOf("Todos")
        for (i in 6 downTo 1) pisos.add("Piso $i")
        pisos.add("Piso 0")
        for (i in -1 downTo -6) pisos.add("Piso $i")

        val adapterPiso = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            pisos
        )
        adapterPiso.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFiltroPiso.adapter = adapterPiso

        binding.spFiltroPiso.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    pisoSeleccionado = parent?.getItemAtPosition(position).toString()
                    aplicarFiltrosLocales()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun configurarSpinnerSupervisor() {
        if (!esAdmin) {
            binding.spFiltroSupervisor.visibility = View.GONE
            return
        }

        val nombres = mutableListOf("Todos")
        nombres.addAll(listaSupervisores.map { it.nombreVisible })

        val adapterSup = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombres
        )
        adapterSup.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFiltroSupervisor.adapter = adapterSup

        binding.spFiltroSupervisor.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (!esAdmin || modoMuro != "ASIGNADAS") return

                    filtroSupervisor = if (position == 0) {
                        null
                    } else {
                        val supervisorSeleccionado = listaSupervisores[position - 1]
                        supervisorSeleccionado.username
                    }
                    cargarTareasDesdeFirestore()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun configurarEventos() {

        binding.btnTareasPendientes.setOnClickListener {
            modoMuro = "PENDIENTES"
            filtroSupervisor = null
            binding.spFiltroSupervisor.visibility = View.GONE
            marcarBotonActivo(binding.btnTareasPendientes)
            cargarTareasDesdeFirestore()
        }

        binding.btnTareasRealizadas.setOnClickListener {
            modoMuro = "REALIZADAS"
            filtroSupervisor = null
            binding.spFiltroSupervisor.visibility = View.GONE
            marcarBotonActivo(binding.btnTareasRealizadas)
            cargarTareasDesdeFirestore()
        }

        binding.btnTareasAsignadas.setOnClickListener {
            modoMuro = "ASIGNADAS"
            marcarBotonActivo(binding.btnTareasAsignadas)

            if (esAdmin) {
                binding.spFiltroSupervisor.visibility = View.VISIBLE
                cargarTareasDesdeFirestore()
            } else {
                binding.spFiltroSupervisor.visibility = View.GONE
                filtroSupervisor = null
                cargarTareasDesdeFirestore()
            }
        }

        binding.etBuscarDescripcionOUbicacion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                textoBusqueda = s?.toString()?.trim().orEmpty()
                aplicarFiltrosLocales()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarBottomNav() {
        val bottomNav = binding.bottomNav

        bottomNav.selectedItemId = R.id.nav_muro_tareas

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    startActivity(
                        Intent(this, CrearTareaActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                            putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                        }
                    )
                    true
                }

                R.id.nav_muro_tareas -> true

                else -> false
            }
        }
    }

    private fun marcarBotonActivo(botonActivo: Button) {
        val btnPendientes = binding.btnTareasPendientes
        val btnRealizadas = binding.btnTareasRealizadas
        val btnAsignadas = binding.btnTareasAsignadas

        // Cambia estos colores por los tuyos reales del colors.xml
        val colorPendienteActivo = ContextCompat.getColorStateList(this, R.color.rojo_pendiente_activo)
        val colorPendienteInactivo = ContextCompat.getColorStateList(this, R.color.rojo)

        val colorAsignadasActivo = ContextCompat.getColorStateList(this, R.color.naranjo_asignadas_activo)
        val colorAsignadasInactivo = ContextCompat.getColorStateList(this, R.color.naranjo)

        val colorRealizadasActivo = ContextCompat.getColorStateList(this, R.color.verde_realizadas_activo)
        val colorRealizadasInactivo = ContextCompat.getColorStateList(this, R.color.verde)

        fun activar(btn: Button, activo: Boolean) {
            when (btn.id) {
                R.id.btnTareasPendientes -> {
                    btn.backgroundTintList = if (activo) colorPendienteActivo else colorPendienteInactivo
                }
                R.id.btnTareasAsignadas -> {
                    btn.backgroundTintList = if (activo) colorAsignadasActivo else colorAsignadasInactivo
                }
                R.id.btnTareasRealizadas -> {
                    btn.backgroundTintList = if (activo) colorRealizadasActivo else colorRealizadasInactivo
                }
            }
            // Texto SIEMPRE blanco
            btn.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        activar(btnPendientes, botonActivo == btnPendientes)
        activar(btnAsignadas, botonActivo == btnAsignadas)
        activar(btnRealizadas, botonActivo == btnRealizadas)
    }



    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBarMuro.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.rvTareas.isEnabled = !mostrar
        binding.btnTareasPendientes.isEnabled = !mostrar
        binding.btnTareasRealizadas.isEnabled = !mostrar
        binding.btnTareasAsignadas.isEnabled = !mostrar
    }

    // -------------------- FIRESTORE --------------------
    private fun cargarTareasDesdeFirestore() {
        listenerTareas?.remove()
        mostrarCargando(true)

        val coleccion = firestore.collection("tareas")

        val query: Query = when (modoMuro) {
            "PENDIENTES" -> {
                // Pendientes sin asignar
                coleccion
                    .whereEqualTo("estado", "Pendiente")
                    .whereEqualTo("asignadaA", "")
            }

            "REALIZADAS" -> {
                coleccion.whereEqualTo("estado", "Realizada")
            }

            "ASIGNADAS" -> {
                if (esAdmin) {
                    // Admin: ve pendientes asignadas (filtramos por asignadaA más abajo)
                    coleccion.whereEqualTo("estado", "Pendiente")
                } else {
                    // Supervisor: pendientes asignadas a él
                    coleccion
                        .whereEqualTo("estado", "Pendiente")
                        .whereEqualTo("asignadaA", usernameActual)
                }
            }

            else -> coleccion
        }

        listenerTareas = query.addSnapshotListener { snapshot, error ->
            mostrarCargando(false)

            if (error != null) {
                Toast.makeText(
                    this,
                    "Error al cargar tareas: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            listaOriginal.clear()

            val docs = snapshot?.documents ?: emptyList()
            for (doc in docs) {
                val tarea = doc.toObject(Tarea::class.java)?.copy(id = doc.id) ?: continue

                val pasaFiltroAsignadas = when (modoMuro) {
                    "ASIGNADAS" -> {
                        if (esAdmin) {
                            // Admin: solo con asignadaA no vacío + filtro opcional
                            if (tarea.asignadaA.isBlank()) {
                                false
                            } else if (filtroSupervisor.isNullOrEmpty()) {
                                true
                            } else {
                                tarea.asignadaA == filtroSupervisor
                            }
                        } else {
                            // supervisor ya viene filtrado por query
                            true
                        }
                    }

                    else -> true
                }

                if (pasaFiltroAsignadas) {
                    listaOriginal.add(tarea)
                }
            }

            aplicarFiltrosLocales()
        }
    }

    // -------------------- FILTROS LOCALES --------------------

    private fun aplicarFiltrosLocales() {
        listaFiltrada.clear()

        val texto = textoBusqueda.lowercase()

        listaFiltrada.addAll(
            listaOriginal.filter { tarea ->
                val coincideTexto =
                    texto.isBlank() ||
                            tarea.descripcion.lowercase().contains(texto) ||
                            tarea.ubicacion.lowercase().contains(texto)

                val coincidePiso =
                    pisoSeleccionado == "Todos" ||
                            tarea.piso.equals(pisoSeleccionado, ignoreCase = true)

                coincideTexto && coincidePiso
            }
        )

        adapter.actualizarLista(listaFiltrada.toList())
    }

    // -------------------- SWIPE: ELIMINAR / ASIGNAR --------------------

    private fun configurarSwipeConRol() {
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

                // solo admin puede eliminar/asignar
                if (!esAdmin) {
                    Toast.makeText(
                        this@MuroTareasActivity,
                        "Solo el administrador puede asignar o eliminar tareas",
                        Toast.LENGTH_SHORT
                    ).show()
                    adapter.notifyItemChanged(position)
                    return
                }

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        confirmarRechazoTarea(tarea, position)
                    }

                    ItemTouchHelper.RIGHT -> {
                        mostrarDialogoAsignarSupervisor(tarea)
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.rvTareas)
    }

    private fun confirmarRechazoTarea(tarea: Tarea, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de que deseas eliminar esta solicitud de limpieza?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                rechazarTarea(tarea)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                adapter.notifyItemChanged(position)
            }
            .setOnCancelListener {
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun rechazarTarea(tarea: Tarea) {
        if (tarea.id.isEmpty()) return

        mostrarCargando(true)

        val docRef = firestore.collection("tareas").document(tarea.id)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()

                listaOriginal.removeAll { it.id == tarea.id }
                listaFiltrada.removeAll { it.id == tarea.id }
                adapter.actualizarLista(listaFiltrada.toList())

                mostrarCargando(false)
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                Toast.makeText(
                    this,
                    "Error al eliminar: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarDialogoAsignarSupervisor(tarea: Tarea) {
        val nombres = listaSupervisores.map { it.nombreVisible }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Asignar tarea a supervisor")
            .setItems(nombres) { dialog, which ->
                val supervisorSeleccionado = listaSupervisores[which]
                asignarTareaASupervisor(tarea, supervisorSeleccionado)
                dialog.dismiss()
            }
            .show()
    }

    private fun asignarTareaASupervisor(tarea: Tarea, supervisor: SupervisorUsuario) {
        if (tarea.id.isEmpty()) return

        mostrarCargando(true)

        val docRef = firestore.collection("tareas").document(tarea.id)
        docRef.update(
            mapOf(
                "asignadaA" to supervisor.username,
                "estado" to "Pendiente"
            )
        ).addOnSuccessListener {
            Toast.makeText(
                this,
                "Tarea asignada a ${supervisor.nombreVisible}",
                Toast.LENGTH_SHORT
            ).show()
            mostrarCargando(false)
        }.addOnFailureListener { e ->
            mostrarCargando(false)
            Toast.makeText(
                this,
                "Error al asignar: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // -------------------- MARCAR COMO REALIZADA --------------------

    private fun mostrarDialogoMarcarRealizada(tarea: Tarea) {
        AlertDialog.Builder(this)
            .setTitle("Marcar tarea como realizada")
            .setMessage("¿Confirmas que esta solicitud de limpieza fue atendida correctamente?")
            .setPositiveButton("Sí, marcar como realizada") { _, _ ->
                marcarTareaComoRealizada(tarea)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun marcarTareaComoRealizada(tarea: Tarea) {
        if (tarea.id.isEmpty()) return

        mostrarCargando(true)

        val docRef = firestore.collection("tareas").document(tarea.id)
        docRef.update(
            mapOf(
                "estado" to "Realizada",
                "fechaRespuesta" to Timestamp.now()
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Tarea marcada como realizada", Toast.LENGTH_SHORT).show()

            if (modoMuro == "PENDIENTES") {
                listaOriginal.removeAll { it.id == tarea.id }
                listaFiltrada.removeAll { it.id == tarea.id }
                adapter.actualizarLista(listaFiltrada.toList())
            }

            mostrarCargando(false)
        }.addOnFailureListener { e ->
            mostrarCargando(false)
            Toast.makeText(
                this,
                "Error al marcar como realizada: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
