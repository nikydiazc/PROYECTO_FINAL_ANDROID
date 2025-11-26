package cl.unab.proyecto_final_android

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import cl.unab.proyecto_final_android.databinding.ActivityVisualizadorImagenBinding
import cl.unab.proyecto_final_android.ui.muro.FullScreenImageAdapter

class VisualizadorImagenActivity : AppCompatActivity() {

    private var _binding: ActivityVisualizadorImagenBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val EXTRA_IMAGE_URLS = "extra_image_urls"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityVisualizadorImagenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // Ocultamos progress siempre (la carga real ocurre en el adapter)
        binding.progressBarImage.visibility = View.GONE

        // 1. Recibir URLs
        val imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS)

        if (imageUrls.isNullOrEmpty()) {
            Toast.makeText(this, "No se encontraron imágenes.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Configurar ViewPager2
        val adapter = FullScreenImageAdapter(imageUrls) {
            finish() // Cerrar al tocar la imagen
        }
        binding.viewPagerFullScreen.adapter = adapter

        // 3. Indicador "X / Y"
        if (imageUrls.size > 1) {
            binding.tvImageIndicator.visibility = View.VISIBLE
            binding.tvImageIndicator.text = "1 / ${imageUrls.size}"

            binding.viewPagerFullScreen.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        binding.tvImageIndicator.text = "${position + 1} / ${imageUrls.size}"
                    }
                }
            )
        } else {
            binding.tvImageIndicator.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
