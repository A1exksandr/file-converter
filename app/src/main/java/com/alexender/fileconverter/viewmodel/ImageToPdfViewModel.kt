package com.alexender.fileconverter.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.alexender.fileconverter.model.SelectedImage

class ImageToPdfViewModel : ViewModel() {

    val selectedImages =
        mutableListOf<SelectedImage>()

    fun addImages(uris: List<Uri>) {

        selectedImages.addAll(
            uris.map { uri ->
                SelectedImage(uri)
            }
        )
    }

    fun removeImage(position: Int) {

        if (position in selectedImages.indices) {
            selectedImages.removeAt(position)
        }
    }

    fun moveImage(
        fromPosition: Int,
        toPosition: Int
    ) {

        if (
            fromPosition !in selectedImages.indices ||
            toPosition !in selectedImages.indices
        ) {
            return
        }

        val image =
            selectedImages.removeAt(fromPosition)

        selectedImages.add(
            toPosition,
            image
        )
    }

    fun rotateImage(position: Int) {

        if (position !in selectedImages.indices) {
            return
        }

        val image =
            selectedImages[position]

        image.rotation =
            (image.rotation + 90) % 360
    }

    fun clearImages() {
        selectedImages.clear()
    }
}