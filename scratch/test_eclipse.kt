import com.falak.falakpro.premium.*

fun main() {
    val jde = AstroTime.kmjd(2, 8, 2027, 10.0, 0.0) // 2027 Aug 02
    val deltaT = 72.8 // Force NASA deltaT
    
    val engine = EclipseParityEngine()
    
    // Evaluate near 10:07 TD
    val result = engine.calculateFullDetail(jde, deltaT, 0.0)
    
    println("T0 Hour: ${result.t0}")
    println("Magnitude: ${result.magnitude}")
    println("Gamma: ${result.gamma}")
    println("Type: ${result.type}")
    println("Besselian Table:")
    for (row in result.besselianTable) {
        println("${row.orde} | ${row.x} | ${row.y} | ${row.d} | ${row.L1} | ${row.L2} | ${row.mu}")
    }
}
