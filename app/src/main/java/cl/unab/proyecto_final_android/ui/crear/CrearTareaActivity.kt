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
    private var rolUsuario: String = LoginActivity.Companion.ROL_CREAR

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

        rolUsuario = intent.getStringExtra(LoginActivity.Companion.EXTRA_ROL_USUARIO)
            ?: LoginActivity.Companion.ROL_CREAR

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
                            putExtra(LoginActivity.Companion.EXTRA_ROL_USUARIO, rolUsuario)
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
        for (piso in 6 downTo -6) {
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

        if (descripcion.isEmpty()) {
            binding.txtDescripcion.error = "Ingrese una descripción"
            return
        }

        if (ubicacion.isEmpty()) {
            binding.autoCompleteUbicacion.error = "Ingrese una ubicación"
            return
        }

        if (pisoSeleccionado == "Selecciona piso" || pisoSeleccionado.isEmpty()) {
            toast("Debe seleccionar un piso")
            return
        }

        val usuarioActual = auth.currentUser
        if (usuarioActual == null) {
            toast("Error: no hay usuario autenticado")
            return
        }

        mostrarCargando(true)

        val coleccion = firestore.collection("tareas")
        val nuevoDoc = coleccion.document()
        val tareaId = nuevoDoc.id

        // Si hay imagen, primero la subimos
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
                        creador = usuarioActual.email ?: usuarioActual.uid
                    )
                }
                .addOnFailureListener { e ->
                    mostrarCargando(false)
                    toast("Error al subir la imagen: ${e.message}")
                }
        } ?: run {
            // Sin imagen, igual guardamos la tarea
            guardarTareaEnFirestore(
                docRef = nuevoDoc,
                tareaId = tareaId,
                descripcion = descripcion,
                ubicacion = ubicacion,
                piso = pisoSeleccionado,
                fotoAntesUrl = "",
                creador = usuarioActual.email ?: usuarioActual.uid
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
            fechaCreacion = Timestamp.Companion.now(),
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