package com.alexender.fileconverter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter(
    private val images: MutableList<Uri>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagePreview: ImageView =
            view.findViewById(R.id.imagePreview)

        val imageName: TextView =
            view.findViewById(R.id.imageName)

        val deleteButton: ImageButton =
            view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {
        val uri = images[position]

        holder.imagePreview.setImageURI(uri)

        holder.imageName.text =
            uri.lastPathSegment ?: "Изображение"

        holder.deleteButton.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition

            if (currentPosition != RecyclerView.NO_POSITION) {
                onDeleteClick(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }
}