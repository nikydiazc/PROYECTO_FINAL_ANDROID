package cl.unab.proyecto_final_android.util

import cl.unab.proyecto_final_android.R
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import cl.unab.proyecto_final_android.ui.muro.TareaAdapter
import cl.unab.proyecto_final_android.ui.muro.TareasViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

object MuroConfigurator {

    // Lista de Supervisores (Movida aquí)
    data class SupervisorUsuario(val nombreVisible: String, val username: String)

    private val listaSupervisores = listOf(
        SupervisorUsuario("Delfina Cabello (Poniente)", "delfina.cabello"),
        // ... (resto de tus supervisores)
        SupervisorUsuario("Jorge Geisbuhler (Oriente)", "jorge.geisbuhler")
    )

    fun configurarRecyclerView(rv: RecyclerView, adapter: TareaAdapter) {
        rv.apply {
            layoutManager = LinearLayoutManager(rv.context)
            this.adapter = adapter
        }
    }

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
                viewModel.cambiarPiso(parent?.getItemAtPosition(position).toString())
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

    fun configurarEventosUI(
        binding: ActivityMuroTareasBinding,
        viewModel: TareasViewModel,
        esAdmin: Boolean
    ) {
        // Botones de Modo
        binding.btnTareasPendientes.setOnClickListener {
            cambiarModoYActualizarUI(
                binding,
                viewModel,
                esAdmin,
                ModoMuro.PENDIENTES
            )
        }
        binding.btnTareasRealizadas.setOnClickListener {
            cambiarModoYActualizarUI(
                binding,
                viewModel,
                esAdmin,
                ModoMuro.REALIZADAS
            )
        }
        binding.btnTareasAsignadas.setOnClickListener {
            cambiarModoYActualizarUI(
                binding,
                viewModel,
                esAdmin,
                ModoMuro.ASIGNADAS
            )
        }

        // Buscador
        binding.etBuscarDescripcionOUbicacion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.actualizarBusqueda(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })


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
                    cl.unab.proyecto_final_android.R.color.white
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
        binding.layoutFiltroFechas.visibility = View.VISIBLE
    }


    // Archivo: MuroConfigurator.kt (Función correcta)

    fun configurarBottomNav(activity: MuroTareasActivity, bottomNav: BottomNavigationView, rolUsuario: String, usernameActual: String) {

        // 🎨 Esto fuerza a que los iconos usen su color original (o el definido en el XML)
        bottomNav.itemIconTintList = null

        bottomNav.selectedItemId = R.id.nav_muro_tareas
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    // USA los parámetros rolUsuario y usernameActual que se le pasaron a la función
                    activity.startActivity(Intent(activity, CrearTareaActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                        putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                    })
                    true
                }
                R.id.nav_muro_tareas -> {
                    true // Permanece aquí
                }
                R.id.nav_usuario -> {
                    mostrarDialogoCerrarSesion(activity)
                    false
                }
                else -> false
            }
        }
    }




    private fun mostrarDialogoCerrarSesion(activity: MuroTareasActivity) {
        AlertDialog.Builder(activity)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro que deseas cerrar tu sesión actual?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // -------------------- LÓGICA DE EDICIÓN / ELIMINACIÓN (Ahora aquí) --------------------

    fun confirmarEliminacionTarea(context: Context, viewModel: TareasViewModel, tarea: Tarea) {
        AlertDialog.Builder(context)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar la tarea #${tarea.id} permanentemente?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarTarea(tarea) { ok, error ->
                    if (ok) Toast.makeText(context, "Tarea eliminada con éxito.", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "Error al eliminar: ${error}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

// Archivo: MuroConfigurator.kt

    fun mostrarDialogoEditarTarea(context: Context, viewModel: TareasViewModel, tarea: Tarea) {
        val view = (context as MuroTareasActivity).layoutInflater.inflate(cl.unab.proyecto_final_android.R.layout.dialog_editar_tarea, null)
        val etDesc = view.findViewById<AppCompatEditText>(cl.unab.proyecto_final_android.R.id.etDescripcionEditar)
        val etUbi = view.findViewById<AppCompatEditText>(cl.unab.proyecto_final_android.R.id.etUbicacionEditar)
        val spPiso = view.findViewById<Spinner>(cl.unab.proyecto_final_android.R.id.spPisoEditar)

        // ...
        etDesc.setText(tarea.descripcion)
        etUbi.setText(tarea.ubicacion)

        val pisos = mutableListOf<String>()
        for (i in 6 downTo 1) pisos.add("Piso $i")
        for (i in -1 downTo -6) pisos.add("Piso $i")

        // 💡 CORRECCIÓN CLAVE: Usar android.R.layout
        val adp = ArrayAdapter(context, android.R.layout.simple_spinner_item, pisos)

        spPiso.adapter = adp
        val idx = pisos.indexOfFirst { it.equals(tarea.piso, ignoreCase = true) }
        if (idx >= 0) spPiso.setSelection(idx)
        // ...

        AlertDialog.Builder(context)
            .setTitle("Editar tarea")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                // ... (Resto del código)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // -------------------- SWIPE (Ahora aquí) --------------------

    fun configurarSwipeConRol(activity: MuroTareasActivity, rv: RecyclerView, adapter: TareaAdapter, viewModel: TareasViewModel, esAdmin: Boolean) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val tarea = adapter.obtenerTareaEnPosicion(position) ?: return

                if (!esAdmin) {
                    Toast.makeText(activity, "Solo el administrador puede gestionar tareas", Toast.LENGTH_SHORT).show()
                    adapter.notifyItemChanged(position)
                    return
                }

                if (direction == ItemTouchHelper.LEFT) confirmarRechazoTarea(activity, viewModel, tarea, position)
                else mostrarDialogoAsignarSupervisor(activity, viewModel, tarea)

                adapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(rv)
    }

    private fun confirmarRechazoTarea(context: Context, viewModel: TareasViewModel, tarea: Tarea, position: Int?) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar tarea")
            .setMessage("¿Eliminar esta solicitud?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.rechazarTarea(tarea) { ok, error ->
                    if (ok) Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAsignarSupervisor(context: Context, viewModel: TareasViewModel, tarea: Tarea) {
        val nombres = listaSupervisores.map { it.nombreVisible }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Asignar tarea")
            .setItems(nombres) { _, which ->
                val sup = listaSupervisores[which]
                viewModel.asignarTarea(tarea, sup.username) { ok, _ ->
                    if (ok) Toast.makeText(context, "Asignada a ${sup.nombreVisible}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}