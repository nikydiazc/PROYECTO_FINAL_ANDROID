package cl.unab.proyecto_final_android.ui.muro

import android.R
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
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MuroTareasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMuroTareasBinding
    private lateinit var viewModel: TareasViewModel
    private lateinit var adapter: TareaAdapter

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

    // Supervisores (los mismos que definimos antes)
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
        observarViewModel()

        // Cargar por primera vez
        viewModel.cargarTareas()
        marcarBotonActivo(binding.btnTareasPendientes)
    }

    // -------------------- CONFIGURACIÓN UI --------------------

    private fun configurarRecyclerView() {
        adapter = TareaAdapter(
            tareas = emptyList(),
            rolUsuario = rolUsuario,
            onResponderClick = { tarea ->
                if (tarea.estado != "Pendiente") {
                    Toast.makeText(
                        this,
                        "Solo se pueden responder las tareas pendientes",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    tareaSeleccionadaParaRespuesta = tarea
                    abrirCamaraParaRespuesta()
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
        for (i in 6 downTo 1) {
            pisos.add("Piso $i")
        }
        for (i in -1 downTo -6) {
            pisos.add("Piso $i")
        }

        val adapterPiso = ArrayAdapter(
            this,
            R.layout.simple_spinner_item,
            pisos
        )
        adapterPiso.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spFiltroPiso.adapter = adapterPiso

        binding.spFiltroPiso.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val pisoSeleccionado = parent?.getItemAtPosition(position).toString()
                    viewModel.cambiarPiso(pisoSeleccionado)
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
            R.layout.simple_spinner_item,
            nombres
        )
        adapterSup.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
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

                    val usernameSupervisor = if (position == 0) {
                        null
                    } else {
                        val supervisorSeleccionado = listaSupervisores[position - 1]
                        supervisorSeleccionado.username
                    }
                    viewModel.cambiarSupervisor(usernameSupervisor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun configurarEventosUI() {

        binding.btnTareasPendientes.setOnClickListener {
            binding.spFiltroSupervisor.visibility = View.GONE
            marcarBotonActivo(binding.btnTareasPendientes)
            viewModel.cambiarModo(ModoMuro.PENDIENTES)
        }

        binding.btnTareasRealizadas.setOnClickListener {
            binding.spFiltroSupervisor.visibility = View.GONE
            marcarBotonActivo(binding.btnTareasRealizadas)
            viewModel.cambiarModo(ModoMuro.REALIZADAS)
        }

        binding.btnTareasAsignadas.setOnClickListener {
            marcarBotonActivo(binding.btnTareasAsignadas)

            if (esAdmin) {
                binding.spFiltroSupervisor.visibility = View.VISIBLE
            } else {
                binding.spFiltroSupervisor.visibility = View.GONE
            }

            viewModel.cambiarModo(ModoMuro.ASIGNADAS)
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
                viewModel.actualizarBusqueda(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarBottomNav() {
        val bottomNav = binding.bottomNav
        bottomNav.selectedItemId = cl.unab.proyecto_final_android.R.id.nav_muro_tareas

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                cl.unab.proyecto_final_android.R.id.nav_crear_tarea -> {
                    startActivity(
                        Intent(this, CrearTareaActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                            putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                        }
                    )
                    true
                }
                cl.unab.proyecto_final_android.R.id.nav_muro_tareas -> true
                else -> false
            }
        }
    }

    private fun observarViewModel() {
        viewModel.uiState.observe(this) { state ->
            // loading
            binding.progressBarMuro.visibility =
                if (state.cargando) View.VISIBLE else View.GONE

            // lista
            adapter.actualizarLista(state.tareas)

            // error
            state.error?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun marcarBotonActivo(botonActivo: Button) {
        val botones = listOf(
            binding.btnTareasPendientes,
            binding.btnTareasAsignadas,
            binding.btnTareasRealizadas
        )

        botones.forEach { btn ->
            btn.alpha = if (btn == botonActivo) 1f else 0.5f
            btn.setTextColor(ContextCompat.getColor(this, cl.unab.proyecto_final_android.R.color.white))
        }
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

                val tarea = adapter.obtenerTareaEnPosicion(position) ?: run {
                    adapter.notifyItemChanged(position)
                    return
                }

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
                viewModel.rechazarTarea(tarea) { ok, error ->
                    if (ok) {
                        Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error al eliminar: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                        adapter.notifyItemChanged(position)
                    }
                }
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

    private fun mostrarDialogoAsignarSupervisor(tarea: Tarea) {
        val nombres = listaSupervisores.map { it.nombreVisible }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Asignar tarea a supervisor")
            .setItems(nombres) { dialog, which ->
                val supervisorSeleccionado = listaSupervisores[which]
                viewModel.asignarTarea(tarea, supervisorSeleccionado.username) { ok, error ->
                    if (ok) {
                        Toast.makeText(
                            this,
                            "Tarea asignada a ${supervisorSeleccionado.nombreVisible}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error al asignar: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    // -------------------- RESPUESTA (FOTO + COMENTARIO) --------------------

    private fun abrirCamaraParaRespuesta() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "No se encontró una app de cámara", Toast.LENGTH_SHORT).show()
            return
        }

        val photoFile: File? = try {
            crearArchivoImagenRespuesta()
        } catch (ex: IOException) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
            null
        }

        if (photoFile != null) {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(intent, REQUEST_FOTO_RESPUESTA)
        }
    }

    @Throws(IOException::class)
    private fun crearArchivoImagenRespuesta(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "RESPUESTA_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPathRespuesta = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FOTO_RESPUESTA && resultCode == RESULT_OK) {
            val path = currentPhotoPathRespuesta
            val tarea = tareaSeleccionadaParaRespuesta

            if (path != null && tarea != null) {
                mostrarDialogoComentarioRespuesta(tarea, path)
            } else {
                Toast.makeText(this, "No se pudo obtener la foto de respuesta", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun mostrarDialogoComentarioRespuesta(tarea: Tarea, photoPath: String) {
        val input = AppCompatEditText(this).apply {
            hint = "Comentario (opcional)"
            setPadding(40, 40, 40, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Respuesta a la solicitud")
            .setMessage("Puedes agregar un comentario sobre la limpieza realizada (opcional):")
            .setView(input)
            .setPositiveButton("Guardar respuesta") { _, _ ->
                val comentario = input.text?.toString()?.trim().orEmpty()
                guardarRespuestaTarea(tarea, photoPath, comentario)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun guardarRespuestaTarea(tarea: Tarea, photoPath: String, comentario: String) {
        viewModel.guardarRespuesta(tarea, photoPath, comentario) { ok, error ->
            if (ok) {
                Toast.makeText(
                    this,
                    "Respuesta guardada y tarea marcada como realizada",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Error al guardar respuesta: $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
