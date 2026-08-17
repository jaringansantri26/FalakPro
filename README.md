# FalakPro

**FalakPro** adalah aplikasi Android profesional untuk astronomi Islam, hisab rukyat, kalender astronomis, jadwal ibadah, arah kiblat, gerhana, ephemeris, almanak, dan kalkulator falak. Aplikasi ini menggabungkan perhitungan astrofisika modern presisi tinggi berbasis teori **VSOP87D**, **ELP/MPP02**, nutasi **IAU 2000A/2006**, dan algoritma **Jean Meeus** dengan khazanah metode hisab klasik turats pesantren seperti **Ad-Durr al-Aniq fi Ilm al-Falak**.

Dikembangkan di bawah naungan **Lembaga Falakiyah Pengurus Wilayah Nahdlatul Ulama (PWNU) Jawa Barat** untuk pesantren, akademisi, lembaga hisab rukyat, kementerian agama, guru, santri, mahasiswa, peneliti, masjid, dan umat Islam luas.

---

## 🇮🇩 Bahasa Indonesia

### 🌟 Fitur Utama

1. **Hisab Awal Bulan Hijriyah Terpadu**
   - Jalur hisab ganda: **Astronomi Modern (VSOP87D + ELP/MPP02)** dan **Kitab Turats (Ad-Durr al-Aniq)**.
   - Parameter lengkap: Ijtimak Geosentris & Toposentris, Ghurub Matahari & Bulan, Tinggi Hilal (Hakiki, Mar'i, Apparent, Airless), Elongasi, Umur Hilal, Fraksi Iluminasi, Lebar Sabit, *Bright Limb Angle* (BLA), *Lag Time* (Mukts), dan *Best Time*.
   - Perbandingan multi-kriteria otomatis: **MABIMS Baru (3°/6,4°)**, **MABIMS Lama (2°/3°/8j)**, **Wujudul Hilal**, **Neo-MABIMS**, **Kriteria Danjon**, **Kriteria Odeh / Q Odeh**, **Kriteria Yallop**, **Kriteria LAPAN**, **Ummul Qura**, dan **Turki / KGHT**.

2. **Peta Visibilitas Hilal Global Interaktif**
   - Pemetaan visibilitas hilal di seluruh dunia secara real-time dan independen.
   - Pilihan layer: **Garis Batas Zona Visibilitas** dan **Garis Ketinggian Hilal (0°, 2°, 3°, 5°, 7°)**.
   - Kontrol dinamis: Pemilih Bulan & Tahun Hijriyah, Offset Hari (+0 H-29, +1 H-30, +2), serta 8 kriteria hisab internasional.
   - Komputasi cepat berbasis *multi-threading* dan *disk caching* biner.

3. **Kamera Rukyat Hilal & Matahari (Augmented Reality)**
   - Proyeksi vektor 3D benda langit di atas layar kamera *real-time* berbasis sensor orientasi (*Rotation Vector*).
   - **Kunci Posisi (*Lock Sensor*)**: Mengunci arah sensor perangkat saat terpasang pada tripod, sementara hisab dan ikon tetap bergerak dinamis sesuai perputaran waktu langit.
   - **Penggaris / Grid Derajat**: Menampilkan garis panduan koordinat langit (ketinggian horizontal 0°..70° dan azimut vertikal 0°..360°).
   - **Koreksi Manual**: Pengguna dapat menyelaraskan (*align*) posisi ikon dengan Matahari nyata via geser layar (*touch drag*) untuk mengoreksi gangguan medan magnet.
   - Panel HUD monospaced: `Sensor Az/h`, `Delta Az/h`, `SUN Az/h`, `MOON Az/h`, dan `Magnetic Declination`.
   - Panduan observasi 5 langkah terintegrasi dan fitur *Keep Screen On*.

4. **Arah Kiblat & Kamera Kiblat AR**
   - Perhitungan azimut kiblat berbasis rumus **Great Circle (Spherical)**, **Ellipsoid WGS84**, dan **Vincenty Geodesic Formula**.
   - Kompas digital responsif dengan getaran dan suara *beep* saat perangkat lurus menghadap Ka'bah.
   - Kamera AR Kiblat dengan penunjuk visual Ka'bah 3D di layar.
   - Data harian & tahunan **Rashdul Qiblah** (Istiwa A'zham) saat bayangan Matahari mengarah tepat ke Ka'bah.

5. **Jadwal Salat & Imsakiyah Terpadu**
   - Waktu salat akurat: Subuh, Syuruq, Dhuha, Zuhur, Asar (Syafi'i / Hanafi), Magrib, Isya, dan Imsak.
   - Koreksi ketinggian tempat (elevasi/kerendahan ufuk), zona waktu, dan menit ikhtiyath kustom.
   - Opsi kriteria hisab: **LFNU**, **Kemenag RI**, **MWL**, **ISNA**, **Ummul Qura**, **Mesir (EGAS)**, **Karachi**, dll.
   - Ekspor dan cetak **Jadwal Salat / Imsakiyah format PDF A4** siap cetak.

6. **Kalender Astronomis 4-in-1**
   - Integrasi dan sinkronisasi 4 sistem kalender sekaligus: **Kalender Masehi**, **Kalender Hijriyah**, **Saka Jawa (Pasaran: Legi, Pahing, Pon, Wage, Kliwon)**, dan **Caka Sunda (Pancawara, Sadwara, Wuku)**.

7. **Prediksi Gerhana Matahari & Bulan**
   - Prediksi komprehensif gerhana: Total, Cincin, Sebagian, Penumbra, dan Hibrida.
   - Waktu kontak presisi (U1, U2, Puncak/Greatest, U3, U4), magnitude, fraksi obscuration, durasi, dan data geometri bayangan lokal pengamat.

8. **Data Falak & Ephemeris Lengkap**
   - Ephemeris harian/jam-jaman Matahari & Bulan: Asensio Rekta (RA), Deklinasi, GHA, LHA, Equation of Time (EoT), Obliquity, GAST, Jarak (AU/km), dan Delta T.
   - Ekspor spreadsheet **Excel (.xlsx)** dengan tabel terformat rapi.

9. **Kalkulator Ilmiah Falak**
   - Kalkulator astronomis khusus dengan fungsi trigonometri derajat/rad, invers, konversi DMS (Derajat-Menit-Detik) ↔ Desimal, konversi HMS ↔ Desimal, Julian Day (JD/MJD), Delta T, GAST/LST/HA, normalisasi sudut (0..360° dan -180..+180°), faktorial, permutasi, kombinasi, FPB/KPK, dan memori falak.

10. **Manajemen Lokasi & Offline-First**
    - Input manual koordinat DMS/Desimal, pencarian dari database ribuan kota, dan penentuan posisi instan via GPS.
    - 100% *Offline-First* — seluruh kalkulasi astronomis diproses secara lokal di perangkat tanpa memerlukan koneksi internet.

---

### 🔬 Basis Algoritma dan Data Biner

| Komponen Astronomi | Teori / Algoritma | File Aset Biner | Jumlah Suku Koreksi |
| :--- | :--- | :--- | ---: |
| **Matahari (Sun)** | VSOP87D Earth Heliocentric Series | `earth_vsop87d.bin` | 2.425 term |
| **Bulan (Moon)** | ELP/MPP02 Lunar Theory (DE405-matched) | `mpp02_core.bin` | 35.882 term |
| **Nutasi Bumi** | IAU 2000A Series & IAU 2006 Precession | `iau2000a_nutation.bin` | 1.365 term |
| **Hisab Ad-Durrul Aniq** | Harakat Ijtimak & Ta'dil (KH. Ahmad Ghozali) | `harokat_ijtima.bin`, `ta_dil.bin` | 108 record, 361 baris |
| **Database Kota** | Koordinat kota-kota dunia & Indonesia | `data_kota/*.txt` | 50 file, 5.494 record |
| **Refraksi Atmosfer** | Formula Bennett / Meeus AA Ch.16 | Terkomputasi di runtime | Presisi tinggi |

---

### 🏛️ Tim Pengembang & Kontak

- **Institusi**: Lembaga Falakiyah Pengurus Wilayah Nahdlatul Ulama (PWNU) Jawa Barat
- **Lead Developer & System Architect**: **Asep Jalaludin Bakrie**
- **Kontak / WhatsApp**: `+62 817-2238-56`
- **Website Resmi**: [pwnujabar.or.id](https://pwnujabar.or.id)

---

## 🇬🇧 English

### Overview

**FalakPro** is a state-of-the-art Android suite for Islamic astronomy, calendrical computation, and professional celestial observation. It uniquely synthesizes **modern computational astrophysics** (VSOP87D solar theory, ELP/MPP02 lunar ephemeris, IAU 2000A nutation, and Jean Meeus algorithms) with revered **traditional Islamic astronomical methods** (*Ad-Durr al-Aniq*).

### Key Features

- **Integrated Hijri Month Calculation**: High-precision VSOP87D/ELP-MPP02 and classical Ad-Durrul Aniq calculations with full parameters (Conjunction, Topocentric/Apparent Crescent Altitude, Elongation, Crescent Width, Moon Age, Illumination, Lag Time, and Best Time).
- **Interactive Global Visibility Maps**: Independent real-time global crescent visibility mapping with contour altitude layers (0°, 2°, 3°, 5°, 7°), zone boundaries, day offsets (+0 H-29, +1 H-30, +2), and 8 criteria (MABIMS, Wujudul Hilal, Odeh, Yallop, Danjon, Turkey/KGHT).
- **AR Crescent Observation Camera (Kamera Rukyat)**: Augmented reality celestial targeting with **Sensor Lock** for tripod stability, coordinate degree grid, manual drag alignment, and real-time Sun/Moon tracking.
- **Precision Qibla Compass & AR Camera**: Great Circle, WGS84 Ellipsoid, and Vincenty geodesic algorithms with live camera AR alignment and daily Rashdul Qiblah times.
- **Prayer Times & Ramadan Timetable**: Elevation-adjusted prayer schedules, international calculation conventions (LFNU, Kemenag, MWL, ISNA, Ummul Qura), and A4 PDF printing.
- **4-in-1 Integrated Calendar**: Gregorian, Hijri, Javanese Saka (Pasaran cycles), and Sundanese Caka systems.
- **Solar & Lunar Eclipse Predictions**: Contact timing (U1..U4), magnitude, obscuration, and local circumstance geometry.
- **Astronomical Ephemeris & Data Falak**: Daily/hourly solar and lunar ephemeris tables with **Excel (.xlsx)** export.
- **Scientific Falak Calculator**: Comprehensive trigonometric, DMS/HMS, Julian Day, Delta T, GAST/LST/HA, and memory functions.
- **100% Offline-First Architecture**: All calculations run locally on-device without internet dependency.

---

## 🇸🇦 العربية

### نبذة عن التطبيق

**فلك برو (FalakPro)** هو تطبيق أندرويد رائد ومتخصص في الحسابات الفلكية الإسلامية وعلم الفلك العملي، تم تطويره بواسطة **مؤسسة الفلكية التابعة لـ PWNU جاوة الغربية، إندونيسيا**.

يجمع التطبيق بين **أحدث النظريات الفلكية العالمية عالية الدقة** (مثل نظريات VSOP87D للشمس، ونظرية ELP/MPP02 للقمر، ونماذج IAU 2000A/2006 للنوتيشن) وبين **كتب الفلك الإسلامي التراثية المعتمدة** (مثل كتاب *الدر الأنيق في علم الفلك* وكتاب *سلم النيرين*).

### أبرز المزايا

- **حساب أوائل الشهور الهجرية**: دمج الحسابات الفلكية الحديثة مع الحسابات التراثية، مع استخراج كامل عناصر الهلال (الاقتران، الارتفاع الحقيقي والمرئي، الاستطالة، سمك الهلال، عمر القمر، وكسر الإضاءة).
- **خريطة إمكانية رؤية الهلال التفاعلية**: خريطة عالمية فورية مع خطوط تساوي الارتفاع (0°، 2°، 3°، 5°، 7°) وتصنيف المناطق لمعايير الرؤية العالمية (MABIMS، عودة، يالوب، دانجون، تركيا/KGHT).
- **كاميرا رصد الهلال بالواقع المعزز (AR)**: تراكب دقيق لمواقع الشمس والقمر على خط الأفق مع ميزة قفل المستشعر (*Lock Sensor*) للاستخدام مع التلسكوب والحامل الثلاثي.
- **تحديد اتجاه القبلة بدقة**: نماذج جيوديسية متعددة (WGS84 و Vincenty) مع إرشاد بصري عبر الكاميرا ومواقيت رصد القبلة بظل الشمس.
- **مواقيت الصلاة وإمساكية رمضان**: مواقيت دقيقة مع مراعاة ارتفاع الراصد عن سطح البحر وتصدير جداول بصيغة PDF قابلة للطباعة.
- **تقويم شامل 4 في 1**: مزامنة التقاويم الميلادية والهجرية والجاوية والسوندية.
- **الكسوف والخسوف والزيج الفلكي**: حساب دقيق لظواهر الكسوف والخسوف وتصدير جداول الزيج الفلكي بصيغة ملفات Excel (.xlsx).
- **العمل دون اتصال بالإنترنت (Offline-First)**: تتم جميع العمليات الحسابية محلياً على الجهاز لضمان الخصوصية والسرعة.

---

### ⚖️ تنبيه فقهي وأمانة علمية

تطبيق **FalakPro** هو أداة حسابية وتعليمية وبحثية دقيقة. أما القرار الرسمي لثبوت أوائل الشهور الهجرية وتحديد مناسبات الأعياد فيرجع شرعاً ونظاماً إلى الجهات الدينية والمؤسسات الرسمية المخولة في كل دولة.

---
© 2026 **FalakPro** — Lembaga Falakiyah PWNU Jawa Barat. All Rights Reserved. 🌙🕌☀️🧭🔭
