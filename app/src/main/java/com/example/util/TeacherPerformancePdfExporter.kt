package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.ModulAjarProgressStatus
import com.example.model.SchoolSupervisionOverview
import com.example.model.TeacherSupervisionSummary
import com.example.model.User
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to generate and export official Teacher Performance and Supervision
 * reports in PDF format for the School Principal (Kepala Sekolah).
 */
object TeacherPerformancePdfExporter {

    // A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT // 523f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 800f

    data class ExportOptions(
        val schoolName: String,
        val principalName: String,
        val principalNip: String = "",
        val notes: String = "",
        val filterLabel: String = "Semua Guru"
    )

    /**
     * Generates a PDF document from the supervision summary data and writes it to app cache.
     * Returns the created [File].
     */
    fun generatePdf(
        context: Context,
        options: ExportOptions,
        schoolOverview: SchoolSupervisionOverview,
        summaries: List<TeacherSupervisionSummary>
    ): File {
        val document = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        val currentDateStr = dateFormat.format(Date())

        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }

        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedSchoolName = options.schoolName.replace("[^a-zA-Z0-9]".toRegex(), "_").take(20)
        val fileName = "Laporan_Supervisi_Guru_${sanitizedSchoolName}_$fileTimestamp.pdf"
        val outputFile = File(reportsDir, fileName)

        var pageNumber = 1
        var currentPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = currentPage.canvas

        // Render First Page Header and KPI Summary
        var currentY = MARGIN_TOP
        currentY = drawHeader(canvas, paint, options, currentDateStr, currentY)
        currentY = drawKpiSummary(canvas, paint, schoolOverview, summaries.size, options.filterLabel, currentY)

        // Draw Table Header
        currentY = drawTableHeader(canvas, paint, currentY)

        val rowHeight = 32f
        val signatureHeight = 120f

        for ((index, item) in summaries.withIndex()) {
            // Check if row exceeds printable page limit
            if (currentY + rowHeight > MARGIN_BOTTOM - 40f) {
                drawPageFooter(canvas, paint, pageNumber)
                document.finishPage(currentPage)

                pageNumber++
                currentPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = currentPage.canvas

                currentY = MARGIN_TOP
                currentY = drawContinuationHeader(canvas, paint, options.schoolName, pageNumber, currentY)
                currentY = drawTableHeader(canvas, paint, currentY)
            }

            drawTableRow(canvas, paint, index + 1, item, currentY, rowHeight)
            currentY += rowHeight
        }

        // Draw Principal Notes if provided
        if (options.notes.isNotBlank()) {
            if (currentY + 50f > MARGIN_BOTTOM - signatureHeight) {
                drawPageFooter(canvas, paint, pageNumber)
                document.finishPage(currentPage)

                pageNumber++
                currentPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = currentPage.canvas
                currentY = MARGIN_TOP + 10f
            }
            currentY = drawNotesSection(canvas, paint, options.notes, currentY)
        }

        // Check if there is enough space for the signature block
        if (currentY + signatureHeight > MARGIN_BOTTOM - 20f) {
            drawPageFooter(canvas, paint, pageNumber)
            document.finishPage(currentPage)

            pageNumber++
            currentPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = currentPage.canvas
            currentY = MARGIN_TOP + 20f
        }

        drawSignatureBlock(canvas, paint, options, currentY)
        drawPageFooter(canvas, paint, pageNumber)

        document.finishPage(currentPage)

        // Write to file
        FileOutputStream(outputFile).use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()

