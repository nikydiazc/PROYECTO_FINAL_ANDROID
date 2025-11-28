package cl.unab.proyecto_final_android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cl.unab.proyecto_final_android.data.ModoMuro
import cl.unab.proyecto_final_android.data.TareaRepository
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.ui.muro.TareasViewModel
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Calendar

class TareasViewModelTest {

    // Hace que LiveData funcione en tests de JVM
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Creamos un ViewModel con un repositorio "mock", pero SIN stubs
    private fun crearViewModelComoAdmin(): TareasViewModel {
        val repoMock = mock(TareaRepository::class.java)
        return TareasViewModel(
            tareaRepository = repoMock,
            rolUsuario = LoginActivity.ROL_ADMIN,
            usernameActual = "admin@miapp.com"
        )
    }

    @Test
    fun cambiarModo_actualizaModoEnUiState() {
        val viewModel = crearViewModelComoAdmin()

        // Aseguramos estado inicial
        val estadoInicial = viewModel.uiState.value!!
        assertEquals(ModoMuro.PENDIENTES, estadoInicial.modoMuro)

        // Act
        viewModel.cambiarModo(ModoMuro.REALIZADAS)

        // Assert
        val estadoFinal = viewModel.uiState.value!!
        assertEquals(ModoMuro.REALIZADAS, estadoFinal.modoMuro)
    }

    @Test
    fun limpiarTodosLosFiltros_reseteaFiltrosEnUiState() {
        val viewModel = crearViewModelComoAdmin()

        // Simulamos que el usuario aplicó filtros
        viewModel.cambiarPiso("Piso 3")
        viewModel.cambiarSupervisor("delfina")
        viewModel.actualizarBusqueda("baño")

        val calDesde = Calendar.getInstance().apply { set(2025, 0, 1, 0, 0, 0) }
        val calHasta = Calendar.getInstance().apply { set(2025, 0, 31, 23, 59, 59) }
        val tsDesde = Timestamp(calDesde.time)
        val tsHasta = Timestamp(calHasta.time)

        viewModel.aplicarFiltroFechas(tsDesde, tsHasta)

        // Verificamos que los filtros quedaron seteados
        val estadoConFiltros = viewModel.uiState.value!!
        assertEquals("Piso 3", estadoConFiltros.filtroPiso)
        assertEquals("delfina", estadoConFiltros.filtroSupervisor)
        assertEquals("baño", estadoConFiltros.filtroBusqueda)
        assertEquals(tsDesde, estadoConFiltros.filtroFechaDesde)
        assertEquals(tsHasta, estadoConFiltros.filtroFechaHasta)

        // Act: limpiar filtros
        viewModel.limpiarTodosLosFiltros()

        // Assert: filtros reseteados
        val estadoLimpio = viewModel.uiState.value!!
        assertEquals("Todos", estadoLimpio.filtroPiso)
        assertEquals(null, estadoLimpio.filtroSupervisor)
        assertEquals("", estadoLimpio.filtroBusqueda)
        assertEquals(null, estadoLimpio.filtroFechaDesde)
        assertEquals(null, estadoLimpio.filtroFechaHasta)

        // El modo de muro NO se toca al limpiar filtros
        assertEquals(estadoConFiltros.modoMuro, estadoLimpio.modoMuro)
    }

    @Test
    fun actualizarRangoFechas_actualizaFechasEnUiState() {
        val viewModel = crearViewModelComoAdmin()

        val calDesde = Calendar.getInstance().apply { set(2025, 1, 10, 0, 0, 0) }
        val calHasta = Calendar.getInstance().apply { set(2025, 1, 15, 23, 59, 59) }
        val tsDesde = Timestamp(calDesde.time)
        val tsHasta = Timestamp(calHasta.time)

        // Act
        viewModel.actualizarRangoFechas(tsDesde, tsHasta)

        // Assert
        val estado = viewModel.uiState.value!!
        assertEquals(tsDesde, estado.filtroFechaDesde)
        assertEquals(tsHasta, estado.filtroFechaHasta)
    }
}

