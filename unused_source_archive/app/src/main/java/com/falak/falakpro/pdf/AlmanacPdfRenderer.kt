package com.falak.falakpro.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

class AlmanacPdfRenderer(
    private val context: Context
) {

    data class HourRow(
        val h: String,
        val sunGha: String,
        val sunDec: String,
        val moonGha: String,
        val nu: String,
        val moonDec: String,
        val d: String,
        val hp: String
    )

    data class DailySummary(
        val title: String,
        val sunSd: String,
        val sunD: String,
        val moonSd: String
    )

    data class TwilightRow(
        val lat: String,
        val nautRise: String,
        val civilRise: String,
        val sunrise: String,
        val sunset: String,
        val civilSet: String,
        val nautSet: String
    )

    data class MoonriseRow(
        val lat: String,
        val riseD1: String,
        val riseD2: String,
        val riseD3: String,
        val setD1: String,
        val setD2: String,
        val setD3: String
    )

    data class SummaryRow(
        val day: String,
        val eqt00: String,
        val eqt12: String,
        val sunMerPass: String,
        val moonMerPassUpper: String,
        val moonMerPassLower: String,
        val moonAge: String
    )

    companion object {
        fun defaultSummaries() = listOf(
            DailySummary("Tue. 13", "16.3'", "0.4", "14.7'"),
            DailySummary("Wed. 14", "16.3'", "0.4", "14.8'"),
            DailySummary("Thu. 15", "16.3'", "0.5", "14.8'")
        )

        fun defaultTwilightRows(): List<TwilightRow> {
            return listOf(
                TwilightRow("72", "08:03", "09:55", "--:--", "--:--", "14:23", "16:16"),
                TwilightRow("70", "07:48", "09:21", "--:--", "--:--", "14:58", "16:31"),
                TwilightRow("68", "07:36", "08:56", "10:38", "13:43", "15:23", "16:43"),
                TwilightRow("66", "07:25", "08:36", "09:56", "14:23", "15:43", "16:54"),
                TwilightRow("64", "07:16", "08:20", "09:28", "14:51", "15:58", "17:02"),
                TwilightRow("62", "07:09", "08:07", "09:06", "15:12", "16:11", "17:10"),
                TwilightRow("60", "07:02", "07:56", "08:49", "15:30", "16:23", "17:17"),
                TwilightRow("58", "06:56", "07:46", "08:35", "15:44", "16:32", "17:23"),
                TwilightRow("56", "06:50", "07:38", "08:22", "15:56", "16:41", "17:28"),
                TwilightRow("54", "06:45", "07:30", "08:11", "16:07", "16:49", "17:34"),
                TwilightRow("52", "06:40", "07:23", "08:02", "16:17", "16:56", "17:38"),
                TwilightRow("50", "06:36", "07:16", "07:53", "16:25", "17:02", "17:43"),
                TwilightRow("45", "06:26", "07:02", "07:35", "16:43", "17:16", "17:52"),
                TwilightRow("40", "06:17", "06:51", "07:20", "16:58", "17:28", "18:01"),
                TwilightRow("35", "06:09", "06:40", "07:08", "17:11", "17:38", "18:09"),
                TwilightRow("30", "06:01", "06:31", "06:57", "17:22", "17:47", "18:17"),
                TwilightRow("20", "05:47", "06:14", "06:38", "17:40", "18:04", "18:31"),
                TwilightRow("10", "05:33", "05:59", "06:21", "17:57", "18:20", "18:46"),
                TwilightRow("0", "05:17", "05:43", "06:05", "18:13", "18:35", "19:01"),
                TwilightRow("10", "05:00", "05:27", "05:50", "18:28", "18:51", "19:18"),
                TwilightRow("20", "04:40", "05:08", "05:32", "18:45", "19:10", "19:38"),
                TwilightRow("30", "04:13", "04:46", "05:13", "19:05", "19:32", "20:04"),
                TwilightRow("35", "03:57", "04:32", "05:01", "19:17", "19:46", "20:21"),
                TwilightRow("40", "03:36", "04:16", "04:48", "19:30", "20:02", "20:41"),
                TwilightRow("45", "03:09", "03:56", "04:32", "19:46", "20:22", "21:08"),
                TwilightRow("50", "02:32", "03:30", "04:12", "20:06", "20:47", "21:45"),
                TwilightRow("52", "02:11", "03:17", "04:02", "20:15", "20:58", "22:06"),
                TwilightRow("54", "01:43", "03:02", "03:52", "20:25", "21:15", "22:33"),
                TwilightRow("56", "00:57", "02:44", "03:40", "20:37", "21:32", "23:15"),
                TwilightRow("58", "--:--", "02:22", "03:26", "20:51", "21:54", "--:--"),
                TwilightRow("60", "--:--", "01:52", "03:09", "21:08", "22:23", "--:--")
            )
        }

        fun defaultMoonriseRows(): List<MoonriseRow> {
            return listOf(
                MoonriseRow("72", "--:--", "--:--", "--:--", "--:--", "--:--", "--:--"),
                MoonriseRow("70", "--:--", "--:--", "--:--", "--:--", "--:--", "--:--"),
                MoonriseRow("68", "--:--", "--:--", "--:--", "--:--", "--:--", "--:--"),
                MoonriseRow("66", "04:27", "--:--", "--:--", "08:45", "--:--", "--:--"),
                MoonriseRow("64", "03:36", "--:--", "--:--", "09:36", "--:--", "--:--"),
                MoonriseRow("62", "03:05", "06:52", "09:00", "10:07", "09:59", "09:35"),
                MoonriseRow("60", "04:41", "06:14", "07:42", "10:33", "10:37", "10:56"),
                MoonriseRow("58", "04:23", "05:48", "07:06", "10:52", "11:04", "11:32"),
                MoonriseRow("56", "04:07", "05:27", "06:40", "11:08", "11:25", "11:57"),
                MoonriseRow("54", "03:54", "05:10", "06:20", "11:21", "11:42", "12:16"),
                MoonriseRow("52", "03:42", "04:55", "06:03", "11:32", "11:57", "12:34"),
                MoonriseRow("50", "03:32", "04:43", "05:49", "11:42", "12:10", "12:48"),
                MoonriseRow("45", "03:11", "04:16", "05:18", "12:03", "12:37", "13:18"),
                MoonriseRow("40", "02:53", "03:55", "04:55", "12:21", "12:59", "13:42"),
                MoonriseRow("35", "02:39", "03:38", "04:36", "12:36", "13:16", "14:01"),
                MoonriseRow("30", "02:26", "03:24", "04:20", "12:50", "13:32", "14:18"),
                MoonriseRow("20", "02:05", "02:59", "03:53", "13:14", "13:59", "14:46"),
                MoonriseRow("10", "01:47", "02:37", "03:29", "13:34", "14:23", "15:11"),
                MoonriseRow("0", "01:30", "02:17", "03:07", "13:52", "14:43", "15:33"),
                MoonriseRow("10", "01:13", "01:57", "02:45", "14:11", "15:05", "15:56"),
                MoonriseRow("20", "00:55", "01:36", "02:22", "14:31", "15:27", "16:21"),
                MoonriseRow("30", "00:34", "01:12", "01:55", "14:54", "15:53", "16:49"),
                MoonriseRow("35", "00:22", "00:58", "01:39", "15:08", "16:09", "17:06"),
                MoonriseRow("40", "00:09", "00:41", "01:21", "15:25", "16:28", "17:26"),
                MoonriseRow("45", "--:--", "00:21", "00:59", "15:45", "16:50", "17:50"),
                MoonriseRow("50", "23:57", "--:--", "00:30", "16:09", "17:18", "18:22"),
                MoonriseRow("52", "23:45", "--:--", "00:16", "16:21", "17:31", "18:37"),
                MoonriseRow("54", "23:32", "--:--", "00:00", "16:34", "17:47", "18:55"),
                MoonriseRow("56", "23:16", "23:41", "--:--", "16:49", "18:05", "19:16"),
                MoonriseRow("58", "22:58", "23:19", "23:53", "17:07", "18:28", "19:42"),
                MoonriseRow("60", "22:36", "22:47", "23:15", "17:29", "18:58", "20:16")
            )
        }

        fun defaultSummaryRows() = listOf(
            SummaryRow("13", "08:27", "08:39", "12:09", "07:41", "20:05", "24(24%)"),
            SummaryRow("14", "08:50", "09:01", "12:09", "08:30", "20:54", "25(17%)"),
            SummaryRow("15", "09:12", "09:22", "12:09", "09:18", "21:43", "26(10%)")
        )
    }

    fun generate(
        fileName: String,
        tue: List<HourRow>,
        wed: List<HourRow>,
        thu: List<HourRow>,
        titleDateRange: String = "2026 January 13 to Jan. 15",
        summaries: List<DailySummary> = defaultSummaries(),
        twilightRows: List<TwilightRow> = defaultTwilightRows(),
        moonriseRows: List<MoonriseRow> = defaultMoonriseRows(),
        summaryRows: List<SummaryRow> = defaultSummaryRows(),
        pageNumber: Int = 11
    ): File {

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(2480, 3508, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create("serif", Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create("serif", Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 44f
            typeface = Typeface.create("serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Top centered title
        canvas.drawText(titleDateRange.uppercase(), 1240f, 160f, titlePaint)

        val leftX = 100f
        val rightX = 1270f

        // Draw 3 Daily Tables on the Left
        val daysData = listOf(tue, wed, thu)
        val yStarts = listOf(240f, 1280f, 2320f)

        for (i in 0..2) {
            drawDailyTable(
                canvas,
                leftX,
                yStarts[i],
                summaries.getOrElse(i) { defaultSummaries()[0] },
                daysData[i],
                borderPaint,
                textPaint,
                boldPaint
            )
        }

        // Draw 3 Phenomena Tables on the Right
        drawTwilightTable(canvas, rightX, 240f, twilightRows, borderPaint, textPaint, boldPaint)
        drawMoonriseTable(canvas, rightX, 1280f, moonriseRows, borderPaint, textPaint, boldPaint)
        drawSummaryTable(canvas, rightX, 2320f, summaryRows, borderPaint, textPaint, boldPaint)

        // Footer page number
        val footerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.create("serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("$pageNumber", 2330f, 3420f, footerPaint)

        pdf.finishPage(page)

        val file = File(context.getExternalFilesDir(null), fileName)
        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        return file
    }

    private fun drawDailyTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        summary: DailySummary,
        rows: List<HourRow>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val width = 1110f
        val headerHeight1 = 45f
        val headerHeight2 = 45f
        val rowHeight = 35f
        val summaryHeight = 45f
        val totalHeight = headerHeight1 + headerHeight2 + (24 * rowHeight) + summaryHeight

        // Outer box
        canvas.drawRect(x, y, x + width, y + totalHeight, border)

        // Header Row 1 lines & text
        canvas.drawLine(x, y + headerHeight1, x + width, y + headerHeight1, border)
        
        bold.textAlign = Paint.Align.LEFT
        canvas.drawText(summary.title, x + 15f, y + 32f, bold)

        bold.textAlign = Paint.Align.CENTER
        canvas.drawText("SUN", x + 65f + 185f, y + 32f, bold)
        canvas.drawText("MOON", x + 435f + 337.5f, y + 32f, bold)

        // Header Row 2 lines & text
        canvas.drawLine(x, y + headerHeight1 + headerHeight2, x + width, y + headerHeight1 + headerHeight2, border)

        val cols = floatArrayOf(65f, 185f, 185f, 195f, 85f, 195f, 85f, 115f)
        val headers2 = listOf("h", "GHA", "Dec", "GHA", "v", "Dec", "d", "HP")

        var cx = x
        cols.forEachIndexed { index, w ->
            if (index > 0) {
                canvas.drawLine(cx, y + headerHeight1, cx, y + totalHeight, border)
            }
            bold.textAlign = Paint.Align.CENTER
            canvas.drawText(headers2[index], cx + w / 2, y + headerHeight1 + 32f, bold)
            cx += w
        }

        // 24 Data Rows
        val startY = y + headerHeight1 + headerHeight2
        rows.take(24).forEachIndexed { r, row ->
            val yy = startY + (r * rowHeight)
            canvas.drawLine(x, yy, x + width, yy, border)

            val textY = yy + 25f
            drawCenteredText(canvas, row.h, x + cols[0]/2, textY, text)
            drawRightText(canvas, row.sunGha, x + cols[0] + cols[1] - 15f, textY, text)
            drawRightText(canvas, row.sunDec, x + cols[0] + cols[1] + cols[2] - 15f, textY, text)
            drawRightText(canvas, row.moonGha, x + 435f + cols[3] - 15f, textY, text)
            drawCenteredText(canvas, row.nu, x + 435f + cols[3] + cols[4]/2, textY, text)
            drawRightText(canvas, row.moonDec, x + 435f + cols[3] + cols[4] + cols[5] - 15f, textY, text)
            drawCenteredText(canvas, row.d, x + 435f + cols[3] + cols[4] + cols[5] + cols[6]/2, textY, text)
            drawRightText(canvas, row.hp, x + width - 15f, textY, text)
        }

        // Bottom Summary Row
        val summaryY = startY + (24 * rowHeight)
        canvas.drawLine(x, summaryY, x + width, summaryY, border)

        text.textAlign = Paint.Align.LEFT
        canvas.drawText("SD=${summary.sunSd} d=${summary.sunD}", x + cols[0] + 15f, summaryY + 32f, text)
        canvas.drawText("SD=${summary.moonSd}", x + 435f + 15f, summaryY + 32f, text)
    }

    private fun drawTwilightTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        rows: List<TwilightRow>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val width = 1110f
        val headerHeight1 = 45f
        val headerHeight2 = 45f
        val rowHeight = 30f
        val totalHeight = headerHeight1 + headerHeight2 + (31 * rowHeight)

        canvas.drawRect(x, y, x + width, y + totalHeight, border)
        canvas.drawLine(x, y + headerHeight1, x + width, y + headerHeight1, border)
        canvas.drawLine(x, y + headerHeight1 + headerHeight2, x + width, y + headerHeight1 + headerHeight2, border)

        val topCols = floatArrayOf(100f, 320f, 185f, 185f, 320f)
        val topHeaders = listOf("Lat.", "Twilight", "Sunrise", "Sunset", "Twilight")
        var cx = x
        topCols.forEachIndexed { i, w ->
            if (i > 0) canvas.drawLine(cx, y, cx, y + totalHeight, border)
            bold.textAlign = Paint.Align.CENTER
            canvas.drawText(topHeaders[i], cx + w/2, y + 32f, bold)
            cx += w
        }

        // Sub headers
        val subCols = floatArrayOf(100f, 160f, 160f, 185f, 185f, 160f, 160f)
        val subHeaders = listOf("", "Naut.", "Civil", "", "", "Civil", "Naut.")
        var scx = x
        subCols.forEachIndexed { i, w ->
            if (i in listOf(1, 2, 5, 6)) {
                if (i == 2 || i == 6) canvas.drawLine(scx, y + headerHeight1, scx, y + totalHeight, border)
                bold.textAlign = Paint.Align.CENTER
                canvas.drawText(subHeaders[i], scx + w/2, y + headerHeight1 + 32f, bold)
            }
            scx += w
        }

        val startY = y + headerHeight1 + headerHeight2
        rows.take(31).forEachIndexed { r, row ->
            val yy = startY + (r * rowHeight)
            canvas.drawLine(x, yy, x + width, yy, border)

            val textY = yy + 22f
            drawCenteredText(canvas, row.lat, x + 50f, textY, bold)
            drawCenteredText(canvas, row.nautRise, x + 100f + 80f, textY, text)
            drawCenteredText(canvas, row.civilRise, x + 260f + 80f, textY, text)
            drawCenteredText(canvas, row.sunrise, x + 420f + 92.5f, textY, text)
            drawCenteredText(canvas, row.sunset, x + 605f + 92.5f, textY, text)
            drawCenteredText(canvas, row.civilSet, x + 790f + 80f, textY, text)
            drawCenteredText(canvas, row.nautSet, x + 950f + 80f, textY, text)
        }
    }

    private fun drawMoonriseTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        rows: List<MoonriseRow>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val width = 1110f
        val headerHeight1 = 45f
        val headerHeight2 = 45f
        val rowHeight = 30f
        val totalHeight = headerHeight1 + headerHeight2 + (31 * rowHeight)

        canvas.drawRect(x, y, x + width, y + totalHeight, border)
        canvas.drawLine(x, y + headerHeight1, x + width, y + headerHeight1, border)
        canvas.drawLine(x, y + headerHeight1 + headerHeight2, x + width, y + headerHeight1 + headerHeight2, border)

        val topCols = floatArrayOf(100f, 505f, 505f)
        val topHeaders = listOf("Lat.", "Moonrise", "Moonset")
        var cx = x
        topCols.forEachIndexed { i, w ->
            if (i > 0) canvas.drawLine(cx, y, cx, y + totalHeight, border)
            bold.textAlign = Paint.Align.CENTER
            canvas.drawText(topHeaders[i], cx + w/2, y + 32f, bold)
            cx += w
        }

        val subCols = floatArrayOf(100f, 168.33f, 168.33f, 168.34f, 168.33f, 168.33f, 168.34f)
        val subHeaders = listOf("", "Tue", "Wed", "Thu", "Tue", "Wed", "Thu")
        var scx = x
        subCols.forEachIndexed { i, w ->
            if (i > 0) {
                if (i != 4) canvas.drawLine(scx, y + headerHeight1, scx, y + totalHeight, border)
                bold.textAlign = Paint.Align.CENTER
                canvas.drawText(subHeaders[i], scx + w/2, y + headerHeight1 + 32f, bold)
            }
            scx += w
        }

        val startY = y + headerHeight1 + headerHeight2
        rows.take(31).forEachIndexed { r, row ->
            val yy = startY + (r * rowHeight)
            canvas.drawLine(x, yy, x + width, yy, border)

            val textY = yy + 22f
            drawCenteredText(canvas, row.lat, x + 50f, textY, bold)
            drawCenteredText(canvas, row.riseD1, x + 100f + 84f, textY, text)
            drawCenteredText(canvas, row.riseD2, x + 268.33f + 84f, textY, text)
            drawCenteredText(canvas, row.riseD3, x + 436.66f + 84f, textY, text)
            drawCenteredText(canvas, row.setD1, x + 605f + 84f, textY, text)
            drawCenteredText(canvas, row.setD2, x + 773.33f + 84f, textY, text)
            drawCenteredText(canvas, row.setD3, x + 941.66f + 84f, textY, text)
        }
    }

    private fun drawSummaryTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        rows: List<SummaryRow>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val width = 1110f
        val headerHeight1 = 45f
        val headerHeight2 = 45f
        val rowHeight = 50f
        val totalHeight = headerHeight1 + headerHeight2 + (3 * rowHeight)

        canvas.drawRect(x, y, x + width, y + totalHeight, border)
        canvas.drawLine(x, y + headerHeight1, x + width, y + headerHeight1, border)
        canvas.drawLine(x, y + headerHeight1 + headerHeight2, x + width, y + headerHeight1 + headerHeight2, border)

        val topCols = floatArrayOf(100f, 510f, 500f)
        val topHeaders = listOf("", "Sun", "Moon")
        var cx = x
        topCols.forEachIndexed { i, w ->
            if (i > 0) canvas.drawLine(cx, y, cx, y + totalHeight, border)
            bold.textAlign = Paint.Align.CENTER
            canvas.drawText(topHeaders[i], cx + w/2, y + 32f, bold)
            cx += w
        }

        val subCols = floatArrayOf(100f, 140f, 140f, 230f, 160f, 160f, 180f)
        val subHeaders = listOf("Day", "Eqn.00h", "Eqn.12h", "Mer.Pass", "Mer.Upper", "Mer.Lower", "Age")
        var scx = x
        subCols.forEachIndexed { i, w ->
            if (i > 0 && i != 1 && i != 4) {
                canvas.drawLine(scx, y + headerHeight1, scx, y + totalHeight, border)
            }
            bold.textAlign = Paint.Align.CENTER
            canvas.drawText(subHeaders[i], scx + w/2, y + headerHeight1 + 32f, bold)
            scx += w
        }

        val startY = y + headerHeight1 + headerHeight2
        rows.take(3).forEachIndexed { r, row ->
            val yy = startY + (r * rowHeight)
            canvas.drawLine(x, yy, x + width, yy, border)

            val textY = yy + 32f
            drawCenteredText(canvas, row.day, x + 50f, textY, bold)
            drawCenteredText(canvas, row.eqt00, x + 100f + 70f, textY, text)
            drawCenteredText(canvas, row.eqt12, x + 240f + 70f, textY, text)
            drawCenteredText(canvas, row.sunMerPass, x + 380f + 115f, textY, text)
            drawCenteredText(canvas, row.moonMerPassUpper, x + 610f + 80f, textY, text)
            drawCenteredText(canvas, row.moonMerPassLower, x + 770f + 80f, textY, text)
            drawCenteredText(canvas, row.moonAge, x + 930f + 90f, textY, text)
        }
    }

    private fun drawCenteredText(canvas: Canvas, value: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(value, x, y, paint)
    }

    private fun drawRightText(canvas: Canvas, value: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, x, y, paint)
    }
}
