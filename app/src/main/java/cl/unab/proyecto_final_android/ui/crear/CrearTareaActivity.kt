package cl.unab.proyecto_final_android.ui.crear

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.databinding.ActivityCrearTareaBinding
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class CrearTareaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearTareaBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imagenUri: Uri? = null
    private var rolUsuario: String = LoginActivity.ROL_CREAR

    // Selector de imagen de galería
    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagenUri = uri
                binding.btnImgAgregarFotografias.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearTareaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FORZAR ELIMINACIÓN DEL TINT (los iconos de la barra quedaban de un solo color)
        binding.bottomNav.itemIconTintList = null

        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_CREAR

        configurarBottomNav()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        configurarSpinnerPiso()
        configurarEventos()
    }

    private fun configurarBottomNav() {
        val bottomNav = binding.bottomNav

        bottomNav.selectedItemId = R.id.nav_crear_tarea // esta es la vista actual

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> {
                    true // ya estamos aquí
                }
                R.id.nav_muro_tareas -> {
                    startActivity(
                        Intent(this, MuroTareasActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                        }
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun configurarSpinnerPiso() {
        val pisos = mutableListOf("Selecciona piso")

        // CORRECCIÓN: Iterar del 6 al 1 y del -1 al -6 (excluyendo el 0)
        for (piso in 6 downTo 1) {
            pisos.add(piso.toString())
        }
        for (piso in -1 downTo -6) {
            pisos.add(piso.toString())
        }

        val adapterPisos = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            pisos
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spPiso.adapter = adapterPisos
    }

    private fun configurarEventos() {
        binding.btnImgAgregarFotografias.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        binding.btnCrearSolicitud.setOnClickListener {
            crearTarea()
        }
    }
    private fun crearTarea() {
        val descripcion = binding.txtDescripcion.text.toString().trim()
        val ubicacion = binding.autoCompleteUbicacion.text.toString().trim()
        val pisoSeleccionado = binding.spPiso.selectedItem?.toString() ?: ""

        // 1. VALIDACIÓN DE DESCRIPCIÓN
        if (descripcion.isEmpty()) {
            binding.txtDescripcion.error = "Debe ingresar una descripción"
            return
        }

        // 2. VALIDACIÓN DE UBICACIÓN
        if (ubicacion.isEmpty()) {
            binding.autoCompleteUbicacion.error = "Debe ingresar una ubicación"
            return
        }

        // 3. VALIDACIÓN DE PISO
        if (pisoSeleccionado == "Selecciona piso" || pisoSeleccionado.isEmpty()) {
            toast("Debe seleccionar un piso")
            return
        }

        // 4. VALIDACIÓN DE FOTOGRAFÍA (NUEVO REQUERIMIENTO)
        if (imagenUri == null) {
            toast("Debe agregar una fotografía para ingresar la solicitud.")
            return
        }

        // --- Bloque de Autenticación (Se mantiene la corrección anterior) ---
        val usuarioActual = auth.currentUser
        val creador: String

        if (usuarioActual == null) {
            val usernameExtra = intent.getStringExtra(LoginActivity.EXTRA_USERNAME)
            if (usernameExtra.isNullOrEmpty()) {
                toast("Error: No se pudo obtener el usuario de la sesión.")
                return
            }
            creador = usernameExtra
        } else {
            creador = usuarioActual.email ?: usuarioActual.uid
        }
        // --- Fin Bloque de Autenticación ---

        mostrarCargando(true)

        val coleccion = firestore.collection("tareas")
        val nuevoDoc = coleccion.document()
        val tareaId = nuevoDoc.id

        // Continuamos con el proceso de subida de imagen, ahora 'imagenUri' no es nulo
        imagenUri?.let { uri ->
            val referenciaImagen = storage.reference
                .child("tareas")
                .child("antes")
                .child("$tareaId.jpg")

            referenciaImagen.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    referenciaImagen.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    val fotoAntesUrl = downloadUri.toString()
                    guardarTareaEnFirestore(
                        docRef = nuevoDoc,
                        tareaId = tareaId,
                        descripcion = descripcion,
                        ubicacion = ubicacion,
                        piso = pisoSeleccionado,
                        fotoAntesUrl = fotoAntesUrl,
                        creador = creador
                    )
                }
                .addOnFailureListener { e ->
                    mostrarCargando(false)
                    toast("Error al subir la imagen: ${e.message}")
                }
        } ?: run {
            // Este bloque solo se ejecutaría si 'imagenUri' fuera nulo,
            // pero el código de validación anterior ya lo impide.
            // Por seguridad, si ocurriera, se usa la versión sin foto.
            guardarTareaEnFirestore(
                docRef = nuevoDoc,
                tareaId = tareaId,
                descripcion = descripcion,
                ubicacion = ubicacion,
                piso = pisoSeleccionado,
                fotoAntesUrl = "",
                creador = creador
            )
        }
    }
    private fun guardarTareaEnFirestore(
        docRef: DocumentReference,
        tareaId: String,
        descripcion: String,
        ubicacion: String,
        piso: String,
        fotoAntesUrl: String,
        creador: String
    ) {
        val tarea = Tarea(
            id = tareaId,
            descripcion = descripcion,
            ubicacion = ubicacion,
            piso = piso,
            fotoAntesUrl = fotoAntesUrl,
            fotoDespuesUrl = "",
            estado = "Pendiente",
            // Se usa el Companion para llamar a now()
            fechaCreacion = Timestamp.now(),
            fechaRespuesta = null,
            creadaPor = creador,
            asignadaA = "",
            comentarioRespuesta = ""
        )

        docRef.set(tarea)
            .addOnSuccessListener {
                mostrarCargando(false)
                mostrarDialogoDespuesDeCrear()
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                toast("Error al guardar la tarea: ${e.message}")
            }
    }

    private fun mostrarDialogoDespuesDeCrear() {
        AlertDialog.Builder(this)
            .setTitle("Solicitud ingresada")
            .setMessage("La solicitud se ha ingresado correctamente.\n\n¿Desea ingresar otro requerimiento?")
            .setPositiveButton("Sí") { _, _ ->
                limpiarFormulario()
            }
            .setNegativeButton("No") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun limpiarFormulario() {
        binding.txtDescripcion.setText("")
        binding.autoCompleteUbicacion.setText("")
        binding.spPiso.setSelection(0)
        imagenUri = null
        binding.btnImgAgregarFotografias.setImageResource(R.drawable.camera_icon)
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBarCarga.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnCrearSolicitud.isEnabled = !mostrar
        binding.btnImgAgregarFotografias.isEnabled = !mostrar
    }

    private fun toast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}