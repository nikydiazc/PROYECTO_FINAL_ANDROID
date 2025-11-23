package cl.unab.proyecto_final_android.ui.muro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.unab.proyecto_final_android.data.TareaRepository

class TareasViewModelFactory(
    private val repository: TareaRepository,
    private val esAdmin: Boolean,
    private val usernameActual: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TareasViewModel::class.java)) {
            return TareasViewModel(repository, esAdmin, usernameActual) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
