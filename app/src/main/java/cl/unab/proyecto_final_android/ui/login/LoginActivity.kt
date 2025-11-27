package cl.unab.proyecto_final_android.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.unab.proyecto_final_android.databinding.ActivityLoginBinding
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    companion object {
        const val EXTRA_ROL_USUARIO = "rolUsuario"
        const val EXTRA_USERNAME = "usernameUsuario"
        const val EXTRA_ES_ADMIN = "esAdmin"

        const val ROL_CREAR = "CREAR"
        const val ROL_ADMIN = "ADMIN"
        const val ROL_REALIZAR = "REALIZAR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarEventos()
    }
    private fun configurarEventos() {
        binding.btnIngresar.setOnClickListener { validarCampos() }
    }

    private fun validarCampos() {
        val correo = binding.etCorreo.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString().trim()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Debes ingresar tus credenciales", Toast.LENGTH_SHORT).show()
            return
        }

        hacerLogin(correo, contrasena)
    }

    private fun hacerLogin(correo: String, contrasena: String) {

        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val rol = determinarRolDesdeCorreo(correo)
                    val esAdmin = (rol == ROL_ADMIN)

                    when (rol) {
                        ROL_CREAR -> irACrearTarea(correo, rol, esAdmin)
                        ROL_ADMIN, ROL_REALIZAR -> irAMuroTareas(correo, rol, esAdmin)
                    }

                } else {
                    Toast.makeText(
                        this,
                        "Error de credenciales o de red. Intenta de nuevo.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * Determina el rol según el correo exacto del usuario.
     * Los correos deben coincidir con Firebase Authentication.
     */
    private fun obtenerRolUsuario(correo: String): String {
        val correoLower = correo.lowercase()

        return when (correoLower) {
            "crear_tarea@miapp.com" -> ROL_CREAR
            "administrador@miapp.com" -> ROL_ADMIN
            "realizar_tarea@miapp.com" -> ROL_REALIZAR
            // supervisores son equivalentes a realizar tarea
            "delfina.cabello@miapp.com",
            "rodrigo.reyes@miapp.com",
            "maria.caruajulca@miapp.com",
            "john.vilchez@miapp.com",
            "cristian.vergara@miapp.com",
            "enrique.mendez@miapp.com",
            "norma.marican@miapp.com",
            "libia.florez@miapp.com",
            "jorge.geisbuhler@miapp.com" -> ROL_REALIZAR
            else -> ROL_REALIZAR
        }
    }

    private fun irACrearTarea(username: String, rol: String, esAdmin: Boolean) {
        val intent = Intent(this, CrearTareaActivity::class.java).apply {
            putExtra(EXTRA_ROL_USUARIO, rol)
            putExtra(EXTRA_USERNAME, username)
            putExtra(EXTRA_ES_ADMIN, esAdmin)
        }
        startActivity(intent)
        finish()
    }

    private fun irAMuroTareas(username: String, rol: String, esAdmin: Boolean) {
        val intent = Intent(this, MuroTareasActivity::class.java).apply {
            putExtra(EXTRA_ROL_USUARIO, rol)
            putExtra(EXTRA_USERNAME, username)
            putExtra(EXTRA_ES_ADMIN, esAdmin)
        }
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if (user != null) {
            val correo = user.email ?: ""
            val rol = determinarRolDesdeCorreo(correo)
            val username = correo

            val esAdmin = (rol == ROL_ADMIN)

            when (rol) {
                ROL_CREAR -> irACrearTarea(username, rol, esAdmin)
                ROL_ADMIN, ROL_REALIZAR -> irAMuroTareas(username, rol, esAdmin)
            }
        }
    }

    private fun determinarRolDesdeCorreo(correo: String?): String {
        val correoLower = correo?.lowercase().orEmpty()
        return when (correoLower) {
            "crear_tarea@miapp.com" -> ROL_CREAR
            "administrador@miapp.com" -> ROL_ADMIN
            "realizar_tarea@miapp.com" -> ROL_REALIZAR

            // supervisores -> ROL_REALIZAR
            "delfina.cabello@miapp.com",
            "rodrigo.reyes@miapp.com",
            "maria.caruajulca@miapp.com",
            "john.vilchez@miapp.com",
            "cristian.vergara@miapp.com",
            "enrique.mendez@miapp.com",
            "norma.marican@miapp.com",
            "libia.florez@miapp.com",
            "jorge.geisbuhler@miapp.com" -> ROL_REALIZAR

            else -> ROL_REALIZAR
        }
    }




}
