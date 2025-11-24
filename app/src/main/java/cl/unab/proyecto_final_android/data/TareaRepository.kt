package cl.unab.proyecto_final_android.data

import android.net.Uri
import cl.unab.proyecto_final_android.Tarea
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.File

// Definición del Enum
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
        fechaDesde: Timestamp?,
        fechaHasta: Timestamp?,
        onSuccess: (List<Tarea>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {

        val coleccion = firestore.collection("tareas")
        val campoFecha: String
        var query: Query

        // 1. Lógica de selección de modo y filtros base
        if (modoMuro == ModoMuro.REALIZADAS) {
            campoFecha = "fechaRespuesta"
            query = coleccion.whereEqualTo("estado", "Realizada")
        } else {
            campoFecha = "fechaCreacion"

            query = when (modoMuro) {
                ModoMuro.PENDIENTES -> coleccion
                    .whereEqualTo("estado", "Pendiente")
                    .whereEqualTo("asignadaA", "")

                ModoMuro.ASIGNADAS -> {
                    // LÓGICA CORREGIDA PARA "TODOS" (El cambio que hicimos)
                    if (esAdmin && filtroSupervisor.isNullOrEmpty()) {
                        // Si es Admin y ve "Todos", traemos todas las pendientes
                        // (Filtraremos las "sin asignar" manualmente más abajo)
                        coleccion.whereEqualTo("estado", "Pendiente")
                    } else {
                        // Usuario normal o Admin buscando a alguien específico
                        val targetUsername = filtroSupervisor ?: usernameActual
                        coleccion
                            .whereEqualTo("estado", "Pendiente")
                            .whereEqualTo("asignadaA", targetUsername)
                    }
                }
                else -> coleccion
            }
        }

        // 2. Aplicar Filtros de Fecha y Ordenamiento
        if (fechaDesde != null || fechaHasta != null) {
            query = query.orderBy(campoFecha, Query.Direction.DESCENDING)
            if (fechaDesde != null) query = query.whereGreaterThanOrEqualTo(campoFecha, fechaDesde)
            if (fechaHasta != null) query = query.whereLessThanOrEqualTo(campoFecha, fechaHasta)
        } else {
            query = query.orderBy(campoFecha, Query.Direction.DESCENDING)
        }

        // 3. Listener y RETURN (¡Esto es lo que faltaba!)
        return query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                var tareas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Tarea::class.java)?.apply {
                        this.id = doc.id
                    }
                }

                // --- FILTRO FINAL DE SEGURIDAD ---
                // Si estamos en ASIGNADAS viendo "Todos", ocultamos las que no tienen dueño
                // para que no se mezclen con las "Pendientes" puras.
                if (modoMuro == ModoMuro.ASIGNADAS && esAdmin && filtroSupervisor.isNullOrEmpty()) {
                    tareas = tareas.filter { it.asignadaA.isNotEmpty() }
                }

                onSuccess(tareas)
            } else {
                onSuccess(emptyList())
            }
        }
    }

    // --- MÉTODOS DE ESCRITURA (ELIMINAR, ASIGNAR, RESPONDER, EDITAR) ---

    fun eliminarTarea(tareaId: String, onResult: (Result<Unit>) -> Unit) {
        firestore.collection("tareas").document(tareaId).delete()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun asignarTarea(tareaId: String, usernameSupervisor: String, onResult: (Result<Unit>) -> Unit) {
        firestore.collection("tareas").document(tareaId)
            .update(mapOf("asignadaA" to usernameSupervisor, "estado" to "Pendiente"))
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun guardarRespuesta(tareaId: String, localPhotoPath: String, comentario: String, onResult: (Result<Unit>) -> Unit) {
        val fileUri = Uri.fromFile(File(localPhotoPath))
        val ref = storage.reference.child("respuestas_tareas/${tareaId}_${fileUri.lastPathSegment}")

        ref.putFile(fileUri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                firestore.collection("tareas").document(tareaId).update(
                    mapOf(
                        "fotoDespuesUrl" to uri.toString(),
                        "comentarioRespuesta" to comentario,
                        "estado" to "Realizada",
                        "fechaRespuesta" to Timestamp.now()
                    )
                ).addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { e -> onResult(Result.failure(e)) }
            }
        }.addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun actualizarCamposTarea(tareaId: String, desc: String, ubi: String, piso: String, onResult: (Result<Unit>) -> Unit) {
        firestore.collection("tareas").document(tareaId)
            .update(mapOf("descripcion" to desc, "ubicacion" to ubi, "piso" to piso))
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }
}