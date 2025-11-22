package cl.unab.proyecto_final_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TareaAdapter(
    private val tareas: List<Tarea>,
    private val onResponderClick: (tarea: Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    inner class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTarea: ImageView = itemView.findViewById(R.id.imgTarea)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionTarea)
        val tvUbicacion: TextView = itemView.findViewById(R.id.tvUbicacionTarea)
        val tvPiso: TextView = itemView.findViewById(R.id.tvPisoValor)
        val tvFecha: TextView = itemView.findViewById(R.id.Tarea)  // renombrar quizás id
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstadotarea)
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
        holder.tvPiso.text = "Piso: ${tarea.piso}"
        holder.tvFecha.text = "Creada: ${tarea.fechaCreacion}"
        holder.tvEstado.text = "Estado: ${tarea.estado}"
        tarea.imagenResId?.let {
            holder.imgTarea.setImageResource(it)
        } // o si es URL, cargar con Glide/Picasso

        holder.btnResponder.setOnClickListener {
            onResponderClick(tarea)
        }
    }

    override fun getItemCount(): Int = tareas.size
}
