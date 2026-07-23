import re

file_path = "app/src/main/java/com/falak/falakpro/ui/components/EclipseDetailContent.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. NasaScrollableTable replacement
old_nst = '''@Composable
fun NasaScrollableTable(
    headers: List<String>,
    rows: List<List<String>>,
    columnWidths: List<Dp>
) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB2DFDB)), shape = RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .background(Color(0xFFE0F2F1))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                headers.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(columnWidths[index]),
                        color = tealPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFB2DFDB), thickness = 1.dp)
            
            // Data rows
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    row.forEachIndexed { index, value ->
                        Text(
                            text = value,
                            modifier = Modifier.width(columnWidths[index]),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                        )
                    }
                }
                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                }
            }
        }
    }
}'''

new_nst = '''@Composable
fun NasaScrollableTable(
    headers: List<String>,
    rows: List<List<String>>,
    columnWidths: List<Dp>
) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB2DFDB)), shape = RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .background(Color(0xFFE0F2F1))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(columnWidths[index]).padding(horizontal = 4.dp),
                        color = tealPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                    )
                    if (index < headers.size - 1) {
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFB2DFDB)))
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFB2DFDB), thickness = 1.dp)
            
            // Data rows
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { index, value ->
                        Text(
                            text = value,
                            modifier = Modifier.width(columnWidths[index]).padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                        )
                        if (index < headers.size - 1) {
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
                        }
                    }
                }
                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                }
            }
        }
    }
}'''

# 2. NasaTable replacement
old_nt = '''@Composable
fun NasaTable(headers: List<String>, rows: List<List<String>>, weights: List<Float>) {
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            headers.forEachIndexed { index, header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(weights[index]),
                    color = tealPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                )
            }
        }
        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        
        // Data Rows
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                row.forEachIndexed { index, value ->
                    Text(
                        text = value,
                        modifier = Modifier.weight(weights[index]),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                    )
                }
            }
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        }
    }
}'''

new_nt = '''@Composable
fun NasaTable(headers: List<String>, rows: List<List<String>>, weights: List<Float>) {
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB2DFDB)), shape = RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0F2F1))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            headers.forEachIndexed { index, header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(weights[index]).padding(horizontal = 4.dp),
                    color = tealPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                )
                if (index < headers.size - 1) {
                    Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFFB2DFDB)))
                }
            }
        }
        HorizontalDivider(color = Color(0xFFB2DFDB), thickness = 1.dp)
        
        // Data Rows
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEachIndexed { index, value ->
                    Text(
                        text = value,
                        modifier = Modifier.weight(weights[index]).padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                    )
                    if (index < headers.size - 1) {
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(dividerColor))
                    }
                }
            }
            if (rowIndex < rows.size - 1) {
                HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
            }
        }
    }
}'''


# 3. Local contacts in LocalEclipseDetailContent
old_local = '''                    // Local Contacts Table
                    val contactRows = mutableListOf<List<String>>()
                    val ptStringLocal = { label: String, c: ContactPoint? ->
                        if (c != null) {
                            listOf(
                                label,
                                formatJdeToTimeFull(c.jdeTD + local.timezone / 24.0),
                                formatDm(c.latitude),
                                formatDm(c.longitude),
                                c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                                c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                            )
                        } else null
                    }
                    ptStringLocal("C1  Kontak Pertama", local.u1)?.let { contactRows.add(it) }
                    ptStringLocal("C2  Kontak Internal", local.u2)?.let { contactRows.add(it) }
                    ptStringLocal("Mx  Puncak Gerhana",  local.mx)?.let { contactRows.add(it) }
                    ptStringLocal("C3  Kontak Internal", local.u3)?.let { contactRows.add(it) }
                    ptStringLocal("C4  Kontak Akhir",    local.u4)?.let { contactRows.add(it) }

                    Text("Kontak Lokal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    NasaScrollableTable(
                        headers = listOf("Kontak", "Waktu (LT)", "Alt", "Az", "P. Angle", "Axis Dist"),
                        rows = contactRows,
                        columnWidths = listOf(140.dp, 90.dp, 75.dp, 75.dp, 75.dp, 75.dp)
                    )'''

