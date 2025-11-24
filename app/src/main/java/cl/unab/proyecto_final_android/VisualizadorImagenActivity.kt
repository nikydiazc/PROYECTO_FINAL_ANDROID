package cl.unab.proyecto_final_android

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import cl.unab.proyecto_final_android.databinding.ActivityVisualizadorImagenBinding
import cl.unab.proyecto_final_android.ui.muro.FullScreenImageAdapter


class VisualizadorImagenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisualizadorImagenBinding

    companion object {
        const val EXTRA_IMAGE_URLS = "extra_image_urls"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisualizadorImagenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // 1. Recibimos el array de URLs
        val imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS)

        if (!imageUrls.isNullOrEmpty()) {

            // Ocultamos la ProgressBar
            binding.progressBarImage.visibility = View.GONE

            // 2. Configurar el ViewPager2 con el nuevo adaptador
            val adapter = FullScreenImageAdapter(imageUrls) {
                // Lambda para manejar el clic en la imagen
                finish()
            }
            binding.viewPagerFullScreen.adapter = adapter

            // 3. Configurar el indicador (opcional)
            if (imageUrls.size > 1) {
                binding.tvImageIndicator.visibility = View.VISIBLE
                binding.tvImageIndicator.text = "1 / ${imageUrls.size}"

                binding.viewPagerFullScreen.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        binding.tvImageIndicator.text = "${position + 1} / ${imageUrls.size}"
                    }
                })
            }

        } else {
            Toast.makeText(this, "No se encontraron URLs de imagen.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}