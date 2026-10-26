package com.alexender.fileconverter

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import androidx.activity.viewModels
import com.alexender.fileconverter.viewmodel.ImageToPdfViewModel
import android.content.Intent

private lateinit var pdfNameEditText: EditText
private lateinit var createPdfButton: Button
class MainActivity : AppCompatActivity() {

    private lateinit var selectImagesButton: Button
    private lateinit var selectedImagesText: TextView

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

    private var pendingPdfBytes: ByteArray? = null

    private val savePdfLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->

            if (uri != null) {
                val bytes = pendingPdfBytes ?: return@registerForActivityResult

                contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                }

                Toast.makeText(
                    this,
                    "PDF сохранён",
                    Toast.LENGTH_SHORT
                ).show()

                pendingPdfBytes = null
            }
        }

    private fun createPdfFromImages(): ByteArray {

        val pdfDocument = PdfDocument()

        viewModel.selectedImages.forEachIndexed { index, uri ->

            val inputStream = contentResolver.openInputStream(uri)

            val bitmap = inputStream.use {
                BitmapFactory.decodeStream(it)
            } ?: return@forEachIndexed

            val pageInfo = PdfDocument.PageInfo.Builder(
                bitmap.width,
                bitmap.height,
                index + 1
            ).create()

            val page = pdfDocument.startPage(pageInfo)

            page.canvas.drawBitmap(
                bitmap,
                0f,
                0f,
                null
            )

            pdfDocument.finishPage(page)

            bitmap.recycle()
        }

        val outputStream = ByteArrayOutputStream()

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()

        return outputStream.toByteArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val insetsController =
            WindowInsetsControllerCompat(window, window.decorView)

        insetsController.hide(
            WindowInsetsCompat.Type.navigationBars()
        )

        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        selectImagesButton =
            findViewById(R.id.selectImagesButton)

        selectedImagesText =
            findViewById(R.id.selectedImagesText)

        val imagesRecyclerView =
            findViewById<RecyclerView>(R.id.imagesRecyclerView)

        imageAdapter = ImageAdapter(viewModel.selectedImages) { position ->

            viewModel.removeImage(position)

            imageAdapter.notifyItemRemoved(position)

            updateImagesCount()
        }

        pdfNameEditText =
            findViewById(R.id.pdfNameEditText)

        createPdfButton =
            findViewById(R.id.createPdfButton)

        imagesRecyclerView.layoutManager =
            LinearLayoutManager(this)

        imagesRecyclerView.adapter =
            imageAdapter

        createPdfButton.setOnClickListener {

            if (viewModel.selectedImages.isEmpty()) {
                Toast.makeText(
                    this,
                    "Сначала выберите изображения",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val pdfName = pdfNameEditText.text
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

            val pdfBytes = createPdfFromImages()

            pendingPdfBytes = pdfBytes

            savePdfLauncher.launch("$pdfName.pdf")
        }

        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {

                    val fromPosition = viewHolder.bindingAdapterPosition
                    val toPosition = target.bindingAdapterPosition

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

        itemTouchHelper.attachToRecyclerView(imagesRecyclerView)

        selectImagesButton.setOnClickListener {
            selectImagesLauncher.launch("image/*")
        }

        val intentMessageEditText =
            findViewById<EditText>(R.id.intentMessageEditText)

        val explicitIntentButton =
            findViewById<Button>(R.id.explicitIntentButton)

        val implicitIntentButton =
            findViewById<Button>(R.id.implicitIntentButton)

        explicitIntentButton.setOnClickListener {

            val message = intentMessageEditText.text
                .toString()
                .trim()

            val intent = Intent(
                this,
                PdfToImagesActivity::class.java
            )

            intent.putExtra("message", message)

            startActivity(intent)
        }

        implicitIntentButton.setOnClickListener {

            val message = intentMessageEditText.text
                .toString()
                .trim()

            val intent = Intent(
                "com.alexender.fileconverter.PDF_TO_IMAGES"
            )

            intent.putExtra("message", message)

            startActivity(intent)
        }
    }

    private fun updateImagesCount(){
        selectedImagesText.text =
            if (viewModel.selectedImages.isEmpty()){
                "Изображения не найдены"
            }else{
                "Выбрано изображений: ${viewModel.selectedImages.size}"
            }
    }
}