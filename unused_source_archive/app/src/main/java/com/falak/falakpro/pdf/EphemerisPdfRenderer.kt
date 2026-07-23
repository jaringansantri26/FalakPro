package com.falak.falakpro.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

class EphemerisPdfRenderer(
    private val context: Context
) {

    data class SunRow(
        val jam: String,
        val apparentLongitude: String,
        val apparentLatitude: String,
        val apparentRA: String,
        val apparentDeclination: String,
        val trueGeocentricDistance: String,
        val semiDiameter: String,
        val trueObliquity: String,
        val equationOfTime: String
    )

    data class MoonRow(
        val jam: String,
        val apparentLongitude: String,
        val apparentLatitude: String,
        val apparentRA: String,
        val apparentDeclination: String,
        val horizontalParallax: String,
        val semiDiameter: String,
        val angleBrightLimb: String,
        val illumination: String
    )

    fun generate(
        fileName: String,
        titleDate: String,
        sunData: List<SunRow>,
        moonData: List<MoonRow>,
        pageNumber: Int = 41
    ): File {

        val pdf = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(2480, 3508, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.rgb(245,245,245))

        val border = Paint().apply {
            color = Color.rgb(140,140,140)
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val text = Paint().apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.create("serif", Typeface.NORMAL)
            isAntiAlias = true
        }

        val bold = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create("serif", Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 68f
            typeface = Typeface.create("serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        drawHeader(canvas, bold)

        canvas.drawText(titleDate, 1240f, 330f, titlePaint)

        drawSectionTitle(canvas, "DATA MATAHARI", 1240f, 420f, bold)
        drawSunTable(canvas, 380f, 470f, sunData, border, text, bold)

        drawSectionTitle(canvas, "DATA BULAN", 1240f, 1800f, bold)
        drawMoonTable(canvas, 380f, 1850f, moonData, border, text, bold)

        drawFooter(canvas, pageNumber)

        pdf.finishPage(page)

        val file = File(context.getExternalFilesDir(null), fileName)
        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        return file
    }

    private fun drawHeader(canvas: Canvas, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("❦ Ephemeris Hisab Rukyat 2026 ❦", 1240f, 95f, paint)
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(title, x, y, paint)
    }

    private fun drawSunTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        rows: List<SunRow>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val headers = listOf(
            "Jam","Apparent\nEcliptic\nLongitude","Apparent\nEcliptic\nLatitude",
            "Apparent Right\nAscension","Apparent\nDeclination",
            "True\nGeocentric\nDistance","Semi\nDiameter","True\nObliquity","Equation of\nTime"
        )

        drawTable(canvas, x, y, headers, rows.map {
            listOf(
                it.jam, it.apparentLongitude, it.apparentLatitude,
                it.apparentRA, it.apparentDeclination,
                it.trueGeocentricDistance, it.semiDiameter,
                it.trueObliquity, it.equationOfTime
            )
        }, border, text, bold)
    }

    private fun drawMoonTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        rows: List<MoonRow>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val headers = listOf(
            "Jam","Apparent\nLongitude","Apparent\nLatitude",
            "Apparent Right\nAscension","Apparent\nDeclination",
            "Horizontal\nParallax","Semi\nDiameter",
            "Angle Bright\nLimb","Fraction\nIllumination"
        )

        drawTable(canvas, x, y, headers, rows.map {
            listOf(
                it.jam, it.apparentLongitude, it.apparentLatitude,
                it.apparentRA, it.apparentDeclination,
                it.horizontalParallax, it.semiDiameter,
                it.angleBrightLimb, it.illumination
            )
        }, border, text, bold)
    }

    private fun drawTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        headers: List<String>,
        data: List<List<String>>,
        border: Paint,
        text: Paint,
        bold: Paint
    ) {
        val rowHeight = 52f
        val headerHeight = 110f
        val widths = floatArrayOf(120f,220f,190f,240f,210f,190f,170f,190f,190f)

        var xx = x
        widths.forEach { w ->
            canvas.drawLine(xx, y, xx, y + headerHeight + (rowHeight * 25), border)
            xx += w
        }
        canvas.drawLine(xx, y, xx, y + headerHeight + (rowHeight * 25), border)

        canvas.drawRect(x, y, xx, y + headerHeight + (rowHeight * 25), border)

        var cx = x
        headers.forEachIndexed { i, h ->
            drawCentered(canvas, h, cx + widths[i]/2, y + 40f, bold)
            cx += widths[i]
        }

        canvas.drawLine(x, y + headerHeight, xx, y + headerHeight, border)

        data.take(25).forEachIndexed { r, row ->
            val yy = y + headerHeight + (r * rowHeight)
            canvas.drawLine(x, yy, xx, yy, border)

            var tx = x
            row.forEachIndexed { i, v ->
                drawRight(canvas, v, tx + widths[i] - 12f, yy + 35f, text)
                tx += widths[i]
            }
        }
    }

    private fun drawCentered(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        text.split("\\n").forEachIndexed { idx, line ->
            canvas.drawText(line, x, y + (idx * 24f), paint)
        }
    }

    private fun drawRight(canvas: Canvas, value: String, x: Float, y: Float, paint: Paint) {
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, x, y, paint)
    }

    private fun drawFooter(canvas: Canvas, page: Int) {
        val paint = Paint().apply {
            textSize = 46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        canvas.drawText("❧   $page   ❧", 1240f, 3280f, paint)
    }
}
