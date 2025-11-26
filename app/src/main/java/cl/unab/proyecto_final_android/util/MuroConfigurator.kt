package cl.unab.proyecto_final_android.util

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import cl.unab.proyecto_final_android.ui.muro.TareaAdapter
import cl.unab.proyecto_final_android.ui.muro.TareasViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.widget.AppCompatEditText

object MuroConfigurator {

    // ---------- MODELO PARA SUPERVISORES ----------

    data class SupervisorUsuario(val nombreVisible: String, val username: String)

    // Lista única centralizada, la usan ViewModel y diálogos
    val listaSupervisores = listOf(
        // --- Supervisores Poniente ---
        SupervisorUsuario("Delfina Cabello (Poniente)", "delfina.cabello"),
        SupervisorUsuario("Rodrigo Reyes (Poniente)", "rodrigo.reyes"),
        SupervisorUsuario("Maria Caruajulca (Poniente)", "maria.caruajulca"),
        SupervisorUsuario("Cristian Vergara (Poniente)", "cristian.vergara"),
        SupervisorUsuario("Enrique Mendez (Poniente)", "enrique.mendez"),
        SupervisorUsuario("Norma Marican (Poniente)", "norma.marican"),

        // --- Supervisores Oriente ---
        SupervisorUsuario("John Vilchez (Oriente)", "john.vilchez"),
        SupervisorUsuario("Libia Florez (Oriente)", "libia.florez"),
        SupervisorUsuario("Jorge Geisbuhler (Oriente)", "jorge.geisbuhler")
    )

    // ---------- RECYCLERVIEW ----------

    fun configurarRecyclerView(rv: RecyclerView, adapter: TareaAdapter) {
        rv.apply {
            layoutManager = LinearLayoutManager(rv.context)
            this.adapter = adapter
        }
    }

    // ---------- SPINNERS (PISO + SUPERVISOR) ----------

    fun configurarSpinners(
        context: Context,
        binding: ActivityMuroTareasBinding,
        viewModel: TareasViewModel,
        esAdmin: Boolean
    ) {
        configurarSpinnerPiso(context, binding, viewModel)
        configurarSpinnerSupervisor(context, binding, viewModel, esAdmin)
    }

