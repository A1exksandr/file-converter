package com.alexender.fileconverter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alexender.fileconverter.model.SelectedImage

class ImageAdapter(
    private val images: MutableList<SelectedImage>,
    private val onDeleteClick: (Int) -> Unit,
    private val onRotateClick: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val imagePreview: ImageView =
            view.findViewById(
                R.id.imagePreview
            )

        val imageName: TextView =
            view.findViewById(
                R.id.imageName
            )

        val rotateButton: Button =
            view.findViewById(
                R.id.rotateButton
            )

        val deleteButton: ImageButton =
            view.findViewById(
                R.id.deleteButton
            )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_image,
                    parent,
                    false
                )

        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ImageViewHolder,
        position: Int
    ) {

        val image =
            images[position]

        holder.imagePreview
            .setImageURI(image.uri)

        holder.imagePreview.rotation =
            image.rotation.toFloat()

        holder.imageName.text =
            image.uri.lastPathSegment
                ?: "Изображение"

        holder.rotateButton
            .setOnClickListener {

                val currentPosition =
                    holder.bindingAdapterPosition

                if (
                    currentPosition !=
                    RecyclerView.NO_POSITION
                ) {

                    onRotateClick(
                        currentPosition
                    )
                }
            }

        holder.deleteButton
            .setOnClickListener {

                val currentPosition =
                    holder.bindingAdapterPosition

                if (
                    currentPosition !=
                    RecyclerView.NO_POSITION
                ) {

                    onDeleteClick(
                        currentPosition
                    )
                }
            }
    }

    override fun getItemCount(): Int {
        return images.size
    }
}