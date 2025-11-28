package cl.unab.proyecto_final_android

import cl.unab.proyecto_final_android.ui.muro.TareaAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TareaAdapterTest {

    private lateinit var adapter: TareaAdapter

    @Before
    fun setUp() {
        adapter = TareaAdapter(
            tareas = emptyList(),
            rolUsuario = "CUALQUIERA", // No nos importa para estos tests
            usernameActual = "test",
            onResponderClick = {},
            onEditarClick = {},
            onEliminarClick = {}
        )
    }

    @Test
    fun itemCount_inicialEsCero() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun actualizarTareas_cambiaItemCount() {
        // Usamos instancias reales de Tarea (asumiendo que tiene constructor con valores por defecto)
        val tarea1 = Tarea()
        val tarea2 = Tarea()

        adapter.actualizarTareas(listOf(tarea1, tarea2))

        assertEquals(2, adapter.itemCount)
        assertEquals(tarea1, adapter.obtenerTareaEnPosicion(0))
        assertEquals(tarea2, adapter.obtenerTareaEnPosicion(1))
    }

    @Test
    fun obtenerTareaEnPosicion_fueraDeRangoDevuelveNull() {
        val tarea = Tarea()
        adapter.actualizarTareas(listOf(tarea))

        assertNull(adapter.obtenerTareaEnPosicion(-1))
        assertNull(adapter.obtenerTareaEnPosicion(1))
        assertNull(adapter.obtenerTareaEnPosicion(999))
    }
}
