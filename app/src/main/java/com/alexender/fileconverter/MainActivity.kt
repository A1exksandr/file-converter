package com.alexender.fileconverter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val imageToPdfCard =
            findViewById<MaterialCardView>(R.id.imageToPdfCard)

        val pdfToImagesCard =
            findViewById<MaterialCardView>(R.id.pdfToImagesCard)

        imageToPdfCard.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ImageToPdfActivity::class.java
                )
            )
        }

        pdfToImagesCard.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PdfToImagesActivity::class.java
                )
            )
        }
    }
}