    private fun configurarSpinnerPiso(
        context: Context,
        binding: ActivityMuroTareasBinding,
        viewModel: TareasViewModel
    ) {
        val pisos = mutableListOf("Todos")
        for (i in 6 downTo 1) pisos.add("Piso $i")
        for (i in -1 downTo -6) pisos.add("Piso $i")

        val adapterPiso = ArrayAdapter(context, android.R.layout.simple_spinner_item, pisos)
        adapterPiso.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFiltroPiso.adapter = adapterPiso

        binding.spFiltroPiso.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val valor = parent?.getItemAtPosition(position).toString()
                viewModel.cambiarPiso(valor)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarSpinnerSupervisor(
        context: Context,
        binding: ActivityMuroTareasBinding,
        viewModel: TareasViewModel,
        esAdmin: Boolean
    ) {
        val nombres = mutableListOf("Todos")
        nombres.addAll(listaSupervisores.map { it.nombreVisible })

        val adapterSup = ArrayAdapter(context, android.R.layout.simple_spinner_item, nombres)
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
                    if (!esAdmin) return
                    val usernameSupervisor =
                        if (position == 0) null else listaSupervisores[position - 1].username
                    viewModel.cambiarSupervisor(usernameSupervisor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ---------- EVENTOS GENERALES UI (BOTONES + BUSCADOR) ----------

    fun configurarEventosUI(
        binding: ActivityMuroTareasBinding,
        viewModel: TareasViewModel,
        esAdmin: Boolean
    ) {
        // Botones de modo
        binding.btnTareasPendientes.setOnClickListener {
            cambiarModoYActualizarUI(binding, viewModel, esAdmin, ModoMuro.PENDIENTES)
        }
        binding.btnTareasAsignadas.setOnClickListener {
            cambiarModoYActualizarUI(binding, viewModel, esAdmin, ModoMuro.ASIGNADAS)
        }
        binding.btnTareasRealizadas.setOnClickListener {
            cambiarModoYActualizarUI(binding, viewModel, esAdmin, ModoMuro.REALIZADAS)
        }

        // Buscador por descripción o ubicación
        binding.etBuscarDescripcionOUbicacion.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    viewModel.actualizarBusqueda(s?.toString().orEmpty())
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            }
        )
    }

    private fun cambiarModoYActualizarUI(
        binding: ActivityMuroTareasBinding,
        viewModel: TareasViewModel,
        esAdmin: Boolean,
        modo: ModoMuro
    ) {
        viewModel.cambiarModo(modo)

        when (modo) {
            ModoMuro.PENDIENTES -> marcarBotonActivo(binding.btnTareasPendientes, binding)
            ModoMuro.ASIGNADAS -> marcarBotonActivo(binding.btnTareasAsignadas, binding)
            ModoMuro.REALIZADAS -> marcarBotonActivo(binding.btnTareasRealizadas, binding)
        }

        actualizarVisibilidadFiltros(binding, esAdmin, modo)
    }

    fun marcarBotonActivo(botonActivo: Button, binding: ActivityMuroTareasBinding) {
        val botones = listOf(
            binding.btnTareasPendientes,
            binding.btnTareasAsignadas,
            binding.btnTareasRealizadas
        )

        botones.forEach { btn ->
            btn.alpha = if (btn == botonActivo) 1f else 0.5f
            btn.setTextColor(
                ContextCompat.getColor(
                    botonActivo.context,
                    R.color.white
                )
            )
        }
    }

    fun actualizarVisibilidadFiltros(
        binding: ActivityMuroTareasBinding,
        esAdmin: Boolean,
        modo: ModoMuro
    ) {
        val mostrarSupervisor = (modo == ModoMuro.ASIGNADAS && esAdmin)
        binding.spFiltroSupervisor.visibility = if (mostrarSupervisor) View.VISIBLE else View.GONE

        // El filtro de fecha siempre visible
        binding.layoutFiltroFechas.visibility = View.VISIBLE
    }

    // ---------- BOTTOM NAV ----------

    fun configurarBottomNav(
        activity: MuroTareasActivity,
        bottomNav: BottomNavigationView,
        rolUsuario: String,
        usernameActual: String
    ) {
        // Dejar marcado este ítem porque estamos en el muro
        bottomNav.selectedItemId = R.id.nav_muro_tareas

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    val puedeCrear =
                        rolUsuario == LoginActivity.ROL_ADMIN || rolUsuario == LoginActivity.ROL_CREAR

                    if (puedeCrear) {
                        activity.startActivity(
                            Intent(activity, CrearTareaActivity::class.java).apply {
                                putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                                putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                            }
                        )
                    } else {
                        Toast.makeText(
                            activity,
                            "No tienes permisos para crear tareas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }

                R.id.nav_muro_tareas -> {
                    // Ya estamos aquí
                    true
                }

                R.id.nav_usuario -> {
                    mostrarDialogoCerrarSesion(activity)
                    true
                }

                else -> false
            }
        }
    }

    private fun mostrarDialogoCerrarSesion(activity: MuroTareasActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro que deseas cerrar tu sesión actual?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- EDICIÓN / ELIMINACIÓN ----------

    fun confirmarEliminacionTarea(
        context: Context,
        viewModel: TareasViewModel,
        tarea: Tarea
    ) {
        AlertDialog.Builder(context)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar la tarea #${tarea.id}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarTarea(tarea) { ok, error ->
                    if (ok) {
                        Toast.makeText(context, "Tarea eliminada con éxito", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            context,
                            "Error al eliminar: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun mostrarDialogoEditarTarea(
        context: Context,
        viewModel: TareasViewModel,
        tarea: Tarea
    ) {
        val activity = context as MuroTareasActivity
        val view = activity.layoutInflater.inflate(R.layout.dialog_editar_tarea, null)

        val etDesc = view.findViewById<AppCompatEditText>(R.id.et_descripcion_editar)
        val etUbi = view.findViewById<AppCompatEditText>(R.id.et_ubicacion_editar)
        val spPiso = view.findViewById<Spinner>(R.id.sp_piso_editar)

        // Valores actuales
        etDesc.setText(tarea.descripcion)
        etUbi.setText(tarea.ubicacion)

        // Lista de pisos (mismo formato que usas en el muro: "Piso 6", "Piso -1", etc.)
        val pisos = mutableListOf<String>()
        for (i in 6 downTo 1) pisos.add("Piso $i")
        for (i in -1 downTo -6) pisos.add("Piso $i")

        val adp = ArrayAdapter(context, android.R.layout.simple_spinner_item, pisos).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spPiso.adapter = adp

        // Seleccionar el piso actual si calza con la lista
        val idx = pisos.indexOfFirst { it.equals(tarea.piso, ignoreCase = true) }
        if (idx >= 0) spPiso.setSelection(idx)

        AlertDialog.Builder(context)
            .setTitle("Editar tarea")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaDescripcion = etDesc.text?.toString()?.trim().orEmpty()
                val nuevaUbicacion = etUbi.text?.toString()?.trim().orEmpty()

                // 👇 Aquí nos aseguramos que sea SIEMPRE String no nulo
                val nuevoPiso: String =
                    spPiso.selectedItem?.toString()
                        ?: tarea.piso
                        ?: "Piso 1"  // valor seguro por si ambos vienen nulos

                if (nuevaDescripcion.isBlank()) {
                    Toast.makeText(context, "La descripción no puede estar vacía", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (nuevaUbicacion.isBlank()) {
                    Toast.makeText(context, "La ubicación no puede estar vacía", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.actualizarTarea(
                    tarea = tarea,
                    nuevaDescripcion = nuevaDescripcion,
                    nuevaUbicacion = nuevaUbicacion,
                    nuevoPiso = nuevoPiso   // 👉 ahora es String, no String?
                ) { ok, error ->
                    if (ok) {
                        Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Error al actualizar: ${error ?: "desconocido"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // ---------- SWIPE (ASIGNAR / RECHAZAR) SOLO ADMIN ----------

    fun configurarSwipeConRol(
        activity: MuroTareasActivity,
        rv: RecyclerView,
        adapter: TareaAdapter,
        viewModel: TareasViewModel,
        esAdmin: Boolean
    ) {
        // Solo el admin tiene swipe
        if (!esAdmin) return

        val callback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val tarea = adapter.obtenerTareaEnPosicion(position)

                if (tarea == null) {
                    adapter.notifyItemChanged(position)
                    return
                }

                // Si NO es admin, por seguridad, cancelamos
                if (!esAdmin) {
                    adapter.notifyItemChanged(position)
                    return
                }

                if (direction == ItemTouchHelper.LEFT) {
                    // Rechazar / eliminar visualmente
                    confirmarRechazoTarea(activity, viewModel, tarea)
                } else {
                    // Asignar o reasignar
                    mostrarDialogoAsignarSupervisor(activity, viewModel, tarea)
                }

                // Volver a dibujar el item (para que no quede "swipeado")
                adapter.notifyItemChanged(position)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(rv)
    }

    private fun confirmarRechazoTarea(
        context: Context,
        viewModel: TareasViewModel,
        tarea: Tarea
    ) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar tarea")
            .setMessage("¿Eliminar esta solicitud?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.rechazarTarea(tarea) { ok, error ->
                    if (ok) {
                        Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAsignarSupervisor(
        context: Context,
        viewModel: TareasViewModel,
        tarea: Tarea
    ) {
        val nombres = listaSupervisores.map { it.nombreVisible }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Asignar tarea")
            .setItems(nombres) { _, which ->
                val sup = listaSupervisores[which]
                viewModel.asignarTarea(tarea, sup.username) { ok, _ ->
                    if (ok) {
                        Toast.makeText(
                            context,
                            "Asignada a ${sup.nombreVisible}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }
}
