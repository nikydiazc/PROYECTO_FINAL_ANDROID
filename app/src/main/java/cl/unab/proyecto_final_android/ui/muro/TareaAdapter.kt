package cl.unab.proyecto_final_android.ui.muro

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.VisualizadorImagenActivity
import cl.unab.proyecto_final_android.databinding.ItemTareaBinding
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val esAdmin: Boolean,
    private val usernameActual: String,
    // Definimos interfaces para manejar los clics de botones fuera del Adapter
    private val onResponderClick: (Tarea) -> Unit,
    private val onEditarClick: (Tarea) -> Unit,
    private val onEliminarClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    // Helper para formatear la fecha
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Clase interna ViewHolder
    inner class TareaViewHolder(val binding: ItemTareaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TareaViewHolder(binding)
    }

    override fun getItemCount(): Int = tareas.size

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val tarea = tareas[position]
        val binding = holder.binding
        val context = holder.itemView.context // Contexto para iniciar Activities/Toasts

        // 1. POBLAR DATOS GENERALES
        binding.tvDescripcionTarea.text = tarea.descripcion
        binding.tvUbicacionTarea.text = "Ubicación: ${tarea.ubicacion}"
        binding.tvPisoValor.text = "Piso: ${tarea.piso}"
        binding.tvAsignadaA.text = "Asignada a: ${if (tarea.asignadaA.isNullOrEmpty()) "—" else tarea.asignadaA}"

        // Formatear Fechas
        binding.tvFechaCreacion.text = "Creada: ${formatearTimestamp(tarea.fechaCreacion)}"

        // 2. LÓGICA DE ESTADO (PENDIENTES, ASIGNADAS, REALIZADAS)
        when (tarea.estado) {
            "Pendiente", "Asignada" -> {
                binding.tvEstadoTarea.text = "Estado: ${tarea.estado}"
                binding.tvEstadoTarea.setTextColor(context.getColor(android.R.color.holo_red_dark))

                // Mostrar Responder si la tarea es para el usuario actual o es Admin
                binding.btnResponderFoto.visibility = if (tarea.asignadaA == usernameActual || esAdmin) View.VISIBLE else View.GONE

                // Ocultar elementos de Realizada
                binding.imgRespuesta.visibility = View.GONE
                binding.tvFechaRespuesta.visibility = View.GONE

                // 💡 Corregido: Si está Pendiente, solo cargamos la foto Antes
                if (!tarea.fotoAntesUrl.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(tarea.fotoAntesUrl)
                        .placeholder(R.drawable.camera_icon)
                        .error(R.drawable.error_placeholder) // Útil para depurar fallos
                        .into(binding.imgTarea)
                } else {
                    binding.imgTarea.setImageResource(R.drawable.camera_icon)
                }

                // Mostrar botones de Admin/Supervisor
                val showAdminButtons = esAdmin || tarea.asignadaA == usernameActual
                binding.btnEditarTarea.visibility = if (showAdminButtons) View.VISIBLE else View.GONE
                binding.btnEliminarTarea.visibility = if (showAdminButtons) View.VISIBLE else View.GONE

            }
            "Realizada" -> {
                binding.tvEstadoTarea.text = "Estado: Realizada"
                binding.tvEstadoTarea.setTextColor(context.getColor(android.R.color.holo_green_dark))
                binding.btnResponderFoto.visibility = View.GONE

                // Mostrar elementos de Realizada (Ahora están apilados con imgTarea)
                binding.imgRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.text = "Realizada: ${formatearTimestamp(tarea.fechaRespuesta)}"

                // 💡 CORREGIDO: Cargamos AMBAS fotos. imgTarea no se hace transparente.

                // Cargar Foto ANTES (Parte Superior)
                if (!tarea.fotoAntesUrl.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(tarea.fotoAntesUrl)
                        .placeholder(R.drawable.camera_icon)
                        .error(R.drawable.error_placeholder)
                        .into(binding.imgTarea)
                } else {
                    binding.imgTarea.setImageResource(R.drawable.camera_icon)
                }

                // Cargar Foto DESPUÉS (Parte Inferior)
                if (!tarea.fotoDespuesUrl.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(tarea.fotoDespuesUrl)
                        .placeholder(R.drawable.camera_icon)
                        .error(R.drawable.error_placeholder)
                        .into(binding.imgRespuesta)
                } else {
                    // Si no hay foto después, se oculta o se pone un placeholder, pero la visibilidad ya está en VISIBLE arriba
                    binding.imgRespuesta.setImageResource(R.drawable.camera_icon)
                }

                // Los admins aún pueden editar o eliminar tareas realizadas
                binding.btnEditarTarea.visibility = if (esAdmin) View.VISIBLE else View.GONE
                binding.btnEliminarTarea.visibility = if (esAdmin) View.VISIBLE else View.GONE
            }
            // Agrega otros estados si los tienes (ej: "Rechazada")
            else -> {
                // Estado por defecto
            }
        }

        // 3. LISTENERS DE CLIC (Fotos Individuales para Galería)

        // Función auxiliar para abrir la galería con la URL específica
        fun abrirVisualizador(url: String?) {
            if (!url.isNullOrEmpty()) {
                val intent = Intent(context, VisualizadorImagenActivity::class.java).apply {
                    putStringArrayListExtra(VisualizadorImagenActivity.EXTRA_IMAGE_URLS, arrayListOf(url))
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No hay foto disponible para mostrar", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener para la foto ANTES (imgTarea)
        binding.imgTarea.setOnClickListener {
            abrirVisualizador(tarea.fotoAntesUrl)
        }

        // Listener para la foto DESPUÉS (imgRespuesta)
        binding.imgRespuesta.setOnClickListener {
            if (tarea.estado == "Realizada") {
                abrirVisualizador(tarea.fotoDespuesUrl)
            } else {
                Toast.makeText(context, "Aún no hay foto de respuesta", Toast.LENGTH_SHORT).show()
            }
        }

        // 3.2. Botones de Acción
        binding.btnResponderFoto.setOnClickListener {
            onResponderClick(tarea)
        }

        binding.btnEditarTarea.setOnClickListener {
            onEditarClick(tarea)
        }

        binding.btnEliminarTarea.setOnClickListener {
            onEliminarClick(tarea)
        }
    }

    // Función para actualizar la lista de tareas
    fun actualizarTareas(nuevasTareas: List<Tarea>) {
        this.tareas = nuevasTareas
        notifyDataSetChanged()
    }

    // Función privada para formatear la marca de tiempo
    private fun formatearTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            dateFormat.format(Date(timestamp.toDate().time))
        } else {
            "—"
        }
    }

    // Función requerida por MuroTareasActivity para el ItemTouchHelper (Swipe)
    fun obtenerTareaEnPosicion(position: Int): Tarea? {
        return if (position >= 0 && position < tareas.size) {
            tareas[position]
        } else {
            null
        }
    }
}