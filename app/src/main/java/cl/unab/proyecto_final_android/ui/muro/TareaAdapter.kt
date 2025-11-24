package cl.unab.proyecto_final_android.ui.muro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import com.bumptech.glide.Glide
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import java.text.SimpleDateFormat
import java.util.Locale

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val rolUsuario: String,
    private val onResponderClick: (Tarea) -> Unit,
    private val onEditarClick: (Tarea) -> Unit,
    private val onEliminarClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    // Mapa username -> nombre visible bonito
    private val mapaSupervisores = mapOf(
        "delfina.cabello" to "Delfina Cabello (Poniente)",
        "rodrigo.reyes" to "Rodrigo Reyes (Poniente)",
        "maria.caruajulca" to "Maria Caruajulca (Poniente)",
        "cristian.vergara" to "Cristian Vergara (Poniente)",
        "enrique.mendez" to "Enrique Mendez (Poniente)",
        "norma.marican" to "Norma Marican (Poniente)",
        "john.vilchez" to "John Vilchez (Oriente)",
        "libia.florez" to "Libia Florez (Oriente)",
        "jorge.geisbuhler" to "Jorge Geisbuhler (Oriente)"
    )

    // Se usa el SimpleDateFormat
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAntes: ImageView = itemView.findViewById(R.id.imgTarea)
        val imgDespues: ImageView = itemView.findViewById(R.id.imgRespuesta)

        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionTarea)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacionTarea)
        val tvPiso: TextView = itemView.findViewById(R.id.tvPisoValor)
        val tvFechaCreacion: TextView = itemView.findViewById(R.id.tvFechaCreacion)
        val tvFechaRespuesta: TextView = itemView.findViewById(R.id.tvFechaRespuesta)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoTarea)
        val tvAsignadaA: TextView = itemView.findViewById(R.id.tvAsignadaA)

        val btnResponderFoto: Button = itemView.findViewById(R.id.btnResponderFoto)
        val btnEditarTarea: ImageButton = itemView.findViewById(R.id.btnEditarTarea)
        val btnEliminarTarea: ImageButton = itemView.findViewById(R.id.btnEliminarTarea)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(view)
    }

    override fun getItemCount(): Int = tareas.size

    fun actualizarLista(nuevaLista: List<Tarea>) {
        tareas = nuevaLista
        notifyDataSetChanged()
    }

    fun obtenerTareaEnPosicion(position: Int): Tarea? =
        if (position in tareas.indices) tareas[position] else null

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]
        val context = holder.itemView.context

        // ---------- Texto base ----------
        holder.tvDescripcion.text = tarea.descripcion.ifBlank { "Sin descripción" }
        holder.tvUbicacion.text = "Ubicación: ${tarea.ubicacion.ifBlank { "-" }}"
        holder.tvPiso.text = "Piso ${tarea.piso.ifBlank { "-" }}"

        // Fecha de creación
        // CORRECCIÓN DE SINTAXIS: Uso de .toDate() en el objeto Timestamp
        val fechaCreacionTexto = tarea.fechaCreacion?.let { timestamp ->
            "Creada: ${sdf.format(timestamp.toDate())}"
        } ?: "Creada: -"
        holder.tvFechaCreacion.text = fechaCreacionTexto

        // Fecha de respuesta (solo si existe)
        if (tarea.fechaRespuesta != null) {
            val fechaResp = tarea.fechaRespuesta.toDate()
            holder.tvFechaRespuesta.visibility = View.VISIBLE
            holder.tvFechaRespuesta.text = "Realizada: ${sdf.format(fechaResp)}"
        } else {
            holder.tvFechaRespuesta.visibility = View.GONE
        }

        // Estado (texto + color)
        holder.tvEstado.text = "Estado: ${tarea.estado}"
        val colorEstado = when (tarea.estado.lowercase()) {
            "pendiente" -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
            "realizada" -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
            "en proceso" -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(context, android.R.color.white)
        }
        holder.tvEstado.setTextColor(colorEstado)

        // Asignada a (nombre visible)
        val textoAsignadaA = if (tarea.asignadaA.isBlank()) {
            "Asignada a: -"
        } else {
            val nombreVisible = mapaSupervisores[tarea.asignadaA] ?: tarea.asignadaA
            "Asignada a: $nombreVisible"
        }
        holder.tvAsignadaA.text = textoAsignadaA

        // ---------- Imagen ANTES ----------
        if (tarea.fotoAntesUrl.isNotBlank()) {
            holder.imgAntes.setBackgroundColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            )
            Glide.with(context)
                .load(tarea.fotoAntesUrl)
                .placeholder(R.drawable.camera_icon)
                .centerCrop()
                .into(holder.imgAntes)
        } else {
            holder.imgAntes.setImageResource(R.drawable.camera_icon)
        }

        // ---------- Imagen DESPUÉS ----------
        if (tarea.fotoDespuesUrl.isNotBlank()) {
            holder.imgDespues.visibility = View.VISIBLE
            Glide.with(context)
                .load(tarea.fotoDespuesUrl)
                .placeholder(R.drawable.camera_icon)
                .centerCrop()
                .into(holder.imgDespues)
        } else {
            holder.imgDespues.visibility = View.GONE
        }

        // ---------- Lógica botones según rol y estado ----------
        val esRealizada = tarea.estado.equals("Realizada", ignoreCase = true)
        val esPendiente = tarea.estado.equals("Pendiente", ignoreCase = true)

        // BOTÓN RESPONDER:
        // Solo si NO está realizada y el rol es ADMIN o REALIZAR
        val puedeResponder = !esRealizada &&
                (rolUsuario == LoginActivity.ROL_ADMIN || rolUsuario == LoginActivity.ROL_REALIZAR) &&
                esPendiente

        if (puedeResponder) {
            holder.btnResponderFoto.visibility = View.VISIBLE
            holder.btnResponderFoto.isEnabled = true
            holder.btnResponderFoto.setOnClickListener {
                onResponderClick(tarea)
            }
        } else {
            holder.btnResponderFoto.visibility = View.GONE
            holder.btnResponderFoto.setOnClickListener(null)
        }

        // BOTÓN EDITAR:
        // Solo ADMIN y solo si NO está realizada
        if (rolUsuario == LoginActivity.ROL_ADMIN && !esRealizada) {
            holder.btnEditarTarea.visibility = View.VISIBLE
            holder.btnEditarTarea.setOnClickListener {
                onEditarClick(tarea)
            }
        } else {
            holder.btnEditarTarea.visibility = View.GONE
            holder.btnEditarTarea.setOnClickListener(null)
        }

        // BOTÓN ELIMINAR:
        // Solo ADMIN, en cualquier estado
        if (rolUsuario == LoginActivity.ROL_ADMIN) {
            holder.btnEliminarTarea.visibility = View.VISIBLE
            holder.btnEliminarTarea.setOnClickListener {
                onEliminarClick(tarea)
            }
        } else {
            holder.btnEliminarTarea.visibility = View.GONE
            holder.btnEliminarTarea.setOnClickListener(null)
        }
    }
}