new_local = '''                    // Local Contacts Table
                    val contactRows = mutableListOf<List<String>>()
                    val ptStringLocal = { label: String, c: ContactPoint? ->
                        if (c != null) {
                            val utJde = c.jdeTD - local.deltaT / 86400.0
                            val locJde = utJde + local.timezone / 24.0
                            listOf(
                                label,
                                formatJdeToTimeFull(utJde) + "\\n" + formatDateString(utJde),
                                formatJdeToTimeFull(locJde) + "\\n" + formatDateString(locJde),
                                formatDm(c.latitude),
                                formatDm(c.longitude),
                                c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                                c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                            )
                        } else null
                    }
                    ptStringLocal("C1  Kontak Pertama", local.u1)?.let { contactRows.add(it) }
                    ptStringLocal("C2  Kontak Internal", local.u2)?.let { contactRows.add(it) }
                    ptStringLocal("Mx  Puncak Gerhana",  local.mx)?.let { contactRows.add(it) }
                    ptStringLocal("C3  Kontak Internal", local.u3)?.let { contactRows.add(it) }
                    ptStringLocal("C4  Kontak Akhir",    local.u4)?.let { contactRows.add(it) }

                    Text("Kontak Lokal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    NasaScrollableTable(
                        headers = listOf("Kontak", "Waktu (UT)", "Waktu (LT)", "Alt", "Az", "P. Angle", "Axis Dist"),
                        rows = contactRows,
                        columnWidths = listOf(140.dp, 90.dp, 90.dp, 75.dp, 75.dp, 75.dp, 75.dp)
                    )'''

# 4. Global contacts in CombinedSolarEclipseDetailContent
old_global = '''                val cp = { name: String -> global.contacts.find { it.name == name } }
                val contactRow = { label: String, code: String, c: ContactPoint? ->
                    if (c != null) {
                        val ut = c.jdeTD - global.deltaT / 86400.0
                        listOf(label, code,
                            formatJdeToTimeFull(c.jdeTD) + "\n" + formatDateString(c.jdeTD),
                            formatJdeToTimeFull(ut) + "\n" + formatDateString(ut),
                            formatDms(c.latitude,  isLat = true),
                            formatDms(c.longitude, isLon = true),
                            c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                            c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                        )
                    } else {
                        listOf(label, code, "-", "-", "-", "-", "-", "-")
                    }
                }

                Text("Kontak Penumbra dengan Bumi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                NasaScrollableTable(
                    headers = listOf("Kontak", "Kode", "Waktu TD", "Waktu UT", "Latitude", "Longitude", "P. Angle", "Axis Dist"),
                    rows = listOf(
                        contactRow("Mulai Penumbra (P1)", "P1", cp("P1")),
                        contactRow("Akhir Penumbra (P4)", "P4", cp("P4"))
                    ),
                    columnWidths = listOf(160.dp, 50.dp, 90.dp, 90.dp, 130.dp, 130.dp, 75.dp, 75.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("Kontak Umbra dengan Bumi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                NasaScrollableTable(
                    headers = listOf("Kontak", "Kode", "Waktu TD", "Waktu UT", "Latitude", "Longitude", "P. Angle", "Axis Dist"),
                    rows = listOf(
                        contactRow("Eksternal Umbra (U1)", "U1", cp("U1")),
                        contactRow("Internal Umbra (U2)", "U2", cp("U2")),
                        contactRow("Internal Umbra (U3)", "U3", cp("U3")),
                        contactRow("Eksternal Umbra (U4)", "U4", cp("U4"))
                    ),
                    columnWidths = listOf(160.dp, 50.dp, 90.dp, 90.dp, 130.dp, 130.dp, 75.dp, 75.dp)
                )'''

new_global = '''                val cp = { name: String -> global.contacts.find { it.name == name } }
                val contactRow = { label: String, code: String, c: ContactPoint? ->
                    if (c != null) {
                        val ut = c.jdeTD - global.deltaT / 86400.0
                        listOf(label, code,
                            formatJdeToTimeFull(c.jdeTD) + "\\n" + formatDateString(c.jdeTD),
                            formatJdeToTimeFull(ut) + "\\n" + formatDateString(ut),
                            formatDms(c.latitude,  isLat = true),
                            formatDms(c.longitude, isLon = true),
                            c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                            c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                        )
                    } else null
                }

                Text("Kontak Bayangan dengan Bumi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                val allContactRows = mutableListOf<List<String>>()
                contactRow("Mulai Penumbra (P1)", "P1", cp("P1"))?.let { allContactRows.add(it) }
                contactRow("Mulai Umbra (U1)", "U1", cp("U1"))?.let { allContactRows.add(it) }
                contactRow("Kontak Internal (U2)", "U2", cp("U2"))?.let { allContactRows.add(it) }
                contactRow("Puncak Gerhana (Mx)", "Mx", cp("Mx"))?.let { allContactRows.add(it) }
                contactRow("Kontak Internal (U3)", "U3", cp("U3"))?.let { allContactRows.add(it) }
                contactRow("Akhir Umbra (U4)", "U4", cp("U4"))?.let { allContactRows.add(it) }
                contactRow("Akhir Penumbra (P4)", "P4", cp("P4"))?.let { allContactRows.add(it) }

                NasaScrollableTable(
                    headers = listOf("Peristiwa Kontak", "Kode", "Waktu TD", "Waktu UT", "Lintang", "Bujur", "Sudut P", "Jarak Sumbu"),
                    rows = allContactRows,
                    columnWidths = listOf(175.dp, 45.dp, 90.dp, 90.dp, 110.dp, 110.dp, 75.dp, 85.dp)
                )'''

