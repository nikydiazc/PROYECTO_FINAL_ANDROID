package cl.unab.proyecto_final_android.ui.muro

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import cl.unab.proyecto_final_android.Tarea
import cl.unab.proyecto_final_android.VisualizadorImagenActivity
import cl.unab.proyecto_final_android.databinding.ItemTareaBinding
import cl.unab.proyecto_final_android.ui.login.LoginActivity
import cl.unab.proyecto_final_android.util.ColorStatus
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TareaAdapter(
    private var tareas: List<Tarea>,
    private val rolUsuario: String,
    private val usernameActual: String,
    private val onResponderClick: (Tarea) -> Unit,
    private val onEditarClick: (Tarea) -> Unit,
    private val onEliminarClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    private val esAdmin: Boolean
        get() = rolUsuario == LoginActivity.ROL_ADMIN

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class TareaViewHolder(val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root)

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

        // 1. Datos básicos
        binding.tvDescripcionTarea.text = tarea.descripcion
        binding.tvUbicacionTarea.text = "Ubicación: ${tarea.ubicacion}"
        binding.tvPisoValor.text = "Piso: ${tarea.piso}"
        binding.tvAsignadaA.text =
            "Asignada a: ${if (tarea.asignadaA.isNullOrEmpty()) "—" else tarea.asignadaA}"
        binding.tvFechaCreacion.text = "Creada: ${formatearTimestamp(tarea.fechaCreacion)}"

        // Estado + color de fondo
        binding.tvEstadoTarea.text = "Estado: ${tarea.estado}"
        binding.tvEstadoTarea.setBackgroundResource(
            ColorStatus.getColorResource(tarea.estado)
        )

        // 2. Visibilidad de botones según rol

        // Solo admin puede editar/eliminar
        val puedeAdministrar = esAdmin
        binding.btnEditarTarea.visibility = if (puedeAdministrar) View.VISIBLE else View.GONE
        binding.btnEliminarTarea.visibility = if (puedeAdministrar) View.VISIBLE else View.GONE

        // Permisos para responder:
        val puedeResponder = rolUsuario != LoginActivity.ROL_CREAR &&
                !tarea.estado.equals("Realizada", ignoreCase = true) &&
                !tarea.estado.equals("Rechazada", ignoreCase = true)

        val asignadaAOtroSupervisor =
            !tarea.asignadaA.isNullOrEmpty() &&
                    tarea.asignadaA != usernameActual &&
                    rolUsuario == LoginActivity.ROL_REALIZAR

        binding.btnResponderFoto.visibility =
            if (puedeResponder && !asignadaAOtroSupervisor) View.VISIBLE else View.GONE

        // 3. Vistas condicionales según estado
        when (tarea.estado) {
            "Pendiente", "Asignada" -> {
                binding.imgRespuesta.visibility = View.GONE
                binding.tvFechaRespuesta.visibility = View.GONE

                // Foto ANTES en la imagen superior
                cargarFoto(context, binding.imgTarea, tarea.fotoAntesUrl)
            }

            "Realizada" -> {
                binding.imgRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.visibility = View.VISIBLE

                val fechaFormateada = formatearTimestamp(tarea.fechaRespuesta)
                binding.tvFechaRespuesta.text = "Realizada: $fechaFormateada"

                // 🔁 AQUÍ INVERTIMOS EL ORDEN:
                // Imagen superior = DESPUÉS
                // Imagen inferior = ANTES
                cargarFoto(context, binding.imgTarea, tarea.fotoDespuesUrl)
                cargarFoto(context, binding.imgRespuesta, tarea.fotoAntesUrl)
            }

            "Rechazada" -> {
                binding.imgRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.visibility = View.VISIBLE

                val fechaFormateada = formatearTimestamp(tarea.fechaRespuesta)
                binding.tvFechaRespuesta.text = "Rechazada: $fechaFormateada"

                // También invertimos: primero DESPUÉS si existe (evidencia),
                // segundo ANTES
                cargarFoto(context, binding.imgTarea, tarea.fotoDespuesUrl)
                cargarFoto(context, binding.imgRespuesta, tarea.fotoAntesUrl)
            }

            else -> {
                binding.imgRespuesta.visibility = View.GONE
                binding.tvFechaRespuesta.visibility = View.GONE
                cargarFoto(context, binding.imgTarea, tarea.fotoAntesUrl)
            }
        }

        // 4. Clicks en imágenes

        // Imagen de arriba (imgTarea)
        binding.imgTarea.setOnClickListener {
            when (tarea.estado) {
                "Realizada", "Rechazada" -> {
                    // En realizadas/rechazadas la de arriba es DESPUÉS
                    abrirVisualizador(context, tarea.fotoDespuesUrl)
                }
                else -> {
                    // En pendientes/asignadas la de arriba es ANTES
                    abrirVisualizador(context, tarea.fotoAntesUrl)
                }
            }
        }

        // Imagen de abajo (imgRespuesta)
        binding.imgRespuesta.setOnClickListener {
            when (tarea.estado) {
                "Realizada", "Rechazada" -> {
                    // Abajo = ANTES
                    if (!tarea.fotoAntesUrl.isNullOrEmpty()) {
                        abrirVisualizador(context, tarea.fotoAntesUrl)
                    } else {
                        Toast.makeText(
                            context,
                            "No hay foto de antes disponible",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                else -> {
                    // Por seguridad: si en algún momento usas la segunda imagen para otra cosa
                    if (!tarea.fotoDespuesUrl.isNullOrEmpty()) {
                        abrirVisualizador(context, tarea.fotoDespuesUrl)
                    } else {
                        Toast.makeText(
                            context,
                            "Aún no hay foto de respuesta disponible",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // 5. Botones de acción
        binding.btnResponderFoto.setOnClickListener { onResponderClick(tarea) }
        binding.btnEditarTarea.setOnClickListener { onEditarClick(tarea) }
        binding.btnEliminarTarea.setOnClickListener { onEliminarClick(tarea) }
    }

    // ---------------------- AUXILIARES ----------------------

    private fun cargarFoto(
        context: android.content.Context,
        imageView: android.widget.ImageView,
        url: String?
    ) {
        if (!url.isNullOrEmpty()) {
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.camera_icon)
                .error(R.drawable.error_placeholder)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.camera_icon)
        }
    }

    private fun abrirVisualizador(context: android.content.Context, url: String?) {
        if (!url.isNullOrEmpty()) {
            val intent = Intent(context, VisualizadorImagenActivity::class.java).apply {
                putStringArrayListExtra(
                    VisualizadorImagenActivity.EXTRA_IMAGE_URLS,
                    arrayListOf(url)
                )
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No hay foto disponible para mostrar", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun formatearTimestamp(timestamp: Timestamp?): String {
        return if (timestamp != null) {
            dateFormat.format(Date(timestamp.toDate().time))
        } else {
            "—"
        }
    }

    fun actualizarTareas(nuevasTareas: List<Tarea>) {
        tareas = nuevasTareas
        notifyDataSetChanged()
    }

    fun obtenerTareaEnPosicion(position: Int): Tarea? {
        return if (position in tareas.indices) tareas[position] else null
    }
}
