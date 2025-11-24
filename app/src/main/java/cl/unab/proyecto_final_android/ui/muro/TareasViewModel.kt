package cl.unab.proyecto_final_android.ui.muro

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import com.google.firebase.Timestamp

// ---------------------------------- ESTRUCTURA DE ESTADO DE LA UI ----------------------------------
data class MuroUiState(
    val tareas: List<Tarea> = emptyList(),
    val cargando: Boolean = false, // Maneja el Progress Bar
    val error: String? = null,
    val modoMuro: ModoMuro = ModoMuro.PENDIENTES,
    val filtroPiso: String = "Todos",
    val filtroSupervisor: String? = null,
    val filtroBusqueda: String = "",
    val filtroFechaDesde: Timestamp? = null,
    val filtroFechaHasta: Timestamp? = null
)

// ---------------------------------- VIEWMODEL ----------------------------------
class TareasViewModel(
    private val tareaRepository: TareaRepository,
    private val esAdmin: Boolean,
    private val usernameActual: String
) : ViewModel() {

    private val _uiState = MutableLiveData(MuroUiState())
    val uiState: LiveData<MuroUiState> = _uiState

    // Inicialización: Cargar las tareas la primera vez
    init {
        cargarTareas()
    }

    // ---------------------------------- LÓGICA DE CARGA Y FILTROS (CORREGIDO) ----------------------------------

    /**
     * Función principal para cargar tareas usando los filtros del estado actual (uiState).
     * Esta función reemplaza a la antigua 'cargarTareasDelMuro'.
     */
    fun cargarTareas() {
        val estadoActual = _uiState.value ?: return

        // 1. ANTES: MUESTRA EL PROGRESS BAR (cargando = true) y limpia errores anteriores
        _uiState.value = estadoActual.copy(cargando = true, error = null)

        // 2. Llama al Repositorio usando los filtros del estado
        tareaRepository.getTareas(
            modo = estadoActual.modoMuro,
            piso = estadoActual.filtroPiso,
            busqueda = estadoActual.filtroBusqueda,
            asignadaA = estadoActual.filtroSupervisor,
            fechaDesde = estadoActual.filtroFechaDesde,
            fechaHasta = estadoActual.filtroFechaHasta,
            callback = { tareas, error ->

                // 3. DESPUÉS: Manejo de Respuesta

                val nuevoEstado = if (tareas != null) {
                    // ÉXITO: Actualiza la lista
                    estadoActual.copy(tareas = tareas, error = null)
                } else {
                    // FALLO: Almacena el error
                    val errorMessage = "Error al cargar tareas: ${error ?: "Error desconocido"}"
                    // En caso de error, la lista queda vacía
                    estadoActual.copy(tareas = emptyList(), error = errorMessage)
                }

                // 4. ¡LA CLAVE! OCULTAR EL PROGRESS BAR (cargando = false)
                // Se actualiza el estado completo y se detiene la carga.
                _uiState.value = nuevoEstado.copy(cargando = false)
            }
        )
    }

    // ---------------------------------- RESPONDER TAREA (Función de Cámara) ----------------------------------

    fun subirFotoDeRespuesta(tarea: Tarea, fotoUri: Uri, callback: (Boolean, String?) -> Unit) {
        // 1. Mostrar estado de carga (en la UI general)
        _uiState.value = _uiState.value?.copy(cargando = true, error = null)

        // 2. Delegar la subida y actualización al Repository
        tareaRepository.subirFotoDeRespuesta(tarea, fotoUri) { url, error ->
            // 3. Ocultar estado de carga (en el callback)
            _uiState.value = _uiState.value?.copy(cargando = false)

            if (url != null) {
                // Éxito: Recargamos la vista para reflejar el estado 'Realizada'
                cargarTareas()
                callback(true, null)
            } else {
                // Error: Actualizar estado con el mensaje de fallo
                val errorMessage = error ?: "Error desconocido al subir la foto y actualizar la tarea."
                _uiState.value = _uiState.value?.copy(error = errorMessage)
                callback(false, errorMessage)
            }
        }
    }

    // ---------------------------------- LÓGICA DE FILTROS Y ESTADO ----------------------------------

    // Las funciones de filtro ahora solo actualizan el UIState y luego llaman a cargarTareas()

    fun cambiarModo(modo: ModoMuro) {
        _uiState.value = _uiState.value?.copy(modoMuro = modo)
        cargarTareas()
    }

    fun cambiarPiso(piso: String) {
        _uiState.value = _uiState.value?.copy(filtroPiso = piso)
        cargarTareas()
    }

    fun cambiarSupervisor(supervisorUsername: String?) {
        _uiState.value = _uiState.value?.copy(filtroSupervisor = supervisorUsername)
        cargarTareas()
    }

    fun actualizarBusqueda(busqueda: String) {
        // La búsqueda solo actualiza el filtro, cargarTareas() se encarga de aplicar el filtro localmente.
        _uiState.value = _uiState.value?.copy(filtroBusqueda = busqueda)
        cargarTareas()
    }

    fun aplicarFiltroFechas(desde: Timestamp?, hasta: Timestamp?) {
        _uiState.value = _uiState.value?.copy(filtroFechaDesde = desde, filtroFechaHasta = hasta)
        cargarTareas()
    }

    fun limpiarTodosLosFiltros() {
        _uiState.value = _uiState.value?.copy(
            filtroPiso = "Todos",
            filtroSupervisor = null,
            filtroBusqueda = "",
            filtroFechaDesde = null,
            filtroFechaHasta = null
        )
        cargarTareas()
    }

    // ---------------------------------- ACCIONES CRUD ----------------------------------

    // Después de cualquier acción CRUD, se llama a cargarTareas() para refrescar el muro.

    fun eliminarTarea(tarea: Tarea, callback: (Boolean, String?) -> Unit) {
        tareaRepository.eliminarTarea(tarea.id) { ok, error ->
            if (ok) cargarTareas()
            callback(ok, error)
        }
    }

    fun rechazarTarea(tarea: Tarea, callback: (Boolean, String?) -> Unit) {
        tareaRepository.actualizarEstado(tarea.id, "Rechazada") { ok, error ->
            if (ok) cargarTareas()
            callback(ok, error)
        }
    }

    fun asignarTarea(tarea: Tarea, usernameSupervisor: String, callback: (Boolean, String?) -> Unit) {
        val updates = mapOf(
            "estado" to "Asignada",
            "asignadaA" to usernameSupervisor
        )
        tareaRepository.actualizarTarea(tarea.id, updates) { ok, error ->
            if (ok) cargarTareas()
            callback(ok, error)
        }
    }

    fun editarTarea(
        tarea: Tarea,
        descripcion: String,
        ubicacion: String,
        piso: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "descripcion" to descripcion,
            "ubicacion" to ubicacion,
            "piso" to piso
        )
        tareaRepository.actualizarTarea(tarea.id, updates) { ok, error ->
            if (ok) cargarTareas()
            callback(ok, error)
        }
    }
}