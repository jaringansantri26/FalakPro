# FalakPro

FalakPro adalah aplikasi Android untuk astronomi Islam, hisab rukyat, jadwal ibadah, arah kiblat, gerhana, ephemeris, almanak, dan kalkulator falak. Aplikasi ini menggabungkan perhitungan astronomi modern berbasis teori VSOP87D, ELP/MPP02, nutasi IAU, dan algoritma Jean Meeus dengan metode hisab klasik seperti Ad-Durr al-Aniq.

Proyek ini ditujukan untuk pengguna yang membutuhkan alat falak praktis sekaligus dapat diaudit: pesantren, lembaga falakiyah, guru, santri, mahasiswa, peneliti, masjid, dan pengguna umum.

## Bahasa Indonesia

### Tujuan

FalakPro dibuat sebagai aplikasi terpadu untuk:

- menghitung awal bulan Hijriah;
- menampilkan data hilal, ijtimak, elongasi, umur hilal, iluminasi, lag time, tinggi hilal, azimut, dan parameter rukyat;
- membuat peta visibilitas hilal;
- menghitung jadwal salat dan imsakiyah;
- menentukan arah kiblat, kompas kiblat, kamera kiblat, dan rashdul kiblat;
- menghitung gerhana Matahari dan Bulan;
- menampilkan data Matahari dan Bulan realtime maupun manual;
- menyediakan ephemeris harian dan almanak nautika;
- menyediakan kalkulator ilmiah/falak dengan fungsi JD, Delta T, DMS/HMS, trigonometri, normalisasi sudut, dan memori.

### Fitur Utama

- Hisab Awal Bulan Terpadu: metode VSOP87D/ELP-MPP02 dan Ad-Durr al-Aniq.
- Kriteria hilal: MABIMS Baru/Neo-MABIMS, MABIMS Lama, Yallop, Odeh/Q Odeh, dan kriteria lain yang disediakan modul peta visibilitas.
- Peta visibilitas hilal: klasifikasi area memenuhi/tidak memenuhi kriteria, tenggelam/belum ijtimak, dan ekspor/cetak gambar.
- Jadwal salat: Subuh, Syuruq, Dhuha, Zuhur, Asar, Magrib, Isya, imsakiyah bulanan, koreksi lokasi/elevasi/zona waktu.
- Kiblat: azimut kiblat, kompas, kamera AR, posisi Matahari/Bulan, dan rashdul kiblat.
- Gerhana: prediksi global dan lokal, kontak gerhana, magnitude, obscuration, durasi, dan data lokasi.
- Data Falak: ephemeris, almanak nautika, posisi Matahari/Bulan realtime dan manual.
- Kalkulator Falak: fungsi trigonometri, invers, JD/MJD, Delta T, GAST/LST/HA, DMS/HMS, normalisasi 0..360 dan -180..180, faktorial, kombinasi, permutasi, FPB/KPK, dan memori.
- Lokasi: input manual, daftar kota, dan GPS.
- Offline-first: perhitungan yang tidak membutuhkan jaringan tetap berjalan offline. GPS/geocoding/update aplikasi tetap membutuhkan izin/perangkat/jaringan sesuai fungsi.
- Cetak/PDF: jadwal salat, hasil hisab awal bulan, data gerhana, dan bagan posisi hilal.
- Cek update aplikasi: update metadata dapat dibaca dari `update.json` yang disiapkan di repository rilis.

### Basis Perhitungan

FalakPro memakai beberapa jalur perhitungan:

1. Astronomi modern
   - Matahari: VSOP87D Earth heliocentric series untuk posisi Matahari geosentris.
   - Bulan: ELP/MPP02 lunar theory dengan parameter DE405-compatible.
   - Nutasi: IAU 2000A series dengan precession/obliquity flow IAU 2006.
   - Transformasi koordinat: ekliptika-ekuatorial-horizontal, topocentric parallax, semidiameter, refraction, sidereal time.
   - Kalender dan fase Bulan: algoritma Jean Meeus.

