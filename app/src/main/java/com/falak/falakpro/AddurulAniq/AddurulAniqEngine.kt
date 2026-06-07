package com.falak.falakpro.AddurulAniq

import android.content.Context
import com.falak.falakpro.AddurulAniq.DecEotDatabase
import kotlin.math.*

data class IjtimaRow(
    val label: String,
    val A: Double = 0.0,
    val F: Double = 0.0,
    val M_prime: Double = 0.0,
    val M: Double = 0.0
)

data class PosisiRow(
    val label: String,
    val A: Double = 0.0,
    val S: Double = 0.0,
    val m_khosso: Double = 0.0,
    val M_wasath: Double = 0.0,
    val A_khosso: Double = 0.0,
    val N_hissah: Double = 0.0,
    val D_budu: Double = 0.0,
    val O_mail: Double = 0.0,
    val ST: Double = 0.0,
    val alamat: Long = 0,
    val hari: Int = 0,
    val pasaran: Int = 0
)

data class TadilDetail(
    val dalilName: String,
    val rumus: String,
    val harokat: Double,
    val madkhul1: Int,
    val madkhul2: Int,
    val satar1: Double,
    val satar2: Double,
    val kasru: Double,
    val tadil: Double
)

data class ThulSyamsResult(
    val S_wasath: Double,
    val m_khossotuha: Double,
    val s1_tadil: Double,
    val s2_tadil: Double,
    val sPrimeHaqiqi: Double,
    val dmHaqiqi: Double,
    val amHaqiqi: Double,
    val RHaqiqi: Double,
    val sdHaqiqi: Double,
    val eotHaqiqi: Double,
    val dip: Double,
    val hm: Double,
    val GM: Double,
    val azm: Double,
    val tadilDetails: List<TadilDetail> = emptyList(),
    val jarakDetails: List<TadilDetail> = emptyList()
)

data class IjtimaResult(
    val targetMonth: Int,
    val targetYear: Int,
    val am: String,
    val tgl: Int,
    val namaBulan: String,
    val thn: Int,
    val hariPasaran: String,
    val waktuIjtimaLT: Double,
    val ghrbLmtTaqribi: Double,
    val mGhurub: Double,
    val sdGhurub: Double,
    val dipGhurub: Double,
    val hGhurub: Double,
    val deklinasiGhurub: Double,
    val eotGhurub: Double,
    val ardhQamar: Double,
    val sabq: Double,
    val moResult: Double,
    val dcResult: Double,
    val acResult: Double,
    val distMoon: Double,
    val hpMoon: Double,
    val sdcMoon: Double,
    val gcMoon: Double,
    val hcMarkazi: Double,
    val azcMoon: Double,
    val bedaAzm: Double,
    val refraksi: Double,
    val parallax: Double,
    val hcSathi: Double,
    val eloMarkazi: Double,
    val eloSathi: Double,
    val nurulHilal: Double,
    val muktsulHilal: Double,
    val bedaTinggi: Double,
    val buduZawiyah: Double,
    val ghurubHilal: Double,
    val umurHilal: Double,
    
    val rows: List<PosisiRow>,
    val ijtimaRows: List<IjtimaRow>,
    val jumlah1: PosisiRow,
    val jumlah2: PosisiRow,
    
    val ijtimaTadil: List<TadilDetail> = emptyList(),
    val thulQomarTadil: List<TadilDetail> = emptyList(),
    val ardhQomarTadil: List<TadilDetail> = emptyList(),
    val jarakBumiBulanTadil: List<TadilDetail> = emptyList(),

    val matahari: ThulSyamsResult,
    val ghrbWdHaqiqi: Double
)

class AddurulAniqEngine(private val context: Context) {

    private val hisabDb by lazy { HisabDatabase(context) }
    private val tadilDb by lazy { TadilDatabase(context) }
    private val decEotDb by lazy { DecEotDatabase(context) }
    private val posisiDb by lazy { PosisiDatabase(context) }

    private fun normalize360(v: Double): Double {
        var res = v % 360.0
        if (res < 0) res += 360.0
        return res
    }

    private fun interpolateTadil(key: Double, selector: (TadilRecord) -> Double): Double {
        val absKey = abs(key)
        val k1 = (floor(absKey).toInt()) % 360
        val k2 = (k1 + 1) % 360
        val r1 = tadilDb.data[k1]
        val r2 = tadilDb.data[k2]
        if (r1 == null || r2 == null) return 0.0
        val v1 = selector(r1)
        val v2 = selector(r2)
        val fraction = absKey - floor(absKey)
        val result = v1 + fraction * (v2 - v1)
        return if (key < 0) -result else result
    }

