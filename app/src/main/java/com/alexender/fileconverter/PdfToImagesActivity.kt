package com.alexender.fileconverter

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.RadioButton

class PdfToImagesActivity : AppCompatActivity() {

    private var selectedPdfUri: Uri? = null

    private lateinit var selectedPdfText: TextView
    private lateinit var pageCountText: TextView

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
            findViewById<TextView>(R.id.intentMessageText)

        selectedPdfText =
            findViewById(R.id.selectedPdfText)

        pageCountText =
            findViewById(R.id.pageCountText)

        val selectPdfButton =
            findViewById<Button>(R.id.selectPdfButton)

        val convertPdfButton =
            findViewById<Button>(R.id.convertPdfButton)

        val pagesEditText =
            findViewById<EditText>(R.id.pagesEditText)

        val message =
            intent.getStringExtra("message") ?: ""

        intentMessageText.text =
            "$message (Intent)"

        selectPdfButton.setOnClickListener {

            selectPdfLauncher.launch(
                arrayOf("application/pdf")
            )
        }

        convertPdfButton.setOnClickListener {

            val pdfUri = selectedPdfUri

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
                getPdfPageCount(pdfUri)

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
                if (findViewById<RadioButton>(R.id.pngRadioButton).isChecked) {
                    "PNG"
                } else {
                    "JPEG"
                }

            showConversionDialog(
                fileName = getFileName(pdfUri),
                pages = pages,
                format = selectedFormat
            )
        }
    }

    private fun showConversionDialog(
        fileName: String,
        pages: List<Int>,
        format: String
    ) {

        val pagesText =
            pages.joinToString(", ")

        MaterialAlertDialogBuilder(this)
            .setTitle("Преобразовать PDF?")
            .setMessage(
                "Файл: $fileName\n" +
                        "Страницы: $pagesText\n" +
                        "Формат: $format"
            )
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Преобразовать") { dialog, _ ->

                Toast.makeText(
                    this,
                    "Запуск преобразования в $format",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()

                // Здесь следующим этапом будет настоящее PDF → PNG/JPEG
            }
            .show()
    }
    private fun getPdfPageCount(uri: Uri): Int {

        val descriptor =
            contentResolver.openFileDescriptor(
                uri,
                "r"
            ) ?: return 0

        val renderer =
            PdfRenderer(descriptor)

        val count =
            renderer.pageCount

        renderer.close()
        descriptor.close()

        return count
    }
    private fun getFileName(uri: Uri): String {

        var fileName = "document.pdf"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val nameIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (nameIndex >= 0) {
                    fileName =
                        cursor.getString(nameIndex)
                }
            }
        }

        return fileName
    }

    private fun updatePageCount(uri: Uri) {

        try {

            val count =
                getPdfPageCount(uri)

            pageCountText.text =
                "Количество страниц: $count"

        } catch (e: Exception) {

            pageCountText.text =
                "Не удалось прочитать PDF"

            Toast.makeText(
                this,
                "Ошибка открытия PDF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun parsePages(
        input: String,
        pageCount: Int
    ): List<Int>? {

        val result = mutableSetOf<Int>()

        val parts = input
            .replace(" ", "")
            .split(",")

        for (part in parts) {

            if (part.isEmpty()) {
                return null
            }

            if ("-" in part) {

                val rangeParts = part.split("-")

                if (rangeParts.size != 2) {
                    return null
                }

                val start =
                    rangeParts[0].toIntOrNull()
                        ?: return null

                val end =
                    rangeParts[1].toIntOrNull()
                        ?: return null

                if (
                    start < 1 ||
                    end > pageCount ||
                    start > end
                ) {
                    return null
                }

                for (page in start..end) {
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

        return result.sorted()
    }
}