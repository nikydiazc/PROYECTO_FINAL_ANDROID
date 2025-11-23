package cl.unab.proyecto_final_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import cl.unab.proyecto_final_android.ui.login.LoginActivity.Companion.ROL_ADMIN
import cl.unab.proyecto_final_android.ui.login.LoginActivity.Companion.ROL_CREAR
import cl.unab.proyecto_final_android.ui.login.LoginActivity.Companion.ROL_REALIZAR
import java.text.SimpleDateFormat
import java.util.Locale

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val rolUsuario: String,
    private val onResponderClick: (Tarea) -> Unit
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

    // 👇 ESTA ES LA PARTE QUE ME PEDISTE COMPLETA
    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]

        // Descripción, ubicación, piso
        holder.tvDescripcion.text = tarea.descripcion.ifBlank { "Sin descripción" }
        holder.tvUbicacion.text = "Ubicación: ${tarea.ubicacion.ifBlank { "-" }}"
        holder.tvPiso.text = tarea.piso.ifBlank { "Piso -" }

        // Fecha de creación
        val fechaCreacionTexto = tarea.fechaCreacion?.toDate()?.let {
            "Creada: ${sdf.format(it)}"
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

        // Estado
        holder.tvEstado.text = "Estado: ${tarea.estado}"

        // Color de estado (puedes ajustar a tus colores)
        val context = holder.itemView.context
        val colorEstado = when (tarea.estado.lowercase()) {
            "pendiente" -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
            "realizada" -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
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

        // Imagen ANTES
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

        // Imagen DESPUÉS (solo si hay URL)
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

        // ----- LÓGICA BOTÓN RESPONDER -----
        val estadoEsPendiente = tarea.estado.equals("Pendiente", ignoreCase = true)

        val puedeResponder = when {
            // Si está realizada → nunca se responde
            tarea.estado.equals("Realizada", ignoreCase = true) -> false

            // Si está pendiente/asignada → ADMIN y REALIZAR pueden
            estadoEsPendiente && (rolUsuario == ROL_REALIZAR || rolUsuario == ROL_ADMIN) -> true

            // crear_tarea u otros roles → no
            else -> false
        }

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
    }
}
