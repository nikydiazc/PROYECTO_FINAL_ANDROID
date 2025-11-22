package cl.unab.proyecto_final_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class TareaAdapter(
    private val tareas: List<Tarea>
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTarea: ImageView = itemView.findViewById(R.id.imgTarea)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionTarea)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacionTarea)
        val tvPiso: TextView = itemView.findViewById(R.id.tvPisoValor)
        val tvFecha: TextView = itemView.findViewById(R.id.Tarea)          // tu TextView de fecha
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadotarea)
        val btnResponder: Button = itemView.findViewById(R.id.btnResponderFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(view)
    }

    override fun getItemCount(): Int = tareas.size

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]

        holder.tvDescripcion.text = tarea.descripcion
        holder.tvUbicacion.text = "Ubicación: ${tarea.ubicacion}"
        holder.tvPiso.text = tarea.piso
        holder.tvEstado.text = "Estado: ${tarea.estado}"

        // Formatear fecha si existe
        val fecha = tarea.fechaCreacion?.toDate()
        holder.tvFecha.text = if (fecha != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            "Creada: ${sdf.format(fecha)}"
        } else {
            "Creada: -"
        }

        // Cargar imagen con Glide
        Glide.with(holder.itemView.context)
            .load(tarea.imagenUrl)
            .placeholder(R.drawable.camera_icon)
            .centerCrop()
            .into(holder.imgTarea)

        // Por ahora no hacemos nada con el botón "Responder"
        holder.btnResponder.setOnClickListener {
            // Aquí después podemos abrir otra pantalla o marcar como resuelta, etc.
        }
    }
}