2. Hisab klasik
   - Ad-Durr al-Aniq: tabel harakat, mabsuthah, majmuah, bulan, serta tabel ta'dil yang dikonversi ke data biner agar ringan di Android.
   - Hasil proses menampilkan tahapan hisab, jumlah, dalil, ta'dil, data hilal, dan kesimpulan.

3. Visibilitas hilal
   - MABIMS Baru/Neo-MABIMS: tinggi minimal 3 derajat dan elongasi minimal 6,4 derajat.
   - MABIMS Lama: tinggi minimal 2 derajat, elongasi minimal 3 derajat, umur minimal 8 jam.
   - Yallop: berbasis arc of vision dan crescent width.
   - Odeh/Q Odeh: berbasis arc of vision dan crescent width.

4. Gerhana
   - Menggunakan elemen/pendekatan Besselian untuk kontak dan keadaan global/lokal.
   - Koreksi Delta T digunakan untuk konversi TD/TT ke UT.
   - Modul gerhana dirancang untuk membedakan data global dan keadaan lokal pengamat.

### Data dan Jumlah Suku Koreksi

Jumlah berikut adalah jumlah yang dipaketkan dan dibaca oleh aplikasi saat ini.

| Data | File aset | Jumlah |
| --- | --- | ---: |
| VSOP87D Earth L/B/R | `earth_vsop87d.bin` | 2.425 term |
| VSOP L0..L5 | `earth_vsop87d.bin` | 559, 341, 142, 22, 11, 5 |
| VSOP B0..B5 | `earth_vsop87d.bin` | 184, 99, 49, 11, 5, 0 |
| VSOP R0..R5 | `earth_vsop87d.bin` | 526, 292, 139, 27, 10, 3 |
| ELP/MPP02 longitude | `mpp02_core.bin` | 12.337, 1.199, 219, 2 term |
| ELP/MPP02 latitude | `mpp02_core.bin` | 7.380, 516, 52 term |
| ELP/MPP02 distance | `mpp02_core.bin` | 12.819, 1.165, 210, 2 term |
| ELP/MPP02 total core | `mpp02_core.bin` | 35.882 term |
| Nutasi IAU 2000A | `iau2000a_nutation.bin` | 1.365 term |
| Harakat ijtimak Ad-Durr al-Aniq | `harokat_ijtima.bin` | 66 majmuah, 30 mabsuthah, 12 bulan |
| Tabel ta'dil Ad-Durr al-Aniq | `ta_dil.bin` | 361 baris |
| Data kota sumber | `data_kota/*.txt` | 50 file, 5.494 record non-kosong |

### Referensi Ilmiah dan Metodologi

- Jean Meeus, *Astronomical Algorithms*, 2nd Edition.
- Bretagnon & Francou, VSOP87 planetary theory.
- Chapront-Touze & Chapront, ELP/MPP02 lunar theory.
- IAU 2000A nutation series.
- IAU 2006 precession and mean obliquity formulation.
- Explanatory Supplement to the Astronomical Almanac.
- Espenak/Meeus eclipse computation tradition and Besselian-element based eclipse practice.
- Yallop crescent visibility criterion.
- Odeh/Q Odeh crescent visibility criterion.
- MABIMS criteria for Hijri calendar/imkan rukyat usage in Southeast Asia.
- KH. Ahmad Ghozali Muhammad Fathullah, *Ad-Durr al-Aniq fi Ma'rifat al-Hilal wa al-Kusufain*.

### Catatan Akurasi

FalakPro berusaha menjaga konsistensi antara:

- nilai geosentris dan toposentris;
- true dan apparent coordinates;
- UT, TT/TD, JD, MJD, dan Delta T;
- metode modern dan metode klasik;
- hasil layar dan hasil cetak/PDF.

Walaupun begitu, keputusan resmi awal bulan Hijriah tetap mengikuti otoritas keagamaan dan pemerintah yang berwenang. FalakPro adalah alat bantu hisab, edukasi, dan verifikasi.

### Struktur Penting Proyek

