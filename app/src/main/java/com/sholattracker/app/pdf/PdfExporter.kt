package com.sholattracker.app.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.sholattracker.app.data.SHOLAT_LIST
import com.sholattracker.app.data.SholatRepository
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Typeface

class PdfExporter(private val context: Context, private val repo: SholatRepository) {

    private val pageWidth = 595   // A4 pt
    private val pageHeight = 842  // A4 pt
    private val margin = 56f

    fun export(): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        draw(page.canvas)
        doc.finishPage(page)

        val file = File(context.cacheDir, "sholat-${repo.todayKey()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export PDF via"))
    }

    private fun draw(canvas: Canvas) {
        val today = repo.todayKey()
        val record = repo.getRecord(today)
        val allRecords = repo.getAllRecords()
        val streak = repo.calcStreak()

        // Background
        canvas.drawColor(Color.parseColor("#080c10"))

        val w = pageWidth.toFloat()
        val gold = Color.parseColor("#c9a84c")
        val goldSoft = Color.parseColor("#e2c97e")
        val green = Color.parseColor("#3dba68")
        val textColor = Color.parseColor("#e8edf3")
        val muted = Color.parseColor("#7a8694")
        val surface2 = Color.parseColor("#161d26")

        var y = margin

        // Gold top bar
        val barPaint = Paint().apply { color = gold; strokeWidth = 1f; style = Paint.Style.FILL }
        canvas.drawRect(margin, y - 8f, w - margin, y - 7f, barPaint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldSoft; textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Tracker Sholat Harian", w / 2f, y + 8f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted; textSize = 10f; textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LAPORAN IBADAH SHOLAT", w / 2f, y + 22f, subPaint)

        val dateStr = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        )
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold; textSize = 10f; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateStr, w / 2f, y + 36f, datePaint)
        y += 50f

        // Divider
        val divPaint = Paint().apply { color = Color.parseColor("#1e2a38"); strokeWidth = 1f }
        canvas.drawLine(margin, y, w - margin, y, divPaint)
        y += 14f

        // Progress label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = muted; textSize = 9f }
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldSoft; textSize = 9f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("PROGRESS HARI INI", margin, y, labelPaint)
        canvas.drawText("${record.count} / 5 selesai", w - margin, y, countPaint)
        y += 6f

        // Progress bar
        val trackPaint = Paint().apply { color = Color.parseColor("#1c2535") }
        canvas.drawRoundRect(RectF(margin, y, w - margin, y + 6f), 3f, 3f, trackPaint)
        if (record.count > 0) {
            val fillW = (record.count / 5f) * (w - margin * 2)
            val fillPaint = Paint().apply { color = green }
            canvas.drawRoundRect(RectF(margin, y, margin + fillW, y + 6f), 3f, 3f, fillPaint)
        }
        y += 18f

        // Sholat rows
        SHOLAT_LIST.forEach { sholat ->
            val done = record.completed.contains(sholat.id)
            val rowBg = if (done) Color.parseColor("#162618") else Color.parseColor("#0f1419")
            val rowBorder = if (done) Color.parseColor("#2d6640") else Color.parseColor("#1e2a38")

            val rowPaint = Paint().apply { color = rowBg }
            val borderPaint = Paint().apply {
                color = rowBorder; style = Paint.Style.STROKE; strokeWidth = 1f
            }
            val rowRect = RectF(margin, y, w - margin, y + 22f)
            canvas.drawRoundRect(rowRect, 5f, 5f, rowPaint)
            canvas.drawRoundRect(rowRect, 5f, 5f, borderPaint)

            // Checkbox
            val cbRect = RectF(margin + 6f, y + 5f, margin + 16f, y + 15f)
            val cbPaint = Paint().apply {
                color = if (done) green else Color.parseColor("#1c2535")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(cbRect, 2f, 2f, cbPaint)
            if (done) {
                val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE; strokeWidth = 1.5f; style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawLine(margin + 8f, y + 10f, margin + 10.5f, y + 13f, checkPaint)
                canvas.drawLine(margin + 10.5f, y + 13f, margin + 15f, y + 7f, checkPaint)
            }

            // Name
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (done) Color.parseColor("#a8eccb") else textColor
                textSize = 11f
                typeface = if (done) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            canvas.drawText(sholat.name, margin + 20f, y + 15f, namePaint)

            // Status
            val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (done) Color.parseColor("#a8eccb") else muted
                textSize = 9f; textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(if (done) "Sudah ✓" else "Belum", w - margin - 6f, y + 15f, statusPaint)

            y += 26f
        }

        y += 6f

        // Summary box
        val summaryBg = if (record.isComplete) Color.parseColor("#1e1808") else surface2
        val summaryBorder = if (record.isComplete) Color.parseColor("#8c5a18") else Color.parseColor("#1e2a38")
        val summaryRect = RectF(margin, y, w - margin, y + 26f)
        canvas.drawRoundRect(summaryRect, 5f, 5f, Paint().apply { color = summaryBg })
        canvas.drawRoundRect(summaryRect, 5f, 5f, Paint().apply {
            color = summaryBorder; style = Paint.Style.STROKE; strokeWidth = 1f
        })
        val summaryMsg = if (record.isComplete)
            "Alhamdulillah, semua sholat hari ini selesai. Semoga diterima Allah."
        else "Sudah ${record.count} dari 5 sholat — teruskan semangat!"
        val summaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (record.isComplete) goldSoft else muted
            textSize = 9f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(summaryMsg, w / 2f, y + 16f, summaryTextPaint)
        y += 36f

        // Streak
        if (streak > 0) {
            val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = goldSoft; textSize = 10f; textAlign = Paint.Align.CENTER
            }
            canvas.drawText("🔥 $streak hari berturut-turut sholat lengkap", w / 2f, y, streakPaint)
            y += 16f
        }

        // History
        val historyRecords = allRecords.filter { it.date != today }.take(10)
        if (historyRecords.isNotEmpty()) {
            canvas.drawLine(margin, y, w - margin, y, divPaint)
            y += 12f
            val histTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted; textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("RIWAYAT", margin, y, histTitlePaint)
            y += 8f

            historyRecords.forEach { hr ->
                val dateLabel = LocalDate.parse(hr.date).format(
                    DateTimeFormatter.ofPattern("d MMM", Locale("id", "ID"))
                )
                val histDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = muted; textSize = 8f
                }
                canvas.drawText(dateLabel, margin, y, histDatePaint)

                // Dots
                SHOLAT_LIST.forEachIndexed { i, s ->
                    val filled = hr.completed.contains(s.id)
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (filled) green else Color.parseColor("#2d3a4a")
                    }
                    canvas.drawCircle(margin + 55f + i * 14f, y - 3f, 4f, dotPaint)
                }

                val hrCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = gold; textSize = 8f; textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("${hr.count}/5", w - margin, y, hrCountPaint)
                y += 10f
            }
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2d3a4a"); textSize = 7f; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Sholat Tracker · ${LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id","ID")))}",
            w / 2f, pageHeight - 20f, footerPaint
        )
    }
}
