package com.alexender.fileconverter

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PdfToImagesActivity : AppCompatActivity() {

    private var selectedPdfUri: Uri? = null

    private lateinit var selectedPdfText: TextView
    private lateinit var pageCountText: TextView
    private lateinit var pagesEditText: EditText

    private val selectPdfLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                selectedPdfUri = uri

                selectedPdfText.text =
                    getFileName(uri)

                updatePageCount(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pdf_to_images)

        val intentMessageText =
            findViewById<TextView>(
                R.id.intentMessageText
            )

        selectedPdfText =
            findViewById(
                R.id.selectedPdfText
            )

        pageCountText =
            findViewById(
                R.id.pageCountText
            )

        pagesEditText =
            findViewById(
                R.id.pagesEditText
            )

        val selectPdfButton =
            findViewById<Button>(
                R.id.selectPdfButton
            )

        val convertPdfButton =
            findViewById<Button>(
                R.id.convertPdfButton
            )

        val message =
            intent.getStringExtra("message")
                ?: ""

        if (message.isNotEmpty()) {

            intentMessageText.text =
                "$message (Intent)"

        } else {

            intentMessageText.text =
                "PDF → изображения"
        }

        selectPdfButton.setOnClickListener {

            selectPdfLauncher.launch(
                arrayOf("application/pdf")
            )
        }

        convertPdfButton.setOnClickListener {

            val pdfUri =
                selectedPdfUri

            if (pdfUri == null) {

                Toast.makeText(
                    this,
                    "Сначала выберите PDF",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val pagesText =
                pagesEditText.text
                    .toString()
                    .trim()

            if (pagesText.isEmpty()) {

                Toast.makeText(
                    this,
                    "Введите номера страниц",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val pageCount =
                try {

                    getPdfPageCount(pdfUri)

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Не удалось прочитать PDF",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

            val pages =
                parsePages(
                    pagesText,
                    pageCount
                )

            if (pages == null) {

                Toast.makeText(
                    this,
                    "Некорректный список страниц",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val selectedFormat =
                if (
                    findViewById<RadioButton>(
                        R.id.pngRadioButton
                    ).isChecked
                ) {
                    "PNG"
                } else {
                    "JPEG"
                }

            showConversionDialog(
                pdfUri = pdfUri,
                fileName = getFileName(pdfUri),
                pages = pages,
                format = selectedFormat
            )
        }
    }

    private fun showConversionDialog(
        pdfUri: Uri,
        fileName: String,
        pages: List<Int>,
        format: String
    ) {

        val pagesText =
            pages.joinToString(", ")

        MaterialAlertDialogBuilder(this)
            .setTitle(
                "Преобразовать PDF?"
            )
            .setMessage(
                "Файл: $fileName\n" +
                        "Страницы: $pagesText\n" +
                        "Формат: $format"
            )
            .setNegativeButton(
                "Отмена"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .setPositiveButton(
                "Преобразовать"
            ) { dialog, _ ->

                dialog.dismiss()

                Toast.makeText(
                    this,
                    "Преобразование...",
                    Toast.LENGTH_SHORT
                ).show()

                Thread {

                    try {

                        val savedCount =
                            convertPdfToImages(
                                pdfUri = pdfUri,
                                pages = pages,
                                format = format
                            )

                        runOnUiThread {

                            Toast.makeText(
                                this,
                                "Сохранено изображений: $savedCount",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {

                        runOnUiThread {

                            Toast.makeText(
                                this,
                                "Ошибка: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                }.start()
            }
            .show()
    }

    private fun convertPdfToImages(
        pdfUri: Uri,
        pages: List<Int>,
        format: String
    ): Int {

        var savedCount = 0

        val descriptor =
            contentResolver.openFileDescriptor(
                pdfUri,
                "r"
            )
                ?: throw IllegalStateException(
                    "Не удалось открыть PDF"
                )

        descriptor.use { fileDescriptor ->

            PdfRenderer(
                fileDescriptor
            ).use { renderer ->

                for (pageNumber in pages) {

                    val pageIndex =
                        pageNumber - 1

                    if (
                        pageIndex !in
                        0 until renderer.pageCount
                    ) {
                        continue
                    }

                    renderer
                        .openPage(pageIndex)
                        .use { page ->

                            /*
                             * Рендерим примерно в двойном
                             * разрешении, но ограничиваем
                             * максимальную сторону 3000 px.
                             */
                            val maxDimension =
                                3000f

                            val originalMax =
                                maxOf(
                                    page.width,
                                    page.height
                                ).toFloat()

                            val scale =
                                minOf(
                                    2f,
                                    maxDimension /
                                            originalMax
                                )

                            val width =
                                (
                                        page.width *
                                                scale
                                        )
                                    .toInt()
                                    .coerceAtLeast(1)

                            val height =
                                (
                                        page.height *
                                                scale
                                        )
                                    .toInt()
                                    .coerceAtLeast(1)

                            val bitmap =
                                Bitmap.createBitmap(
                                    width,
                                    height,
                                    Bitmap.Config.ARGB_8888
                                )

                            /*
                             * Белый фон нужен в том числе
                             * для JPEG, потому что JPEG
                             * не поддерживает прозрачность.
                             */
                            Canvas(bitmap)
                                .drawColor(
                                    Color.WHITE
                                )

                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page
                                    .RENDER_MODE_FOR_DISPLAY
                            )

                            saveBitmapToGallery(
                                bitmap = bitmap,
                                pageNumber = pageNumber,
                                format = format
                            )

                            bitmap.recycle()

                            savedCount++
                        }
                }
            }
        }

        return savedCount
    }

    private fun saveBitmapToGallery(
        bitmap: Bitmap,
        pageNumber: Int,
        format: String
    ) {

        val isPng =
            format == "PNG"

        val extension =
            if (isPng) {
                "png"
            } else {
                "jpg"
            }

        val mimeType =
            if (isPng) {
                "image/png"
            } else {
                "image/jpeg"
            }

        val fileName =
            "page_${pageNumber}_" +
                    "${System.currentTimeMillis()}." +
                    extension

        val values =
            ContentValues().apply {

                put(
                    MediaStore.Images.Media
                        .DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.Images.Media
                        .MIME_TYPE,
                    mimeType
                )

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {

                    put(
                        MediaStore.Images.Media
                            .RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/Convectorfiles"
                    )

                    put(
                        MediaStore.Images.Media
                            .IS_PENDING,
                        1
                    )
                }
            }

        val collection =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                MediaStore.Images.Media
                    .getContentUri(
                        MediaStore
                            .VOLUME_EXTERNAL_PRIMARY
                    )

            } else {

                MediaStore.Images.Media
                    .EXTERNAL_CONTENT_URI
            }

        val imageUri =
            contentResolver.insert(
                collection,
                values
            )
                ?: throw IllegalStateException(
                    "Не удалось создать изображение"
                )

        try {

            contentResolver
                .openOutputStream(
                    imageUri
                )
                ?.use { output ->

                    val compressFormat =
                        if (isPng) {

                            Bitmap.CompressFormat.PNG

                        } else {

                            Bitmap.CompressFormat.JPEG
                        }

                    val quality =
                        if (isPng) {
                            100
                        } else {
                            95
                        }

                    val success =
                        bitmap.compress(
                            compressFormat,
                            quality,
                            output
                        )

                    if (!success) {

                        throw IllegalStateException(
                            "Ошибка кодирования изображения"
                        )
                    }

                    output.flush()
                }
                ?: throw IllegalStateException(
                    "Не удалось открыть файл"
                )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.Q
            ) {

                val finishedValues =
                    ContentValues().apply {

                        put(
                            MediaStore.Images.Media
                                .IS_PENDING,
                            0
                        )
                    }

                contentResolver.update(
                    imageUri,
                    finishedValues,
                    null,
                    null
                )
            }

        } catch (e: Exception) {

            contentResolver.delete(
                imageUri,
                null,
                null
            )

            throw e
        }
    }

    private fun getPdfPageCount(
        uri: Uri
    ): Int {

        val descriptor =
            contentResolver
                .openFileDescriptor(
                    uri,
                    "r"
                )
                ?: throw IllegalStateException(
                    "Не удалось открыть PDF"
                )

        descriptor.use { fileDescriptor ->

            PdfRenderer(
                fileDescriptor
            ).use { renderer ->

                return renderer.pageCount
            }
        }
    }

    private fun updatePageCount(
        uri: Uri
    ) {

        try {

            val pageCount =
                getPdfPageCount(uri)

            pageCountText.text =
                "Количество страниц: $pageCount"

        } catch (e: Exception) {

            pageCountText.text =
                "Не удалось прочитать PDF"

            Toast.makeText(
                this,
                "Не удалось прочитать PDF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun parsePages(
        input: String,
        pageCount: Int
    ): List<Int>? {

        val result =
            mutableSetOf<Int>()

        val parts =
            input
                .replace(" ", "")
                .split(",")

        for (part in parts) {

            if (part.isEmpty()) {
                return null
            }

            if (part.contains("-")) {

                val range =
                    part.split("-")

                if (range.size != 2) {
                    return null
                }

                val start =
                    range[0].toIntOrNull()
                        ?: return null

                val end =
                    range[1].toIntOrNull()
                        ?: return null

                if (
                    start < 1 ||
                    end > pageCount ||
                    start > end
                ) {
                    return null
                }

                for (
                page in start..end
                ) {

                    result.add(page)
                }

            } else {

                val page =
                    part.toIntOrNull()
                        ?: return null

                if (
                    page < 1 ||
                    page > pageCount
                ) {
                    return null
                }

                result.add(page)
            }
        }

        return result
            .toList()
            .sorted()
    }

    private fun getFileName(
        uri: Uri
    ): String {

        var fileName =
            "document.pdf"

        contentResolver
            .query(
                uri,
                null,
                null,
                null,
                null
            )
            ?.use { cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (
                    nameIndex >= 0 &&
                    cursor.moveToFirst()
                ) {

                    fileName =
                        cursor.getString(
                            nameIndex
                        )
                }
            }

        return fileName
    }
}