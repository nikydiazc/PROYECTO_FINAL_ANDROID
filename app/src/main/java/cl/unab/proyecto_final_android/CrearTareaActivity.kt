package cl.unab.proyecto_final_android

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CrearTareaActivity : AppCompatActivity() {

    // Firestore
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private lateinit var btnImgAgregarFotografias: ImageButton
    private lateinit var btnIngresar: Button
    private lateinit var autoUbicacion: AutoCompleteTextView
    private lateinit var autoPiso: AutoCompleteTextView
    private lateinit var edtDescripcion: EditText
    private lateinit var progressBarCarga: ProgressBar

    private var imageUri: Uri? = null

    // Galería
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            btnImgAgregarFotografias.setImageURI(it)
        }
    }

    // Cámara (recibe Bitmap)
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val uri = saveBitmapToCacheAndGetUri(it)
            imageUri = uri
            btnImgAgregarFotografias.setImageBitmap(it)
        }
    }

    // Pedir permiso de cámara
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Si acepta, abrimos cámara
            takePhotoLauncher.launch(null)
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate que el layout existe con este nombre
        setContentView(R.layout.activity_crear_tarea)

        btnImgAgregarFotografias = findViewById(R.id.btnImgAgregarFotografias)
        btnIngresar = findViewById(R.id.btnIngresar)
        autoUbicacion = findViewById(R.id.autoCompleteUbicacion)
        autoPiso = findViewById(R.id.autoCompleteUbicacion2)
        edtDescripcion = findViewById(R.id.txtDescripción)
        progressBarCarga = findViewById(R.id.progressBarCarga)

        btnImgAgregarFotografias.setOnClickListener {
            showPhotoPickerDialog()
        }

        btnIngresar.setOnClickListener {
            submitTarea()
        }
    }

    private fun showPhotoPickerDialog() {
        val options = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Galería
                        pickImageLauncher.launch("image/*")
                    }
                    1 -> {
                        // Cámara: revisar/solicitar permiso
                        val granted = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) {
                            takePhotoLauncher.launch(null)
                        } else {
                            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }
            .show()
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBarCarga.visibility = View.VISIBLE
            btnIngresar.isEnabled = false
            btnImgAgregarFotografias.isEnabled = false
        } else {
            progressBarCarga.visibility = View.GONE
            btnIngresar.isEnabled = true
            btnImgAgregarFotografias.isEnabled = true
        }
    }

    private fun submitTarea() {
        val descripcion = edtDescripcion.text.toString().trim()
        val ubicacion = autoUbicacion.text.toString().trim()
        val piso = autoPiso.text.toString().trim()

        if (descripcion.isBlank() || ubicacion.isBlank() || piso.isBlank()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "Seleccione una foto", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("tareas/${UUID.randomUUID()}.jpg")

        fileRef.putFile(imageUri!!)
            .addOnSuccessListener {
                fileRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val imagenUrl = uri.toString()
                        guardarTareaEnFirestore(descripcion, ubicacion, piso, imagenUrl)
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        Toast.makeText(
                            this,
                            "Error al obtener URL de imagen: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "Error al subir imagen: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun guardarTareaEnFirestore(
        descripcion: String,
        ubicacion: String,
        piso: String,
        imagenUrl: String
    ) {
        val tareaData = hashMapOf(
            "descripcion" to descripcion,
            "ubicacion" to ubicacion,
            "piso" to piso,
            "imagenUrl" to imagenUrl,
            "estado" to "Pendiente",
            "fechaCreacion" to Timestamp.now()
        )

        db.collection("tareas")
            .add(tareaData)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Tarea creada exitosamente", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MurosTareasActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "Error al guardar tarea: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveBitmapToCacheAndGetUri(bitmap: Bitmap): Uri {
        val cacheFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(cacheFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.flush()
        fos.close()

        val authority = "${packageName}.provider"
        return FileProvider.getUriForFile(
            this,
            authority,
            cacheFile
        )
    }
}
