package com.google.ai.edge.gallery.ui.fieldmedic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 612   // US Letter
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    fun generate(context: Context, report: SessionReport): File {
        val doc = PdfDocument()
        val pages = mutableListOf<PdfDocument.Page>()
        var pageNum = 1
        var page = doc.startPage(pageInfo(pageNum))
        pages.add(page)
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = textPaint(20f, bold = true)
        val headerPaint = textPaint(14f, bold = true)
        val labelPaint = textPaint(10f, bold = true).apply { color = 0xFF666666.toInt() }
        val bodyPaint = textPaint(11f)
        val smallPaint = textPaint(9f).apply { color = 0xFF888888.toInt() }
        val linePaint = Paint().apply { color = 0xFFCCCCCC.toInt(); strokeWidth = 1f }

        // --- Page 1: Header ---
        y = drawText(canvas, "FIELD MEDIC INCIDENT REPORT", titlePaint, y)
        y += 4f
        y = drawText(canvas, report.dateFormatted, smallPaint, y)
        y += 16f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f

        // --- Patient & session info ---
        y = drawText(canvas, "PATIENT", labelPaint, y)
        y += 2f
        y = drawText(canvas, report.patientName, bodyPaint, y)
        y += 12f

        y = drawText(canvas, "LOCATION", labelPaint, y)
        y += 2f
        y = drawText(canvas, report.location.ifBlank { "Not specified" }, bodyPaint, y)
        y += 12f

        y = drawText(canvas, "SESSION", labelPaint, y)
        y += 2f
        y = drawText(canvas, "Started: ${report.startTimeFormatted}  |  Duration: ${report.durationFormatted}", bodyPaint, y)
        y += 2f
        y = drawText(canvas, "Traveler: ${if (report.soloTraveler) "Solo" else "With group"}", bodyPaint, y)
        y += 12f

        if (report.firstAidKit.isNotEmpty()) {
            y = drawText(canvas, "FIRST AID KIT", labelPaint, y)
            y += 2f
            y = drawText(canvas, report.firstAidKit.joinToString(", "), bodyPaint, y)
            y += 12f
        }

        // --- AI Summary ---
        y += 4f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f
        y = drawText(canvas, "AI INCIDENT SUMMARY", headerPaint, y)
        y += 6f
        y = drawWrapped(canvas, report.aiSummary, bodyPaint, y)
        y += 20f

        // --- Conversation transcript ---
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f
        y = drawText(canvas, "CONVERSATION TRANSCRIPT", headerPaint, y)
        y += 10f

        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (msg in report.conversationLog) {
            if (msg.role == TriageRole.SYSTEM) continue

            val prefix = when (msg.role) {
                TriageRole.USER -> "PATIENT"
                TriageRole.ASSISTANT -> "MEDIC"
                else -> "SYSTEM"
            }
            val time = timeFmt.format(Date(msg.timestamp))
            val line = "[$time] $prefix: ${msg.content}"

            // Check if we need a new page
            val neededHeight = measureWrapped(line, bodyPaint)
            if (y + neededHeight > PAGE_HEIGHT - MARGIN) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(pageInfo(pageNum))
                pages.add(page)
                canvas = page.canvas
                y = MARGIN
            }

            y = drawWrapped(canvas, line, bodyPaint, y)
            y += 6f
        }

        doc.finishPage(page)

        // Write to file
        val dir = File(context.cacheDir, "reports")
        dir.mkdirs()
        val file = File(dir, "incident_report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return file
    }

    private fun pageInfo(pageNum: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()

    private fun textPaint(size: Float, bold: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            color = 0xFF000000.toInt()
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun drawText(canvas: Canvas, text: String, paint: TextPaint, y: Float): Float {
        canvas.drawText(text, MARGIN, y + paint.textSize, paint)
        return y + paint.textSize + 4f
    }

    private fun drawWrapped(canvas: Canvas, text: String, paint: TextPaint, y: Float): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .build()
        canvas.save()
        canvas.translate(MARGIN, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    private fun measureWrapped(text: String, paint: TextPaint): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .build()
        return layout.height.toFloat()
    }
}
