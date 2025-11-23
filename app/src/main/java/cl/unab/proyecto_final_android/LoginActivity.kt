package cl.unab.proyecto_final_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.unab.proyecto_final_android.databinding.ActivityLoginBinding

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
        binding.btnIngresar.setOnClickListener {
            hacerLogin()
        }
    }
    private fun hacerLogin() {
        val usuarioIngresado = binding.etCorreo.text.toString().trim()
        val contrasenaIngresada = binding.etContrasena.text.toString().trim()

        if (usuarioIngresado.isEmpty() || contrasenaIngresada.isEmpty()) {
            Toast.makeText(this, "Ingresa usuario y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) Determinar rol según usuario + contraseña
        val rol = when {
            usuarioIngresado.equals("crear_tarea", ignoreCase = true) &&
                    contrasenaIngresada == "Creartarea01" -> {
                ROL_CREAR
            }

            // 👇 AQUÍ: admin puede ser "administrador" o "administrador@miapp.com"
            (usuarioIngresado.equals("administrador", ignoreCase = true) ||
                    usuarioIngresado.equals("administrador@miapp.com", ignoreCase = true)) &&
                    contrasenaIngresada == "Administrador02" -> {
                ROL_ADMIN
            }

            usuarioIngresado.equals("realizar_tarea", ignoreCase = true) &&
                    contrasenaIngresada == "Realizartarea03" -> {
                ROL_REALIZAR
            }

            else -> {
                // Supervisores u otros → ROL_REALIZAR
                ROL_REALIZAR
            }
        }

        val esAdmin = (rol == ROL_ADMIN)

        // 2) Redirigir según el rol
        when (rol) {
            ROL_CREAR -> irACrearTarea(usuarioIngresado, rol, esAdmin)
            ROL_ADMIN,
            ROL_REALIZAR -> irAMuroTareas(usuarioIngresado, rol, esAdmin)
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
