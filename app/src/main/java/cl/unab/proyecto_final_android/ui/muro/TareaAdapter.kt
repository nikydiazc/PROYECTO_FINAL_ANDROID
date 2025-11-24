package cl.unab.proyecto_final_android.ui.muro

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
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

        // Determinar qué ImageView usaremos para la imagen de referencia (Antes)
        val imgReferencia = binding.imgTarea

        // 2. LÓGICA DE ESTADO (PENDIENTES, ASIGNADAS, REALIZADAS)
        when (tarea.estado) {
            "Pendiente" -> {
                binding.tvEstadoTarea.text = "Estado: Pendiente"
                binding.tvEstadoTarea.setTextColor(context.getColor(android.R.color.holo_red_dark))
                binding.btnResponderFoto.visibility = if (tarea.asignadaA == usernameActual || esAdmin) ViewGroup.VISIBLE else ViewGroup.GONE

                // Ocultar elementos de Realizada
                binding.imgRespuesta.visibility = ViewGroup.GONE
                binding.tvFechaRespuesta.visibility = ViewGroup.GONE

                // Cargar Imagen ANTES
                if (!tarea.fotoAntesUrl.isNullOrEmpty()) {
                    Glide.with(context).load(tarea.fotoAntesUrl).into(imgReferencia)
                } else {
                    imgReferencia.setImageResource(R.drawable.camera_icon)
                }

                // Mostrar botones de Admin/Supervisor solo si es su tarea o es Admin
                val showAdminButtons = esAdmin || tarea.asignadaA == usernameActual
                binding.btnEditarTarea.visibility = if (showAdminButtons) ViewGroup.VISIBLE else ViewGroup.GONE
                binding.btnEliminarTarea.visibility = if (showAdminButtons) ViewGroup.VISIBLE else ViewGroup.GONE
            }
            "Realizada" -> {
                binding.tvEstadoTarea.text = "Estado: Realizada"
                binding.tvEstadoTarea.setTextColor(context.getColor(android.R.color.holo_green_dark))
                binding.btnResponderFoto.visibility = ViewGroup.GONE

                // Mostrar elementos de Realizada
                binding.imgRespuesta.visibility = ViewGroup.VISIBLE
                binding.tvFechaRespuesta.visibility = ViewGroup.VISIBLE
                binding.tvFechaRespuesta.text = "Realizada: ${formatearTimestamp(tarea.fechaRespuesta)}"

                // Cargar Foto DESPUÉS (principal)
                if (!tarea.fotoDespuesUrl.isNullOrEmpty()) {
                    Glide.with(context).load(tarea.fotoDespuesUrl).into(binding.imgRespuesta)
                }
                // Asegurar que la imagen "antes" no se muestre doble o quede con datos antiguos
                imgReferencia.setImageResource(android.R.color.transparent)

                // Los admins aún pueden editar o eliminar tareas realizadas
                binding.btnEditarTarea.visibility = if (esAdmin) ViewGroup.VISIBLE else ViewGroup.GONE
                binding.btnEliminarTarea.visibility = if (esAdmin) ViewGroup.VISIBLE else ViewGroup.GONE
            }
            // Agrega otros estados si los tienes (ej: "Asignada" sin ser Pendiente)
            else -> {
                // Estado por defecto
            }
        }

        // 3. LISTENERS DE CLIC (Botones y Foto)

        // 3.1. Click en la Imagen para ver a Pantalla Completa (Lógica de Swipe/Doble Foto)

        val imageViewClickeable = if (tarea.estado == "Realizada") binding.imgRespuesta else binding.imgTarea

        imageViewClickeable.setOnClickListener {

            val urls = ArrayList<String>()
            val fotoAntes = tarea.fotoAntesUrl
            val fotoDespues = tarea.fotoDespuesUrl

            if (tarea.estado == "Realizada") {
                // Si está Realizada, el orden es: 1. Después, 2. Antes (si existe)
                if (!fotoDespues.isNullOrEmpty()) {
                    urls.add(fotoDespues)
                }
                if (!fotoAntes.isNullOrEmpty() && fotoAntes != fotoDespues) {
                    urls.add(fotoAntes)
                }
            } else {
                // Si está Pendiente, solo se muestra la foto Antes
                if (!fotoAntes.isNullOrEmpty()) {
                    urls.add(fotoAntes)
                }
            }

            if (urls.isNotEmpty()) {
                val intent = Intent(context, VisualizadorImagenActivity::class.java).apply {
                    // Usamos la clave para el ViewPager2/Array de URLs
                    putStringArrayListExtra(VisualizadorImagenActivity.EXTRA_IMAGE_URLS, urls)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "No hay foto disponible para mostrar", Toast.LENGTH_SHORT).show()
            }
        }

        // 3.2. Botones de Acción
        binding.btnResponderFoto.setOnClickListener {
            // ESTO DELEGA LA ACCIÓN A LA ACTIVITY
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