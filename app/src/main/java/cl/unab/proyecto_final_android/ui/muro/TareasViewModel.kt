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

// --------- ESTRUCTURA DE ESTADO DE LA UI ------------
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

// ------------- VIEWMODEL ---------------
class TareasViewModel(
    private val tareaRepository: TareaRepository,
    // Eliminamos esAdmin, usaremos solo el rol
    private val rolUsuario: String,
    private val usernameActual: String
) : ViewModel() {

    private val _uiState = MutableLiveData(MuroUiState())
    val uiState: LiveData<MuroUiState> = _uiState

    // Inicialización: Cargar las tareas la primera vez
    init {
        cargarTareas()
    }

    // ---------- LÓGICA DE CARGA Y FILTROS (ACTUALIZADA CON PERMISOS) -------

    fun cargarTareas() {
        val estadoActual = _uiState.value ?: return

        // 1. ANTES: MUESTRA EL PROGRESS BAR
        _uiState.value = estadoActual.copy(cargando = true, error = null)

        // 2. DETERMINAR LOS PARÁMETROS DE FILTRADO POR ROL

        // El repositorio necesita saber a quién debe buscar tareas asignadas.
        var filtroAsignadoRepo: String? = null
        var modoMuroRepo = estadoActual.modoMuro

        val esSupervisor = MuroConfigurator.listaSupervisores.any { it.username == usernameActual }

        when (rolUsuario) {

            LoginActivity.ROL_ADMIN -> {
                // El Admin puede ver todo, el filtro de supervisor se aplica desde el spinner (estadoActual.filtroSupervisor)
                filtroAsignadoRepo = estadoActual.filtroSupervisor
            }

            // Los usuarios 'realizar_tarea' y 'crear_tarea' no tienen la opción de filtrar por supervisor
            // y ven las tareas asignadas a todos los realizadores/supervisores.
            // Los supervisores específicos (delfina.cabello) también caen aquí,
            // pero si están en modo ASIGNADAS, solo ven las suyas.

            else -> {
                // Si el modo es ASIGNADAS, restringimos la vista.
                if (estadoActual.modoMuro == ModoMuro.ASIGNADAS) {

                    if (esSupervisor) {
                        // Supervisor: Solo ve tareas asignadas a sí mismo.
                        filtroAsignadoRepo = usernameActual
                    }
                    // Nota: Los roles como "crear_tarea" o "realizar_tarea" verán el muro asignado sin
                    // un filtro específico si no son supervisores. El `TareaRepository` deberá manejar
                    // qué tareas son consideradas 'asignadas a nadie' vs 'asignadas a un grupo' si es necesario.

                    // Aquí, asumimos que si no es supervisor ni admin, y está en modo ASIGNADAS,
                    // verá las tareas asignadas a cualquier usuario general (incluido él mismo si usa el rol general).
                    // Pero para el caso de supervisor específico, SOBRESCRIBIMOS el filtro de asignación.
                }

                // Si el modo es PENDIENTES, no se aplica ningún filtro de asignación.
            }
        }

        // Si el modo de muro es PENDIENTES o REALIZADAS, aseguramos que el filtro de asignación sea nulo,
        // ya que la consulta debe ser general.
        if (estadoActual.modoMuro == ModoMuro.PENDIENTES || estadoActual.modoMuro == ModoMuro.REALIZADAS) {
            filtroAsignadoRepo = null
        }


        // 3. Llama al Repositorio con los parámetros definidos
        tareaRepository.getTareas(
            modo = modoMuroRepo,
            piso = estadoActual.filtroPiso,
            busqueda = estadoActual.filtroBusqueda,
            // Usamos el filtro específico para la consulta
            asignadaA = filtroAsignadoRepo,
            fechaDesde = estadoActual.filtroFechaDesde,
            fechaHasta = estadoActual.filtroFechaHasta,
            callback = { tareas, error ->

                // 4. DESPUÉS: Manejo de Respuesta
                val nuevoEstado = if (tareas != null) {
                    // ÉXITO: Actualiza la lista
                    estadoActual.copy(tareas = tareas, error = null)
                } else {
                    // FALLO: Almacena el error
                    val errorMessage = "Error al cargar tareas: ${error ?: "Error desconocido"}"
                    estadoActual.copy(tareas = emptyList(), error = errorMessage)
                }

                // 5. OCULTAR EL PROGRESS BAR (cargando = false)
                _uiState.value = nuevoEstado.copy(cargando = false)
            }
        )
    }

    // --------------- RESPONDER TAREA ----

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

    // ----------------- LÓGICA DE FILTROS Y ESTADO ---------

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
        // Este filtro solo debe ser usado por el Admin (a través del spinner)
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

    // ------------------ ACCIONES CRUD ------------------------------


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