# 5. Local contacts in LocalEclipseDetailContent standalone function (around line 220)
old_local_2 = '''        // 2. Contacts
        NasaSectionHeader("Local Contacts of Eclipse")
        val contactRows = mutableListOf<List<String>>()
        
        val ptStringLocal = { label: String, c: ContactPoint? ->
            if (c != null) {
                listOf(
                    label,
                    formatJdeToTimeFull(c.jdeTD + detail.timezone / 24.0),
                    formatDm(c.latitude),
                    formatDm(c.longitude),
                    c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                    c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                )
            } else null
        }

        ptStringLocal("C1  Kontak Pertama", detail.u1)?.let { contactRows.add(it) }
        ptStringLocal("C2  Kontak Internal", detail.u2)?.let { contactRows.add(it) }
        ptStringLocal("Mx  Puncak Gerhana",  detail.mx)?.let { contactRows.add(it) }
        ptStringLocal("C3  Kontak Internal", detail.u3)?.let { contactRows.add(it) }
        ptStringLocal("C4  Kontak Akhir",    detail.u4)?.let { contactRows.add(it) }

        NasaScrollableTable(
            headers = listOf("Kontak", "Waktu (LT)", "Alt", "Az", "P. Angle", "Axis Dist"),
            rows = contactRows,
            columnWidths = listOf(140.dp, 90.dp, 75.dp, 75.dp, 75.dp, 75.dp)
        )'''

new_local_2 = '''        // 2. Contacts
        NasaSectionHeader("Local Contacts of Eclipse")
        val contactRows = mutableListOf<List<String>>()
        
        val ptStringLocal = { label: String, c: ContactPoint? ->
            if (c != null) {
                val utJde = c.jdeTD - detail.deltaT / 86400.0
                val locJde = utJde + detail.timezone / 24.0
                listOf(
                    label,
                    formatJdeToTimeFull(utJde) + "\\n" + formatDateString(utJde),
                    formatJdeToTimeFull(locJde) + "\\n" + formatDateString(locJde),
                    formatDm(c.latitude),
                    formatDm(c.longitude),
                    c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                    c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                )
            } else null
        }

        ptStringLocal("C1  Kontak Pertama", detail.u1)?.let { contactRows.add(it) }
        ptStringLocal("C2  Kontak Internal", detail.u2)?.let { contactRows.add(it) }
        ptStringLocal("Mx  Puncak Gerhana",  detail.mx)?.let { contactRows.add(it) }
        ptStringLocal("C3  Kontak Internal", detail.u3)?.let { contactRows.add(it) }
        ptStringLocal("C4  Kontak Akhir",    detail.u4)?.let { contactRows.add(it) }

        NasaScrollableTable(
            headers = listOf("Kontak", "Waktu (UT)", "Waktu (LT)", "Alt", "Az", "P. Angle", "Axis Dist"),
            rows = contactRows,
            columnWidths = listOf(140.dp, 90.dp, 90.dp, 75.dp, 75.dp, 75.dp, 75.dp)
        )'''

if old_nst in content:
    content = content.replace(old_nst, new_nst)
    print("NasaScrollableTable updated")
else:
    print("NasaScrollableTable NOT FOUND")

if old_nt in content:
    content = content.replace(old_nt, new_nt)
    print("NasaTable updated")
else:
    print("NasaTable NOT FOUND")

if old_local in content:
    content = content.replace(old_local, new_local)
    print("Local (Combined) updated")
else:
    print("Local (Combined) NOT FOUND")

if old_global in content:
    content = content.replace(old_global, new_global)
    print("Global (Combined) updated")
else:
    print("Global (Combined) NOT FOUND")

if old_local_2 in content:
    content = content.replace(old_local_2, new_local_2)
    print("Local (Standalone) updated")
else:
    print("Local (Standalone) NOT FOUND")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
