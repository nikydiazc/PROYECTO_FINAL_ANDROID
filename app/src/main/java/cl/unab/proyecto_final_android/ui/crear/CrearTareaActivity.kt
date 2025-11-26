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
    private var username: String = ""

    private val TEXTO_PISO_PLACEHOLDER = "Selecciona piso"

    // Lanzador para seleccionar imagen
    private val seleccionarImagenLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagenUri = uri
                binding.btnAgregarFoto.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearTareaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.itemIconTintList = null

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_CREAR

        username = intent.getStringExtra(LoginActivity.EXTRA_USERNAME)
            ?: auth.currentUser?.email
                    ?: ""

        configurarBottomNav()
        configurarSpinnerPiso()
        configurarEventos()
    }

    private fun configurarBottomNav() {
        val bottomNav = binding.bottomNav
        bottomNav.selectedItemId = R.id.nav_crear_tarea

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> true
                R.id.nav_muro_tareas -> {
                    if (rolUsuario == LoginActivity.ROL_CREAR) {
                        toast("No tienes permisos para ver el muro de tareas.")
                        false
                    } else {
                        startActivity(
                            Intent(this, MuroTareasActivity::class.java).apply {
                                putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                                putExtra(LoginActivity.EXTRA_USERNAME, username)
                            }
                        )
                        true
                    }
                }
                else -> false
            }
        }
    }

    private fun configurarSpinnerPiso() {
        val pisos = mutableListOf(TEXTO_PISO_PLACEHOLDER)

        for (piso in 6 downTo 1) pisos.add(piso.toString())
        for (piso in -1 downTo -6) pisos.add(piso.toString())

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
        binding.btnAgregarFoto.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        binding.btnCrearSolicitud.setOnClickListener {
            crearTarea()
        }
    }

    private fun crearTarea() {
        val descripcion = binding.etDescripcion.text.toString().trim()
        val ubicacion = binding.actvUbicacion.text.toString().trim()
        val pisoSeleccionado = binding.spPiso.selectedItem?.toString() ?: ""

        if (descripcion.isEmpty()) {
            binding.etDescripcion.error = "Debe ingresar una descripción"
            return
        }

        if (ubicacion.isEmpty()) {
            binding.actvUbicacion.error = "Debe ingresar una ubicación"
            return
        }

        if (pisoSeleccionado == TEXTO_PISO_PLACEHOLDER) {
            toast("Debe seleccionar un piso")
            return
        }

        if (imagenUri == null) {
            toast("Debe agregar una fotografía para ingresar la solicitud.")
            return
        }

        val creador = username.ifEmpty {
            auth.currentUser?.email ?: auth.currentUser?.uid ?: "desconocido"
        }

        mostrarCargando(true)

        val coleccion = firestore.collection("tareas")
        val nuevoDoc = coleccion.document()
        val tareaId = nuevoDoc.id

        val referenciaImagen = storage.reference
            .child("tareas")
            .child("antes")
            .child("$tareaId.jpg")

        referenciaImagen.putFile(imagenUri!!)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                referenciaImagen.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                guardarTareaEnFirestore(
                    docRef = nuevoDoc,
                    tareaId = tareaId,
                    descripcion = descripcion,
                    ubicacion = ubicacion,
                    piso = pisoSeleccionado,
                    fotoAntesUrl = downloadUri.toString(),
                    creador = creador
                )
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                toast("Error al subir imagen: ${e.message}")
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
            descripcion = descripcion,
            ubicacion = ubicacion,
            piso = piso,
            fotoAntesUrl = fotoAntesUrl,
            creadaPor = creador
        )

        docRef.set(tarea)
            .addOnSuccessListener {
                mostrarCargando(false)
                mostrarDialogoDespuesDeCrear()
            }
            .addOnFailureListener { e ->
                mostrarCargando(false)
                toast("Error al guardar tarea: ${e.message}")
            }
    }

    private fun mostrarDialogoDespuesDeCrear() {
        AlertDialog.Builder(this)
            .setTitle("Solicitud ingresada")
            .setMessage("La solicitud se ha ingresado correctamente.\n\n¿Desea ingresar otra?")
            .setPositiveButton("Sí") { _, _ -> limpiarFormulario() }
            .setNegativeButton("No") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun limpiarFormulario() {
        binding.etDescripcion.setText("")
        binding.actvUbicacion.setText("")
        binding.spPiso.setSelection(0)
        binding.btnAgregarFoto.setImageResource(R.drawable.camera_icon)
        imagenUri = null
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBarCarga.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnCrearSolicitud.isEnabled = !mostrar
        binding.btnAgregarFoto.isEnabled = !mostrar
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
