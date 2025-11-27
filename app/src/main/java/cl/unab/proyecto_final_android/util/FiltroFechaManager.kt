package cl.unab.proyecto_final_android.util

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import cl.unab.proyecto_final_android.databinding.ActivityMuroTareasBinding
import cl.unab.proyecto_final_android.ui.muro.TareasViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FiltroFechaManager(
    private val context: Context,
    private val binding: ActivityMuroTareasBinding,
    private val viewModel: TareasViewModel
) {

    private var fechaSeleccionadaDesde: Calendar? = null
    private var fechaSeleccionadaHasta: Calendar? = null

    init {
        configurarEventosFiltroFecha()
    }

    private fun configurarEventosFiltroFecha() {
        binding.tvFechaDesde.setOnClickListener { mostrarDatePicker(esFechaDesde = true) }
        binding.tvFechaHasta.setOnClickListener { mostrarDatePicker(esFechaDesde = false) }

        binding.btnAplicarFiltroFecha.setOnClickListener { aplicarFiltroDeFechas() }
        binding.btnLimpiarFiltroFecha.setOnClickListener { limpiarTodosLosFiltrosUI() }
    }

    private fun mostrarDatePicker(esFechaDesde: Boolean) {
        val calendario = Calendar.getInstance()
        val fechaInicial = if (esFechaDesde) fechaSeleccionadaDesde else fechaSeleccionadaHasta
        fechaInicial?.let { calendario.timeInMillis = it.timeInMillis }

        val picker = DatePickerDialog(
            context,
            { _, year, month, day ->
                val nuevaFecha = Calendar.getInstance()
                nuevaFecha.set(year, month, day, 0, 0, 0)
                nuevaFecha.set(Calendar.MILLISECOND, 0)

                if (esFechaDesde) {
                    fechaSeleccionadaDesde = nuevaFecha
                    val formateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(nuevaFecha.time)

                    binding.tvFechaDesde.text = "Desde:\n$formateada"
                } else {
                    fechaSeleccionadaHasta = nuevaFecha
                    val formateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(nuevaFecha.time)

                    binding.tvFechaHasta.text = "Hasta:\n$formateada"
                }

            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        picker.show()
    }

    private fun aplicarFiltroDeFechas() {
        val desde = fechaSeleccionadaDesde?.let { Timestamp(it.time) }
        val hasta = fechaSeleccionadaHasta?.let {
            val finDia = Calendar.getInstance().apply {
                timeInMillis = it.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            Timestamp(finDia.time)
        }

        if (desde != null && hasta != null && desde.compareTo(hasta) > 0) {
            Toast.makeText(context, "Fecha 'Desde' mayor a 'Hasta'", Toast.LENGTH_SHORT).show()
            return
        }

        // Ahora usa el método nuevo
        viewModel.aplicarFiltroFechas(desde, hasta)
    }

    private fun limpiarTodosLosFiltrosUI() {
        // 1. Limpiar Variables de Fecha
        fechaSeleccionadaDesde = null
        fechaSeleccionadaHasta = null

        // 2. Limpiar UI (usamos siempre el mismo formato)
        binding.tvFechaDesde.text = ""
        binding.tvFechaDesde.hint = "Desde: DD/MM/AAAA"

        binding.tvFechaHasta.text = ""
        binding.tvFechaHasta.hint = "Hasta: DD/MM/AAAA"

        binding.etBuscarDescripcionOUbicacion.setText("")

        // Volver spinners a "Todos"
        binding.spFiltroPiso.setSelection(0)
        binding.spFiltroSupervisor.setSelection(0)

        // 3. Llamar al ViewModel para resetear lógica
        viewModel.limpiarTodosLosFiltros()
        Toast.makeText(context, "Todos los filtros restablecidos", Toast.LENGTH_SHORT).show()
    }

}
