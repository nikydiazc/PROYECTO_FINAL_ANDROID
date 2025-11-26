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

    private var _binding: ActivityMuroTareasBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TareasViewModel
    private lateinit var adapter: TareaAdapter

    // Variables de Estado y Usuario
    private var rolUsuario: String = LoginActivity.ROL_REALIZAR
    private var usernameActual: String = ""
    // La comprobación de rol debe ser más flexible, pero mantenemos tu lógica de "esAdmin" para los permisos
    private val esAdmin: Boolean
        get() = usernameActual.equals("administrador", ignoreCase = true) ||
                usernameActual.equals("administrador@miapp.com", ignoreCase = true)

    // Variables de Estado para FOTOS
    private var tareaEnRespuesta: Tarea? = null // Tarea seleccionada para responder
    private var fotoAntesUri: Uri? = null // URI temporal para la foto ANTES (Creación de Tarea)
    private var fotoRespuestaUri: Uri? = null // URI temporal para la foto DESPUÉS (Respuesta de Tarea)

    // ------------- LAUNCHERS ----------------

    // LAUNCHER 1: Permisos de Cámara para RESPONDER TAREA
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

    // LAUNCHER 2: Captura de Foto para RESPONDER TAREA
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

    // LAUNCHER 3: Permisos de Cámara para CREAR TAREA
    private val crearTareaCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                lanzarCamaraParaCreacion()
            } else {
                Toast.makeText(this, "El permiso de la cámara es necesario para tomar la foto de la tarea.", Toast.LENGTH_LONG).show()
            }
        }

    // LAUNCHER 4: Captura de Foto para CREAR TAREA (CORREGIDO)
    private val crearTareaCamaraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success && fotoAntesUri != null) {
                // LLAMA A LA FUNCIÓN AGREGADA
                lanzarCrearTareaActivity(fotoAntesUri!!)
            } else {
                Toast.makeText(this, "Captura de foto cancelada o fallida.", Toast.LENGTH_SHORT).show()
                fotoAntesUri = null
            }
        }

    // LAUNCHER 5: Selección de Galería para CREAR TAREA (CORREGIDO)
    internal val crearTareaGaleriaLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // LLAMA A LA FUNCIÓN AGREGADA
                lanzarCrearTareaActivity(uri)
            } else {
                Toast.makeText(this, "Selección de foto cancelada.", Toast.LENGTH_SHORT).show()
            }
        }

    // --------- CICLO DE VIDA -----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMuroTareasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Inicialización de datos y ViewModel
        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO) ?: LoginActivity.ROL_REALIZAR
        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""
        val repo = TareaRepository(FirebaseFirestore.getInstance(), FirebaseStorage.getInstance())
        val factory = TareasViewModelFactory(repo, esAdmin, usernameActual)

        viewModel = ViewModelProvider(this, factory)[TareasViewModel::class.java]

        FiltroFechaManager(this, binding, viewModel)


        // 2. Inicialización del Adapter
        // MODIFICACIÓN: PASAMOS EL ROL COMPLETO AL ADAPTER para la lógica de visibilidad de botones
        adapter = TareaAdapter(
            tareas = emptyList(),
            rolUsuario = rolUsuario, // Pasamos el rol
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
        viewModel.cargarTareas()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        tareaEnRespuesta = null
        fotoRespuestaUri = null
        fotoAntesUri = null
    }

    // ------- FUNCIONES PARA RESPONDER TAREA (FOTO DESPUÉS) -------------

    // Función de entrada que verifica el permiso.
    fun intentarTomarFoto(tarea: Tarea) {
        if (tarea.estado.equals("Realizada", ignoreCase = true) || tarea.estado.equals("Rechazada", ignoreCase = true)) {
            Toast.makeText(this, "Solo se pueden responder las tareas Pendientes o Asignadas", Toast.LENGTH_SHORT).show()
            return
        }

        tareaEnRespuesta = tarea

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarCamaraDespuesDePermiso(tarea)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Función que lanza la cámara (si el permiso es OK)
    internal fun lanzarCamaraDespuesDePermiso(tarea: Tarea) {
        fotoRespuestaUri = crearUriDeArchivoTemporal("RESPUESTA") // Usar prefijo
        val uriParaLanzar = fotoRespuestaUri

        if (uriParaLanzar != null) {
            camaraLauncher.launch(uriParaLanzar)
        } else {
            Toast.makeText(this, "Error al preparar el archivo de foto.", Toast.LENGTH_SHORT).show()
        }
    }

    // Función que delega al ViewModel
    internal fun iniciarProcesoDeRespuesta(tarea: Tarea, uri: Uri) {
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

    // ------- FUNCIONES PARA CREAR TAREA (FOTO ANTES) -------------

    // Función de entrada para la cámara (llamada desde MuroConfigurator)
    fun intentarTomarFotoParaCreacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            lanzarCamaraParaCreacion()
        } else {
            crearTareaCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Función que lanza la galería (llamada desde MuroConfigurator)
    fun lanzarGaleriaParaCreacion() {
        crearTareaGaleriaLauncher.launch("image/*") // Inicia el Launcher 5
    }

    // Función que lanza la cámara (si el permiso es OK)
    private fun lanzarCamaraParaCreacion() {
        fotoAntesUri = crearUriDeArchivoTemporal("ANTES") // Usar prefijo
        val uriParaLanzar = fotoAntesUri

        if (uriParaLanzar != null) {
            crearTareaCamaraLauncher.launch(uriParaLanzar) // Inicia el Launcher 4
        } else {
            Toast.makeText(this, "Error al preparar el archivo de foto.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- FUNCIÓN AGREGADA para manejar la navegación tras tomar/seleccionar la foto ---
    private fun lanzarCrearTareaActivity(uriFotoAntes: Uri) {
        val intent = Intent(this, CrearTareaActivity::class.java).apply {
            // Aseguramos que los datos de usuario vayan a la siguiente Activity
            putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
            putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
            // Pasamos la URI de la foto inicial
            putExtra("EXTRA_FOTO_ANTES_URI", uriFotoAntes.toString())
        }
        startActivity(intent)
    }


    // ------- FUNCIONES AUXILIARES Y OBSERVADORES -------------

    /**
     * Función auxiliar para crear la URI temporal.
     * @param prefijo Prefijo del nombre del archivo (ej: "ANTES", "RESPUESTA").
     */
    private fun crearUriDeArchivoTemporal(prefijo: String): Uri? {
        try {
            val storageDir: File = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: return null

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${prefijo}_${timeStamp}"

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

    private fun observarViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBarMuro.visibility = if (state.cargando) View.VISIBLE else View.GONE
            adapter.actualizarTareas(state.tareas)
            state.error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}