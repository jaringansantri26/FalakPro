import com.falak.falakpro.premium.EclipseParityEngine
import com.falak.falakpro.premium.AstroMath

fun main() {
    val jdeApprox = AstroMath.calculateJulianDate(2026, 3, 3, 12, 0, 0.0)
    val detail = EclipseParityEngine.calculateLunarDetail(jdeApprox, 72.1, 0.0)
    
    println("Penumbral Magnitude: \${detail.magPenumbra}")
    println("Umbral Magnitude: \${detail.magUmbra}")
    println("Gamma: \${detail.gamma}")
    println("Epsilon: \${detail.epsilon}")
    println("P1: \${detail.p1?.jdeTD?.let { AstroMath.getCalendarFromJD(it) }}")
    println("U1: \${detail.u1?.jdeTD?.let { AstroMath.getCalendarFromJD(it) }}")
    println("U2: \${detail.u2?.jdeTD?.let { AstroMath.getCalendarFromJD(it) }}")
    println("Mx: \${detail.mx.jdeTD.let { AstroMath.getCalendarFromJD(it) }}")
    println("U3: \${detail.u3?.jdeTD?.let { AstroMath.getCalendarFromJD(it) }}")
    println("U4: \${detail.u4?.jdeTD?.let { AstroMath.getCalendarFromJD(it) }}")
    println("P4: \${detail.p4?.jdeTD?.let { AstroMath.getCalendarFromJD(it) }}")
}
