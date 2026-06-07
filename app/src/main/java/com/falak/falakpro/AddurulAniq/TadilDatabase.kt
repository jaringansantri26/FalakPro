package com.falak.falakpro.AddurulAniq

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class TadilRecord(

    val dr: Double,

    val t1: Double,
    val t2: Double,
    val t3: Double,
    val t4: Double,
    val t5: Double,
    val t6: Double,
    val t7: Double,
    val t8: Double,

    val l1: Double,
    val l2: Double,

    val r1: Double,
    val r2: Double,

    val m1: Double,
    val m2: Double,
    val m3: Double,
    val m4: Double,
    val m5: Double,
    val m6: Double,
    val m7: Double,
    val m8: Double,
    val m9: Double,

    val b1: Double,
    val b2: Double,
    val b3: Double,
    val b4: Double,

    val rr1: Double,
    val rr2: Double,
    val rr3: Double,
    val rr4: Double,

    val nq: Double
)

class TadilDatabase(
    context: Context
) {

    val data = mutableMapOf<Int, TadilRecord>()

    init {

        val bytes = context.assets
            .open("ta_dil.bin")
            .readBytes()

        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val totalRow = buffer.int

        repeat(totalRow) {

            val rec = TadilRecord(

                dr = buffer.double,

                t1 = buffer.double,
                t2 = buffer.double,
                t3 = buffer.double,
                t4 = buffer.double,
                t5 = buffer.double,
                t6 = buffer.double,
                t7 = buffer.double,
                t8 = buffer.double,

                l1 = buffer.double,
                l2 = buffer.double,

                r1 = buffer.double,
                r2 = buffer.double,

                m1 = buffer.double,
                m2 = buffer.double,
                m3 = buffer.double,
                m4 = buffer.double,
                m5 = buffer.double,
                m6 = buffer.double,
                m7 = buffer.double,
                m8 = buffer.double,
                m9 = buffer.double,

                b1 = buffer.double,
                b2 = buffer.double,
                b3 = buffer.double,
                b4 = buffer.double,

                rr1 = buffer.double,
                rr2 = buffer.double,
                rr3 = buffer.double,
                rr4 = buffer.double,

                nq = buffer.double
            )

            data[rec.dr.toInt()] = rec
        }
    }
}