    private fun getTadilDetail(dalilName: String, rumus: String, key: Double, selector: (TadilRecord) -> Double): TadilDetail {
        val absKey = abs(key)
        val k1 = (floor(absKey).toInt()) % 360
        val k2 = (k1 + 1) % 360
        val r1 = tadilDb.data[k1]
        val r2 = tadilDb.data[k2]
        if (r1 == null || r2 == null) return TadilDetail(dalilName, rumus, key, k1, k2, 0.0, 0.0, 0.0, 0.0)
        
        val v1 = selector(r1)
        val v2 = selector(r2)
        val fraction = absKey - floor(absKey)
        val result = v1 + fraction * (v2 - v1)
        val finalResult = if (key < 0) -result else result

        return TadilDetail(
            dalilName = dalilName,
            rumus = rumus,
            harokat = absKey,
            madkhul1 = k1,
            madkhul2 = k2,
            satar1 = v1,
            satar2 = v2,
            kasru = fraction,
            tadil = finalResult
        )
    }

    private fun MainRow.toIjtimaRow(label: String) = IjtimaRow(
        label = label,
        A = values[0], 
        F = values[1],
        M_prime = values[2],
        M = values[3]
    )

    private fun HisabRecord.toIjtimaRow(label: String) = IjtimaRow(
        label = label,
        A = a,
        F = f,
        M_prime = m1,
        M = m
    )

    private fun sumIjtimaRows(label: String, vararg rows: IjtimaRow): IjtimaRow {
        var a = 0.0; var f = 0.0; var mp = 0.0; var m = 0.0
        for (row in rows) {
            a += row.A
            f += row.F
            mp += row.M_prime
            m += row.M
        }
        return IjtimaRow(label, a, normalize360(f), normalize360(mp), normalize360(m))
    }

    private fun MainRow.toPosisiRow(label: String) = PosisiRow(
        label = label,
        alamat = this.alamat.toLong(),
        hari = this.hari,
        pasaran = this.values[0].toInt(),
        S = values[1],
        m_khosso = values[2],
        M_wasath = values[3],
        A_khosso = values[4],
        N_hissah = values[5],
        D_budu = values[6],
        O_mail = values[7],
        ST = values[8]
    )

    private fun TimeRow.toPosisiRow(label: String, sign: Int = 1) = PosisiRow(
        label = label,
        alamat = 0,
        hari = 0,
        pasaran = 0,
        S = values.getOrElse(0) { 0.0 } * sign,
        m_khosso = values.getOrElse(1) { 0.0 } * sign,
        M_wasath = values.getOrElse(2) { 0.0 } * sign,
        A_khosso = values.getOrElse(3) { 0.0 } * sign,
        N_hissah = values.getOrElse(4) { 0.0 } * sign,
        D_budu = values.getOrElse(5) { 0.0 } * sign,
        O_mail = 0.0,
        ST = values.getOrElse(6) { 0.0 } * sign
    )

    private fun sumPosisiRows(label: String, vararg rows: PosisiRow): PosisiRow {
        var a = 0.0; var s = 0.0; var mk = 0.0; var mw = 0.0
        var ak = 0.0; var nh = 0.0; var db = 0.0; var om = 0.0; var st = 0.0
        var alm = 0L; var hr = 0; var ps = 0
        for (row in rows) {
            alm += row.alamat; hr += row.hari; ps += row.pasaran
            s += row.S; mk += row.m_khosso; mw += row.M_wasath
            ak += row.A_khosso; nh += row.N_hissah; db += row.D_budu; om += row.O_mail; st += row.ST
        }
        var nhr = hr % 7; if (nhr == 0) nhr = 7
        var nps = ps % 5; if (nps == 0) nps = 5
        return PosisiRow(label, 0.0, normalize360(s), normalize360(mk), normalize360(mw), 
                         normalize360(ak), normalize360(nh), normalize360(db), om, normalize360(st),
                         alamat = alm, hari = nhr.toInt(), pasaran = nps.toInt())
    }

