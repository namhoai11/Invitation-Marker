package com.example.invitationcard.model

import android.text.Layout

data class TemplateConfig(
    val id: String,
    val textElements: List<TextElementConfig>
)

data class TextElementConfig(
    val id: String,
    val zIndex: Int,
    val relativeX: Float, // 0.0 - 1.0 (tỷ lệ theo chiều rộng)
    val relativeY: Float, // 0.0 - 1.0 (tỷ lệ theo chiều cao)
    val relativeWidth: Float,
    val relativeHeight: Float,
    val defaultText: String,
    val fontName: String,
    val relativeFontSize: Float, // Tỷ lệ chiều cao SVG
    val colorHex: String,
    val alignment: String = "center", // "center", "left", "right"
    val bold: Boolean = false,
    val italic: Boolean = false,
    val uppercase: Boolean = false
)