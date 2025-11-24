package cl.unab.proyecto_final_android.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.unab.proyecto_final_android.ui.crear.CrearTareaActivity
import cl.unab.proyecto_final_android.databinding.ActivityLoginBinding
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import com.google.firebase.auth.FirebaseAuth


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

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
        //Usar el ID correcto del botón del XML
        binding.btnIngresar.setOnClickListener {
            hacerLogin()
        }
    }

    private fun hacerLogin() {
        val usuarioIngresado = binding.etCorreo.text.toString().trim()
        val contrasenaIngresada = binding.etContrasena.text.toString().trim()

        val auth = FirebaseAuth.getInstance()

        // Intentar autenticar con Firebase Auth
        auth.signInWithEmailAndPassword(usuarioIngresado, contrasenaIngresada)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Autenticación exitosa, ahora determinamos el rol para la lógica de la app
                    val rol = when {
                        // Mantenemos la lógica 'when' para determinar el rol de la aplicación
                        // basado en el email/contraseña simulados.
                        usuarioIngresado.equals("crear_tarea", ignoreCase = true) -> ROL_CREAR
                        usuarioIngresado.equals("administrador", ignoreCase = true) ||
                                usuarioIngresado.equals("administrador@miapp.com", ignoreCase = true) -> ROL_ADMIN
                        usuarioIngresado.equals("realizar_tarea", ignoreCase = true) -> ROL_REALIZAR
                        else -> ROL_REALIZAR
                    }

                    val esAdmin = (rol == ROL_ADMIN)

                    // Redirigir la navegación
                    when (rol) {
                        ROL_CREAR -> irACrearTarea(usuarioIngresado, rol, esAdmin)
                        ROL_ADMIN, ROL_REALIZAR -> irAMuroTareas(usuarioIngresado, rol, esAdmin)
                    }
                } else {
                    // Falló la autenticación en Firebase Auth
                    Toast.makeText(this, "Error de credenciales o de red. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                }
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
}