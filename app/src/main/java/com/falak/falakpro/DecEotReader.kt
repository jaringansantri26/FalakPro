package com.falak.falakpro

import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

data class DecEotData(
    val tanggal: Int,
    val bulan: Int,
    val deklinasi: Double,
    val eot: Double
)

class DecEotReader {

    companion object {

        fun readBin(
            file: File
        ): List<DecEotData> {

            val result =
                mutableListOf<DecEotData>()

            DataInputStream(
                FileInputStream(file)
            ).use { input ->

                // HEADER
                val headerBytes =
                    ByteArray(9)

                input.readFully(headerBytes)

                val header =
                    String(headerBytes)

                if (header != "EPHEMERIS") {
                    throw Exception(
                        "Invalid EPHEMERIS BIN"
                    )
                }

                // TOTAL
                val total =
                    Integer.reverseBytes(
                        input.readInt()
                    )

                repeat(total) {

                    val tanggal =
                        java.lang.Short.reverseBytes(
                            input.readShort()
                        ).toInt()

                    val bulan =
                        input.readUnsignedByte()

                    val deklinasi =
                        java.lang.Double.longBitsToDouble(
                            java.lang.Long.reverseBytes(
                                input.readLong()
                            )
                        )

                    val eot =
                        java.lang.Double.longBitsToDouble(
                            java.lang.Long.reverseBytes(
                                input.readLong()
                            )
                        )

                    result.add(
                        DecEotData(
                            tanggal,
                            bulan,
                            deklinasi,
                            eot
                        )
                    )
                }
            }

            return result
        }
    }
}
