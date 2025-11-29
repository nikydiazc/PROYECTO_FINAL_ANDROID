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

        // 1) Revisar si ya hay sesión guardada
        val prefs = getSharedPreferences("session_prefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            val rolGuardado = prefs.getString("rol", null)
            val usernameGuardado = prefs.getString("username", null)

            val intent = Intent(this, MuroTareasActivity::class.java).apply {
                putExtra(EXTRA_ROL_USUARIO, rolGuardado)
                putExtra(EXTRA_USERNAME, usernameGuardado)
                putExtra(EXTRA_ES_ADMIN, rolGuardado == ROL_ADMIN)
            }
            startActivity(intent)
            finish()
            return
        }

        // 2) Si no hay sesión, muestro el login normal
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnIngresar.setOnClickListener {
            validarCampos()
        }
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
        val auth = FirebaseAuth.getInstance()

        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val correoLower = correo.trim().lowercase()

                    // Determinar rol según el correo
                    val rol = getRolFromCorreo(correoLower)
                    val usernameCorto = extraerUsernameDesdeCorreo(correoLower)
                    val esAdmin = (rol == ROL_ADMIN)

                    // 👉 GUARDAR SESIÓN AQUÍ
                    guardarSesion(usernameCorto, rol)

                    // Navegar según el rol
                    when (rol) {
                        ROL_CREAR -> irACrearTarea(usernameCorto, rol, esAdmin)
                        ROL_ADMIN,
                        ROL_REALIZAR -> irAMuroTareas(usernameCorto, rol, esAdmin)
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

    // Guarda la sesión en SharedPreferences
    private fun guardarSesion(username: String, rol: String) {
        val prefs = getSharedPreferences("session_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("isLoggedIn", true)
            .putString("username", username)
            .putString("rol", rol)
            .apply()
    }

    // Mapea correos a roles
    private fun getRolFromCorreo(correoLower: String): String {
        return when (correoLower) {
            "crear_tarea@miapp.com" -> ROL_CREAR
            "administrador@miapp.com" -> ROL_ADMIN
            "realizar_tarea@miapp.com" -> ROL_REALIZAR

            // supervisores: mismos permisos que REALIZAR
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

    // "delfina.cabello@miapp.com" -> "delfina.cabello"
    private fun extraerUsernameDesdeCorreo(correoLower: String): String {
        return correoLower.substringBefore("@")
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
