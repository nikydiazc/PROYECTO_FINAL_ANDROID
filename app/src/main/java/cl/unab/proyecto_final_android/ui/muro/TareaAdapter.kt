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
        RecyclerView.ViewHolder(binding.root) {

        // true  -> se está mostrando el RESULTADO (imgRespuesta)
        // false -> se está mostrando el ANTES (imgTarea)
        var mostrandoResultado: Boolean = true
    }

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

        // 1) Datos básicos
        binding.tvDescripcionTarea.text = tarea.descripcion
        binding.tvUbicacionTarea.text = "Ubicación: ${tarea.ubicacion}"
        binding.tvPisoValor.text = "Piso: ${tarea.piso}"
        binding.tvAsignadaA.text =
            "Asignada a: ${if (tarea.asignadaA.isNullOrEmpty()) "—" else tarea.asignadaA}"
        binding.tvFechaCreacion.text = "Creada: ${formatearTimestamp(tarea.fechaCreacion)}"

        // Estado + fondo con ColorStatus
        binding.tvEstadoTarea.text = "Estado: ${tarea.estado}"
        binding.tvEstadoTarea.setBackgroundResource(
            ColorStatus.getColorResource(tarea.estado ?: "")
        )

        // 2) Fecha de respuesta según estado
        when (tarea.estado) {
            "Realizada" -> {
                binding.tvFechaRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.text =
                    "Realizada: ${formatearTimestamp(tarea.fechaRespuesta)}"
            }
            "Rechazada" -> {
                binding.tvFechaRespuesta.visibility = View.VISIBLE
                binding.tvFechaRespuesta.text =
                    "Rechazada: ${formatearTimestamp(tarea.fechaRespuesta)}"
            }
            else -> {
                binding.tvFechaRespuesta.visibility = View.GONE
            }
        }

        // 3) Visibilidad de botones según rol
        val puedeAdministrar = esAdmin
        binding.btnEditarTarea.visibility =
            if (puedeAdministrar) View.VISIBLE else View.GONE
        binding.btnEliminarTarea.visibility =
            if (puedeAdministrar) View.VISIBLE else View.GONE

        val puedeResponder = rolUsuario != LoginActivity.ROL_CREAR &&
                !tarea.estado.equals("Realizada", ignoreCase = true) &&
                !tarea.estado.equals("Rechazada", ignoreCase = true)

        val asignadaAOtroCuandoSoyRealizar =
            !tarea.asignadaA.isNullOrEmpty() &&
                    tarea.asignadaA != usernameActual &&
                    rolUsuario == LoginActivity.ROL_REALIZAR

        binding.btnResponderFoto.visibility =
            if (puedeResponder && !asignadaAOtroCuandoSoyRealizar)
                View.VISIBLE
            else
                View.GONE

        // 4) Configurar fotos (antes / resultado) y alternar con TAP
        configurarFotos(holder, tarea)

        // 5) Botones de acción
        binding.btnResponderFoto.setOnClickListener { onResponderClick(tarea) }
        binding.btnEditarTarea.setOnClickListener { onEditarClick(tarea) }
        binding.btnEliminarTarea.setOnClickListener { onEliminarClick(tarea) }
    }

    // ---------------------- FOTOS: ANTES / DESPUÉS ----------------------

    private fun configurarFotos(holder: TareaViewHolder, tarea: Tarea) {
        val binding = holder.binding
        val context = binding.root.context

        // imgTarea  -> ANTES
        // imgRespuesta -> RESULTADO / DESPUÉS
        cargarFoto(context, binding.imgTarea, tarea.fotoAntesUrl)
        cargarFoto(context, binding.imgRespuesta, tarea.fotoDespuesUrl ?: tarea.fotoAntesUrl)

        if (tarea.estado.equals("Realizada", true) ||
            tarea.estado.equals("Rechazada", true)
        ) {
            // En realizadas/rechazadas:
            // 👉 primero mostrar el RESULTADO (imgRespuesta)
            holder.mostrandoResultado = true
            binding.imgRespuesta.visibility = View.VISIBLE
            binding.imgTarea.visibility = View.GONE

            // Un tap alterna entre resultado ↔ antes
            binding.imgTarea.setOnClickListener {
                alternarFotos(holder)
            }
            binding.imgRespuesta.setOnClickListener {
                alternarFotos(holder)
            }

            // Long click abre visualizador a pantalla completa
            binding.imgTarea.setOnLongClickListener {
                abrirVisualizador(context, tarea.fotoAntesUrl)
                true
            }
            binding.imgRespuesta.setOnLongClickListener {
                abrirVisualizador(context, tarea.fotoDespuesUrl ?: tarea.fotoAntesUrl)
                true
            }

        } else {
            // Otros estados: solo mostramos ANTES
            holder.mostrandoResultado = false
            binding.imgRespuesta.visibility = View.GONE
            binding.imgTarea.visibility = View.VISIBLE

            // Tap abre visualizador directamente
            binding.imgTarea.setOnClickListener {
                abrirVisualizador(context, tarea.fotoAntesUrl)
            }
            binding.imgRespuesta.setOnClickListener(null)
            binding.imgTarea.setOnLongClickListener(null)
            binding.imgRespuesta.setOnLongClickListener(null)
        }
    }

    private fun alternarFotos(holder: TareaViewHolder) {
        val binding = holder.binding

        if (holder.mostrandoResultado) {
            // Pasar de RESULTADO → ANTES
            binding.imgRespuesta.visibility = View.GONE
            binding.imgTarea.visibility = View.VISIBLE
            holder.mostrandoResultado = false
        } else {
            // Volver de ANTES → RESULTADO
            binding.imgTarea.visibility = View.GONE
            binding.imgRespuesta.visibility = View.VISIBLE
            holder.mostrandoResultado = true
        }
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

    // ---------------------- PÚBLICOS ----------------------

    fun actualizarTareas(nuevasTareas: List<Tarea>) {
        tareas = nuevasTareas
        notifyDataSetChanged()
    }

    fun obtenerTareaEnPosicion(position: Int): Tarea? {
        return if (position in tareas.indices) tareas[position] else null
    }
}

