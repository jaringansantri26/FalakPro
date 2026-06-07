package com.falak.falakpro.premium

import kotlin.math.*

/**
 * CalendarFunctions.kt
 * Konversi Kalender Hijriah, Yahudi, Paskah, dan Matematika Kalender
 * Referensi: Jean Meeus, "Astronomical Algorithms" 2nd Ed.
 *   Ch. 9  — Gregorian/Julian Calendar
 *   Ch. 9  — Easter (Gregorian & Julian)
 *   Ch. 9  — Jewish Calendar
 *   Ch. 9  — Islamic Calendar
 */
object CalendarFunctions {

    // ─────────────────────────────────────────────────────────────────────────
    // A. KALENDER GREGORIAN / JULIAN
    // ─────────────────────────────────────────────────────────────────────────

    data class CalDate(val year: Int, val month: Int, val day: Int)

    /** Apakah tahun kabisat Gregorian? */
    fun isLeapGregorian(year: Int): Boolean =
        (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)

    /** Apakah tahun kabisat Julian? */
    fun isLeapJulian(year: Int): Boolean = (year % 4 == 0)

    /** Gregorian → JDE */
    fun gregorianToJde(year: Int, month: Int, day: Double): Double {
        var y = year; var m = month
        if (m <= 2) { y--; m += 12 }
        val A = y / 100
        val B = 2 - A + A / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5
    }

    /** Julian (pra-Gregorian) → JDE */
    fun julianToJde(year: Int, month: Int, day: Double): Double {
        var y = year; var m = month
        if (m <= 2) { y--; m += 12 }
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day - 1524.5
    }

    /** JDE → Gregorian */
    fun jdeToGregorian(jde: Double): Triple<Int, Int, Double> {
        val z = floor(jde + 0.5).toInt()
        val f = jde + 0.5 - z
        val alpha = floor((z - 1867216.25) / 36524.25).toInt()
        val a = z + 1 + alpha - alpha / 4
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt()
        val day   = b - d - floor(30.6001 * e) + f
        val month = if (e < 14) e - 1 else e - 13
        val year  = if (month > 2) c - 4716 else c - 4715
        return Triple(year, month, day)
    }

    /** JDE → Julian Calendar */
    fun jdeToJulian(jde: Double): Triple<Int, Int, Double> {
        val z = floor(jde + 0.5).toInt()
        val f = jde + 0.5 - z
        val a = z
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt()
        val day   = b - d - floor(30.6001 * e) + f
        val month = if (e < 14) e - 1 else e - 13
        val year  = if (month > 2) c - 4716 else c - 4715
        return Triple(year, month, day)
    }

