package cl.unab.proyecto_final_android.data

import android.net.Uri
import cl.unab.proyecto_final_android.Tarea
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class TareaRepository(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val COLLECTION_NAME = "tareas"

    // ----------- RESPONDER TAREA (CÁMARA/STORAGE) ---------

    fun subirFotoDeRespuesta(tarea: Tarea, fotoUri: Uri, callback: (String?, String?) -> Unit) {

        // 1. Referencia de Firebase Storage: unique name using ID and timestamp
        val storageRef = storage.reference.child("respuestas/${tarea.id}_${System.currentTimeMillis()}.jpg")

        // 2. Subir el archivo
        storageRef.putFile(fotoUri)
            .addOnSuccessListener { taskSnapshot ->
                // Subida exitosa, obtener URL de descarga
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    val fotoDespuesUrl = downloadUri.toString()

                    // 3. Actualizar Firestore: Cambiar estado, fecha y URL
                    val updates = mapOf(
                        "estado" to "Realizada",
                        "fechaRespuesta" to Timestamp.now(),
                        "fotoDespuesUrl" to fotoDespuesUrl
                    )

                    db.collection(COLLECTION_NAME).document(tarea.id)
                        .update(updates)
                        .addOnSuccessListener {
                            // Éxito total: Subida y actualización completadas
                            callback(fotoDespuesUrl, null)
                        }
                        .addOnFailureListener { firestoreException ->
                            // Fallo al actualizar Firestore
                            callback(null, "Fallo al actualizar Firestore: ${firestoreException.message}")
                        }
                }.addOnFailureListener { urlException ->
                    // Fallo al obtener la URL de descarga
                    callback(null, "Fallo al obtener URL: ${urlException.message}")
                }
            }
            .addOnFailureListener { storageException ->
                // Fallo al subir a Storage
                callback(null, "Fallo al subir foto a Storage: ${storageException.message}")
            }
    }


    // ------------- FILTROS Y LECTURA DE TAREAS ------------------
    fun getTareas(
        modo: ModoMuro,
        piso: String,
        busqueda: String,
        asignadaA: String?,
        fechaDesde: Timestamp?,
        fechaHasta: Timestamp?,
        callback: (List<Tarea>?, String?) -> Unit
    ) {
        var query: Query = db.collection(COLLECTION_NAME)

        // 1. Filtro por Modo/Estado
        query = when (modo) {
            ModoMuro.PENDIENTES -> query.whereEqualTo("estado", "Pendiente")
            ModoMuro.REALIZADAS -> query.whereEqualTo("estado", "Realizada")
            ModoMuro.ASIGNADAS -> query.whereEqualTo("estado", "Asignada")
        }

        // 2. Filtro por Asignación
        if (!asignadaA.isNullOrBlank()) {
            query = query.whereEqualTo("asignadaA", asignadaA)
        }

        // 3. Filtro por Piso
        if (piso != "Todos") {
            query = query.whereEqualTo("piso", piso)
        }

        // 4. Filtro por Fechas (en base al campo fechaCreacion)
        if (fechaDesde != null) {
            query = query.whereGreaterThanOrEqualTo("fechaCreacion", fechaDesde)
        }
        if (fechaHasta != null) {
            query = query.whereLessThanOrEqualTo("fechaCreacion", fechaHasta)
        }

        // 5. Ordenar
        query = query.orderBy("fechaCreacion", Query.Direction.DESCENDING)


        query.get()
            .addOnSuccessListener { result ->
                try {
                    var tareas = result.toObjects(Tarea::class.java)

                    // 6. Filtro Local por Búsqueda
                    if (busqueda.isNotBlank()) {
                        val termino = busqueda.lowercase()
                        tareas = tareas.filter { tarea ->
                            // Verifica si descripcion NO es nula antes de convertir a minúsculas y buscar.
                            val descripcionMatch = tarea.descripcion?.lowercase()?.contains(termino) ?: false
                            // Verifica si ubicacion NO es nula antes de convertir a minúsculas y buscar.
                            val ubicacionMatch = tarea.ubicacion?.lowercase()?.contains(termino) ?: false

                            descripcionMatch || ubicacionMatch
                        }
                    }

                    callback(tareas, null) // Éxito
                } catch (e: Exception) {
                    callback(null, "Error de Deserialización: Verifique Tarea.kt y datos. Detalles: ${e.message}")
                }
            }
            .addOnFailureListener { e ->
                callback(null, e.message)
            }
    }
    // -------------- OTRAS FUNCIONES CRUD ---------------

    fun eliminarTarea(tareaId: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_NAME).document(tareaId)
            .delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun actualizarEstado(tareaId: String, nuevoEstado: String, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_NAME).document(tareaId)
            .update("estado", nuevoEstado)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun actualizarTarea(tareaId: String, updates: Map<String, Any>, callback: (Boolean, String?) -> Unit) {
        db.collection(COLLECTION_NAME).document(tareaId)
            .update(updates)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }


}