package cl.unab.proyecto_final_android.ui.crear

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.databinding.ActivityCrearTareaBinding
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.ui.muro.MuroTareasActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrearTareaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearTareaBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var imagenUri: Uri? = null
    private var rolUsuario: String = LoginActivity.ROL_CREAR
    private var usernameActual: String = ""

    companion object {
        private const val STATE_DESCRIPCION = "state_descripcion"
        private const val STATE_UBICACION = "state_ubicacion"
        private const val STATE_PISO_INDEX = "state_piso_index"
        private const val STATE_IMAGEN_URI = "state_imagen_uri"
    }

    // --------- LAUNCHERS ---------

    // Galería
    private val seleccionarGaleriaLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imagenUri = uri
                binding.btnAgregarFoto.setImageURI(uri)
            }
        }

    // Cámara
    private val camaraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imagenUri != null) {
                binding.btnAgregarFoto.setImageURI(imagenUri)
            } else {
                imagenUri = null
                toast("Captura cancelada o fallida")
            }
        }

    // Permiso de cámara
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                lanzarCamara()
            } else {
                toast("El permiso de cámara es necesario para tomar la fotografía.")
            }
        }

    // --------- CICLO DE VIDA ---------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearTareaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Quitar tint automático de íconos del bottomNav
        binding.bottomNav.itemIconTintList = null

        // Datos que vienen del Login / Muro
        rolUsuario = intent.getStringExtra(LoginActivity.EXTRA_ROL_USUARIO)
            ?: LoginActivity.ROL_CREAR
        usernameActual = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: ""

        // Si viene una foto previa desde el muro (cámara/galería)
        intent.getStringExtra("EXTRA_FOTO_ANTES_URI")?.let { uriString ->
            val uri = Uri.parse(uriString)
            imagenUri = uri
            binding.btnAgregarFoto.setImageURI(uri)
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        configurarBottomNav()
        configurarSpinnerPiso()
        configurarEventos()

        restaurarEstadoFormulario(savedInstanceState)
    }

    // --------- UI / NAV ---------

    private fun configurarBottomNav() {
        val bottomNav = binding.bottomNav
        bottomNav.selectedItemId = R.id.nav_crear_tarea

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_crear_tarea -> true // ya estamos aquí

                R.id.nav_muro_tareas -> {
                    startActivity(
                        Intent(this, MuroTareasActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_ROL_USUARIO, rolUsuario)
                            putExtra(LoginActivity.EXTRA_USERNAME, usernameActual)
                        }
                    )
                    finish()
                    true
                }

                R.id.nav_usuario -> {
                    // Opcional: podrías mandar a una pantalla de perfil/cerrar sesión
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    private fun configurarSpinnerPiso() {
        val pisos = mutableListOf("Selecciona piso")
        for (p in 6 downTo 1) pisos.add("Piso $p")
        for (p in -1 downTo -6) pisos.add("Piso $p")

        val adapterPisos = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            pisos
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.spPiso.adapter = adapterPisos
    }

    private fun configurarEventos() {
        binding.btnAgregarFoto.setOnClickListener {
            mostrarDialogoSeleccionFoto()
        }

        binding.btnCrearSolicitud.setOnClickListener {
            crearTarea()
        }
    }

    // --------- CÁMARA / GALERÍA ---------

    private fun mostrarDialogoSeleccionFoto() {
        val opciones = arrayOf("Tomar foto", "Elegir de galería")
        AlertDialog.Builder(this)
            .setTitle("Agregar fotografía")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> solicitarPermisoCamara()
                    1 -> seleccionarGaleriaLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun solicitarPermisoCamara() {
        val permiso = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permiso)
            == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            lanzarCamara()
        } else {
            cameraPermissionLauncher.launch(permiso)
        }
    }

    private fun lanzarCamara() {
        val uri = crearUriDeArchivoTemporal() ?: run {
            toast("Error al preparar el archivo de imagen.")
            return
        }
        imagenUri = uri
        camaraLauncher.launch(uri)
    }

    private fun crearUriDeArchivoTemporal(): Uri? {
        return try {
            val storageDir: File? =
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (storageDir == null) null
            else {
                val timeStamp =
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File.createTempFile("ANTES_$timeStamp", ".jpg", storageDir)
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --------- LÓGICA CREAR TAREA ---------

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

        if (pisoSeleccionado == "Selecciona piso" || pisoSeleccionado.isEmpty()) {
            toast("Debe seleccionar un piso")
            return
        }

        if (imagenUri == null) {
            toast("Debe agregar una fotografía para ingresar la solicitud.")
            return
        }

        val usuarioActual = auth.currentUser
        val creador = when {
            usuarioActual != null -> usuarioActual.email ?: usuarioActual.uid
            !usernameActual.isNullOrEmpty() -> usernameActual
            else -> {
                toast("Error: No se pudo obtener el usuario de la sesión.")
                return
            }
        }

        mostrarCargando(true)

        val coleccion = firestore.collection("tareas")
        val nuevoDoc = coleccion.document()
        val tareaId = nuevoDoc.id

        imagenUri?.let { uri ->
            val refImagen = storage.reference
                .child("tareas")
                .child("antes")
                .child("$tareaId.jpg")

            refImagen.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    refImagen.downloadUrl
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
                    toast("Error al subir la imagen: ${e.message}")
                }
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
            creadaPor = creador
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
        binding.etDescripcion.setText("")
        binding.actvUbicacion.setText("")
        binding.spPiso.setSelection(0)
        imagenUri = null
        binding.btnAgregarFoto.setImageResource(R.drawable.camera_icon)
    }

    private fun mostrarCargando(mostrar: Boolean) {
        binding.progressBarCarga.visibility = if (mostrar) View.VISIBLE else View.GONE
        binding.btnCrearSolicitud.isEnabled = !mostrar
        binding.btnAgregarFoto.isEnabled = !mostrar
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_DESCRIPCION, binding.etDescripcion.text.toString())
        outState.putString(STATE_UBICACION, binding.actvUbicacion.text.toString())
        outState.putInt(STATE_PISO_INDEX, binding.spPiso.selectedItemPosition)
        outState.putString(STATE_IMAGEN_URI, imagenUri?.toString())
    }

    private fun restaurarEstadoFormulario(savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        // descripción
        val desc = savedInstanceState.getString(STATE_DESCRIPCION, "")
        binding.etDescripcion.setText(desc)

        // ubicación
        val ubi = savedInstanceState.getString(STATE_UBICACION, "")
        binding.actvUbicacion.setText(ubi)

        // piso
        val idxPiso = savedInstanceState.getInt(STATE_PISO_INDEX, 0)
        if (idxPiso in 0 until binding.spPiso.count) {
            binding.spPiso.setSelection(idxPiso)
        }

        // imagen
        val uriString = savedInstanceState.getString(STATE_IMAGEN_URI, null)
        if (!uriString.isNullOrEmpty()) {
            imagenUri = Uri.parse(uriString)
            binding.btnAgregarFoto.setImageURI(imagenUri)
        }

    }
}