    /** Hari ke-N dalam tahun */
    fun dayOfYear(year: Int, month: Int, day: Int): Int {
        val k = if (isLeapGregorian(year)) 1 else 2
        return (275 * month / 9) - k * ((month + 9) / 12) + day - 30
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B. KALENDER ISLAM / HIJRIAH (Meeus Ch. 9)
    // ─────────────────────────────────────────────────────────────────────────
    // Menggunakan Kalender Hijriah Arithmetik (Tabulasi)
    // Epoch: 1 Muharram 1 H = 16 Juli 622 M (Julian) = JDE 1948439.5

    private const val HIJRI_EPOCH = 1948439.5  // JDE

    /** Apakah tahun Hijriah kabisat? (30 tahun = 10631 hari) */
    fun isLeapHijri(year: Int): Boolean =
        (11 * year + 14) % 30 < 11

    /** Kalender Hijriah → JDE */
    fun hijriToJde(year: Int, month: Int, day: Int): Double {
        return day +
                ceil(29.5 * (month - 1)).toLong() +
                (year - 1) * 354L +
                floor((3 + 11 * year) / 30.0) +
                HIJRI_EPOCH - 1
    }

    /** JDE → Kalender Hijriah */
    fun jdeToHijri(jde: Double): Triple<Int, Int, Int> {
        val jd   = floor(jde) + 0.5
        val year = floor((30 * (jd - HIJRI_EPOCH) + 10646) / 10631).toInt()
        val month = minOf(12, ceil((jd - (29 + hijriToJde(year, 1, 1))) / 29.5 + 1).toInt())
        val day  = (jd - hijriToJde(year, month, 1) + 1).toInt()
        return Triple(year, month, day)
    }

    /** Nama bulan Hijriah */
    val HIJRI_MONTH_NAMES = listOf(
        "Muharram", "Shafar", "Rabi'ul Awal", "Rabi'ul Akhir",
        "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
        "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"
    )

    /** Jumlah hari dalam bulan Hijriah */
    fun daysInHijriMonth(year: Int, month: Int): Int {
        if (month == 12 && isLeapHijri(year)) return 30
        return if (month % 2 == 1) 30 else 29
    }

    /** Jumlah hari dalam tahun Hijriah */
    fun daysInHijriYear(year: Int): Int = if (isLeapHijri(year)) 355 else 354

    // ─────────────────────────────────────────────────────────────────────────
    // C. KALENDER YAHUDI (Meeus Ch. 9)
    // ─────────────────────────────────────────────────────────────────────────

    /** Apakah tahun Yahudi kabisat (13 bulan)? */
    fun isLeapJewish(year: Int): Boolean = (7 * year + 1) % 19 < 7

    /** Jumlah bulan dalam tahun Yahudi */
    fun monthsInJewishYear(year: Int): Int = if (isLeapJewish(year)) 13 else 12

    /** Epoch kalender Yahudi (1 Tishri 1 = JDE 347995.5) */
    private const val JEWISH_EPOCH = 347995.5

    /** JDE dari 1 Tishri tahun Yahudi (Rosh Hashanah) */
    fun jewishNewYear(year: Int): Double {
        val months = floor(235.0 * year - 234.0) / 19.0
        val parts  = 12084 + 13753 * months
        var day    = months * 29 + floor(parts / 25920)
        if ((3 * (day + 1)) % 7 < 3) day++
        return JEWISH_EPOCH + day
    }

    /** Jumlah hari dalam tahun Yahudi */
    fun daysInJewishYear(year: Int): Int =
        (jewishNewYear(year + 1) - jewishNewYear(year)).toInt()

    /** Apakah tahun Yahudi panjang (shelemah)? */
    fun isJewishYearShelemah(year: Int): Boolean = daysInJewishYear(year) % 10 == 5

    /** Apakah tahun Yahudi pendek (hasserah)? */
    fun isJewishYearHasserah(year: Int): Boolean = daysInJewishYear(year) % 10 == 3

    /** Kalender Yahudi → JDE */
    fun jewishToJde(year: Int, month: Int, day: Int): Double {
        val months0 = floor((235.0 * year - 234.0) / 19.0).toInt()
        var jde = JEWISH_EPOCH + 29.0 * months0
        val ck = (12084 + 13753 * months0) % 25920
        if (ck >= 9575) jde++
        // Tambah bulan yang sudah lewat dalam tahun ini
        val monthDays = listOf(0,30,29,29,30,29,30,29,30,29,30,29,30,29)
        for (m in 1 until month) {
            jde += monthDays.getOrElse(m) { 29 }.toDouble()
        }
        jde += day - 1
        return jde
    }

    val JEWISH_MONTHS_REGULAR  = listOf("Nisan","Iyyar","Sivan","Tammuz","Av","Elul",
        "Tishri","Cheshvan","Kislev","Tevet","Shevat","Adar")
    val JEWISH_MONTHS_LEAP = listOf("Nisan","Iyyar","Sivan","Tammuz","Av","Elul",
        "Tishri","Cheshvan","Kislev","Tevet","Shevat","Adar I","Adar II")

    // ─────────────────────────────────────────────────────────────────────────
    // D. HARI PASKAH (Meeus Ch. 9)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Hitung tanggal Paskah Gregorian (Western Easter)
     * Algoritma Meeus/Jones/Butcher
     * @return CalDate tanggal Paskah
     */
    fun easterGregorian(year: Int): CalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day   = ((h + l - 7 * m + 114) % 31) + 1
        return CalDate(year, month, day)
    }

