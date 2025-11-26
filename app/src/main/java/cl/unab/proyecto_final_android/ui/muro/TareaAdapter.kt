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
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.util.ColorStatus // Asegúrate de tener ColorStatus.kt
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaAdapter(
    private var tareas: List<Tarea>,

    // 1. MODIFICACIÓN CRUCIAL: Reemplazamos 'esAdmin' por 'rolUsuario'
    private val rolUsuario: String,
    private val usernameActual: String,

    // Callbacks
    private val onResponderClick: (Tarea) -> Unit,
    private val onEditarClick: (Tarea) -> Unit,
    private val onEliminarClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    // Helper para determinar si es administrador
    private val esAdmin: Boolean
        get() = rolUsuario == LoginActivity.ROL_ADMIN

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
        val context = holder.itemView.context

        // 1. POBLAR DATOS GENERALES
        binding.tvDescripcionTarea.text = tarea.descripcion
        binding.tvUbicacionTarea.text = "Ubicación: ${tarea.ubicacion}"
        binding.tvPisoValor.text = "Piso: ${tarea.piso}"
        binding.tvAsignadaA.text = "Asignada a: ${if (tarea.asignadaA.isNullOrEmpty()) "—" else tarea.asignadaA}"
        binding.tvFechaCreacion.text = "Creada: ${formatearTimestamp(tarea.fechaCreacion)}"

        // Asignar estado y color/fondo usando ColorStatus
        binding.tvEstadoTarea.text = "Estado: ${tarea.estado}"
        binding.tvEstadoTarea.setBackgroundResource(ColorStatus.getColorResource(tarea.estado))


        // 2. LÓGICA DE VISIBILIDAD DE BOTONES Y CAMPOS DE RESPUESTA

        // --- Permisos de Edición/Eliminación ---
        // SOLO Admin puede editar/eliminar. Ningún supervisor ni creador.
        val puedeAdministrar = esAdmin
        binding.btnEditarTarea.visibility = if (puedeAdministrar) View.VISIBLE else View.GONE
        binding.btnEliminarTarea.visibility = if (puedeAdministrar) View.VISIBLE else View.GONE

        // --- Permisos para Responder ---
        val puedeResponder = rolUsuario != LoginActivity.ROL_ADMIN && tarea.estado != "Realizada" && tarea.estado != "Rechazada"

        // Si la tarea está asignada, solo puede responder el asignado (supervisor/realizador específico)
        val asignadaAotro = !tarea.asignadaA.isNullOrEmpty() && tarea.asignadaA != usernameActual

        // Mostrar botón Responder: Si tiene permiso general Y no está asignada a otro.
        binding.btnResponderFoto.visibility = if (puedeResponder && !asignadaAotro) View.VISIBLE else View.GONE


// Dentro de TareaAdapter.kt -> override fun onBindViewHolder(...)

// ... (Resto del código de permisos y configuración inicial)

// 3. VISTAS CONDICIONALES POR ESTADO
        when (tarea.estado) {
            "Pendiente", "Asignada" -> {
                // Ocultar elementos de Realizada
                binding.imgRespuesta.visibility = View.GONE
                binding.tvFechaRespuesta.visibility = View.GONE
                // ¡Se elimina la referencia a tvFechaRespuestaTitle!

                // Cargar Foto ANTES
                cargarFoto(context, binding.imgTarea, tarea.fotoAntesUrl)
            }
            "Realizada", "Rechazada" -> {
                // Mostrar elementos de Realizada
                binding.imgRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.visibility = View.VISIBLE

                // CORRECCIÓN: Usamos tvFechaRespuesta para mostrar etiqueta y valor
                val fechaFormateada = formatearTimestamp(tarea.fechaRespuesta)
                binding.tvFechaRespuesta.text = "Realizada: $fechaFormateada"

                // Cargar Foto ANTES (Original)
                cargarFoto(context, binding.imgTarea, tarea.fotoAntesUrl)

                // Cargar Foto DESPUÉS (Respuesta)
                cargarFoto(context, binding.imgRespuesta, tarea.fotoDespuesUrl)
            }
        }

// ... (Resto de listeners y funciones auxiliares)

        // 4. LISTENERS DE CLIC (Fotos y Botones de Acción)

        // Listener para la foto ANTES (imgTarea)
        binding.imgTarea.setOnClickListener {
            abrirVisualizador(context, tarea.fotoAntesUrl)
        }

        // Listener para la foto DESPUÉS (imgRespuesta)
        binding.imgRespuesta.setOnClickListener {
            if (tarea.estado == "Realizada") {
                abrirVisualizador(context, tarea.fotoDespuesUrl)
            } else {
                Toast.makeText(context, "Aún no hay foto de respuesta disponible", Toast.LENGTH_SHORT).show()
            }
        }

        // Botones de Acción
        binding.btnResponderFoto.setOnClickListener { onResponderClick(tarea) }
        binding.btnEditarTarea.setOnClickListener { onEditarClick(tarea) }
        binding.btnEliminarTarea.setOnClickListener { onEliminarClick(tarea) }
    }

    // ---------------------- FUNCIONES AUXILIARES ----------------------

    private fun cargarFoto(context: android.content.Context, imageView: android.widget.ImageView, url: String?) {
        if (!url.isNullOrEmpty()) {
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.camera_icon) // Asegúrate de tener este drawable
                .error(R.drawable.error_placeholder) // Asegúrate de tener este drawable
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.camera_icon)
        }
    }

    private fun abrirVisualizador(context: android.content.Context, url: String?) {
        if (!url.isNullOrEmpty()) {
            val intent = Intent(context, VisualizadorImagenActivity::class.java).apply {
                putStringArrayListExtra(VisualizadorImagenActivity.EXTRA_IMAGE_URLS, arrayListOf(url))
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No hay foto disponible para mostrar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatearTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            dateFormat.format(Date(timestamp.toDate().time))
        } else {
            "—"
        }
    }

    // ---------------------- MÉTODOS REQUERIDOS ----------------------

    fun actualizarTareas(nuevasTareas: List<Tarea>) {
        this.tareas = nuevasTareas
        notifyDataSetChanged()
    }

    fun obtenerTareaEnPosicion(position: Int): Tarea? {
        return if (position >= 0 && position < tareas.size) tareas[position] else null
    }
}