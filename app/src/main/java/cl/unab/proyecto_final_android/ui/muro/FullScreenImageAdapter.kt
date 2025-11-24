package cl.unab.proyecto_final_android.ui.muro

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class FullScreenImageAdapter(
    // Lista de URLs (Después y Antes, si aplica)
    private val urls: List<String>,
    // Callback para que la Activity sepa cuándo cerrar
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder>() {

    // El ViewHolder contiene la ImageView y la ProgressBar para cada página
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Los IDs deben coincidir con los definidos en item_fullscreen_image.xml
        val imageView: ImageView = itemView.findViewById(R.id.fullScreenImageViewItem)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarImageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        // Inflamos el layout de cada página (item_fullscreen_image.xml)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fullscreen_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = urls[position]

        // 1. Configurar listener de clic para cerrar la Activity
        holder.imageView.setOnClickListener { clickListener() }

        // 2. Mostrar ProgressBar y cargar imagen con Glide
        holder.progressBar.visibility = View.VISIBLE

        Glide.with(holder.itemView.context)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                // Al fallar la carga
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                    holder.progressBar.visibility = View.GONE
                    // Opcional: mostrar un Toast o un placeholder de error
                    return false
                }

                // Al cargar con éxito
                override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    holder.progressBar.visibility = View.GONE
                    return false
                }
            })
            // Asegúrate de que la ImageView está centrada y lista
            .into(holder.imageView)
    }
}