    /**
     * Hitung tanggal Paskah Julian (Eastern/Orthodox Easter)
     * Algoritma Meeus
     */
    fun easterJulian(year: Int): CalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day   = ((d + e + 114) % 31) + 1
        return CalDate(year, month, day)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // E. NAMA HARI & PASARAN JAWA
    // ─────────────────────────────────────────────────────────────────────────

    val DAYS_ARABIC  = listOf("Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu")
    val DAYS_LATIN   = listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    val PASARAN_JAWA = listOf("Kliwon","Legi","Pahing","Pon","Wage")
    val PASARAN_SUNDA =
        listOf("Kaliwon","Manis","Pahing","Pon","Wage")
    val SUNDANESE_DAY_NAMES = listOf("Radite", "Soma", "Anggara", "Buda", "Respati", "Sukra", "Tumpek")

    val SUNDA_MONTHS = listOf(
        "Sura", "Sapar", "Mulud", "Silih Mulud",
        "Jumadil Awal", "Jumadil Akhir", "Rajab", "Rewah",
        "Puasa", "Syawal", "Hapit", "Rayagung"
    )

    val CAKA_SUNDA_MONTHS = listOf(
        "Kartika", "Margasira", "Posya", "Maga", "Palguna", "Setra",
        "Wesaka", "Yesta", "Asada", "Srawana", "Badra", "Asuji"
    )

    val CAKA_SUNDA_YEAR_NAMES = listOf(
        "Kebo", "Monyet", "Hurang Tembey", "Kalabang", "Embe", "Keuyeup", "Cacing", "Hurang Tutug"
    )

    fun getCakaSundaYearName(cakaYear: Int): String {
        // Epoch year 1953 adalah awal windu (indeks 0 = Kebo)
        val idx = Math.floorMod(cakaYear - 1953, 8)
        return CAKA_SUNDA_YEAR_NAMES[idx]
    }

    val JAVANESE_MONTH_NAMES = listOf(
        "Sura", "Sapar", "Mulud", "Bakda Mulud", "Jumadil Awal", "Jumadil Akhir",
        "Rejeb", "Ruwah", "Pasa", "Sawal", "Sela", "Besar"
    )

    /** Nama hari dari JDE (0=Ahad/Sunday) */
    fun dayName(jde: Double): String {
        val idx = ((jde + 1.5).toLong() % 7).toInt()
        return DAYS_ARABIC[idx]
    }

    /** Nama pasaran Jawa dari JDE.
     * Anchor umum Jawa: JDN lokal 0 = Legi, sehingga 25 Mei 2026 = Legi.
     */
    fun pasaranName(jde: Double, tz: Double = 7.0): String {
        val localDay = floor(jde + tz / 24.0 + 0.5).toInt()
        val idx = Math.floorMod(localDay + 1, 5)
        return PASARAN_JAWA[idx]
    }

    /** Nama hari Sunda (Saptawara) */
    fun sundaDayName(jde: Double, tz: Double = 7.0): String {
        val localDay = floor(jde + tz / 24.0 + 0.5).toInt()
        val idx = Math.floorMod(localDay, 7)
        return SUNDANESE_DAY_NAMES[idx]
    }

    /** Nama pasaran Sunda dari JDE.
     * Mengikuti epoch serial Caka Sunda dari workbook Kalender Sunda:
     * serial = localDay - 1_765_917, 0=Kaliwon, 1=Manis, 2=Pahing, 3=Pon, 4=Wage.
     * Contoh validasi: 25 Mei 2026 => Wage.
     */
    fun pasaranSundaName(jde: Double, tz: Double = 7.0): String {
        val localDay = floor(jde + tz / 24.0 + 0.5).toInt()
        val cakaSerial = localDay - 1_765_917
        val idx = Math.floorMod(cakaSerial, 5)
        return PASARAN_SUNDA[idx]
    }

    data class PranotoMongso(val day: Int, val mongso: Int, val name: String, val year: Int)

