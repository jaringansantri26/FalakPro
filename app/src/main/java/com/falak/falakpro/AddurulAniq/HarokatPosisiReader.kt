package com.falak.falakpro.AddurulAniq

import android.content.Context
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.nio.charset.Charset

class HarokatPosisiReader(private val context: Context) {
    fun read(): List<PosisiSheet> {
        val sheets = mutableListOf<PosisiSheet>()

        context.assets.open("harokat_posisi.bin").use { inputStream ->
            val input = DataInputStream(BufferedInputStream(inputStream))
            val headerBytes = ByteArray(7)
            input.readFully(headerBytes)
            val header = String(headerBytes, Charset.defaultCharset())
            if (header != "HAROKAT") return emptyList()

            val totalSheets = Integer.reverseBytes(input.readInt())

            repeat(totalSheets) {
                val nameLen = input.readUnsignedByte()
                val nameBytes = ByteArray(nameLen)
                input.readFully(nameBytes)
                val sheetName = String(nameBytes, Charset.defaultCharset())

                val sheetType = input.readUnsignedByte().toInt()
                val totalRow = Integer.reverseBytes(input.readInt())

                val mainRows = mutableListOf<MainRow>()
                val timeRows = mutableListOf<TimeRow>()

                repeat(totalRow) {
                    if (sheetType == 1) { // MAIN
                        val thn = Integer.reverseBytes(input.readInt())
                        val alm = Integer.reverseBytes(input.readInt())
                        val hri = Integer.reverseBytes(input.readInt())
                        
                        val values = DoubleArray(10) // SESUAI KODE BAPAK (10 DESIMAL)
                        for (i in 0 until 10) {
                            values[i] = java.lang.Double.longBitsToDouble(
                                java.lang.Long.reverseBytes(input.readLong())
                            )
                        }
                        // Kita simpan p (Pasaran) di values[0] jika dia ada di kolom ke-4 (setelah h)
                        mainRows.add(MainRow(thn, alm, hri, 0, values))
                    } else if (sheetType == 2) { // TIME
                        val waktu = Integer.reverseBytes(input.readInt())
                        val values = DoubleArray(7) // SESUAI KODE BAPAK (7 DESIMAL)
                        for (i in 0 until 7) {
                            values[i] = java.lang.Double.longBitsToDouble(
                                java.lang.Long.reverseBytes(input.readLong())
                            )
                        }
                        timeRows.add(TimeRow(waktu, values))
                    }
                }
                sheets.add(PosisiSheet(sheetName, sheetType, mainRows, timeRows))
            }
        }
        return sheets
    }
}

data class PosisiSheet(
    val name: String,
    val type: Int,
    val mainRows: List<MainRow>,
    val timeRows: List<TimeRow>
)

data class MainRow(val tahun: Int, val alamat: Int, val hari: Int, val psr: Int, val values: DoubleArray)
data class TimeRow(val waktu: Int, val values: DoubleArray)
