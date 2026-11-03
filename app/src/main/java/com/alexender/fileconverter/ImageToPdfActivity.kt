package com.alexender.fileconverter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexender.fileconverter.model.SelectedImage
import com.alexender.fileconverter.viewmodel.ImageToPdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageToPdfActivity : AppCompatActivity() {

    private enum class PageOrientation {
        AUTO,
        PORTRAIT,
        LANDSCAPE
    }

    private lateinit var selectImagesButton: Button
    private lateinit var selectedImagesText: TextView
    private lateinit var pdfNameEditText: EditText
    private lateinit var createPdfButton: Button

    private lateinit var orientationRadioGroup: RadioGroup

    private lateinit var progressContainer: View
    private lateinit var progressText: TextView
    private lateinit var pdfProgressBar: ProgressBar

    private lateinit var imagesRecyclerView: RecyclerView
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
                updateRecyclerViewHeight()
            }
        }

    private val savePdfLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/pdf"
            )
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            createPdfInBackground(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_image_to_pdf
        )

        selectImagesButton =
            findViewById(
                R.id.selectImagesButton
            )

        selectedImagesText =
            findViewById(
                R.id.selectedImagesText
            )

        pdfNameEditText =
            findViewById(
                R.id.pdfNameEditText
            )

        createPdfButton =
            findViewById(
                R.id.createPdfButton
            )

        orientationRadioGroup =
            findViewById(
                R.id.orientationRadioGroup
            )

        progressContainer =
            findViewById(
                R.id.progressContainer
            )

        progressText =
            findViewById(
                R.id.progressText
            )

        pdfProgressBar =
            findViewById(
                R.id.pdfProgressBar
            )

        imagesRecyclerView =
            findViewById(
                R.id.imagesRecyclerView
            )

        setupRecyclerView()

        selectImagesButton.setOnClickListener {

            selectImagesLauncher.launch(
                "image/*"
            )
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
                pdfNameEditText
                    .text
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
        updateRecyclerViewHeight()
    }

    private fun setupRecyclerView() {

        imageAdapter =
            ImageAdapter(
                images = viewModel.selectedImages,

                onDeleteClick = { position ->

                    viewModel.removeImage(
                        position
                    )

                    imageAdapter.notifyItemRemoved(
                        position
                    )

                    updateImagesCount()
                    updateRecyclerViewHeight()
                },

                onRotateClick = { position ->

                    viewModel.rotateImage(
                        position
                    )

                    imageAdapter.notifyItemChanged(
                        position
                    )
                }
            )

        imagesRecyclerView.layoutManager =
            LinearLayoutManager(this)

        imagesRecyclerView.adapter =
            imageAdapter

        val itemTouchHelper =
            ItemTouchHelper(
                object :
                    ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP or
                                ItemTouchHelper.DOWN,
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
                            fromPosition ==
                            RecyclerView.NO_POSITION ||
                            toPosition ==
                            RecyclerView.NO_POSITION
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
                        // Свайп не используется.
                    }
                }
            )

        itemTouchHelper.attachToRecyclerView(
            imagesRecyclerView
        )
    }

    private fun updateRecyclerViewHeight() {

        /*
         * Примерная высота одного элемента item_image.xml.
         */
        val itemHeightDp = 80

        /*
         * После четырёх элементов список больше не растёт,
         * а начинает прокручиваться самостоятельно.
         */
        val maxHeightDp = 320

        val density =
            resources.displayMetrics.density

        val itemCount =
            viewModel.selectedImages.size

        val desiredHeightDp =
            itemHeightDp * itemCount

        val finalHeightDp =
            minOf(
                desiredHeightDp,
                maxHeightDp
            )

        val layoutParams =
            imagesRecyclerView.layoutParams

        layoutParams.height =
            (finalHeightDp * density)
                .toInt()

        imagesRecyclerView.layoutParams =
            layoutParams

        /*
         * Если элементов мало, внутренняя прокрутка
         * вообще не нужна.
         */
        imagesRecyclerView.isNestedScrollingEnabled =
            itemCount > 4
    }

    private fun getSelectedOrientation():
            PageOrientation {

        return when (
            orientationRadioGroup
                .checkedRadioButtonId
        ) {

            R.id.orientationPortrait ->
                PageOrientation.PORTRAIT

            R.id.orientationLandscape ->
                PageOrientation.LANDSCAPE

            else ->
                PageOrientation.AUTO
        }
    }

    private fun createPdfInBackground(
        outputUri: Uri
    ) {

        val imagesSnapshot =
            viewModel.selectedImages.map {
                it.copy()
            }

        val orientation =
            getSelectedOrientation()

        val total =
            imagesSnapshot.size

        progressContainer.visibility =
            View.VISIBLE

        pdfProgressBar.max =
            total

        pdfProgressBar.progress =
            0

        progressText.text =
            "Создание PDF: 0 / $total"

        createPdfButton.isEnabled =
            false

        selectImagesButton.isEnabled =
            false

        lifecycleScope.launch {

            try {

                createPdfFromImages(
                    outputUri = outputUri,
                    images = imagesSnapshot,
                    orientation = orientation
                ) { current ->

                    pdfProgressBar.progress =
                        current

                    progressText.text =
                        "Создание PDF: $current / $total"
                }

                Toast.makeText(
                    this@ImageToPdfActivity,
                    "PDF успешно сохранён",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@ImageToPdfActivity,
                    "Ошибка создания PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                createPdfButton.isEnabled =
                    true

                selectImagesButton.isEnabled =
                    true
            }
        }
    }

    private suspend fun createPdfFromImages(
        outputUri: Uri,
        images: List<SelectedImage>,
        orientation: PageOrientation,
        onProgress: (Int) -> Unit
    ) {

        withContext(Dispatchers.IO) {

            val pdfDocument =
                PdfDocument()

            try {

                var pageNumber = 1
                var processedCount = 0

                for (selectedImage in images) {

                    val bitmap =
                        loadBitmapWithCorrectOrientation(
                            uri =
                                selectedImage.uri,

                            manualRotation =
                                selectedImage.rotation
                        )
                            ?: throw
                            IllegalStateException(
                                "Не удалось прочитать изображение $pageNumber"
                            )

                    try {

                        val pageWidth: Int
                        val pageHeight: Int

                        when (orientation) {

                            PageOrientation.AUTO -> {

                                pageWidth =
                                    bitmap.width

                                pageHeight =
                                    bitmap.height
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

                        page.canvas.drawColor(
                            Color.WHITE
                        )

                        val scale =
                            minOf(
                                pageWidth.toFloat() /
                                        bitmap.width,

                                pageHeight.toFloat() /
                                        bitmap.height
                            )

                        val imageWidth =
                            bitmap.width *
                                    scale

                        val imageHeight =
                            bitmap.height *
                                    scale

                        val left =
                            (
                                    pageWidth -
                                            imageWidth
                                    ) / 2f

                        val top =
                            (
                                    pageHeight -
                                            imageHeight
                                    ) / 2f

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

                        pdfDocument.finishPage(
                            page
                        )

                    } finally {

                        bitmap.recycle()
                    }

                    pageNumber++
                    processedCount++

                    withContext(
                        Dispatchers.Main
                    ) {

                        onProgress(
                            processedCount
                        )
                    }
                }

                contentResolver
                    .openOutputStream(
                        outputUri,
                        "w"
                    )
                    ?.use { output ->

                        pdfDocument.writeTo(
                            output
                        )

                        output.flush()
                    }
                    ?: throw
                    IllegalStateException(
                        "Не удалось открыть PDF для записи"
                    )

            } finally {

                pdfDocument.close()
            }
        }
    }

    private fun loadBitmapWithCorrectOrientation(
        uri: Uri,
        manualRotation: Int
    ): Bitmap? {

        val bitmap =
            contentResolver
                .openInputStream(uri)
                ?.use { input ->

                    BitmapFactory.decodeStream(
                        input
                    )
                }
                ?: return null

        val exifRotation =
            try {

                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        ExifInterface(input)
                            .rotationDegrees
                    }
                    ?: 0

            } catch (_: Exception) {

                0
            }

        val totalRotation =
            (
                    exifRotation +
                            manualRotation
                    ) % 360

        if (totalRotation == 0) {

            return bitmap
        }

        val matrix =
            Matrix().apply {

                postRotate(
                    totalRotation.toFloat()
                )
            }

        val rotatedBitmap =
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )

        if (
            rotatedBitmap !== bitmap
        ) {

            bitmap.recycle()
        }

        return rotatedBitmap
    }

    private fun updateImagesCount() {

        selectedImagesText.text =
            if (
                viewModel.selectedImages.isEmpty()
            ) {

                "Изображения не выбраны"

            } else {

                "Выбрано изображений: " +
                        viewModel.selectedImages.size
            }
    }
}