package cl.unab.proyecto_final_android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrearTareaActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(CrearTareaActivity::class.java)

    /**
     * 1) Si se intenta crear la tarea sin descripción,
     * debe marcar error en et_descripcion.
     */
    @Test
    fun crearTarea_conDescripcionVacia_muestraErrorEnDescripcion() {
        // Click directo en "Ingresar solicitud"
        onView(withId(R.id.btn_crear_solicitud)).perform(click())

        // Verifica que el EditText de descripción tenga el error correcto
        onView(withId(R.id.et_descripcion))
            .check(matches(hasErrorText("Debe ingresar una descripción")))
    }

    /**
     * 2) Si se ingresa descripción pero la ubicación está vacía,
     * debe marcar error en actv_ubicacion.
     */
    @Test
    fun crearTarea_conUbicacionVacia_muestraErrorEnUbicacion() {
        // Escribimos una descripción válida
        onView(withId(R.id.et_descripcion))
            .perform(
                androidx.test.espresso.action.ViewActions.replaceText("Basurero lleno"),
                androidx.test.espresso.action.ViewActions.closeSoftKeyboard()
            )

        // Click en "Ingresar solicitud"
        onView(withId(R.id.btn_crear_solicitud)).perform(click())

        // Verifica que el campo de ubicación tenga el error correcto
        onView(withId(R.id.actv_ubicacion))
            .check(matches(hasErrorText("Debe ingresar una ubicación")))
    }

    /**
     * 3) Al iniciar la activity, el ProgressBar debe estar oculto
     * y el botón de crear solicitud visible.
     */
    @Test
    fun alIniciar_progressBarOculto_yBotonCrearVisible() {
        onView(withId(R.id.progressBar_carga))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(withId(R.id.btn_crear_solicitud))
            .check(matches(isDisplayed()))
    }

    /**
     * 4) El spinner de piso debe iniciar con "Selecciona piso".
     */
    @Test
    fun alIniciar_spinnerPiso_muestraSeleccionaPiso() {
        onView(withId(R.id.sp_piso))
            .check(matches(withSpinnerText(containsString("Selecciona piso"))))
    }
}