    fun hitungIjtima(targetYear: Int, targetMonth: Int, lat: Double, lon: Double, tinggi: Double, timezone: Double): IjtimaResult {
        val (initialDay, initialMonth, initialYear) = jdToGregorian(2451545.0 + (targetYear - 1420) * 354.36)
        val decimalYear = initialYear + (initialMonth - 1.0) / 12.0 + initialDay / 365.25
        val deltaT = calculateDeltaT(decimalYear)
        val refYear = if (targetMonth == 1) targetYear - 1 else targetYear
        val refMonth = if (targetMonth == 1) 12 else targetMonth - 1

        val keyMajmuah = (refYear / 30) * 30
        val keyMabsuthoh = refYear % 30
        val keyBulan = refMonth

        val hMaj = hisabDb.getMajmuah(keyMajmuah) ?: throw Exception("Majmu'ah $keyMajmuah not found")
        val hMab = hisabDb.getMabsuthoh(keyMabsuthoh) ?: throw Exception("Mabsuthoh $keyMabsuthoh not found")
        val hBul = hisabDb.getBulan(keyBulan) ?: throw Exception("Bulan $keyBulan not found")
 
        val sumM = (hMaj.m + hMab.m + hBul.m) % 360
        val sumM1 = (hMaj.m1 + hMab.m1 + hBul.m1) % 360
        val sumF = (hMaj.f + hMab.f + hBul.f) % 360

        val t1Detail = getTadilDetail("Dalil 1", "M", sumM) { it.t1 }
        val t2Detail = getTadilDetail("Dalil 2", "2xM", sumM * 2) { it.t2 }
        val t3Detail = getTadilDetail("Dalil 3", "M'", sumM1) { it.t3 }
        val t4Detail = getTadilDetail("Dalil 4", "2xM'", sumM1 * 2) { it.t4 }
        val t5Detail = getTadilDetail("Dalil 5", "M+M'", sumM + sumM1) { it.t5 }
        val t6Detail = getTadilDetail("Dalil 6", "M-M'", sumM - sumM1) { it.t6 }
        val t7Detail = getTadilDetail("Dalil 7", "2xF", sumF * 2) { it.t7 }
        val t8Detail = getTadilDetail("Dalil 8", "2xF-M'", sumF * 2 - sumM1) { it.t8 }

        val ijtimaTadilList = listOf(t1Detail, t2Detail, t3Detail, t4Detail, t5Detail, t6Detail, t7Detail, t8Detail)
        val totalT = ijtimaTadilList.sumOf { it.tadil }
        
        val rMajI = hMaj.toIjtimaRow("MAJMU'AH")
        val rMabI = hMab.toIjtimaRow("MABSUTHOH")
        val rBulI = hBul.toIjtimaRow("BULAN")

        val amTemp = (hMaj.a + hMab.a + hBul.a) + totalT + 0.5
        
        val et = (amTemp % 1.0) * 24.0
        val ut = et - (deltaT / 3600.0)
        var wd = ut + timezone
        while (wd >= 24.0) wd -= 24.0
        while (wd < 0.0) wd += 24.0

        val hpMaj = posisiDb.getMajmuah(keyMajmuah)?.hari ?: 0
        val hpMab = posisiDb.getMabsuthoh(keyMabsuthoh)?.hari ?: 0
        val hpBul = posisiDb.getBulan(keyBulan)?.hari ?: 0
        var f45 = (hpMaj + hpMab + hpBul) % 7
        if (f45 == 0) f45 = 7
        
        val amFinal = amTemp - (deltaT / 86400.0)
        val localJd = amFinal + (timezone / 24.0)
        
        var f44 = (floor(localJd).toLong() + 2) % 7
        if (f44 == 0L) f44 = 7
        val trialDay = when ((if (f44 <= f45) f44 + 7 else f44) - f45) {
            6L -> 27; 7L -> 28; 1L -> 29; else -> 30
        }
        
        val rMajP = posisiDb.getMajmuah(keyMajmuah)?.toPosisiRow("MAJMU'AH") ?: PosisiRow("MAJMU'AH")
        val rMabP = posisiDb.getMabsuthoh(keyMabsuthoh)?.toPosisiRow("MABSUTHOH") ?: PosisiRow("MABSUTHOH")
        val rBulP = posisiDb.getBulan(keyBulan)?.toPosisiRow("BULAN") ?: PosisiRow("BULAN")
        val rHarP = posisiDb.getHari(trialDay)?.toPosisiRow("HARI") ?: PosisiRow("HARI")
        
        val mBase = (rMajP.m_khosso + rMabP.m_khosso + rBulP.m_khosso + rHarP.m_khosso) % 360
        val (gDaySunset, gMonthSunset, gYearSunset) = jdToGregorian(amFinal + (timezone / 24.0))
        val gInitial = hitungGhurubLMT(gDaySunset, gMonthSunset, mBase, lat, lon, tinggi)
        
        val gAbs = abs(gInitial.ghrbLmt)
        val rJamG = posisiDb.getJam(floor(gAbs).toInt())?.toPosisiRow("JAM GHRB") ?: PosisiRow("JAM GHRB")
        val rMenG = posisiDb.getMenit(floor((gAbs - floor(gAbs)) * 60).toInt())?.toPosisiRow("Menit GHRB") ?: PosisiRow("Menit GHRB")
        val rDetG = posisiDb.getDetik(round(((gAbs - floor(gAbs)) * 60 - floor((gAbs - floor(gAbs)) * 60)) * 60).toInt())?.toPosisiRow("Detik GHRB") ?: PosisiRow("Detik GHRB")
        
        val jumlah1 = sumPosisiRows("JUMLAH 1", rMajP, rMabP, rBulP, rHarP, rJamG, rMenG, rDetG)
        
        val diffTime = (113.25 - lon) / 15.0
        val sftSign = if (diffTime >= 0) 1 else -1
        val absSft = abs(diffTime)
        val sftH = floor(absSft).toInt()
        val sftM = floor((absSft - sftH) * 60).toInt()
        val sftS = round(((absSft - sftH) * 60 - sftM) * 60).toInt()
        
        val rSftJ = posisiDb.getJam(sftH)?.toPosisiRow("SFT JAM", sftSign) ?: PosisiRow("SFT JAM")
        val rSftM = posisiDb.getMenit(sftM)?.toPosisiRow("SFT MENIT", sftSign) ?: PosisiRow("SFT MENIT")
        val rSftD = posisiDb.getDetik(sftS)?.toPosisiRow("SFT DETIK", sftSign) ?: PosisiRow("SFT DETIK")
        
        val jumlah2 = sumPosisiRows("JUMLAH 2", jumlah1, rSftJ, rSftM, rSftD)
        
        // 1. Thul Syams (S')
        val s1Detail = getTadilDetail("DALIL 1", "m", jumlah2.m_khosso) { it.l1 }
        val s2Detail = getTadilDetail("DALIL 2", "2 x m", normalize360(jumlah2.m_khosso * 2.0)) { it.l2 }
        val sPrime = normalize360(jumlah2.S + s1Detail.tadil + s2Detail.tadil)
        
        // 2. Deklinasi (dm)
        val obliquity = jumlah2.O_mail
        val dm = Math.toDegrees(asin(sin(Math.toRadians(sPrime)) * sin(Math.toRadians(obliquity))))
        
        // 3. Ascensiorekta (am)
        var amRaw = Math.toDegrees(atan(tan(Math.toRadians(sPrime)) * cos(Math.toRadians(obliquity))))
        if (sPrime >= 90 && sPrime < 270) amRaw += 180.0 else if (sPrime >= 270 && sPrime < 360) amRaw += 360.0
        val amResult = normalize360(amRaw)
        
        // 4. Jarak Bumi-Matahari (R)
        val r1Detail = getTadilDetail("DALIL 1", "m", jumlah2.m_khosso) { it.r1 }
        val r2Detail = getTadilDetail("DALIL 2", "2xm", normalize360(jumlah2.m_khosso * 2.0)) { it.r2 }
        val R = 1.00014 + r1Detail.tadil + r2Detail.tadil
        
        // 5. Semidiameter (sd)
        val sdHaqiqi = (15.0/60.0 + 59.63/3600.0) / R
        
        // 6. Equation of Time (e)
        var eotHaqiqi = (jumlah2.S - amResult) / 15.0
        if (eotHaqiqi > 12) eotHaqiqi -= 24.0 else if (eotHaqiqi < -12) eotHaqiqi += 24.0

        // 7. Dip
        val dip = (1.76 / 60.0) * sqrt(tinggi)
        
        // 8. hm (Altitude)
        val hmSun = -(sdHaqiqi + 34.5/60.0 + dip)
        
        // 9. GM (Sudut Waktu)
        val latRad = Math.toRadians(lat)
        val dmRad = Math.toRadians(dm)
        val hmSunRad = Math.toRadians(hmSun)
        val cosGM = (sin(hmSunRad) - sin(latRad) * sin(dmRad)) / (cos(latRad) * cos(dmRad))
        val gmDeg = Math.toDegrees(acos(cosGM))
        
        // 10. Sunset WD
        val ghrbWdHaqiqi = gmDeg / 15.0 + 12.0 - eotHaqiqi + ((timezone * 15.0) - lon) / 15.0
        
        // 11. Azm Matahari
        val azmSun = normalize360(270.0 + Math.toDegrees(atan(-sin(latRad)/tan(Math.toRadians(gmDeg)) + cos(latRad)*tan(dmRad)/sin(Math.toRadians(gmDeg)))))

        val matahari = ThulSyamsResult(
            S_wasath = jumlah2.S,
            m_khossotuha = jumlah2.m_khosso,
            s1_tadil = s1Detail.tadil,
            s2_tadil = s2Detail.tadil,
            sPrimeHaqiqi = sPrime,
            dmHaqiqi = dm,
            amHaqiqi = amResult,
            RHaqiqi = R,
            sdHaqiqi = sdHaqiqi,
            eotHaqiqi = eotHaqiqi,
            dip = dip,
            hm = hmSun,
            GM = gmDeg,
            azm = azmSun,
            tadilDetails = listOf(s1Detail, s2Detail),
            jarakDetails = listOf(r1Detail, r2Detail)
        )

        // 1. Thulul Qomar (Mo)
        val m1Detail = getTadilDetail("DALIL 1", "A", jumlah2.A_khosso) { it.m1 }
        val m2Detail = getTadilDetail("DALIL 2", "2 x D - A", normalize360(2.0 * jumlah2.D_budu - jumlah2.A_khosso)) { it.m2 }
        val m3Detail = getTadilDetail("DALIL 3", "2 x D", normalize360(2.0 * jumlah2.D_budu)) { it.m3 }
        val m4Detail = getTadilDetail("DALIL 4", "2 x A", normalize360(2.0 * jumlah2.A_khosso)) { it.m4 }
        val m5Detail = getTadilDetail("DALIL 5", "m", jumlah2.m_khosso) { it.m5 }
        val m6Detail = getTadilDetail("DALIL 6", "2 x N", normalize360(2.0 * jumlah2.N_hissah)) { it.m6 }
        val m7Detail = getTadilDetail("DALIL 7", "2 x D - 2 x A", normalize360(2.0 * jumlah2.D_budu - 2.0 * jumlah2.A_khosso)) { it.m7 }
        val m8Detail = getTadilDetail("DALIL 8", "2 x D - m - A", normalize360(2.0 * jumlah2.D_budu - jumlah2.m_khosso - jumlah2.A_khosso)) { it.m8 }
        val m9Detail = getTadilDetail("DALIL 9", "2 x D + A", normalize360(2.0 * jumlah2.D_budu + jumlah2.A_khosso)) { it.m9 }
        
        val thulQomarTadilList = listOf(m1Detail, m2Detail, m3Detail, m4Detail, m5Detail, m6Detail, m7Detail, m8Detail, m9Detail)
        val moRaw = jumlah2.M_wasath + thulQomarTadilList.sumOf { it.tadil }
        val moResult = normalize360(moRaw)
        
        // 2. Ardhul Qomar (B)
        val b1Detail = getTadilDetail("DALIL 1", "N", jumlah2.N_hissah) { it.b1 }
        val b2Detail = getTadilDetail("DALIL 2", "A + N", normalize360(jumlah2.A_khosso + jumlah2.N_hissah)) { it.b2 }
        val b3Detail = getTadilDetail("DALIL 3", "A - N", normalize360(jumlah2.A_khosso - jumlah2.N_hissah)) { it.b3 }
        val b4Detail = getTadilDetail("DALIL 4", "2 x D - N", normalize360(2.0 * jumlah2.D_budu - jumlah2.N_hissah)) { it.b4 }
        
        val ardhQomarTadilList = listOf(b1Detail, b2Detail, b3Detail, b4Detail)
        val bResult = ardhQomarTadilList.sumOf { it.tadil }
        
        // 3. Deklinasi Bulan (dc)
        val bRad = Math.toRadians(bResult)
        val oRad = Math.toRadians(jumlah2.O_mail)
        val moRadForDc = Math.toRadians(moResult)
        val dcRad = asin(sin(bRad) * cos(oRad) + cos(bRad) * sin(oRad) * sin(moRadForDc))
        val dcResult = Math.toDegrees(dcRad)
        
        // 4. Ascensiorekta Bulan (ac)
        val cosMo = cos(moRadForDc)
        val cosB = cos(bRad)
        val cosDc = cos(dcRad)
        var acResult = Math.toDegrees(acos(cosMo * cosB / cosDc))
        if (moResult > 180.0) acResult = 360.0 - acResult
        
        // 5. Jarak Bumi-Bulan (r)
        val mr1Detail = getTadilDetail("DALIL 1", "A", jumlah2.A_khosso) { it.rr1 }
        val mr2Detail = getTadilDetail("DALIL 2", "2 x D - A", normalize360(2.0 * jumlah2.D_budu - jumlah2.A_khosso)) { it.rr2 }
        val mr3Detail = getTadilDetail("DALIL 3", "2 x D", normalize360(2.0 * jumlah2.D_budu)) { it.rr3 }
        val mr4Detail = getTadilDetail("DALIL 4", "2 x A", normalize360(2.0 * jumlah2.A_khosso)) { it.rr4 }
        
        val jarakBulanTadilList = listOf(mr1Detail, mr2Detail, mr3Detail, mr4Detail)
        val distMoon = 385000.56 + jarakBulanTadilList.sumOf { it.tadil }
        
        // 6. Horizontal Parallax (Hp)
        val hpMoon = Math.toDegrees(asin(6378.14 / distMoon))
        
        // 7. Semidiameter Bulan (sdc)
        val sdcMoon = 0.272476 * hpMoon
        
        // 8. Sudut Waktu Bulan (GC)
        val gcMoon = normalize360(jumlah2.ST - acResult + lon)
        
        // 9. Altitude Bulan Geocentric (hc)
        val phiRad = Math.toRadians(lat)
        val gcRad = Math.toRadians(gcMoon)
        val hcMarkaziRad = asin(sin(phiRad) * sin(dcRad) + cos(phiRad) * cos(dcRad) * cos(gcRad))
        val hcMarkazi = Math.toDegrees(hcMarkaziRad)
        
        // 10. Azimut Bulan (azc)
        val azcMoon = normalize360(Math.toDegrees(atan2(sin(gcRad), cos(gcRad) * sin(phiRad) - tan(dcRad) * cos(phiRad))) + 180.0)
        
        // 11. Beda Azimut (z)
        val bedaAzm = azcMoon - matahari.azm
        
        // 12. Refraction (Ref)
        val refraksi = 0.0167 / tan(Math.toRadians(hcMarkazi + 7.31 / (hcMarkazi + 4.4)))
        
        // 13. Parallax Bulan (P)
        val parallax = hpMoon * cos(hcMarkaziRad)
        
        // 14. Altitude Bulan Toposentrik (hc')
        val hcSathi = hcMarkazi - parallax
        
        // 15. Elongasi Markazi (Mengikuti rumus Excel: =ACOS(COS((T69-T35)*PI)*COS(T77*PI))/PI)
        val t69 = moResult
        val t35 = matahari.sPrimeHaqiqi
        val t77 = bResult
        val eloMarkazi = Math.toDegrees(acos(cos(Math.toRadians(t69 - t35)) * cos(Math.toRadians(t77))))
        
        val hcSathiRad = Math.toRadians(hcSathi)
        val zRad = Math.toRadians(bedaAzm)
        val eloSathi = Math.toDegrees(acos(sin(hcSathiRad) * sin(hmSunRad) + cos(hcSathiRad) * cos(hmSunRad) * cos(zRad)))
        
        // sudut i (Phase Angle)
        val iAngle = 180.0 - eloMarkazi - 0.1468 * (1.0 - 0.0549 * sin(Math.toRadians(jumlah2.A_khosso)) / (1.0 - 0.0167 * sin(Math.toRadians(jumlah2.m_khosso)))) * sin(Math.toRadians(eloMarkazi))
        
        // 16. Nurul Hilal (nh) - Interpolasi dari Tabel nq
        val nurulHilalPercent = interpolateTadil(iAngle) { it.nq }
        val nurulHilal = nurulHilalPercent * 100.0
        
        // 17. Muktsul Hilal (mh)
        val muktsulHilal = hcMarkazi * 4.0
        
        // 18. Beda Tinggi (Y)
        val bedaTinggi = hcMarkazi - matahari.hm
        
        // 19. Beda Jarak Sudut (C)
        val yRad = Math.toRadians(bedaTinggi)
        val buduZawiyah = Math.toDegrees(acos(cos(zRad) * cos(yRad)))
        
        // 20. Ghurubul Hilal (GH)
        val ghurubHilal = ghrbWdHaqiqi + (muktsulHilal / 60.0)
        
        // 21. Umur Hilal (Age)
        val umurHilal = ghrbWdHaqiqi - wd
        
        val gregorianMonths = listOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val daysNames = listOf("", "Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu")
        val pasaranNames = listOf("Kliwon", "Legi", "Paing", "Pon", "Wage")

        val hrIdx = ((floor(localJd).toLong() + 2) % 7).toInt()
        val psrIdx = ((floor(localJd).toLong() + 1) % 5).toInt()
        
        return IjtimaResult(
            targetMonth = targetMonth,
            targetYear = targetYear,
            am = String.format("%.3f", amTemp),
            tgl = gDaySunset,
            namaBulan = gregorianMonths[gMonthSunset], 
            thn = gYearSunset,
            hariPasaran = "${daysNames[if(hrIdx==0) 7 else hrIdx]} ${pasaranNames[psrIdx]}",
            waktuIjtimaLT = wd,
            ghrbLmtTaqribi = gInitial.ghrbLmt,
            mGhurub = gInitial.m,
            sdGhurub = gInitial.sd,
            dipGhurub = gInitial.dip,
            hGhurub = gInitial.h,
            deklinasiGhurub = gInitial.deklinasi,
            eotGhurub = gInitial.eqTime,
            ardhQamar = bResult,
            sabq = 0.51,
            moResult = moResult,
            dcResult = dcResult,
            acResult = acResult,
            distMoon = distMoon,
            hpMoon = hpMoon,
            sdcMoon = sdcMoon,
            gcMoon = gcMoon,
            hcMarkazi = hcMarkazi,
            azcMoon = azcMoon,
            bedaAzm = bedaAzm,
            refraksi = refraksi,
            parallax = parallax,
            hcSathi = hcSathi,
            eloMarkazi = eloMarkazi,
            eloSathi = eloSathi,
            nurulHilal = nurulHilal,
            muktsulHilal = muktsulHilal,
            bedaTinggi = bedaTinggi,
            buduZawiyah = buduZawiyah,
            ghurubHilal = ghurubHilal,
            umurHilal = umurHilal,
            rows = listOf(rMajP, rMabP, rBulP, rHarP, rJamG, rMenG, rDetG, rSftJ, rSftM, rSftD),
            ijtimaRows = listOf(rMajI, rMabI, rBulI),
            jumlah1 = jumlah1,
            jumlah2 = jumlah2,
            ijtimaTadil = ijtimaTadilList,
            thulQomarTadil = thulQomarTadilList,
            ardhQomarTadil = ardhQomarTadilList,
            jarakBumiBulanTadil = jarakBulanTadilList,
            matahari = matahari,
            ghrbWdHaqiqi = ghrbWdHaqiqi
        )
    }

