package com.falak.falakpro.premium

/**
 * Shared data models for Eclipse Engine and UI.
 */
data class EclipseResultItem(
    val title: String,
    val dateString: String,
    val typeString: String,
    val isSolar: Boolean,
    val jdeGreatest: Double,
    val magnitude: Double,
    val localTime: String? = null
)

data class EclipseDetail(
    val t0: Double,
    val deltaT: Double,
    val besselianTable: List<BesselianRow>,
    val contacts: List<ContactPoint>,
    val magnitude: Double,
    val gamma: Double,
    val sunRA: Double,
    val sunDec: Double,
    val sunSD: Double,
    val sunHP: Double,
    val moonRA: Double,
    val moonDec: Double,
    val moonSD: Double,
    val moonHP: Double,
    val type: EclipseParityEngine.EclipseType
)

data class BesselianRow(
    val orde: Int,
    val x: Double,
    val y: Double,
    val d: Double,
    val L1: Double,
    val L2: Double,
    val mu: Double
)

data class ContactPoint(
    val name: String,
    val jdeTD: Double,
    val latitude: Double, // Kept for backwards compatibility (e.g., Solar global path)
    val longitude: Double,
    val zenithLat: Double? = null,
    val zenithLon: Double? = null,
    val positionAngle: Double? = null,
    val axisDistance: Double? = null
)

data class LocalEclipseDetail(
    val type: String,
    val magnitude: Double,
    val obscuration: Double,
    val tzLabel: String,
    val timezone: Double,
    val t0: Double = 0.0,
    val deltaT: Double = 0.0,
    val tanf1: Double = 0.0,
    val tanf2: Double = 0.0,
    val besselianTable: List<BesselianRow> = emptyList(),
    val p1: ContactPoint?,
    val u1: ContactPoint?,
    val u2: ContactPoint?,
    val mx: ContactPoint,
    val u3: ContactPoint?,
    val u4: ContactPoint?,
    val p4: ContactPoint?,
    val sunRA: Double = 0.0,
    val sunDec: Double = 0.0,
    val sunSD: Double = 0.0,
    val sunHP: Double = 0.0,
    val moonRA: Double = 0.0,
    val moonDec: Double = 0.0,
    val moonSD: Double = 0.0,
    val moonHP: Double = 0.0,
    val sunrise: String = "--:--",
    val transit: String = "--:--",
    val sunset: String = "--:--",
    // Observer location fields
    val obsLat: Double = 0.0,
    val obsLon: Double = 0.0,
    val obsElev: Double = 0.0,
    val obsName: String = ""
)

data class LunarEclipseDetail(
    val type: String,
    val magUmbra: Double,
    val magPenumbra: Double,
    val gamma: Double = 0.0,
    val epsilon: Double = 0.0,
    val p1: ContactPoint?,
    val u1: ContactPoint?,
    val u2: ContactPoint?,
    val mx: ContactPoint,
    val u3: ContactPoint?,
    val u4: ContactPoint?,
    val p4: ContactPoint?,
    val sunRA: Double = 0.0,
    val sunDec: Double = 0.0,
    val sunSD: Double = 0.0,
    val sunHP: Double = 0.0,
    val moonRA: Double = 0.0,
    val moonDec: Double = 0.0,
    val moonSD: Double = 0.0,
    val moonHP: Double = 0.0,
    val rUmbra: Double = 0.0,
    val rPenumbra: Double = 0.0,
    val deltaT: Double = 0.0,
    val besselianTable: List<BesselianRow> = emptyList(),
    val shadowRule: String = "Herald/Sinnott",
    val shadowEnlargement: String = "1.000",
    val sarosSeries: String = "-",
    val timezone: Double = 0.0,
    val tzLabel: String = "UT"
)
