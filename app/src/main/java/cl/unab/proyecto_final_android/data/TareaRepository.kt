package cl.unab.proyecto_final_android.data

import android.net.Uri
import cl.unab.proyecto_final_android.Tarea
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

enum class ModoMuro {
    PENDIENTES, ASIGNADAS, REALIZADAS
}

class TareaRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    fun escucharTareas(
        modoMuro: ModoMuro,
        esAdmin: Boolean,
        usernameActual: String,
        filtroSupervisor: String?,
        onSuccess: (List<Tarea>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {

        val coleccion = firestore.collection("tareas")

        val query: Query = when (modoMuro) {
            ModoMuro.PENDIENTES -> {
                coleccion
                    .whereEqualTo("estado", "Pendiente")
                    .whereEqualTo("asignadaA", "")
            }
            ModoMuro.REALIZADAS -> {
                coleccion.whereEqualTo("estado", "Realizada")
            }
            ModoMuro.ASIGNADAS -> {
                if (esAdmin) {
                    // admin ve pendientes; filtraremos asignadas más abajo
                    coleccion.whereEqualTo("estado", "Pendiente")
                } else {
                    coleccion
                        .whereEqualTo("estado", "Pendiente")
                        .whereEqualTo("asignadaA", usernameActual)
                }
            }
        }

        return query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }

            val lista = mutableListOf<Tarea>()
            val docs = snapshot?.documents ?: emptyList()

            for (doc in docs) {
                val tarea = doc.toObject(Tarea::class.java)?.copy(id = doc.id) ?: continue

                val pasaFiltroAsignadas = when (modoMuro) {
                    ModoMuro.ASIGNADAS -> {
                        if (esAdmin) {
                            if (tarea.asignadaA.isBlank()) {
                                false
                            } else if (filtroSupervisor.isNullOrEmpty()) {
                                true
                            } else {
                                tarea.asignadaA == filtroSupervisor
                            }
                        } else true
                    }
                    else -> true
                }

                if (pasaFiltroAsignadas) {
                    lista.add(tarea)
                }
            }

            onSuccess(lista)
        }
    }

    fun eliminarTarea(
        tareaId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection("tareas")
            .document(tareaId)
            .delete()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun asignarTarea(
        tareaId: String,
        usernameSupervisor: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection("tareas")
            .document(tareaId)
            .update(
                mapOf(
                    "asignadaA" to usernameSupervisor,
                    "estado" to "Pendiente"
                )
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun guardarRespuesta(
        tareaId: String,
        localPhotoPath: String,
        comentario: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val fileUri = Uri.fromFile(java.io.File(localPhotoPath))
        val storageRef = storage.reference
        val fotoRef = storageRef.child("respuestas_tareas/${tareaId}_${fileUri.lastPathSegment}")

        fotoRef.putFile(fileUri)
            .addOnSuccessListener {
                fotoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    actualizarRespuestaEnFirestore(
                        tareaId,
                        downloadUri.toString(),
                        comentario,
                        onResult
                    )
                }.addOnFailureListener { e ->
                    onResult(Result.failure(e))
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    private fun actualizarRespuestaEnFirestore(
        tareaId: String,
        fotoDespuesUrl: String,
        comentario: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        firestore.collection("tareas")
            .document(tareaId)
            .update(
                mapOf(
                    "fotoDespuesUrl" to fotoDespuesUrl,
                    "comentarioRespuesta" to comentario,
                    "estado" to "Realizada",
                    "fechaRespuesta" to Timestamp.now()
                )
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun actualizarCamposTarea(
        tareaId: String,
        nuevaDescripcion: String,
        nuevaUbicacion: String,
        nuevoPiso: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val data = mapOf(
            "descripcion" to nuevaDescripcion,
            "ubicacion" to nuevaUbicacion,
            "piso" to nuevoPiso
        )

        firestore.collection("tareas")
            .document(tareaId)
            .update(data)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

}
