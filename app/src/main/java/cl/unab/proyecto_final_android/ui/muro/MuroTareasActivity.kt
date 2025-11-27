package cl.unab.proyecto_final_android.ui.muro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.util.FiltroFechaManager
import cl.unab.proyecto_final_android.util.MuroConfigurator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MuroTareasActivity : AppCompatActivity() {

    // ---------------------------
    // ViewBinding + ViewModel
    // ---------------------------
    private var _binding: ActivityMuroTareasBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TareasViewModel
    private lateinit var adapter: TareaAdapter

    // ---------------------------
    // Usuario y Rol
    // ---------------------------
    private var rolUsuario: String = LoginActivity.ROL_REALIZAR
    private var usernameActual: String = ""

    private val esAdmin: Boolean
        get() = usernameActual.equals("administrador", ignoreCase = true) ||
                usernameActual.equals("administrador@miapp.com", ignoreCase = true)

    companion object {
        private const val STATE_SCROLL_POSITION = "state_scroll_position"
    }

    // ---------------------------
    // Variables de Foto
    // ---------------------------
    private var tareaEnRespuesta: Tarea? = null
    private var fotoAntesUri: Uri? = null
    private var fotoRespuestaUri: Uri? = null

    // ---------------------------
    // Launchers
    // ---------------------------

    // Permiso cámara → responder tarea
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                tareaEnRespuesta?.let { lanzarCamaraDespuesDePermiso(it) }
            } else {
                Toast.makeText(this, "El permiso de la cámara es necesario.", Toast.LENGTH_LONG).show()
                tareaEnRespuesta = null
            }
        }

    // Tomar foto → responder tarea
    private val camaraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val tarea = tareaEnRespuesta
            val uri = fotoRespuestaUri

            if (success && tarea != null && uri != null) {
                iniciarProcesoDeRespuesta(tarea, uri)
            } else {
                Toast.makeText(this, "Captura cancelada o fallida.", Toast.LENGTH_SHORT).show()
            }

            tareaEnRespuesta = null
            fotoRespuestaUri = null
        }

    // Permiso cámara → crear tarea
    private val crearTareaCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) lanzarCamaraParaCreacion()
            else Toast.makeText(this, "Se requiere cámara para tomar la foto.", Toast.LENGTH_LONG).show()
        }

    // Tomar foto → crear tarea
    private val crearTareaCamaraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && fotoAntesUri != null) {
                lanzarCrearTareaActivity(fotoAntesUri!!)
            } else {
                Toast.makeText(this, "Foto cancelada.", Toast.LENGTH_SHORT).show()
                fotoAntesUri = null
            }
        }

    // Galería → crear tarea
    internal val crearTareaGaleriaLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { lanzarCrearTareaActivity(it) }
                ?: Toast.makeText(this, "Selección cancelada.", Toast.LENGTH_SHORT).show()
        }

    // ---------------------------
    // Ciclo de vida
    // ---------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMuroTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Quitar el relleno solido de la barra de navegación
        binding.bottomNav.itemIconTintList = null

        // Roles recibidos por Intent
        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO) ?: LoginActivity.ROL_REALIZAR
        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""

        // ViewModel + Repositorio
        val repo = TareaRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
        val factory = TareasViewModelFactory(repo, rolUsuario, usernameActual)
        viewModel = ViewModelProvider(this, factory)[TareasViewModel::class.java]


        // Inicializadores UI
        FiltroFechaManager(this, binding, viewModel)

        adapter = TareaAdapter(
            tareas = emptyList(),
            rolUsuario = rolUsuario,
            usernameActual = usernameActual,
            onResponderClick = { tarea -> intentarTomarFoto(tarea) },
            onEditarClick = { tarea -> MuroConfigurator.mostrarDialogoEditarTarea(this, viewModel, tarea) },
            onEliminarClick = { tarea -> MuroConfigurator.confirmarEliminacionTarea(this, viewModel, tarea) }
        )

        MuroConfigurator.configurarRecyclerView(binding.rvTareas, adapter)
        MuroConfigurator.configurarSpinners(this, binding, viewModel, esAdmin)
        MuroConfigurator.configurarEventosUI(binding, viewModel, esAdmin)
        MuroConfigurator.configurarBottomNav(this, binding.bottomNav, rolUsuario, usernameActual)
        MuroConfigurator.configurarSwipeConRol(this, binding.rvTareas, adapter, viewModel, esAdmin)

        observarViewModel()

        viewModel.cargarTareas()
        MuroConfigurator.marcarBotonActivo(binding.btnTareasPendientes, binding)
        MuroConfigurator.actualizarVisibilidadFiltros(binding, esAdmin, ModoMuro.PENDIENTES)


        val scrollPos = savedInstanceState?.getInt(STATE_SCROLL_POSITION, 0) ?: 0
        binding.rvTareas.post {
            (binding.rvTareas.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(scrollPos, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarTareas()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        tareaEnRespuesta = null
        fotoRespuestaUri = null
        fotoAntesUri = null
    }

    // Responder Tarea (Foto Después)
    fun intentarTomarFoto(tarea: Tarea) {
        if (!puedeResponderTarea(tarea)) {
            Toast.makeText(this, "No tienes permisos para responder esta tarea.", Toast.LENGTH_SHORT).show()
            return
        }

        tareaEnRespuesta = tarea

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            lanzarCamaraDespuesDePermiso(tarea)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    internal fun lanzarCamaraDespuesDePermiso(tarea: Tarea) {
        fotoRespuestaUri = crearUriDeArchivoTemporal("RESPUESTA")
        fotoRespuestaUri?.let { camaraLauncher.launch(it) }
            ?: Toast.makeText(this, "No se pudo preparar la foto.", Toast.LENGTH_SHORT).show()
    }

    internal fun iniciarProcesoDeRespuesta(tarea: Tarea, uri: Uri) {
        viewModel.subirFotoDeRespuesta(tarea, uri) { ok, error ->
            if (ok) Toast.makeText(this, "Tarea realizada con éxito.", Toast.LENGTH_LONG).show()
            else Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        }
    }

    // Crear Tarea (Foto Antes)
    fun intentarTomarFotoParaCreacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarCamaraParaCreacion()
        } else {
            crearTareaCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun lanzarGaleriaParaCreacion() {
        crearTareaGaleriaLauncher.launch("image/*")
    }

    private fun lanzarCamaraParaCreacion() {
        fotoAntesUri = crearUriDeArchivoTemporal("ANTES")
        fotoAntesUri?.let { crearTareaCamaraLauncher.launch(it) }
            ?: Toast.makeText(this, "Error al preparar la foto.", Toast.LENGTH_SHORT).show()
    }

    private fun lanzarCrearTareaActivity(uriFotoAntes: Uri) {
        startActivity(
            Intent(this, CrearTareaActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                putExtra("EXTRA_FOTO_ANTES_URI", uriFotoAntes.toString())
            }
        )
    }

    // Utilidades
    private fun crearUriDeArchivoTemporal(prefijo: String): Uri? {
        return try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: return null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File.createTempFile("${prefijo}_${timeStamp}", ".jpg", storageDir)

            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Error al crear archivo: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun observarViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBarMuro.visibility = if (state.cargando) View.VISIBLE else View.GONE
            adapter.actualizarTareas(state.tareas)
            state.error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val layoutManager = binding.rvTareas.layoutManager as? LinearLayoutManager
        val pos = layoutManager?.findFirstVisibleItemPosition() ?: 0
        outState.putInt(STATE_SCROLL_POSITION, pos)
    }

    private fun puedeResponderTarea(tarea: Tarea): Boolean {
        // No responder tareas cerradas
        if (tarea.estado.equals("Realizada", true) || tarea.estado.equals("Rechazada", true)) {
            return false
        }

        // El rol CREAR nunca responde
        if (rolUsuario == LoginActivity.ROL_CREAR) return false

        // Admin siempre puede
        if (esAdmin) return true

        // Para supervisores y realizar_tarea, usamos username corto
        val usernameCorto = usernameActual.substringBefore("@").lowercase()

        // Si no hay asignado, puede responder
        if (tarea.asignadaA.isNullOrEmpty()) return true

        // Si hay asignado, solo si coincide con su username
        return tarea.asignadaA.equals(usernameCorto, ignoreCase = true)
    }



}