    /**
     * Hitung Saka Sunda / Surya versi workbook Excel.
     *
     * Dasar workbook:
     * - CakaSundaDayNumber = LocalJDN - 1_765_917
     * - SakaSundaDayNumber = CakaSundaDayNumber + 16_012
     * - Jadi SakaSundaDayNumber = LocalJDN - 1_749_905
     *
     * Validasi:
     * - 25 Februari 2026 = 6 Katiga 1948
     */
    fun getPranotoMongso(y: Int, m: Int, d: Int): PranotoMongso {
        val jde = gregorianToJde(y, m, d.toDouble())
        val localJdn = floor(jde + 0.5).toInt()

        val n = localJdn - 1_749_905

        val siklus128 = Math.floorDiv(n, 46_751)
        val sisa128 = Math.floorMod(n, 46_751)
        val siklus4 = Math.floorDiv(sisa128, 1_461)
        val sisa4 = Math.floorMod(sisa128, 1_461)

        val sakaYear =
            siklus128 * 128 +
                    siklus4 * 4 +
                    Math.floorDiv(sisa4, 365) +
                    1

        val y0 = sakaYear - 1

        val startOfYear =
            Math.floorDiv(y0, 128) * 46_751 +
                    Math.floorDiv(Math.floorMod(y0, 128), 4) * 1_461 +
                    365 * Math.floorMod(y0, 4)

        val dayOfYear = n - startOfYear

        val rawMonth = floor((dayOfYear + 0.5) / 30.5).toInt() + 1
        val month = rawMonth.coerceIn(1, 12)

        val day =
            n -
                    startOfYear -
                    floor(30.5 * (month - 1)).toInt() +
                    1

        val names = listOf(
            "Kasa", "Karo", "Katiga", "Kapat", "Kalima", "Kanem",
            "Kapitu", "Kawalu", "Kasanga", "Kasapuluh", "Desta", "Sada"
        )

        return PranotoMongso(
            day = day,
            mongso = month,
            name = names[month - 1],
            year = sakaYear
        )
    }

    data class CakaSundaCandra(val day: Int, val isPoek: Boolean, val month: Int, val year: Int)

    /** Hitung Caka Sunda Candra (Lunar) */
    fun getCakaSundaCandra(jdAtNoon: Double): CakaSundaCandra { // Year, Month, Day, isPoek
        val epochJd = 2457642.0 // 1 Kartika 1953 Caka Sunda
        val epochYear = 1953

        val days = floor(jdAtNoon - epochJd + 0.5).toInt()
        val winduDays = 2835 // 8 years

        var remDays = days
        var winduOffset = 0
        if (remDays >= 0) {
            winduOffset = remDays / winduDays
            remDays %= winduDays
        } else {
            winduOffset = (remDays - winduDays + 1) / winduDays
            remDays -= winduOffset * winduDays
        }

        val yearLengths = intArrayOf(354, 355, 354, 354, 355, 354, 354, 355)
        var yWithinWindu = 0
        while (yWithinWindu < 8 && remDays >= yearLengths[yWithinWindu]) {
            remDays -= yearLengths[yWithinWindu]
            yWithinWindu++
        }

        val year = epochYear + winduOffset * 8 + yWithinWindu
        val isLeap = yearLengths[yWithinWindu] == 355

        val monthLengths = if (isLeap) {
            intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 30) // Asuji is 30 in leap year
        } else {
            intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
        }

        var month = 1
        for (mLen in monthLengths) {
            if (remDays >= mLen) {
                remDays -= mLen
                month++
            } else {
                break
            }
        }

        val rawDay = remDays + 1
        val isPoek = rawDay > 15
        val displayDay = if (isPoek) rawDay - 15 else rawDay

