package cl.unab.proyecto_final_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cl.unab.proyecto_final_android.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        const val EXTRA_ROL_USUARIO = "ROL_USUARIO"
        const val ROL_CREAR = "CREAR"
        const val ROL_ADMIN = "ADMIN"
        const val ROL_REALIZAR = "REALIZAR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnIngresar.setOnClickListener {
            realizarLogin()
        }
    }

    private fun realizarLogin() {
        val usuarioInput = binding.etCorreo.text.toString().trim()
        val password = binding.etContrasena.text.toString().trim()

        if (usuarioInput.isEmpty()) {
            binding.etCorreo.error = "Ingrese usuario o correo"
            return
        }

        if (password.isEmpty()) {
            binding.etContrasena.error = "Ingrese la contraseña"
            return
        }

        // Si no tiene @, asumimos que es nombre de usuario (crear_tarea, administrador, etc.)
        val email = if (usuarioInput.contains("@")) {
            usuarioInput
        } else {
            "${usuarioInput}@miapp.com"
        }

        mostrarCargando(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { resultado ->
                val user = resultado.user
                if (user == null) {
                    mostrarCargando(false)
                    toast("Error al obtener el usuario")
                    return@addOnSuccessListener
                }

                val username = email.substringBefore("@").lowercase()

                val rol = when (username) {
                    "crear_tarea" -> ROL_CREAR
                    "administrador" -> ROL_ADMIN
                    "realizar_tarea" -> ROL_REALIZAR
                    else -> {
                        // Por ahora, cualquier otro usuario lo tratamos como REALIZAR
                        ROL_REALIZAR
                    }
                }

                irSegunRol(rol)
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                toast("Error de autenticación: ${e.message}")
            }
    }

    private fun irSegunRol(rol: String) {
        mostrarCargando(false)

        when (rol) {
            ROL_CREAR -> {
                // Solo puede crear tareas
                val intent = Intent(this, CrearTareaActivity::class.java).apply {
                    putExtra(EXTRA_ROL_USUARIO, rol)
                }
                startActivity(intent)
                finish()
            }

            ROL_ADMIN, ROL_REALIZAR -> {
                // Ambos van al muro de tareas, pero con distinto rol
                val intent = Intent(this, MuroTareasActivity::class.java).apply {
                    putExtra(EXTRA_ROL_USUARIO, rol)
                }
                startActivity(intent)
                finish()
            }

            else -> {
                toast("Usuario no reconocido")
            }
        }
    }

    private fun mostrarCargando(mostrar: Boolean) {

        binding.btnIngresar.isEnabled = !mostrar
    }

    private fun toast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}
