package com.alexender.fileconverter.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel

class ImageToPdfViewModel : ViewModel() {

    val selectedImages = mutableListOf<Uri>()

    fun addImages(uris: List<Uri>){
        selectedImages.addAll(uris)
    }

    fun removeImage(position: Int){
        if (position in selectedImages.indices){
            selectedImages.removeAt(position)
        }
    }

    fun moveImage(fromPosition: Int, toPosition: Int){
        if(fromPosition !in selectedImages.indices || toPosition !in selectedImages.indices){
            return
        }

        val image = selectedImages.removeAt(fromPosition)
        selectedImages.add(toPosition, image)
    }

    fun clearImages(){
        selectedImages.clear()
    }
}