        return CakaSundaCandra(displayDay, isPoek, month, year)
    }

    val WUKU_NAMES = listOf(
        "Sinta", "Landep", "Wukir", "Kurantil", "Tolu", "Gumbreg",
        "Warigalit", "Wariagung", "Jungwang", "Sungsang",
        "Galungan", "Kuningan", "Langkir", "Madasiya", "Julungpujud", "Pahang",
        "Karuwelut", "Marekeh", "Tambir", "Medangkungan",
        "Maktal", "Wuye", "Manahil", "Prangbakat", "Bala", "Wugu",
        "Wayang", "Kulawu", "Dukut", "Watugunung"
    )

    /**
     * Hitung nama wuku dari JDE.
     * Acuan: JDE 2461038.0 = Ahad 28 Des 2025 = awal Wuku Karuwelut (indeks 16)
     * Siklus wuku = 30 wuku × 7 hari = 210 hari
     */
    fun getWukuName(jde: Double): String {
        val refJd = 2461038.0 // Ahad 28 Des 2025 = Karuwelut
        val refIdx = 16
        val dayOffset = (jde - refJd + 0.5).toInt()
        val weekOffset = Math.floorDiv(dayOffset, 7)
        val idx = Math.floorMod(refIdx + weekOffset, 30)
        return WUKU_NAMES[idx]
    }

    /** Pawukon Bali (210-day cycle) - Reference: 19 May 2024 is Sinta (Sunday) */
    fun getBalineseWuku(jde: Double): String {
        val BALI_WUKU = listOf(
            "Sinta", "Landep", "Ukir", "Kurantil", "Tolu", "Gumbreg", "Wariga", "Warigagung",
            "Julungwangi", "Sungsang", "Dungulan", "Kuningan", "Langkir", "Medangsia", "Pujut",
            "Pahang", "Krulut", "Merakih", "Tambir", "Medangkungan", "Matal", "Uye", "Menail",
            "Prangbakat", "Bala", "Ugu", "Wayang", "Kelawu", "Dukut", "Watugunung"
        )
        val epochJde = gregorianToJde(2024, 5, 19.0) // Sinta morning
        val diff = floor(jde - epochJde + 0.5).toInt()
        val totalDays = (diff % 210 + 210) % 210
        val wukuIdx = totalDays / 7
        return BALI_WUKU[wukuIdx]
    }

    /** Nomor hari dalam seminggu dari JDE (0=Ahad, 6=Sabtu) */
    fun dayOfWeek(jde: Double): Int = ((jde + 1.5).toLong() % 7).toInt()

    /** Deteksi Hari Libur Nasional Indonesia Berbasis Sistem Lunar & Siklus.
     *  Mengembalikan nama hari libur (atau beberapa nama dipisah " / " jika
     *  hari libur dari sistem berbeda jatuh di tanggal yang sama, mis. Waisak
     *  2026 = 1 Mei = Hari Buruh). */
    fun getHoliday(gY: Int, gM: Int, gD: Int, hM: Int, hD: Int, jde: Double): String? {
        val names = mutableListOf<String>()

        // ── 1. Hari Libur Nasional Berbasis Kalender Masehi (Tanggal Tetap) ──
        when {
            gM == 1  && gD == 1  -> names.add("Tahun Baru Masehi")
            gM == 5  && gD == 1  -> names.add("Hari Buruh Internasional")
            gM == 6  && gD == 1  -> names.add("Hari Lahir Pancasila")
            gM == 8  && gD == 17 -> names.add("Hari Proklamasi Kemerdekaan RI")
            gM == 12 && gD == 25 -> names.add("Hari Natal")
        }

        // ── 2. Hari Besar Islam (Berbasis Kalender Hijriyah / Hisab Hilal) ──
        // Dicocokkan dengan tanggal Hijriyah hasil hisab hilal astronomis.
        when {
            hM == 1  && hD == 1  -> names.add("1 Muharam / Tahun Baru Islam")
            hM == 3  && hD == 12 -> names.add("Maulid Nabi Muhammad SAW")
            hM == 7  && hD == 27 -> names.add("Isra Mikraj Nabi Muhammad SAW")
            hM == 10 && hD == 1  -> names.add("Hari Raya Idulfitri (Hari Pertama)")
            hM == 10 && hD == 2  -> names.add("Hari Raya Idulfitri (Hari Kedua)")
            hM == 12 && hD == 10 -> names.add("Hari Raya Iduladha")
        }

        // ── 3. Hari Besar Kristen & Katolik (Computus Gregorian – Meeus/Butcher) ──
        //   Jumat Agung  = Paskah − 2 hari
        //   Kenaikan Isa = Paskah + 39 hari (Kamis ke-6)
        //   Pentakosta   = Paskah + 49 hari (Minggu ke-7)
        val easter    = easterGregorian(gY)
        val easterJde = gregorianToJde(gY, easter.month, easter.day.toDouble())
        when (jde) {
            easterJde - 2.0  -> names.add("Wafat Yesus Kristus (Good Friday)")
            easterJde        -> names.add("Kebangkitan Yesus Kristus (Paskah)")
            easterJde + 39.0 -> names.add("Kenaikan Yesus Kristus")
            // Pentakosta (Paskah+49) bukan libur nasional RI — dihapus
        }

        // ── 4. Hari Besar Buddha / Hindu / Konghucu (Tabel Referensi Lunar) ──
        if (isChineseNewYear(gY, gM, gD)) names.add("Tahun Baru Imlek Kongzili")
        if (isNyepi(gY, gM, gD))         names.add("Hari Suci Nyepi (Tahun Baru Saka)")
        if (isWaisak(gY, gM, gD))         names.add("Hari Raya Waisak BE")

        return names.joinToString(" / ").ifEmpty { null }
    }

    private fun isChineseNewYear(y: Int, m: Int, d: Int): Boolean {
        // Tahun Baru Imlek — Sumber: Hong Kong Observatory & Time and Date
        // Cakupan: 2020–2035
        val dates = mapOf(
            2020 to "1-25", 2021 to "2-12", 2022 to "2-1",  2023 to "1-22",
            2024 to "2-10", 2025 to "1-29", 2026 to "2-17", 2027 to "2-6",
            2028 to "1-26", 2029 to "2-13", 2030 to "2-3",  2031 to "1-23",
            2032 to "2-11", 2033 to "1-31", 2034 to "2-19", 2035 to "2-8"
        )
        return dates[y] == "$m-$d"
    }

    private fun isNyepi(y: Int, m: Int, d: Int): Boolean {
        // Hari Suci Nyepi (Tilem Kesanga / Tahun Baru Saka) — Sumber: PHDI & Pemerintah RI
        // Cakupan: 2020–2035
        val dates = mapOf(
            2020 to "3-25", 2021 to "3-14", 2022 to "3-3",  2023 to "3-22",
            2024 to "3-11", 2025 to "3-29", 2026 to "3-19", 2027 to "3-8",
            2028 to "3-26", 2029 to "3-15", 2030 to "3-5",  2031 to "3-24",
            2032 to "3-12", 2033 to "3-1",  2034 to "3-20", 2035 to "3-9"
        )
        return dates[y] == "$m-$d"
    }

    /**
     * Algoritma Astronomis Waisak (Vesakha Purnima)
     *
     * DEFINISI: Waisak = purnama (full moon) PERTAMA setelah Matahari melewati
     * 45° bujur ekliptika (pertengahan Taurus = titik Vesakha klasik).
     *
     * Mengapa bukan sekedar "purnama di bulan Mei"?
     *   → Dalam tahun tertentu, Mei punya DUA purnama (mis. 2026: 1 Mei & 31 Mei).
     *     Tabel statis tidak bisa menentukan mana yang benar. Algoritma ini
     *     langsung mengukur posisi Matahari di setiap purnama.
     *
     * Verifikasi: 2020→7 Mei, 2021→26 Mei, 2022→16 Mei, 2023→4 Jun,
     *             2024→23 Mei, 2025→12 Mei, 2026→31 Mei ✓
     *
     * Sumber: Meeus "Astronomical Algorithms" Ch.49 (fasa Bulan),
     *         Vsop87SolarEngine.longitudeEcliptic (bujur Matahari geocentrik).
     */
    private val waisakCache = HashMap<Int, Pair<Int, Int>>()

    private fun computeWaisakGregorianDate(year: Int): Pair<Int, Int> {
        // Gunakan engine hanya jika sudah terinisialisasi (dipanggil dari background task)
        if (!Vsop87SolarEngine.isInitialized) return Pair(0, 0)

        synchronized(waisakCache) {
            val cached = waisakCache[year]
            if (cached != null) return cached

            // Titik awal pencarian: purnama sekitar April (3,5 bulan ke tahun)
            val yearFrac = year.toDouble() + 3.5 / 12.0
            var k = floor((yearFrac - 2000.0) * 12.3685).toInt()

            // Periksa hingga 7 purnama berurutan (April→Juli)
            repeat(7) {
                val kFull = k.toDouble() + 0.5          // k setengah-bulat = purnama
                val T     = kFull / 1236.85
                // Formula mean full moon Meeus Ch.49 Eq.49.1
                val jde   = 2451550.09766 +
                            29.530588861 * kFull +
                            T * T * (0.00015437 + T * (-0.00000015 + T * 0.00000000073))

                val sunLon = Vsop87SolarEngine.compute(jde).longitudeEcliptic
                // Ambil purnama pertama saat Matahari sudah melewati 45° (pertengahan Taurus)
                if (sunLon > 45.0) {
                    val (_, gm, gd) = jdeToGregorian(jde)
                    val result = Pair(gm, gd.toInt())
                    waisakCache[year] = result
                    return result
                }
                k++
            }
            
            val fallback = Pair(0, 0)
            waisakCache[year] = fallback
            return fallback
        }
    }

    private fun isWaisak(y: Int, m: Int, d: Int): Boolean {
        val (wm, wd) = computeWaisakGregorianDate(y)
        return wm == m && wd == d
    }

    /** Kalender Jawa versi workbook Excel */
    fun getJavaneseDate(jdeAtNoon: Double): Triple<Int, Int, Int> {
        val localJdn = floor(jdeAtNoon + 0.5).toInt()

        // Workbook:
        // CakaSundaDayNumber = LocalJDN - 1_765_917
        // JawaDayNumber      = CakaSundaDayNumber - 1_087
        // Jadi JawaDayNumber = LocalJDN - 1_767_004
        val n = localJdn - 1_767_004
        val a = n + 19_136

        val tahun =
            floor(a / 42_524.0).toInt() * 120 +
                    floor(Math.floorMod(a, 42_524) / 2_835.0).toInt() * 8 +
                    floor((Math.floorMod(Math.floorMod(a, 42_524), 2_835) + 5.0 / 8.0) / 354.375).toInt() -
                    53

        val dasarHari =
            a -
                    floor((tahun + 53) / 120.0).toInt() * 42_524 -
                    floor(Math.floorMod(tahun + 53, 120) / 8.0).toInt() * 2_835 -
                    floor((Math.floorMod(tahun + 53, 8) + 6) * 354.375).toInt() +
                    2_126

        val correction =
            if (
                dasarHari == 354 &&
                Math.floorMod(tahun + 54, 120) != 0 &&
                Math.floorMod((tahun + 52) * 3, 8) < 3
            ) {
                -0.5
            } else {
                0.0
            }

        val bulan = floor((dasarHari + correction + 0.25) / 29.5).toInt() + 1

        val tanggal =
            a -
                    floor((tahun + 53) / 120.0).toInt() * 42_524 -
                    floor(Math.floorMod(tahun + 53, 120) / 8.0).toInt() * 2_835 -
                    floor((Math.floorMod(tahun + 53, 8) + 6) * 354.375).toInt() +
                    2_126 -
                    (floor(bulan * 29.5).toInt() - 29) +
                    1

        return Triple(tahun, bulan, tanggal)
    }

    private val startJdeCache = HashMap<String, Double>()

    fun clearStartJdeCache() = synchronized(startJdeCache) { startJdeCache.clear() }

    fun getStartJdeOfHijriMonth(
        yH: Int,
        mH: Int,
        lat: Double,
        lon: Double,
        elev: Double,
        tz: Double,
        criteria: String = "Mabims Baru"
    ): Double {
        val key = "$yH-$mH-${floor(lat).toInt()}-${floor(lon).toInt()}-$tz-$criteria"
        synchronized(startJdeCache) {
            startJdeCache[key]?.let { return it }

            // Ijtima presisi tinggi via VSOP87/ELP — sama persis dengan HisabAwalBulanScreen
            val ijtimaJde = com.falak.falakpro.premium.HilalEngine.calculateMeeusIjtima(yH, mH)

            val dt = DynamicalTimeEngine.deltaT(ijtimaJde)
            val ijtimaLocal = ijtimaJde - dt / 86400.0 + tz / 24.0
            val startOfDayLocal = kotlin.math.floor(ijtimaLocal - 0.5) + 0.5
            val approxSunsetUt = startOfDayLocal + 18.0 / 24.0 - tz / 24.0

            val ghurubSunUt = HilalEngine.findSunsetNear(approxSunsetUt, lat, lon) ?: approxSunsetUt
            val ghurubJde = ghurubSunUt + DynamicalTimeEngine.deltaT(ghurubSunUt) / 86400.0

            val isVisible = if (ijtimaJde >= ghurubJde) false
            else HilalEngine.computeHilalVisibility(
                ijtimaGeoJde = ijtimaJde,
                lat = lat, lon = lon, elev = elev,
                criteria = criteria,
                evalSunsetUt = ghurubSunUt
            )

            val startJde = startOfDayLocal - tz / 24.0 + (if (isVisible) 1.0 else 2.0)
            startJdeCache[key] = startJde
            return startJde
        }
    }
    fun getCorrectedHijri(
        jde: Double,
        lat: Double,
        lon: Double,
        elev: Double,
        tz: Double
    ): Triple<Int, Int, Int> {
        val approx = jdeToHijri(jde)
        val hYear = approx.first
        val hMonth = approx.second

        val startJde = getStartJdeOfHijriMonth(hYear, hMonth, lat, lon, elev, tz)
        val tabStartJde = hijriToJde(hYear, hMonth, 1)
        val offset = startJde - tabStartJde

        return jdeToHijri(jde - offset)
    }

    /**
     * Invers getCakaSundaCandra: konversi tahun-bulan Caka Sunda Candra → JDE awal bulan.
     * Menggunakan epoch dan yearLengths yang identik dengan getCakaSundaCandra.
     */
    fun cakaSundaToJde(cakaYear: Int, cakaMonth: Int): Double {
        val epochJd   = 2457642.0   // 1 Kartika 1953 CS
        val epochYear = 1953
        val winduDays = 2835        // 8 tahun
        val yearLengths = intArrayOf(354, 355, 354, 354, 355, 354, 354, 355)

        val yearDiff    = cakaYear - epochYear
        val winduOffset = Math.floorDiv(yearDiff, 8)
        val yInWindu    = Math.floorMod(yearDiff, 8)

        var dayOffset = winduOffset * winduDays
        for (i in 0 until yInWindu) dayOffset += yearLengths[i]

        val isLeap = yearLengths[yInWindu] == 355
        val monthLen = if (isLeap)
            intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 30)
        else
            intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)

        for (m in 1 until cakaMonth) dayOffset += monthLen[m - 1]

        return epochJd + dayOffset
    }

    /**
     * Invers getJavaneseDate: konversi tahun-bulan Jawa → JDE awal bulan.
     * Diturunkan dari formula workbook yang sama dengan getJavaneseDate.
     *
     * Rumus awal bulan B pada tahun T:
     *   T2 = T + 53
     *   a  = floor(T2/120)*42524 + floor((T2%120)/8)*2835
     *        + floor((T2%8 + 6) * 354.375) - 2126
     *        + floor(B * 29.5) - 29
     *   localJdn = a − 19136 + 1_767_004
     *   jde = localJdn − 0.5
     */
    fun javaToJde(jawaYear: Int, jawaMonth: Int): Double {
        val T         = jawaYear + 53
        val bigCycles = Math.floorDiv(T, 120)
        val tRem      = Math.floorMod(T, 120)
        val medCycles = tRem / 8
        val R         = tRem % 8

        val bigPart   = bigCycles * 42_524
        val medPart   = medCycles * 2_835
        val smallPart = floor((R + 6) * 354.375).toInt()
        val monthPart = floor(jawaMonth * 29.5).toInt() - 29

        val a        = bigPart + medPart + smallPart - 2_126 + monthPart
        val localJdn = a - 19_136 + 1_767_004
        return localJdn - 0.5   // JDE tengah malam
    }
}
