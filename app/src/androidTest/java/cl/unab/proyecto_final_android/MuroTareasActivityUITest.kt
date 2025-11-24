package cl.unab.proyecto_final_android.ui.muro

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MuroTareasActivityUITest {

    // 1. DEFINICIÓN DE INTENTS A NIVEL DE CLASE
    private val adminIntent: Intent = Intent(ApplicationProvider.getApplicationContext(), MuroTareasActivity::class.java).apply {
        putExtra(LoginActivity.EXTRA_ROL_USUARIO, LoginActivity.ROL_ADMIN)
        putExtra(LoginActivity.EXTRA_USERNAME, "administrador")
    }

    private val realizadorIntent: Intent = Intent(ApplicationProvider.getApplicationContext(), MuroTareasActivity::class.java).apply {
        putExtra(LoginActivity.EXTRA_ROL_USUARIO, LoginActivity.ROL_REALIZAR)
        putExtra(LoginActivity.EXTRA_USERNAME, "usuario_normal")
    }

    @get:Rule
    val activityRule = ActivityScenarioRule<MuroTareasActivity>(adminIntent)

    // --- Tests para el rol de ADMINISTRADOR ---
    @Test
    fun admin_botonesDeAccionEnPrimerItem_debenSerVisibles() {
        onView(withId(R.id.rvTareas))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<TareaAdapter.TareaViewHolder>(
                    0,
                    object : androidx.test.espresso.ViewAction {
                        override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
                        override fun getDescription() = "Verificar botones Admin"
                        override fun perform(uiController: androidx.test.espresso.UiController?, view: View?) {
                            // Usamos ViewAssertions.matches(...).check(view, null) para verificar visibilidad
                            view?.findViewById<View>(R.id.btnEditarTarea)?.let {
                                ViewAssertions.matches(isDisplayed()).check(it, null)
                            }
                            view?.findViewById<View>(R.id.btnEliminarTarea)?.let {
                                ViewAssertions.matches(isDisplayed()).check(it, null)
                            }
                        }
                    }
                )
            )
    }

    @Test
    fun realizador_botonesDeEdicion_debenEstarOcultosEnTareaNoAsignada() {
        activityRule.scenario.recreate()

        onView(withId(R.id.rvTareas))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<TareaAdapter.TareaViewHolder>(
                    0,
                    object : androidx.test.espresso.ViewAction {
                        override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
                        override fun getDescription() = "Verificar botones de Realizador"
                        override fun perform(uiController: androidx.test.espresso.UiController?, view: View?) {
                            // btnEditarTarea debe estar oculto (GONE)
                            view?.findViewById<View>(R.id.btnEditarTarea)?.let {
                                ViewAssertions.matches(withEffectiveVisibility(Visibility.GONE)).check(it, null)
                            }
                            // btnEliminarTarea debe estar oculto (GONE)
                            view?.findViewById<View>(R.id.btnEliminarTarea)?.let {
                                ViewAssertions.matches(withEffectiveVisibility(Visibility.GONE)).check(it, null)
                            }
                            // btnResponderFoto debe estar visible si aplica la lógica de la tarea
                            view?.findViewById<View>(R.id.btnResponderFoto)?.let {
                                ViewAssertions.matches(isDisplayed()).check(it, null)
                            }

                            // Ejemplo usando assertEquals para verificar visibilidad nativa (opcional):
                            // val btnResponder = view?.findViewById<View>(R.id.btnResponderFoto)
                            // assertEquals("El botón Responder debe estar visible", View.VISIBLE, btnResponder?.visibility)
                        }
                    }
                )
            )
    }
}