- `app/src/main/java/com/falak/falakpro/premium`: mesin astronomi modern, hisab awal bulan, gerhana, visibilitas, VSOP, ELP, nutasi, dan Delta T.
- `app/src/main/java/com/falak/falakpro/AddurulAniq`: mesin hisab Ad-Durr al-Aniq dan pembaca tabel biner.
- `app/src/main/java/com/falak/falakpro/ui`: layar aplikasi, kalkulator, jadwal salat, kiblat, data falak, cetak/PDF, dan update checker.
- `app/src/main/assets`: data biner yang dipakai runtime.
- `data_kota`: sumber data lokasi dalam TXT.
- `tools`: script pembuat/konversi data biner.

### Developer

FalakPro dikembangkan untuk kebutuhan falak, hisab rukyat, edukasi, dan penelitian.

Lead Developer: Asep Jalaludin Bakrie

## English

### Overview

FalakPro is an Android application for Islamic astronomy and practical astronomical computation. It combines modern celestial mechanics, Islamic calendrical computation, prayer-time calculation, Qibla direction, eclipse prediction, crescent visibility mapping, ephemerides, nautical almanac data, and a falak-oriented scientific calculator.

The application is designed for pesantren, Islamic astronomy institutions, teachers, students, researchers, mosques, and general users who need accurate and explainable astronomical data.

### Main Features

- Integrated Hijri month-start computation using modern VSOP87D/ELP-MPP02 and classical Ad-Durr al-Aniq methods.
- Crescent visibility criteria including Neo-MABIMS, old MABIMS, Yallop, and Odeh/Q Odeh.
- Crescent visibility map with printable/exportable graphics.
- Prayer times and monthly imsakiyah with location, elevation, and timezone handling.
- Qibla tools: azimuth, compass, AR camera, Sun/Moon assistance, and rashdul qiblah.
- Solar and lunar eclipse prediction with global and local circumstances.
- Ephemeris, nautical almanac, and Sun/Moon position data.
- Scientific falak calculator with JD, MJD, Delta T, DMS/HMS, trigonometry, sidereal time, angle normalization, and memory functions.
- Location input by manual entry, city list, or GPS.
- Offline-first calculation model for computations that do not require network access.
- PDF/print output for selected reports.
- Update-check support through a release metadata JSON file.

### Computational Basis

FalakPro uses:

- VSOP87D Earth series for solar position.
- ELP/MPP02 lunar theory for lunar longitude, latitude, and distance.
- IAU 2000A nutation series with IAU 2006 style precession/obliquity handling.
- Meeus algorithms for calendars, lunar phases, coordinate transforms, and auxiliary astronomy.
- Topocentric parallax, semidiameter, refraction, sidereal time, and apparent/true coordinate handling.
- Classical Ad-Durr al-Aniq tables for traditional Islamic astronomical computation.
- Crescent visibility criteria from MABIMS, Yallop, and Odeh.

### Packaged Computational Data

| Data | Asset | Count |
| --- | --- | ---: |
| VSOP87D Earth L/B/R | `earth_vsop87d.bin` | 2,425 terms |
| ELP/MPP02 lunar core | `mpp02_core.bin` | 35,882 terms |
| IAU 2000A nutation | `iau2000a_nutation.bin` | 1,365 terms |
| Ad-Durr al-Aniq ijtimak harakat | `harokat_ijtima.bin` | 108 records |
| Ad-Durr al-Aniq ta'dil table | `ta_dil.bin` | 361 rows |
| Location source data | `data_kota/*.txt` | 50 files, 5,494 non-empty records |

### References

- Jean Meeus, *Astronomical Algorithms*.
- Bretagnon & Francou, VSOP87 theory.
- Chapront-Touze & Chapront, ELP/MPP02 lunar theory.
- IAU 2000A nutation and IAU 2006 precession/obliquity conventions.
- Explanatory Supplement to the Astronomical Almanac.
- Espenak/Meeus eclipse calculation tradition and Besselian elements.
- Yallop and Odeh crescent visibility criteria.
- MABIMS crescent visibility criteria.
- KH. Ahmad Ghozali Muhammad Fathullah, *Ad-Durr al-Aniq*.

### Accuracy Notice

