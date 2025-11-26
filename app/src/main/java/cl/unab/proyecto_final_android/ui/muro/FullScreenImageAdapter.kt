package cl.unab.proyecto_final_android.ui.muro

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import cl.unab.proyecto_final_android.R
import com.github.chrisbanes.photoview.PhotoView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class FullScreenImageAdapter(
    private val urls: List<String>,
    private val clickListener: () -> Unit
) : RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.fullScreenImageViewItem)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarImageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fullscreen_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount(): Int = urls.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = urls[position]

        // Tap en la imagen → cerrar Activity
        holder.photoView.setOnViewTapListener { _, _, _ ->
            clickListener()
        }

        holder.progressBar.visibility = View.VISIBLE

        Glide.with(holder.itemView.context)
            .load(url)
            .listener(object : RequestListener<Drawable> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressBar.visibility = View.GONE
                    // devolver false para que Glide siga con el placeholder/error
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressBar.visibility = View.GONE
                    // devolver false para que Glide pinte la imagen normalmente
                    return false
                }
            })
            .placeholder(R.drawable.camera_icon)
            .error(R.drawable.error_placeholder)
            .into(holder.photoView)
    }
}

