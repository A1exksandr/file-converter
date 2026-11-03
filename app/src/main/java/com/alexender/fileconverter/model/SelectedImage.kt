package com.alexender.fileconverter.model

import android.net.Uri

data class SelectedImage(
    val uri: Uri,
    var rotation: Int = 0
)