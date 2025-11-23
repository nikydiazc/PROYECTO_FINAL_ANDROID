package cl.unab.proyecto_final_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val rolUsuario: String,
    private val onResponderClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    inner class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAntes: ImageView = itemView.findViewById(R.id.imgTarea)
        val imgDespues: ImageView = itemView.findViewById(R.id.imgRespuesta)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionTarea)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacionTarea)
        val tvPiso: TextView = itemView.findViewById(R.id.tvPisoValor)
        val tvFechaCreacion: TextView = itemView.findViewById(R.id.tvFechaCreacion)
        val tvFechaRespuesta: TextView = itemView.findViewById(R.id.tvFechaRespuesta)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadoTarea)
        val btnResponder: Button = itemView.findViewById(R.id.btnResponderFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(view)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]

        holder.tvDescripcion.text = tarea.descripcion
        holder.tvUbicacion.text = "Ubicación: ${tarea.ubicacion}"
        holder.tvPiso.text = "Piso ${tarea.piso}"
        holder.tvEstado.text = "Estado: ${tarea.estado}"

        holder.tvFechaCreacion.text = "Creada: ${formatearFecha(tarea.fechaCreacion)}"

        // Fecha respuesta: solo si existe
        if (tarea.fechaRespuesta != null) {
            holder.tvFechaRespuesta.visibility = View.VISIBLE
            holder.tvFechaRespuesta.text = "Realizada: ${formatearFecha(tarea.fechaRespuesta)}"
        } else {
            holder.tvFechaRespuesta.visibility = View.GONE
        }

        // Imagen ANTES
        if (tarea.fotoAntesUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(tarea.fotoAntesUrl)
                .into(holder.imgAntes)
        } else {
            holder.imgAntes.setImageResource(R.drawable.camera_icon)
        }

        // Imagen DESPUÉS
        if (tarea.fotoDespuesUrl.isNotEmpty()) {
            holder.imgDespues.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(tarea.fotoDespuesUrl)
                .into(holder.imgDespues)
        } else {
            holder.imgDespues.visibility = View.GONE
        }

        // Botón responder:
        // - Solo visible si la tarea está Pendiente
        // - Para ADMIN y REALIZAR
        val esPendiente = tarea.estado == "Pendiente"
        val puedeResponder = rolUsuario == LoginActivity.ROL_ADMIN ||
                rolUsuario == LoginActivity.ROL_REALIZAR

        if (esPendiente && puedeResponder) {
            holder.btnResponder.visibility = View.VISIBLE
            holder.btnResponder.setOnClickListener {
                onResponderClick(tarea)
            }
        } else {
            holder.btnResponder.visibility = View.GONE
            holder.btnResponder.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = tareas.size

    fun actualizarLista(nuevaLista: List<Tarea>) {
        tareas = nuevaLista
        notifyDataSetChanged()
    }

    private fun formatearFecha(timestamp: Timestamp?): String {
        if (timestamp == null) return "-"
        val date = timestamp.toDate()
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return formato.format(date)
    }
}