FalakPro is a calculation, education, and verification tool. Official Hijri calendar decisions remain the authority of the relevant religious and governmental bodies.

## العربية

### نبذة

فلك برو هو تطبيق أندرويد للحسابات الفلكية الإسلامية وعلم الفلك العملي. يجمع التطبيق بين الحسابات الفلكية الحديثة، وحساب أوائل الشهور الهجرية، ومواقيت الصلاة، واتجاه القبلة، والكسوف والخسوف، وخريطة رؤية الهلال، والجداول الفلكية، والحاسبة العلمية الفلكية.

صمم التطبيق للمعاهد الإسلامية، والهيئات الفلكية، والمدرسين، والطلاب، والباحثين، والمساجد، وكل من يحتاج إلى بيانات فلكية واضحة وقابلة للمراجعة.

### أهم الميزات

- حساب بداية الشهر الهجري بطريقة حديثة VSOP87D/ELP-MPP02 وبطريقة Ad-Durr al-Aniq.
- معايير رؤية الهلال: مابيمس الجديد، مابيمس القديم، يالوب، وعودة.
- خريطة إمكانية رؤية الهلال مع إمكانية الطباعة أو التصدير.
- مواقيت الصلاة والإمساكية الشهرية مع مراعاة الموقع والارتفاع والمنطقة الزمنية.
- أدوات القبلة: سمت القبلة، البوصلة، كاميرا القبلة، ورصد اتجاه الظل.
- حساب الكسوف والخسوف عالميا ومحليا.
- بيانات الشمس والقمر، الجداول الفلكية، والألمانك الملاحي.
- حاسبة فلكية تتضمن JD و MJD و Delta T و DMS و HMS والدوال المثلثية والزمن النجمي.
- إدخال الموقع يدويا أو من قائمة المدن أو باستخدام GPS.
- الحسابات التي لا تحتاج إلى الإنترنت تعمل دون اتصال.

### أساس الحساب

يعتمد التطبيق على:

- نظرية VSOP87D لحساب موقع الشمس.
- نظرية ELP/MPP02 لحساب موقع القمر والمسافة.
- نموذج IAU 2000A للنوتيشن مع منهج IAU 2006 للسبق وميل فلك البروج.
- خوارزميات Jean Meeus للتقاويم والتحويلات الفلكية.
- التصحيحات الموضعية مثل اختلاف المنظر، نصف القطر الظاهري، الانكسار، والزمن النجمي.
- جداول Ad-Durr al-Aniq للحسابات التراثية.
- معايير مابيمس ويالوب وعودة لرؤية الهلال.

### بيانات التصحيح المضمنة

| البيانات | الملف | العدد |
| --- | --- | ---: |
| VSOP87D Earth | `earth_vsop87d.bin` | 2,425 حد |
| ELP/MPP02 للقمر | `mpp02_core.bin` | 35,882 حد |
| IAU 2000A Nutation | `iau2000a_nutation.bin` | 1,365 حد |
| Harakat Ad-Durr al-Aniq | `harokat_ijtima.bin` | 108 سجل |
| Tabel ta'dil Ad-Durr al-Aniq | `ta_dil.bin` | 361 صف |
| بيانات المدن | `data_kota/*.txt` | 50 ملفا، 5,494 سجلا غير فارغ |

### المراجع

- Jean Meeus, *Astronomical Algorithms*.
- Bretagnon & Francou, VSOP87.
- Chapront-Touze & Chapront, ELP/MPP02.
- IAU 2000A Nutation و IAU 2006.
- Explanatory Supplement to the Astronomical Almanac.
- حسابات الكسوف والخسوف بطريقة العناصر البيسلية.
- معايير Yallop و Odeh لرؤية الهلال.
- معايير MABIMS.
- كتاب *Ad-Durr al-Aniq* للشيخ KH. Ahmad Ghozali Muhammad Fathullah.

### تنبيه

فلك برو أداة للحساب والتعليم والمراجعة. أما القرار الرسمي لبداية الشهر الهجري فيرجع إلى الجهات الدينية والرسمية المختصة.
