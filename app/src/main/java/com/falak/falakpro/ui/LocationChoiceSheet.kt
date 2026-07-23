package com.falak.falakpro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.location.CityLocation
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.ui.theme.GreenPrimary

fun applyCityLocationToPrefs(prefs: PreferencesHelper, city: CityLocation) {
    prefs.manualLokasiNama = city.displayName
    prefs.manualLat = city.latitude
    prefs.manualLon = city.longitude
    prefs.manualElev = city.elevation
    prefs.manualTimezone = city.timezone
    prefs.ketinggianDataranTinggi = city.elevation
    prefs.locationInputMode = "DAFTAR_KOTA"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationChoiceSheet(
    onDismiss: () -> Unit,
    onSearchLocation: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Sesuaikan lokasimu, yuk!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp
            )
            Text(
                text = "Waktu shalat serta kiblat akan menyesuaikan lokasi yang kamu pilih.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 26.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = onSearchLocation,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Cari Lokasi", fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Lokasi Saat Ini", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
