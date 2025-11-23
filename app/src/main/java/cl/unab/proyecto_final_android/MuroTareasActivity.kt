package cl.unab.proyecto_final_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.Button
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
    private var usernameActual: String = ""

    private lateinit var adapter: TareaAdapter

    private var rolUsuario: String = LoginActivity.ROL_REALIZAR

    // Estado actual del muro: "Pendiente" o "Realizada"
    private var estadoActual: String = "Pendiente"
    private var modoMuro: String = "PENDIENTES"

    // Listas para filtros
    private val listaOriginal = mutableListOf<Tarea>()
    private val listaFiltrada = mutableListOf<Tarea>()

    // Filtro de texto (busca en descripción o ubicación)
    private var textoFiltro: String = ""

    // Filtro por piso
    private var pisoFiltro: String = "Todos"

    private var listenerTareas: ListenerRegistration? = null

    // Lista de supervisores disponibles para asignar tareas
    private data class SupervisorUsuario(
        val nombre: String,
        val username: String,
        val zona: String
    )

    private val supervisores = listOf(
        // Poniente
        SupervisorUsuario("Delfina Cabello", "delfina.cabello", "Poniente"),
        SupervisorUsuario("Rodrigo Reyes", "rodrigo.reyes", "Poniente"),
        SupervisorUsuario("Maria Caruajulca", "maria.caruajulca", "Poniente"),
        SupervisorUsuario("Cristian Vergara", "cristian.vergara", "Poniente"),
        SupervisorUsuario("Enrique Mendez", "enrique.mendez", "Poniente"),
        SupervisorUsuario("Norma Marican", "norma.marican", "Poniente"),

        // Oriente
        SupervisorUsuario("John Vilchez", "john.vilchez", "Oriente"),
        SupervisorUsuario("Libia Florez", "libia.florez", "Oriente"),
        SupervisorUsuario("Jorge Geisbuhler", "jorge.geisbuhler", "Oriente")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMuroTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_REALIZAR

        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""

        Toast.makeText(this, "Rol: $rolUsuario - User: $usernameActual", Toast.LENGTH_LONG).show()

        configurarRecyclerView()
        configurarSpinnerPiso()
        configurarEventos()
        configurarSwipeConRol()
        configurarBottomNav()
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
                // Solo tiene sentido en pendientes
                if (tarea.estado != "Pendiente") {
                    Toast.makeText(
                        this,
                        "Solo se pueden marcar como realizadas las tareas pendientes",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@TareaAdapter
                }

                mostrarDialogoMarcarRealizada(tarea)
            }
        )


        binding.rvTareas.apply {
            layoutManager = LinearLayoutManager(this@MuroTareasActivity)
            adapter = this@MuroTareasActivity.adapter
        }
    }

    private fun configurarBottomNav() {
        val bottomNav = binding.bottomNav

        // marcar como seleccionada la opción del muro
        bottomNav.selectedItemId = R.id.nav_muro_tareas  // AJUSTA este id según tu menú

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    // Si ya estamos en muro, vamos a CrearTareaActivity
                    startActivity(
                        Intent(this, CrearTareaActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                        }
                    )
                    // Opcional: finish() si no quieres volver con back
                    true
                }

                R.id.nav_muro_tareas -> {
                    // Ya estamos aquí
                    true
                }

                else -> false
            }
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

        binding.btnTareasPendientes.setOnClickListener {
            modoMuro = "PENDIENTES"
            cargarTareasDesdeFirestore()
            marcarBotonActivo(binding.btnTareasPendientes)
        }

        binding.btnTareasRealizadas.setOnClickListener {
            modoMuro = "REALIZADAS"
            cargarTareasDesdeFirestore()
            marcarBotonActivo(binding.btnTareasRealizadas)
        }

        binding.btnTareasAsignadas.setOnClickListener {
            modoMuro = "ASIGNADAS"
            cargarTareasDesdeFirestore()
            marcarBotonActivo(binding.btnTareasAsignadas)
        }
    }

    //Se marca el botón activo: PENDIENTES, REALIZADAS O ASIGNADAS
    private fun marcarBotonActivo(botonActivo: Button) {
        val botones = listOf(
            binding.btnTareasPendientes,
            binding.btnTareasRealizadas,
            binding.btnTareasAsignadas
        )

        botones.forEach { btn ->
            if (btn == botonActivo) {
                btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.verde))
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                btn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white))
                btn.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }
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

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // 👈 Eliminar con confirmación
                        confirmarRechazoTarea(tarea, position)
                    }
                    ItemTouchHelper.RIGHT -> {
                        // 👉 Asignar a supervisor
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
                // no hacemos notifyItemChanged aquí porque la eliminamos en la lista
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                adapter.notifyItemChanged(position) // devolvemos la tarjeta a su lugar
            }
            .setOnCancelListener {
                adapter.notifyItemChanged(position)
            }
            .show()
    }


    private fun mostrarDialogoMarcarRealizada(tarea: Tarea) {
        AlertDialog.Builder(this)
            .setTitle("Marcar tarea como realizada")
            .setMessage("¿Confirmas que esta solicitud de limpieza fue atendida correctamente?")
            .setPositiveButton("Sí, marcar como realizada") { _, _ ->
                marcarTareaComoRealizada(tarea)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    private fun mostrarDialogoAsignarSupervisor(tarea: Tarea) {
        if (tarea.id.isEmpty()) return

        val nombres = supervisores.map { "${it.nombre} (${it.zona})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Asignar tarea a supervisor")
            .setItems(nombres) { _, which ->
                val supervisorSeleccionado = supervisores[which]
                asignarTareaASupervisor(tarea, supervisorSeleccionado)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
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
                "estado" to "Pendiente" // o "En Proceso" si decides cambiarlo más adelante
            )
        ).addOnSuccessListener {
            Toast.makeText(
                this,
                "Tarea asignada a ${supervisor.nombre}",
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

    private fun rechazarTarea(tarea: Tarea) {
        if (tarea.id.isEmpty()) return

        mostrarCargando(true)

        val docRef = firestore.collection("tareas").document(tarea.id)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()

                // Sacar de las listas locales para que desaparezca
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

            if (estadoActual == "Pendiente") {
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

    private fun cargarTareasDesdeFirestore() {
        // cortar listener anterior si existía
        listenerTareas?.remove()

        mostrarCargando(true)

        val coleccion = firestore.collection("tareas")

        // 1) armamos el Query según el modo del muro
        val query: Query = when (modoMuro) {
            "PENDIENTES" -> {
                coleccion.whereEqualTo("estado", "Pendiente")
            }
            "REALIZADAS" -> {
                coleccion.whereEqualTo("estado", "Realizada")
            }
            "ASIGNADAS" -> {
                // ADMIN ve todas las tareas asignadas (asignadaA != "")
                if (rolUsuario == LoginActivity.ROL_ADMIN) {
                    coleccion.whereNotEqualTo("asignadaA", "")
                } else {
                    // supervisores ven solo las suyas
                    coleccion.whereEqualTo("asignadaA", usernameActual)
                }
            }
            else -> {
                // por si acaso
                coleccion
            }
        }

        // 2) escuchamos los cambios del query ya armado
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

            val documentos = snapshot?.documents ?: emptyList()

            listaOriginal.clear()

            for (doc in documentos) {
                val tarea = doc.toObject(Tarea::class.java)?.copy(id = doc.id)
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

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBarMuro.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.rvTareas.isEnabled = !mostrar
        binding.btnTareasPendientes.isEnabled = !mostrar
        binding.btnTareasRealizadas.isEnabled = !mostrar
    }

}