        return outputFile
    }

    /**
     * Draws the main formal header of the report on the first page.
     */
    private fun drawHeader(
        canvas: Canvas,
        paint: Paint,
        options: ExportOptions,
        currentDateStr: String,
        startY: Float
    ): Float {
        var y = startY

        // Decorative top bar
        paint.color = Color.rgb(0, 105, 92) // Principal Teal
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + 4f, paint)
        y += 18f

        // Title
        paint.color = Color.rgb(20, 30, 45)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 15f
        canvas.drawText("LAPORAN SUPERVISI & EVALUASI KINERJA DEWAN GURU", MARGIN_LEFT, y, paint)
        y += 16f

        // School Name Subtitle
        paint.color = Color.rgb(0, 105, 92)
        paint.textSize = 12f
        val schoolDisplayName = options.schoolName.ifBlank { "Sekolah Terdaftar" }.uppercase(Locale.ROOT)
        canvas.drawText(schoolDisplayName, MARGIN_LEFT, y, paint)
        y += 14f

        // Metadata: Date and Signee
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        val metaInfo = "Waktu Penerbitan: $currentDateStr  •  Penanggung Jawab: ${options.principalName}"
        canvas.drawText(metaInfo, MARGIN_LEFT, y, paint)
        y += 12f

        // Separator
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paint)
        paint.style = Paint.Style.FILL
        y += 14f

        return y
    }

    /**
     * Draws continuation header on subsequent pages.
     */
    private fun drawContinuationHeader(
        canvas: Canvas,
        paint: Paint,
        schoolName: String,
        pageNumber: Int,
        startY: Float
    ): Float {
        var y = startY
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        paint.textSize = 8f
        canvas.drawText("Laporan Supervisi Kinerja Guru — $schoolName (Lanjutan Halaman $pageNumber)", MARGIN_LEFT, y, paint)
        y += 8f

        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paint)
        paint.style = Paint.Style.FILL
        y += 12f

        return y
    }

    /**
     * Draws executive KPI summary cards on the first page.
     */
    private fun drawKpiSummary(
        canvas: Canvas,
        paint: Paint,
        overview: SchoolSupervisionOverview,
        teacherCount: Int,
        filterLabel: String,
        startY: Float
    ): Float {
        var y = startY
        val cardHeight = 58f
        val cardRect = RectF(MARGIN_LEFT, y, MARGIN_RIGHT, y + cardHeight)

        // Box background
        paint.color = Color.rgb(240, 253, 250) // Teal 50
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 6f, 6f, paint)

        // Border
        paint.color = Color.rgb(153, 246, 228) // Teal 200
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(cardRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Metrics columns
        val colWidth = CONTENT_WIDTH / 4f
        val metricLabels = listOf(
            "Rata-rata Ketepatan",
            "Tugas Tepat Waktu",
            "Modul Ajar Tuntas",
            "Guru Terdaftar"
        )
        val metricValues = listOf(
            "${overview.averageOnTimeRatePercent}%",
            "${overview.totalOnTimeTasks} tugas",
            "${overview.modulAjarCompliancePercent}%",
            "$teacherCount guru"
        )

        for (i in 0 until 4) {
            val colX = MARGIN_LEFT + (i * colWidth) + 12f

            paint.color = Color.rgb(100, 116, 139)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7.5f
            canvas.drawText(metricLabels[i], colX, y + 20f, paint)

            paint.color = if (i == 0) Color.rgb(0, 121, 107) else Color.rgb(15, 23, 42)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText(metricValues[i], colX, y + 42f, paint)

            if (i < 3) {
                val divX = MARGIN_LEFT + ((i + 1) * colWidth)
                paint.color = Color.rgb(204, 251, 241)
                paint.strokeWidth = 1f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(divX, y + 10f, divX, y + cardHeight - 10f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        y += cardHeight + 12f

        // Filter / Scope Note
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        canvas.drawText("Cakupan Data: $filterLabel (${teacherCount} Guru Terdaftar Dalam Evaluasi)", MARGIN_LEFT, y, paint)
        y += 12f

        return y
    }

    /**
     * Draws the table column headers.
     */
    private fun drawTableHeader(canvas: Canvas, paint: Paint, startY: Float): Float {
        val y = startY
        val headerHeight = 22f

        // Dark header background
        paint.color = Color.rgb(30, 41, 59) // Slate 800
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + headerHeight, paint)

        // Column Titles
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f

        canvas.drawText("NO", MARGIN_LEFT + 6f, y + 14f, paint)
        canvas.drawText("NAMA GURU & MAPEL", MARGIN_LEFT + 28f, y + 14f, paint)
        canvas.drawText("SELESAI / TOTAL", MARGIN_LEFT + 200f, y + 14f, paint)
        canvas.drawText("TERLAMBAT", MARGIN_LEFT + 285f, y + 14f, paint)
        canvas.drawText("STATUS MODUL", MARGIN_LEFT + 355f, y + 14f, paint)
        canvas.drawText("DISIPLIN", MARGIN_LEFT + 440f, y + 14f, paint)
        canvas.drawText("PREDIKAT", MARGIN_LEFT + 490f, y + 14f, paint)

        return y + headerHeight
    }

    /**
     * Draws an individual teacher row.
     */
    private fun drawTableRow(
        canvas: Canvas,
        paint: Paint,
        rowNumber: Int,
        item: TeacherSupervisionSummary,
        startY: Float,
        rowHeight: Float
    ) {
        val y = startY

        // Alternate row background
        paint.color = if (rowNumber % 2 == 0) Color.rgb(248, 250, 252) else Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + rowHeight, paint)

        // Bottom border line
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.6f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN_LEFT, y + rowHeight, MARGIN_RIGHT, y + rowHeight, paint)
        paint.style = Paint.Style.FILL

        // Col 1: Number
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        canvas.drawText("$rowNumber.", MARGIN_LEFT + 6f, y + 18f, paint)

        // Col 2: Name & Subject
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        val teacherName = item.teacher.displayName.take(28)
        canvas.drawText(teacherName, MARGIN_LEFT + 28f, y + 13f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        val subject = if (item.teacher.subjectSpecialty.isNotBlank()) item.teacher.subjectSpecialty else "Guru Kelas"
        canvas.drawText(subject.take(30), MARGIN_LEFT + 28f, y + 24f, paint)

        // Col 3: Selesai / Total
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        val completionText = "${item.onTimeCompletedTasks} on-time / ${item.totalAssignedTasks}"
        canvas.drawText(completionText, MARGIN_LEFT + 200f, y + 18f, paint)

        // Col 4: Terlambat
        paint.color = if (item.lateTasks > 0) Color.rgb(225, 29, 72) else Color.rgb(100, 116, 139)
        paint.typeface = if (item.lateTasks > 0) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        val lateText = if (item.lateTasks > 0) "${item.lateTasks} tugas" else "-"
        canvas.drawText(lateText, MARGIN_LEFT + 285f, y + 18f, paint)

        // Col 5: Modul Ajar Status
        val (modulText, modulColor) = when (item.modulAjarStatus) {
            ModulAjarProgressStatus.COMPLETED_ON_TIME -> Pair("Tuntas On-Time", Color.rgb(13, 148, 136))
            ModulAjarProgressStatus.COMPLETED_LATE -> Pair("Tuntas Terlambat", Color.rgb(217, 119, 6))
            ModulAjarProgressStatus.PENDING -> Pair("Dikerjakan", Color.rgb(37, 99, 235))
            ModulAjarProgressStatus.OVERDUE -> Pair("Terlambat", Color.rgb(225, 29, 72))
            ModulAjarProgressStatus.NO_TASK -> Pair("Belum Ada Tugas", Color.rgb(148, 163, 184))
        }
        paint.color = modulColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7.5f
        canvas.drawText(modulText, MARGIN_LEFT + 355f, y + 18f, paint)

        // Col 6: Disiplin %
        val rate = item.onTimeRatePercent
        paint.color = when {
            rate >= 80 -> Color.rgb(16, 185, 129)
            rate >= 60 -> Color.rgb(217, 119, 6)
            else -> Color.rgb(225, 29, 72)
        }
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText("$rate%", MARGIN_LEFT + 440f, y + 18f, paint)

        // Col 7: Predikat
        val predikat = when {
            rate >= 90 -> "Sangat Baik"
            rate >= 80 -> "Baik"
            rate >= 60 -> "Cukup"
            else -> "Bimbingan"
        }
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        canvas.drawText(predikat, MARGIN_LEFT + 490f, y + 18f, paint)
    }

    /**
     * Draws the optional Principal's notes section.
     */
    private fun drawNotesSection(canvas: Canvas, paint: Paint, notes: String, startY: Float): Float {
        var y = startY + 10f

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("Catatan & Rekomendasi Supervisi Kepala Sekolah:", MARGIN_LEFT, y, paint)
        y += 12f

        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f

        // Word wrap note lines
        val bounds = Rect()
        val words = notes.split(" ")
        var currentLine = StringBuilder()
        val maxWidth = CONTENT_WIDTH - 20f

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            paint.getTextBounds(testLine, 0, testLine.length, bounds)
            if (bounds.width() > maxWidth) {
                canvas.drawText(currentLine.toString(), MARGIN_LEFT + 8f, y, paint)
                y += 11f
                currentLine = StringBuilder(word)
            } else {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), MARGIN_LEFT + 8f, y, paint)
            y += 12f
        }

        return y
    }

    /**
     * Draws the signature section at the bottom.
     */
    private fun drawSignatureBlock(canvas: Canvas, paint: Paint, options: ExportOptions, startY: Float) {
        val y = startY + 16f
        val signX = MARGIN_RIGHT - 180f

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        canvas.drawText("Mengetahui,", signX, y, paint)
        canvas.drawText("Kepala Sekolah", signX, y + 12f, paint)

        val signLineY = y + 54f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        canvas.drawText(options.principalName, signX, signLineY, paint)

        if (options.principalNip.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7.5f
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("NIP. ${options.principalNip}", signX, signLineY + 10f, paint)
        }
    }

    /**
     * Draws footer with page numbers and standard disclaimer.
     */
    private fun drawPageFooter(canvas: Canvas, paint: Paint, pageNumber: Int) {
        val y = MARGIN_BOTTOM + 18f

        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(MARGIN_LEFT, y - 8f, MARGIN_RIGHT, y - 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(148, 163, 184)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7f
        canvas.drawText("Dokumen Resmi Supervisi Sekolah • Digenerate otomatis dari Aplikasi Manajemen Sekolah", MARGIN_LEFT, y, paint)

        val pageStr = "Halaman $pageNumber"
        paint.getTextBounds(pageStr, 0, pageStr.length, Rect())
        canvas.drawText(pageStr, MARGIN_RIGHT - 50f, y, paint)
    }

    /**
     * Launches the system share sheet allowing the Principal to send the PDF via
     * WhatsApp, Gmail, Drive, or Print services.
     */
    fun sharePdf(context: Context, pdfFile: File, schoolName: String) {
        val uri = getFileUri(context, pdfFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Supervisi & Kinerja Guru - $schoolName")
            putExtra(
                Intent.EXTRA_TEXT,
                "Yth. Bapak/Ibu,\n\nTerlampir Laporan Evaluasi Supervisi Dewan Guru dari $schoolName. Silakan unduh dokumen PDF untuk melihat detail ketepatan waktu dan progres modul ajar.\n\nTerima kasih."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Bagikan Laporan PDF Supervisi")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Opens the generated PDF in an installed PDF viewer or print service.
     */
    fun viewPdf(context: Context, pdfFile: File) {
        val uri = getFileUri(context, pdfFile)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to share intent if no dedicated PDF viewer is registered
            Toast.makeText(context, "Tidak ada aplikasi pembuka PDF langsung, membuka opsi bagikan...", Toast.LENGTH_SHORT).show()
            sharePdf(context, pdfFile, "")
        }
    }

    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