    fun jdToGregorian(jd: Double): Triple<Int, Int, Int> {
        val z = floor(jd).toLong()
        val a = if (z < 2299161) z else {
            val alpha = floor((z - 1867216.25) / 36524.25).toLong()
            z + 1 + alpha - floor(alpha / 4.0).toLong()
        }
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toLong()
        val d = floor(365.25 * c).toLong()
        val e = floor((b - d) / 30.6001).toLong()
        val day = (b - d - floor(30.6001 * e).toLong()).toInt()
        val month = (if (e < 14) e - 1 else e - 13).toInt()
        val year = (if (month > 2) c - 4716 else c - 4715).toInt()
        return Triple(day, month, year)
    }

    fun gregorianToJd(day: Int, month: Int, year: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun hitungGhurubLMT(day: Int, month: Int, m: Double, lat: Double, lon: Double, tinggi: Double): GhurubResult {
        val decEot = decEotDb.getData(day, month)
        val deklinasi = decEot?.deklinasi ?: 0.0
        val eqTime = decEot?.eot ?: 0.0
        val sd = 0.267 / (1.0 - 0.017 * cos(Math.toRadians(m)))
        val dip = (1.76 / 60.0) * sqrt(tinggi)
        val h = -(sd + (34.5 / 60.0) + dip)
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(deklinasi)
        val hRad = Math.toRadians(h)
        val cosH = (sin(hRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val hourAngle = Math.toDegrees(acos(cosH)) / 15.0
        val ghrbLmt = hourAngle + 12.0 - eqTime
        return GhurubResult(m, sd, dip, h, deklinasi, eqTime, ghrbLmt)
    }

    data class GhurubResult(val m: Double, val sd: Double, val dip: Double, val h: Double, val deklinasi: Double, val eqTime: Double, val ghrbLmt: Double)

    private fun calculateDeltaT(year: Double): Double {
        return when {
            year <= -500 -> -20.0 + 32.0 * ((year / 100.0) - 18.2).pow(2)
            year <= 500 -> 10583.6 - 1014.41 * (year / 100.0) + 33.78311 * (year / 100.0).pow(2) - 5.952053 * (year / 100.0).pow(3) - 0.1798452 * (year / 100.0).pow(4) + 0.022174192 * (year / 100.0).pow(5) + 0.0090316521 * (year / 100.0).pow(6)
            year <= 1600 -> 1574.2 - 556.01 * ((year / 100.0) - 10.0) + 71.23472 * ((year / 100.0) - 10.0).pow(2) + 0.319781 * ((year / 100.0) - 10.0).pow(3) - 0.8503463 * ((year / 100.0) - 10.0).pow(4) - 0.005050998 * ((year / 100.0) - 10.0).pow(5) + 0.0083572073 * ((year / 100.0) - 10.0).pow(6)
            year <= 1700 -> 120.0 - 0.9808 * (year - 1600.0) - 0.01532 * (year - 1600.0).pow(2) + (year - 1600.0).pow(3) / 7129.0
            year <= 1800 -> 8.83 + 0.1603 * (year - 1700.0) - 0.0059285 * (year - 1700.0).pow(2) + 0.00013336 * (year - 1700.0).pow(3) - (year - 1700.0).pow(4) / 1174000.0
            year <= 1860 -> 13.72 - 0.332447 * (year - 1800.0) + 0.0068612 * (year - 1800.0).pow(2) + 0.0041116 * (year - 1800.0).pow(3) - 0.00037436 * (year - 1800.0).pow(4) + 0.0000121272 * (year - 1800.0).pow(5) - 0.0000001699 * (year - 1800.0).pow(6) + 0.000000000875 * (year - 1800.0).pow(7)
            year <= 1900 -> 7.62 + 0.5737 * (year - 1860.0) - 0.251754 * (year - 1860.0).pow(2) + 0.01680668 * (year - 1860.0).pow(3) - 0.0004473624 * (year - 1860.0).pow(4) + (year - 1860.0).pow(5) / 233174.0
            year <= 1920 -> -2.79 + 1.494119 * (year - 1900.0) - 0.0598939 * (year - 1900.0).pow(2) + 0.0061966 * (year - 1900.0).pow(3) - 0.000197 * (year - 1900.0).pow(4)
            year <= 1941 -> 21.2 + 0.84493 * (year - 1920.0) - 0.0761 * (year - 1920.0).pow(2) + 0.0020936 * (year - 1920.0).pow(3)
            year <= 1961 -> 29.07 + 0.407 * (year - 1950.0) - (year - 1950.0).pow(2) / 233.0 + (year - 1950.0).pow(3) / 2547.0
            year <= 1986 -> 45.45 + 1.067 * (year - 1975.0) - (year - 1975.0).pow(2) / 260.0 - (year - 1975.0).pow(3) / 718.0
            year <= 2005 -> 63.86 + 0.3345 * (year - 2000.0) - 0.060374 + (year - 2000.0).pow(2) + 0.0017275 * (year - 2000.0).pow(2) + 0.000651814 * (year - 2000.0).pow(4) + 0.00002373599 * (year - 2000.0).pow(5)
            year <= 2050 -> 62.92 + 0.32217 * (year - 2000.0) + 0.005589 * (year - 2000.0).pow(2)
            year <= 2150 -> -20.0 + 32.0 * ((year - 1820.0) / 100.0).pow(2) - 0.5628 * (2150.0 - year)
            else -> -20.0 + 32.0 * ((year - 1820.0) / 100.0).pow(2)
        }
    }
}
