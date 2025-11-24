package cl.unab.proyecto_final_android.ui.muro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp

data class TareasUiState(
    val modoMuro: ModoMuro = ModoMuro.PENDIENTES,
    val tareas: List<Tarea> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val textoBusqueda: String = "",
    val pisoSeleccionado: String = "Todos",
    val filtroSupervisor: String? = null,
    val filtroFechaDesde: Timestamp? = null,
    val filtroFechaHasta: Timestamp? = null
)

class TareasViewModel(
    private val tareaRepository: TareaRepository,
    private val esAdmin: Boolean,
    private val usernameActual: String
) : ViewModel() {

    private val _uiState = MutableLiveData(TareasUiState())
    val uiState: LiveData<TareasUiState> = _uiState

    private var listener: ListenerRegistration? = null

    // lista base desde Firestore sin filtros locales
    private var listaBase: List<Tarea> = emptyList()

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }

    fun cargarTareas() {
        val state = _uiState.value ?: return

        _uiState.value = state.copy(cargando = true, error = null)

        listener?.remove()
        listener = tareaRepository.escucharTareas(
            modoMuro = state.modoMuro,
            esAdmin = esAdmin,
            usernameActual = usernameActual,
            filtroSupervisor = state.filtroSupervisor,
            fechaDesde = state.filtroFechaDesde,
            fechaHasta = state.filtroFechaHasta,
            onSuccess = { lista ->
                listaBase = lista
                aplicarFiltrosLocalesYPublicar()
            },
            onError = { e ->
                _uiState.postValue(
                    state.copy(
                        cargando = false,
                        error = e.message ?: "Error al cargar tareas"
                    )
                )
            }
        )
    }


    private fun aplicarFiltrosLocalesYPublicar() {
        val state = _uiState.value ?: return

        val texto = state.textoBusqueda.lowercase()
        val filtradas = listaBase.filter { tarea ->
            val coincideTexto =
                texto.isBlank() ||
                        tarea.descripcion.lowercase().contains(texto) ||
                        tarea.ubicacion.lowercase().contains(texto)

            val coincidePiso =
                state.pisoSeleccionado == "Todos" ||
                        tarea.piso.equals(state.pisoSeleccionado, ignoreCase = true)

            coincideTexto && coincidePiso
        }

        _uiState.postValue(
            state.copy(
                cargando = false,
                tareas = filtradas,
                error = null
            )
        )
    }

    fun cambiarModo(modo: ModoMuro) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(modoMuro = modo)
        cargarTareas()
    }

    fun actualizarBusqueda(texto: String) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(textoBusqueda = texto)
        aplicarFiltrosLocalesYPublicar()
    }

    fun cambiarPiso(piso: String) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(pisoSeleccionado = piso)
        aplicarFiltrosLocalesYPublicar()
    }

    fun cambiarSupervisor(usernameSupervisor: String?) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(filtroSupervisor = usernameSupervisor)
        cargarTareas()
    }

    fun rechazarTarea(tarea: Tarea, onResultado: (Boolean, String?) -> Unit) {
        if (tarea.id.isEmpty()) return

        tareaRepository.eliminarTarea(tarea.id) { result ->
            result.onSuccess {
                onResultado(true, null)
            }.onFailure { e ->
                onResultado(false, e.message)
            }
        }
    }

    fun asignarTarea(
        tarea: Tarea,
        usernameSupervisor: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        if (tarea.id.isEmpty()) return

        tareaRepository.asignarTarea(tarea.id, usernameSupervisor) { result ->
            result.onSuccess { onResultado(true, null) }
                .onFailure { e -> onResultado(false, e.message) }
        }
    }

    fun guardarRespuesta(
        tarea: Tarea,
        localPhotoPath: String,
        comentario: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        if (tarea.id.isEmpty()) return

        tareaRepository.guardarRespuesta(tarea.id, localPhotoPath, comentario) { result ->
            result.onSuccess { onResultado(true, null) }
                .onFailure { e -> onResultado(false, e.message) }
        }
    }

    fun editarTarea(
        tarea: Tarea,
        nuevaDescripcion: String,
        nuevaUbicacion: String,
        nuevoPiso: String,
        onResultado: (Boolean, String?) -> Unit
    ) {
        if (tarea.id.isEmpty()) {
            onResultado(false, "ID de tarea vacío")
            return
        }

        tareaRepository.actualizarCamposTarea(
            tarea.id,
            nuevaDescripcion,
            nuevaUbicacion,
            nuevoPiso
        ) { result ->
            result.onSuccess {
                onResultado(true, null)
            }.onFailure { e ->
                onResultado(false, e.message)
            }
        }
    }
    fun aplicarFiltroFechas(desde: Timestamp?, hasta: Timestamp?) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(filtroFechaDesde = desde, filtroFechaHasta = hasta)
        cargarTareas()
    }
// TareasViewModel.kt

    fun limpiarTodosLosFiltros() {
        val state = _uiState.value ?: return

        _uiState.value = state.copy(
            textoBusqueda = "",
            pisoSeleccionado = "Todos",
            filtroSupervisor = null,
            filtroFechaDesde = null,
            filtroFechaHasta = null
        )

        // Recargamos la lista limpia
        cargarTareas()
    }

    fun eliminarTarea(tarea: Tarea, callback: (Boolean, String?) -> Unit) {
        if (tarea.id.isEmpty()) {
            callback(false, "ID de tarea inválido para eliminar.")
            return
        }

        // 1. Mostrar estado de carga (opcional)
        _uiState.value = _uiState.value?.copy(cargando = true)

        // 2. Llamar a la función del repositorio usando el callback de Result<Unit>
        tareaRepository.eliminarTarea(tarea.id) { result ->
            // 3. Evaluar el resultado del repositorio
            if (result.isSuccess) {
                // Si tiene éxito: forzar la recarga y notificar
                cargarTareas() // El ViewModel se encarga de actualizar la lista
                callback(true, null)
            } else {
                // Si falla: mostrar error y detener la carga
                val e = result.exceptionOrNull()
                val errorMessage = "Error al eliminar la tarea: ${e?.message}"
                _uiState.value = _uiState.value?.copy(error = errorMessage, cargando = false)
                callback(false, errorMessage)
            }
        }
    }

}

