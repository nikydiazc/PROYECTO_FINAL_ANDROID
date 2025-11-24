package cl.unab.proyecto_final_android

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class MuroTareasActivityCameraTest {

    private lateinit var tareaPendiente: Tarea
    private lateinit var tareaRealizada: Tarea
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Mock de datos de tarea
        tareaPendiente = Tarea(id = "T1", descripcion = "Limpiar", estado = "Pendiente", asignadaA = "usuario_normal")
        tareaRealizada = Tarea(id = "T2", descripcion = "Revisada", estado = "Realizada")
    }

    @Test
    fun intentarTomarFoto_tareaRealizada_debeMostrarToastYNoLanzarCamara() {
        val intent = Intent(context, MuroTareasActivity::class.java).apply {
            putExtra(LoginActivity.Companion.EXTRA_USERNAME, "usuario_normal")
        }
        val scenario = ActivityScenario.launch<MuroTareasActivity>(intent)

        scenario.onActivity { activity ->
            // Usamos un mock para verificar que no se llama a lanzarCamaraDespuesDePermiso
            val spyActivity = spy(activity)

            spyActivity.intentarTomarFoto(tareaRealizada)

            // Verificamos que la tareaEnRespuesta no se haya guardado
            val tareaEnRespuestaField = MuroTareasActivity::class.java.getDeclaredField("tareaEnRespuesta").apply { isAccessible = true }
            Assert.assertNull(tareaEnRespuestaField.get(spyActivity))

            // Verificamos que no se intentó lanzar la cámara (se saltó la lógica)
            verify(spyActivity, never()).lanzarCamaraDespuesDePermiso(any())

        }
    }

    @Test
    fun crearUriDeArchivoTemporal_debeRetornarUriValida() {
        val intent = Intent(context, MuroTareasActivity::class.java).apply {
            putExtra(LoginActivity.Companion.EXTRA_USERNAME, "usuario_normal")
        }
        val scenario = ActivityScenario.launch<MuroTareasActivity>(intent)

        scenario.onActivity { activity ->
            activity.intentarTomarFoto(tareaPendiente)

            val metodoCrearUri = MuroTareasActivity::class.java.getDeclaredMethod("crearUriDeArchivoTemporal").apply { isAccessible = true }
            val uri = metodoCrearUri.invoke(activity)

            // Verificamos que la URI no sea nula y contenga el FileProvider
            Assert.assertNotNull(uri)
            assertTrue(uri.toString().contains("${context.packageName}.fileprovider"))
        }
    }
}