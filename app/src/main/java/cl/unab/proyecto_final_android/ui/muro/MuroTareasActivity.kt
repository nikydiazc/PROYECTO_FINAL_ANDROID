package cl.unab.proyecto_final_android.ui.muro

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.Spinner

class MuroTareasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuroTareasBinding
    private lateinit var viewModel: TareasViewModel
    private lateinit var adapter: TareaAdapter

    // Variables para el filtro de fecha
    private var fechaSeleccionadaDesde: Calendar? = null
    private var fechaSeleccionadaHasta: Calendar? = null

    private var rolUsuario: String = LoginActivity.ROL_REALIZAR
    private var usernameActual: String = ""

    private val esAdmin: Boolean
        get() = usernameActual.equals("administrador", ignoreCase = true) ||
                usernameActual.equals("administrador@miapp.com", ignoreCase = true)

    // Para responder con foto
    private var tareaSeleccionadaParaRespuesta: Tarea? = null
    private var currentPhotoPathRespuesta: String? = null
    private val REQUEST_FOTO_RESPUESTA = 2001

    data class SupervisorUsuario(
        val nombreVisible: String,
        val username: String
    )

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

        // Datos que vienen del login
        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_REALIZAR
        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""

        // Crear repository y ViewModel
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val repo = TareaRepository(firestore, storage)
        val factory = TareasViewModelFactory(repo, esAdmin, usernameActual)
        viewModel = ViewModelProvider(this, factory)[TareasViewModel::class.java]

        configurarRecyclerView()
        configurarSpinnerPiso()
        configurarSpinnerSupervisor()
        configurarEventosUI()
        configurarBottomNav()
        configurarSwipeConRol()
        configurarEventosFiltroFecha()
        observarViewModel()

        // Cargar por primera vez (Por defecto Pendientes)
        viewModel.cargarTareas()
        marcarBotonActivo(binding.btnTareasPendientes)
        actualizarVisibilidadFiltros(ModoMuro.PENDIENTES)
    }

    // -------------------- CONFIGURACIÓN UI --------------------

    private fun configurarRecyclerView() {
        adapter = TareaAdapter(
            tareas = emptyList(),
            rolUsuario = rolUsuario,
            onResponderClick = { tarea ->
                if (!tarea.estado.equals("Pendiente", ignoreCase = true)) {
                    Toast.makeText(this, "Solo se pueden responder las tareas pendientes", Toast.LENGTH_SHORT).show()
                    return@TareaAdapter
                }
                tareaSeleccionadaParaRespuesta = tarea
                abrirCamaraParaRespuesta()
            },
            onEditarClick = { tarea ->
                if (!esAdmin) {
                    Toast.makeText(this, "Solo el administrador puede editar tareas", Toast.LENGTH_SHORT).show()
                    return@TareaAdapter
                }
                mostrarDialogoEditarTarea(tarea)
            },
            onEliminarClick = { tarea ->
                if (!esAdmin) {
                    Toast.makeText(this, "Solo el administrador puede eliminar tareas", Toast.LENGTH_SHORT).show()
                    return@TareaAdapter
                }
                confirmarRechazoTarea(tarea, position = null)
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
        for (i in -1 downTo -6) pisos.add("Piso $i")

        val adapterPiso = ArrayAdapter(this, android.R.layout.simple_spinner_item, pisos)
        adapterPiso.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFiltroPiso.adapter = adapterPiso

        binding.spFiltroPiso.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val pisoSeleccionado = parent?.getItemAtPosition(position).toString()
                viewModel.cambiarPiso(pisoSeleccionado)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarSpinnerSupervisor() {
        val nombres = mutableListOf("Todos")
        nombres.addAll(listaSupervisores.map { it.nombreVisible })

        val adapterSup = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
        adapterSup.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFiltroSupervisor.adapter = adapterSup

        binding.spFiltroSupervisor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!esAdmin) return
                val usernameSupervisor = if (position == 0) null else listaSupervisores[position - 1].username
                viewModel.cambiarSupervisor(usernameSupervisor)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarEventosUI() {
        // Botones de Modo (Usan la función centralizada)
        binding.btnTareasPendientes.setOnClickListener { cambiarModoYActualizarUI(ModoMuro.PENDIENTES) }
        binding.btnTareasRealizadas.setOnClickListener { cambiarModoYActualizarUI(ModoMuro.REALIZADAS) }
        binding.btnTareasAsignadas.setOnClickListener { cambiarModoYActualizarUI(ModoMuro.ASIGNADAS) }

        // Buscador
        binding.etBuscarDescripcionOUbicacion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.actualizarBusqueda(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // --- LÓGICA CENTRAL DE MODO Y VISIBILIDAD ---

    private fun cambiarModoYActualizarUI(modo: ModoMuro) {
        // 1. Notificar al ViewModel
        viewModel.cambiarModo(modo)

        // 2. Actualizar botones visualmente
        when (modo) {
            ModoMuro.PENDIENTES -> marcarBotonActivo(binding.btnTareasPendientes)
            ModoMuro.ASIGNADAS -> marcarBotonActivo(binding.btnTareasAsignadas)
            ModoMuro.REALIZADAS -> marcarBotonActivo(binding.btnTareasRealizadas)
        }

        // 3. Actualizar Filtros
        actualizarVisibilidadFiltros(modo)
    }

    private fun actualizarVisibilidadFiltros(modo: ModoMuro) {
        // Filtro Supervisor: Solo visible en ASIGNADAS si es Admin
        val mostrarSupervisor = (modo == ModoMuro.ASIGNADAS && esAdmin)
        binding.spFiltroSupervisor.visibility = if (mostrarSupervisor) View.VISIBLE else View.GONE

        // Filtro Fechas: Visible SIEMPRE (para que puedas filtrar en todos los estados)
        binding.layoutFiltroFechas.visibility = View.VISIBLE
    }

    // -------------------- FILTRO DE FECHAS Y LIMPIEZA --------------------

    private fun configurarEventosFiltroFecha() {
        binding.tvFechaDesde.setOnClickListener { mostrarDatePicker(esFechaDesde = true) }
        binding.tvFechaHasta.setOnClickListener { mostrarDatePicker(esFechaDesde = false) }

        // Botón Aplicar (Verde)
        binding.btnAplicarFiltroFecha.setOnClickListener { aplicarFiltroDeFechas() }

        // Botón Limpiar Todo (Rojo)
        binding.btnLimpiarFiltroFecha.setOnClickListener {
            limpiarTodosLosFiltrosUI()
        }
    }

    private fun mostrarDatePicker(esFechaDesde: Boolean) {
        val calendario = Calendar.getInstance()
        val fechaInicial = if (esFechaDesde) fechaSeleccionadaDesde else fechaSeleccionadaHasta
        fechaInicial?.let { calendario.timeInMillis = it.timeInMillis }

        val picker = DatePickerDialog(this, { _, year, month, day ->
            val nuevaFecha = Calendar.getInstance()
            nuevaFecha.set(year, month, day, 0, 0, 0)
            nuevaFecha.set(Calendar.MILLISECOND, 0)

            if (esFechaDesde) {
                fechaSeleccionadaDesde = nuevaFecha
                binding.tvFechaDesde.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(nuevaFecha.time)
            } else {
                fechaSeleccionadaHasta = nuevaFecha
                binding.tvFechaHasta.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(nuevaFecha.time)
            }
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH))

        picker.show()
    }

    private fun aplicarFiltroDeFechas() {
        val desde = fechaSeleccionadaDesde?.let { Timestamp(it.time) }
        val hasta = fechaSeleccionadaHasta?.let {
            val finDia = Calendar.getInstance().apply {
                timeInMillis = it.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            Timestamp(finDia.time)
        }

        if (desde != null && hasta != null && desde.compareTo(hasta) > 0) {
            Toast.makeText(this, "Fecha 'Desde' mayor a 'Hasta'", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.aplicarFiltroFechas(desde, hasta)
    }

    private fun limpiarTodosLosFiltrosUI() {
        // 1. Limpiar Variables de Fecha
        fechaSeleccionadaDesde = null
        fechaSeleccionadaHasta = null

        // 2. Limpiar UI
        binding.tvFechaDesde.text = ""
        binding.tvFechaDesde.hint = "Desde: DD/MM/AAAA"
        binding.tvFechaHasta.text = ""
        binding.tvFechaHasta.hint = "Hasta: DD/MM/AAAA"
        binding.etBuscarDescripcionOUbicacion.setText("")

        // Volver spinners a "Todos"
        if (binding.spFiltroPiso.adapter != null && binding.spFiltroPiso.count > 0) {
            binding.spFiltroPiso.setSelection(0)
        }
        if (binding.spFiltroSupervisor.adapter != null && binding.spFiltroSupervisor.count > 0) {
            binding.spFiltroSupervisor.setSelection(0)
        }

        // 3. Llamar al ViewModel para resetear lógica
        viewModel.limpiarTodosLosFiltros()
        Toast.makeText(this, "Todos los filtros restablecidos", Toast.LENGTH_SHORT).show()
    }

    // -------------------- BOTTOM NAV & OBSERVERS --------------------

    private fun configurarBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_muro_tareas
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    startActivity(Intent(this, CrearTareaActivity::class.java).apply {
                        putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                        putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                    })
                    true
                }
                R.id.nav_muro_tareas -> true
                else -> false
            }
        }
    }

    private fun observarViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBarMuro.visibility = if (state.cargando) View.VISIBLE else View.GONE
            // Usamos submitList (si usas ListAdapter) o actualizarLista (si usas RecyclerView.Adapter estándar)
            // Según tu código anterior era 'actualizarLista'
            adapter.actualizarLista(state.tareas)
            state.error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun marcarBotonActivo(botonActivo: Button) {
        val botones = listOf(binding.btnTareasPendientes, binding.btnTareasAsignadas, binding.btnTareasRealizadas)
        botones.forEach { btn ->
            btn.alpha = if (btn == botonActivo) 1f else 0.5f
            btn.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    // -------------------- ACCIONES (Swipe, Edit, Delete) --------------------

    private fun configurarSwipeConRol() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val tarea = adapter.obtenerTareaEnPosicion(position) ?: return

                if (!esAdmin) {
                    Toast.makeText(this@MuroTareasActivity, "Solo el administrador puede gestionar tareas", Toast.LENGTH_SHORT).show()
                    adapter.notifyItemChanged(position)
                    return
                }

                if (direction == ItemTouchHelper.LEFT) confirmarRechazoTarea(tarea, position)
                else mostrarDialogoAsignarSupervisor(tarea)

                adapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvTareas)
    }

    private fun confirmarRechazoTarea(tarea: Tarea, position: Int?) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Eliminar esta solicitud?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.rechazarTarea(tarea) { ok, error ->
                    if (ok) Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoAsignarSupervisor(tarea: Tarea) {
        val nombres = listaSupervisores.map { it.nombreVisible }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Asignar tarea")
            .setItems(nombres) { _, which ->
                val sup = listaSupervisores[which]
                viewModel.asignarTarea(tarea, sup.username) { ok, _ ->
                    if (ok) Toast.makeText(this, "Asignada a ${sup.nombreVisible}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun mostrarDialogoEditarTarea(tarea: Tarea) {
        val view = layoutInflater.inflate(R.layout.dialog_editar_tarea, null)
        val etDesc = view.findViewById<AppCompatEditText>(R.id.etDescripcionEditar)
        val etUbi = view.findViewById<AppCompatEditText>(R.id.etUbicacionEditar)
        val spPiso = view.findViewById<Spinner>(R.id.spPisoEditar)

        etDesc.setText(tarea.descripcion)
        etUbi.setText(tarea.ubicacion)

        val pisos = mutableListOf<String>()
        for (i in 6 downTo 1) pisos.add("Piso $i")
        for (i in -1 downTo -6) pisos.add("Piso $i")
        val adp = ArrayAdapter(this, android.R.layout.simple_spinner_item, pisos)
        spPiso.adapter = adp
        val idx = pisos.indexOfFirst { it.equals(tarea.piso, ignoreCase = true) }
        if (idx >= 0) spPiso.setSelection(idx)

        AlertDialog.Builder(this)
            .setTitle("Editar tarea")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                viewModel.editarTarea(tarea, etDesc.text.toString(), etUbi.text.toString(), spPiso.selectedItem.toString()) { ok, _ ->
                    if (ok) Toast.makeText(this, "Actualizada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // -------------------- CÁMARA / RESPUESTA --------------------

    private fun abrirCamaraParaRespuesta() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) == null) return

        try {
            val photoFile = crearArchivoImagenRespuesta()
            val photoURI = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", photoFile)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, REQUEST_FOTO_RESPUESTA)
        } catch (e: Exception) {
            Toast.makeText(this, "Error cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearArchivoImagenRespuesta(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("RESP_${timeStamp}_", ".jpg", storageDir).apply { currentPhotoPathRespuesta = absolutePath }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FOTO_RESPUESTA && resultCode == RESULT_OK) {
            val path = currentPhotoPathRespuesta
            val tarea = tareaSeleccionadaParaRespuesta
            if (path != null && tarea != null) mostrarDialogoComentarioRespuesta(tarea, path)
        }
    }

    private fun mostrarDialogoComentarioRespuesta(tarea: Tarea, path: String) {
        val input = AppCompatEditText(this).apply { hint = "Comentario (opcional)" }
        AlertDialog.Builder(this)
            .setTitle("Finalizar Tarea")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                viewModel.guardarRespuesta(tarea, path, input.text.toString()) { ok, _ ->
                    if (ok) Toast.makeText(this, "Tarea realizada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}