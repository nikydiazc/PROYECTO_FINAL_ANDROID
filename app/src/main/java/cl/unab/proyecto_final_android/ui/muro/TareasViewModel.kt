package cl.unab.proyecto_final_android.ui.muro

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.util.MuroConfigurator
import com.google.firebase.Timestamp

// --------- ESTADO DE LA UI DEL MURO ------------
data class MuroUiState(
    val tareas: List<Tarea> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val modoMuro: ModoMuro = ModoMuro.PENDIENTES,
    val filtroPiso: String = "Todos",
    val filtroSupervisor: String? = null,
    val filtroBusqueda: String = "",
    val filtroFechaDesde: Timestamp? = null,
    val filtroFechaHasta: Timestamp? = null
)

// ------------- VIEWMODEL ---------------
class TareasViewModel(
    private val tareaRepository: TareaRepository,
    private val rolUsuario: String,
    private val usernameActual: String
) : ViewModel() {

    private val _uiState = MutableLiveData(MuroUiState())
    val uiState: LiveData<MuroUiState> = _uiState

    private val esAdmin: Boolean
        get() = rolUsuario == LoginActivity.ROL_ADMIN

    private val esSupervisor: Boolean
        get() = MuroConfigurator.listaSupervisores.any { it.username == usernameActual }

    init {
        cargarTareas()
    }

    // ---------- CARGA PRINCIPAL CON FILTROS + ROLES -----------

    fun cargarTareas() {
        val estadoActual = _uiState.value ?: return

        // Mostrar progress
        _uiState.value = estadoActual.copy(cargando = true, error = null)

        // Parámetros base
        val modoMuro = estadoActual.modoMuro
        val piso = estadoActual.filtroPiso
        val textoBusqueda = estadoActual.filtroBusqueda
        val fechaDesde = estadoActual.filtroFechaDesde
        val fechaHasta = estadoActual.filtroFechaHasta

        // Filtro por asignado según rol y modo
        val filtroAsignado: String? = when {
            esAdmin -> {
                // Admin filtra por supervisor seleccionado en spinner
                estadoActual.filtroSupervisor
            }
            modoMuro == ModoMuro.ASIGNADAS && esSupervisor -> {
                // Supervisor en modo asignadas: solo ve las suyas
                usernameActual
            }
            else -> {
                // Otros roles: sin filtro asignadoA específico
                null
            }
        }

        tareaRepository.obtenerTareasFiltradas(
            modoMuro = modoMuro,
            piso = piso,
            asignadoA = filtroAsignado,
            textoBusqueda = textoBusqueda,
            fechaDesde = fechaDesde,
            fechaHasta = fechaHasta
        ) { tareas, error ->

            val nuevoEstado = _uiState.value ?: MuroUiState()

            if (error != null) {
                _uiState.postValue(
                    nuevoEstado.copy(
                        cargando = false,
                        error = error,
                        tareas = emptyList()
                    )
                )
            } else {
                _uiState.postValue(
                    nuevoEstado.copy(
                        cargando = false,
                        error = null,
                        tareas = tareas
                    )
                )
            }
        }
    }

    // ---------- CAMBIOS DE FILTRO / MODO -------------

    fun cambiarModo(modo: ModoMuro) {
        val estado = _uiState.value ?: return
        _uiState.value = estado.copy(modoMuro = modo)
        cargarTareas()
    }

    fun cambiarPiso(nuevoPiso: String) {
        val estado = _uiState.value ?: return
        _uiState.value = estado.copy(filtroPiso = nuevoPiso)
        cargarTareas()
    }

    fun cambiarSupervisor(usernameSupervisor: String?) {
        val estado = _uiState.value ?: return
        _uiState.value = estado.copy(filtroSupervisor = usernameSupervisor)
        cargarTareas()
    }

    fun actualizarBusqueda(texto: String) {
        val estado = _uiState.value ?: return
        _uiState.value = estado.copy(filtroBusqueda = texto)
        cargarTareas()
    }

    fun actualizarRangoFechas(desde: Timestamp?, hasta: Timestamp?) {
        aplicarFiltroFechas(desde, hasta)
    }

    fun aplicarFiltroFechas(desde: Timestamp?, hasta: Timestamp?) {
        val estado = _uiState.value ?: return
        _uiState.value = estado.copy(
            filtroFechaDesde = desde,
            filtroFechaHasta = hasta
        )
        cargarTareas()
    }


    fun limpiarTodosLosFiltros() {
        // Dejamos el modo de muro como está, solo limpiamos filtros
        val estado = _uiState.value ?: MuroUiState()
        _uiState.value = estado.copy(
            filtroPiso = "Todos",
            filtroSupervisor = null,
            filtroBusqueda = "",
            filtroFechaDesde = null,
            filtroFechaHasta = null
        )
        cargarTareas()
    }


    // ---------- ACCIONES SOBRE TAREAS (REPO) -------------

    fun subirFotoDeRespuesta(
        tarea: Tarea,
        uriFoto: Uri,
        callback: (Boolean, String?) -> Unit
    ) {
        tareaRepository.subirFotoRespuesta(tarea, uriFoto) { ok, error ->
            if (ok) {
                cargarTareas()
                callback(true, null)
            } else {
                callback(false, error)
            }
        }
    }

    fun asignarTarea(
        tarea: Tarea,
        usernameSupervisor: String,
        callback: (Boolean, String?) -> Unit
    ) {
        tareaRepository.asignarTarea(tarea, usernameSupervisor) { ok, error ->
            if (ok) {
                cargarTareas()
                callback(true, null)
            } else {
                callback(false, error)
            }
        }
    }

    fun rechazarTarea(
        tarea: Tarea,
        callback: (Boolean, String?) -> Unit
    ) {
        tareaRepository.rechazarTarea(tarea) { ok, error ->
            if (ok) {
                cargarTareas()
                callback(true, null)
            } else {
                callback(false, error)
            }
        }
    }

    fun eliminarTarea(
        tarea: Tarea,
        callback: (Boolean, String?) -> Unit
    ) {
        tareaRepository.eliminarTarea(tarea) { ok, error ->
            if (ok) {
                cargarTareas()
                callback(true, null)
            } else {
                callback(false, error)
            }
        }
    }

    // 👉 NUEVO: actualizar tarea (editar descripción, ubicación y piso)
    fun actualizarTarea(
        tarea: Tarea,
        nuevaDescripcion: String,
        nuevaUbicacion: String,
        nuevoPiso: String,
        callback: (Boolean, String?) -> Unit
    ) {
        tareaRepository.actualizarTarea(
            tarea = tarea,
            nuevaDescripcion = nuevaDescripcion,
            nuevaUbicacion = nuevaUbicacion,
            nuevoPiso = nuevoPiso
        ) { ok, error ->
            if (ok) {
                cargarTareas()
                callback(true, null)
            } else {
                callback(false, error)
            }
        }
    }
}
