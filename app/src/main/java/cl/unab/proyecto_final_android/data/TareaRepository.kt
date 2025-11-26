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

    private val COLLECTION = "tareas"

    // -------------------------------------------------------------
    //  A)  OBTENER TAREAS FILTRADAS (USADA POR EL VIEWMODEL NUEVO)
    // -------------------------------------------------------------
    fun obtenerTareasFiltradas(
        modoMuro: ModoMuro,
        piso: String,
        asignadoA: String?,
        textoBusqueda: String,
        fechaDesde: Timestamp?,
        fechaHasta: Timestamp?,
        callback: (List<Tarea>, String?) -> Unit
    ) {
        var query: Query = db.collection(COLLECTION)

        // 1. FILTRO POR ESTADO SEGÚN MODO MURO
        query = when (modoMuro) {
            ModoMuro.PENDIENTES -> query.whereEqualTo("estado", "Pendiente")
            ModoMuro.ASIGNADAS -> query.whereEqualTo("estado", "Asignada")
            ModoMuro.REALIZADAS -> query.whereEqualTo("estado", "Realizada")
        }

        // 2. FILTRO POR ASIGNACIÓN
        if (!asignadoA.isNullOrBlank()) {
            query = query.whereEqualTo("asignadaA", asignadoA)
        }

        // 3. FILTRO POR PISO
        if (piso != "Todos") {
            query = query.whereEqualTo("piso", piso)
        }

        // 4. FILTRO POR FECHAS
        fechaDesde?.let { query = query.whereGreaterThanOrEqualTo("fechaCreacion", it) }
        fechaHasta?.let { query = query.whereLessThanOrEqualTo("fechaCreacion", it) }

        // 5. ORDEN
        query = query.orderBy("fechaCreacion", Query.Direction.DESCENDING)

        query.get()
            .addOnSuccessListener { result ->
                var tareas = result.toObjects(Tarea::class.java)

                // 6. FILTRO LOCAL POR TEXTO
                if (textoBusqueda.isNotBlank()) {
                    val termino = textoBusqueda.lowercase()
                    tareas = tareas.filter { t ->
                        (t.descripcion?.lowercase()?.contains(termino) ?: false) ||
                                (t.ubicacion?.lowercase()?.contains(termino) ?: false)
                    }
                }

                callback(tareas, null)
            }
            .addOnFailureListener { e ->
                callback(emptyList(), e.message)
            }
    }

    // -------------------------------------------------------------
    //  B)  SUBIR FOTO DESPUÉS (respuesta)
    // -------------------------------------------------------------
    fun subirFotoRespuesta(
        tarea: Tarea,
        uri: Uri,
        callback: (Boolean, String?) -> Unit
    ) {
        val ref = storage.reference.child("respuestas/${tarea.id}_${System.currentTimeMillis()}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->

                    val updates = mapOf(
                        "estado" to "Realizada",
                        "fechaRespuesta" to Timestamp.now(),
                        "fotoDespuesUrl" to downloadUrl.toString()
                    )

                    db.collection(COLLECTION).document(tarea.id)
                        .update(updates)
                        .addOnSuccessListener { callback(true, null) }
                        .addOnFailureListener { e -> callback(false, e.message) }

                }.addOnFailureListener { e ->
                    callback(false, e.message)
                }
            }
            .addOnFailureListener { e ->
                callback(false, e.message)
            }
    }

    // -------------------------------------------------------------
    //  C) ASIGNAR TAREA
    // -------------------------------------------------------------
    fun asignarTarea(
        tarea: Tarea,
        supervisorUsername: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "estado" to "Asignada",
            "asignadaA" to supervisorUsername
        )

        db.collection(COLLECTION).document(tarea.id)
            .update(updates)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // -------------------------------------------------------------
    //  D) RECHAZAR / ELIMINAR TAREA
    // -------------------------------------------------------------
    fun rechazarTarea(
        tarea: Tarea,
        callback: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "estado" to "Rechazada"
        )

        db.collection(COLLECTION).document(tarea.id)
            .update(updates)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    fun eliminarTarea(
        tarea: Tarea,
        callback: (Boolean, String?) -> Unit
    ) {
        db.collection(COLLECTION).document(tarea.id)
            .delete()
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }

    // -------------------------------------------------------------
    //  E) ACTUALIZAR TAREA (Editar desde el diálogo)
    // -------------------------------------------------------------
    fun actualizarTarea(
        tarea: Tarea,
        nuevaDescripcion: String,
        nuevaUbicacion: String,
        nuevoPiso: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "descripcion" to nuevaDescripcion,
            "ubicacion" to nuevaUbicacion,
            "piso" to nuevoPiso
        )

        db.collection(COLLECTION).document(tarea.id)
            .update(updates)
            .addOnSuccessListener { callback(true, null) }
            .addOnFailureListener { e -> callback(false, e.message) }
    }


}
