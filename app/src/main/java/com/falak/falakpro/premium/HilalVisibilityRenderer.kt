package com.falak.falakpro.premium

import androidx.compose.ui.graphics.Color

object HilalVisibilityRenderer {
    fun overlayColor(zone: HilalVisibilityZone): Color = when (zone) {
        HilalVisibilityZone.EASY_NAKED_EYE -> Color(0xAA00AA46)
        HilalVisibilityZone.POSSIBLE_NAKED_EYE -> Color(0x9965BD78)
        HilalVisibilityZone.OPTICAL_AID_TO_FIND -> Color(0x99F2D06B)
        HilalVisibilityZone.OPTICAL_AID -> Color(0x99F99E59)
        HilalVisibilityZone.TELESCOPE_ONLY -> Color(0x99E66C52)
        HilalVisibilityZone.NOT_VISIBLE -> Color.Transparent
        HilalVisibilityZone.BEFORE_CONJUNCTION -> Color(0x99626D72)
        HilalVisibilityZone.MOON_SET_BEFORE_SUN -> Color(0x99626D72)
        HilalVisibilityZone.NO_EVENT -> Color.Transparent
        HilalVisibilityZone.GLOBAL_ACCEPTED -> Color(0x5590EE90)  // hijau muda transparan = diterima global
    }

    fun legendColor(zone: HilalVisibilityZone): Color = when (zone) {
        HilalVisibilityZone.EASY_NAKED_EYE -> Color(0xFF2D7E37)
        HilalVisibilityZone.POSSIBLE_NAKED_EYE -> Color(0xFF56AA6F)
        HilalVisibilityZone.OPTICAL_AID_TO_FIND -> Color(0xFFE7C75D)
        HilalVisibilityZone.OPTICAL_AID -> Color(0xFFE58546)
        HilalVisibilityZone.TELESCOPE_ONLY -> Color(0xFFCD4E3D)
        HilalVisibilityZone.NOT_VISIBLE -> Color(0xFFFFFFFF)
        HilalVisibilityZone.BEFORE_CONJUNCTION -> Color(0xFF626D72)
        HilalVisibilityZone.MOON_SET_BEFORE_SUN -> Color(0xFF626D72)
        HilalVisibilityZone.NO_EVENT -> Color(0xFFFFFFFF)
        HilalVisibilityZone.GLOBAL_ACCEPTED -> Color(0xFF90EE90)  // hijau muda = diterima global
    }
}
