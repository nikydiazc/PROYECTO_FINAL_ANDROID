package cl.unab.proyecto_final_android.ui.muro

import android.Manifest
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
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
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

    private var _binding: ActivityMuroTareasBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TareasViewModel
    private lateinit var adapter: TareaAdapter

    // Variables de Estado y Usuario
    private var rolUsuario: String = LoginActivity.ROL_REALIZAR
    private var usernameActual: String = ""
    private val esAdmin: Boolean
        get() = usernameActual.equals("administrador", ignoreCase = true) ||
                usernameActual.equals("administrador@miapp.com", ignoreCase = true)

    // Variables para la respuesta de Tarea (Cámara)
    private var tareaEnRespuesta: Tarea? = null
    private var fotoRespuestaUri: Uri? = null

    // -------------------- LAUNCHERS (DEBE ESTAR EN LA ACTIVITY) --------------------

    // LAUNCHER 1: Solicitud de Permiso de la Cámara (Resuelve el SecurityException)
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val tareaParaLanzar = tareaEnRespuesta
                if (tareaParaLanzar != null) {
                    lanzarCamaraDespuesDePermiso(tareaParaLanzar)
                }
            } else {
                Toast.makeText(this, "El permiso de la cámara es necesario para responder.", Toast.LENGTH_LONG).show()
                tareaEnRespuesta = null
            }
        }

    // LAUNCHER 2: Captura de Foto
    private val camaraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->

            val uriTomada = fotoRespuestaUri
            val tareaEnCurso = tareaEnRespuesta

            if (success && uriTomada != null && tareaEnCurso != null) {
                iniciarProcesoDeRespuesta(tareaEnCurso, uriTomada)
            } else {
                Toast.makeText(this, "Captura de foto cancelada o fallida.", Toast.LENGTH_SHORT).show()
                tareaEnRespuesta = null
                fotoRespuestaUri = null
            }
        }

    // -------------------- CICLO DE VIDA --------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMuroTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inicialización de datos y ViewModel
        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO) ?: LoginActivity.ROL_REALIZAR
        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""
        val repo = TareaRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
        val factory = TareasViewModelFactory(repo, esAdmin, usernameActual)

        // ¡IMPORTANTE! El ViewModel debe inicializarse antes de ser usado.
        viewModel = ViewModelProvider(this, factory)[TareasViewModel::class.java]

        // 💡 Corrección: FiltroFechaManager utiliza el viewModel, por lo que debe inicializarse después.
        FiltroFechaManager(this, binding, viewModel)


        // 2. Inicialización del Adapter
        adapter = TareaAdapter(
            tareas = emptyList(),
            esAdmin = esAdmin,
            usernameActual = usernameActual,
            onResponderClick = { tarea -> intentarTomarFoto(tarea) },
            onEditarClick = { tarea -> MuroConfigurator.mostrarDialogoEditarTarea(this, viewModel, tarea) },
            onEliminarClick = { tarea -> MuroConfigurator.confirmarEliminacionTarea(this, viewModel, tarea) }
        )

        // 3. Delegar Configuración UI
        MuroConfigurator.configurarRecyclerView(binding.rvTareas, adapter)
        MuroConfigurator.configurarSpinners(this, binding, viewModel, esAdmin)
        MuroConfigurator.configurarEventosUI(binding, viewModel, esAdmin)
        MuroConfigurator.configurarBottomNav(this, binding.bottomNav, rolUsuario, usernameActual)
        MuroConfigurator.configurarSwipeConRol(this, binding.rvTareas, adapter, viewModel, esAdmin)

        // 4. Observación y Carga Inicial
        observarViewModel()
        viewModel.cargarTareas()
        MuroConfigurator.marcarBotonActivo(binding.btnTareasPendientes, binding)
        MuroConfigurator.actualizarVisibilidadFiltros(binding, esAdmin, ModoMuro.PENDIENTES)
    }

    override fun onResume() {
        super.onResume()
        // 🟢 ENFOQUE: Recargar la lista garantiza que los datos estén frescos.
        viewModel.cargarTareas()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar el View Binding y referencias de estado.
        _binding = null
        tareaEnRespuesta = null
        fotoRespuestaUri = null
    }

    // ------- CÁMARA -------------

    // Función de entrada que verifica el permiso.
    fun intentarTomarFoto(tarea: Tarea) {
        if (tarea.estado.equals("Realizada", ignoreCase = true) || tarea.estado.equals("Rechazada", ignoreCase = true)) {
            Toast.makeText(this, "Solo se pueden responder las tareas Pendientes o Asignadas", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Guardar la tarea antes de la solicitud de permiso
        tareaEnRespuesta = tarea

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarCamaraDespuesDePermiso(tarea)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Función que lanza la cámara (si el permiso es OK)
    private fun lanzarCamaraDespuesDePermiso(tarea: Tarea) {
        fotoRespuestaUri = crearUriDeArchivoTemporal()
        val uriParaLanzar = fotoRespuestaUri

        if (uriParaLanzar != null) {
            camaraLauncher.launch(uriParaLanzar)
        } else {
            Toast.makeText(this, "Error al preparar el archivo de foto.", Toast.LENGTH_SHORT).show()
        }
    }

    // Función auxiliar para crear la URI temporal
    private fun crearUriDeArchivoTemporal(): Uri? {
        try {
            val storageDir: File = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "RESPUESTA_${tareaEnRespuesta?.id ?: "TEMP"}_${timeStamp}"

            val imagenTemporal = File.createTempFile(fileName, ".jpg", storageDir)

            return FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
                imagenTemporal
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error de Sistema al crear archivo: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    // Función que delega al ViewModel
    private fun iniciarProcesoDeRespuesta(tarea: Tarea, uri: Uri) {
        Toast.makeText(this, "Iniciando subida de foto para la tarea: ${tarea.descripcion}", Toast.LENGTH_SHORT).show()

        viewModel.subirFotoDeRespuesta(tarea, uri) { exito, error ->
            if (exito) {
                Toast.makeText(this, "Tarea realizada con éxito.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error al subir/actualizar tarea: ${error ?: "Desconocido"}", Toast.LENGTH_LONG).show()
            }

            tareaEnRespuesta = null
            fotoRespuestaUri = null
        }
    }

    // ------- OBSERVER ---------

    private fun observarViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBarMuro.visibility = if (state.cargando) View.VISIBLE else View.GONE
            adapter.actualizarTareas(state.tareas)
            state.error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}