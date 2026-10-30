package com.alexender.fileconverter

import android.graphics.RectF
import android.widget.RadioGroup
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexender.fileconverter.viewmodel.ImageToPdfViewModel

class ImageToPdfActivity : AppCompatActivity() {

    private enum class PageOrientation {
        AUTO,
        PORTRAIT,
        LANDSCAPE
    }
    private fun getSelectedOrientation(): PageOrientation {

        val orientationGroup =
            findViewById<RadioGroup>(R.id.orientationRadioGroup)

        return when (orientationGroup.checkedRadioButtonId) {

            R.id.orientationPortrait ->
                PageOrientation.PORTRAIT

            R.id.orientationLandscape ->
                PageOrientation.LANDSCAPE

            else ->
                PageOrientation.AUTO
        }
    }
    private lateinit var selectImagesButton: Button
    private lateinit var selectedImagesText: TextView
    private lateinit var pdfNameEditText: EditText
    private lateinit var createPdfButton: Button

    private lateinit var imageAdapter: ImageAdapter

    private val viewModel: ImageToPdfViewModel by viewModels()

    private val selectImagesLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->

            if (uris.isNotEmpty()) {

                viewModel.addImages(uris)

                imageAdapter.notifyDataSetChanged()

                updateImagesCount()
            }
        }

    private val savePdfLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            try {

                createPdfFromImages(uri)

                Toast.makeText(
                    this,
                    "PDF успешно сохранён",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Ошибка создания PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_to_pdf)

        selectImagesButton =
            findViewById(R.id.selectImagesButton)

        selectedImagesText =
            findViewById(R.id.selectedImagesText)

        pdfNameEditText =
            findViewById(R.id.pdfNameEditText)

        createPdfButton =
            findViewById(R.id.createPdfButton)

        val imagesRecyclerView =
            findViewById<RecyclerView>(R.id.imagesRecyclerView)

        imageAdapter =
            ImageAdapter(viewModel.selectedImages) { position ->

                viewModel.removeImage(position)

                imageAdapter.notifyItemRemoved(position)

                updateImagesCount()
            }

        imagesRecyclerView.layoutManager =
            LinearLayoutManager(this)

        imagesRecyclerView.adapter =
            imageAdapter

        val itemTouchHelper =
            ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    0
                ) {

                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {

                        val fromPosition =
                            viewHolder.bindingAdapterPosition

                        val toPosition =
                            target.bindingAdapterPosition

                        if (
                            fromPosition == RecyclerView.NO_POSITION ||
                            toPosition == RecyclerView.NO_POSITION
                        ) {
                            return false
                        }

                        viewModel.moveImage(
                            fromPosition,
                            toPosition
                        )

                        imageAdapter.notifyItemMoved(
                            fromPosition,
                            toPosition
                        )

                        return true
                    }

                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int
                    ) {
                    }
                }
            )

        itemTouchHelper.attachToRecyclerView(
            imagesRecyclerView
        )

        selectImagesButton.setOnClickListener {
            selectImagesLauncher.launch("image/*")
        }

        createPdfButton.setOnClickListener {

            if (viewModel.selectedImages.isEmpty()) {

                Toast.makeText(
                    this,
                    "Сначала выберите изображения",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val pdfName =
                pdfNameEditText.text
                    .toString()
                    .trim()

            if (pdfName.isEmpty()) {

                Toast.makeText(
                    this,
                    "Введите имя PDF",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            savePdfLauncher.launch(
                "$pdfName.pdf"
            )
        }

        updateImagesCount()
    }

    private fun createPdfFromImages(
        outputUri: Uri
    ) {

        val pdfDocument = PdfDocument()

        try {

            var pageNumber = 1

            for (imageUri in viewModel.selectedImages) {

                val bitmap =
                    contentResolver
                        .openInputStream(imageUri)
                        ?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                        ?: continue

                val orientation =
                    getSelectedOrientation()

                val pageWidth: Int
                val pageHeight: Int

                when (orientation) {

                    PageOrientation.AUTO -> {
                        pageWidth = bitmap.width
                        pageHeight = bitmap.height
                    }

                    PageOrientation.PORTRAIT -> {
                        pageWidth =
                            minOf(
                                bitmap.width,
                                bitmap.height
                            )

                        pageHeight =
                            maxOf(
                                bitmap.width,
                                bitmap.height
                            )
                    }

                    PageOrientation.LANDSCAPE -> {
                        pageWidth =
                            maxOf(
                                bitmap.width,
                                bitmap.height
                            )

                        pageHeight =
                            minOf(
                                bitmap.width,
                                bitmap.height
                            )
                    }
                }

                val pageInfo =
                    PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        pageNumber
                    ).create()

                val page =
                    pdfDocument.startPage(
                        pageInfo
                    )

                val scale =
                    minOf(
                        pageWidth.toFloat() / bitmap.width,
                        pageHeight.toFloat() / bitmap.height
                    )

                val imageWidth =
                    bitmap.width * scale

                val imageHeight =
                    bitmap.height * scale

                val left =
                    (pageWidth - imageWidth) / 2f

                val top =
                    (pageHeight - imageHeight) / 2f

                val destination =
                    RectF(
                        left,
                        top,
                        left + imageWidth,
                        top + imageHeight
                    )

                page.canvas.drawBitmap(
                    bitmap,
                    null,
                    destination,
                    null
                )

                pdfDocument.finishPage(page)

                bitmap.recycle()

                pageNumber++
            }

            contentResolver
                .openOutputStream(
                    outputUri,
                    "w"
                )
                ?.use { output ->

                    pdfDocument.writeTo(output)

                    output.flush()
                }
                ?: throw IllegalStateException(
                    "Не удалось открыть файл"
                )

        } finally {

            pdfDocument.close()
        }
    }
    private fun updateImagesCount() {

        selectedImagesText.text =
            if (viewModel.selectedImages.isEmpty()) {

                "Изображения не выбраны"

            } else {

                "Выбрано изображений: " +
                        viewModel.selectedImages.size
            }
    }
}