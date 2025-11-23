package cl.unab.proyecto_final_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val rolUsuario: String,
    private val onResponderClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

        // Mapa username -> nombre visible
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

        inner class TareaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTarea: ImageView = itemView.findViewById(R.id.imgTarea)
        val imgRespuesta: ImageView = itemView.findViewById(R.id.imgRespuesta)

        val tvDescripcionTarea: TextView = itemView.findViewById(R.id.tvDescripcionTarea)
        val tvUbicacionTarea: TextView = itemView.findViewById(R.id.tvUbicacionTarea)
        val tvPisoValor: TextView = itemView.findViewById(R.id.tvPisoValor)
        val tvFechaCreacion: TextView = itemView.findViewById(R.id.tvFechaCreacion)
        val tvFechaRespuesta: TextView = itemView.findViewById(R.id.tvFechaRespuesta)
        val tvEstadoTarea: TextView = itemView.findViewById(R.id.tvEstadoTarea)
        val tvAsignadaA: TextView = itemView.findViewById(R.id.tvAsignadaA)

        val btnResponderFoto: Button = itemView.findViewById(R.id.btnResponderFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarea, parent, false)
        return TareaViewHolder(view)
    }

    override fun getItemCount(): Int = tareas.size

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]

        // Texto básico
        holder.tvDescripcionTarea.text = tarea.descripcion
        holder.tvUbicacionTarea.text = "Ubicación: ${tarea.ubicacion}"
        holder.tvPisoValor.text = tarea.piso

        // Fechas
        holder.tvFechaCreacion.text =
            tarea.fechaCreacion?.toDate()?.let { "Creada: $it" } ?: ""

        holder.tvFechaRespuesta.text =
            tarea.fechaRespuesta?.toDate()?.let { "Realizada: $it" } ?: ""

        // Estado
        holder.tvEstadoTarea.text = "Estado: ${tarea.estado}"

        // Supervisor asignado
        val textoAsignadaA = if (tarea.asignadaA.isBlank()) {
            "Asignada a: -"
        } else {
            val nombreVisible = mapaSupervisores[tarea.asignadaA] ?: tarea.asignadaA
            "Asignada a: $nombreVisible"
        }
        holder.tvAsignadaA.text = textoAsignadaA


        // Imagen ANTES
        if (tarea.fotoAntesUrl.isNotBlank()) {
            Glide.with(holder.itemView.context)
                .load(tarea.fotoAntesUrl)
                .into(holder.imgTarea)
        } else {
            holder.imgTarea.setImageResource(R.drawable.camera_icon)
        }

        // Imagen DESPUÉS
        if (tarea.fotoDespuesUrl.isNotBlank()) {
            holder.imgRespuesta.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(tarea.fotoDespuesUrl)
                .into(holder.imgRespuesta)
        } else {
            holder.imgRespuesta.visibility = View.GONE
        }

        // Botón responder
        holder.btnResponderFoto.setOnClickListener {
            onResponderClick(tarea)
        }
    }

    fun actualizarLista(nuevaLista: List<Tarea>) {
        tareas = nuevaLista
        notifyDataSetChanged()
    }
}
