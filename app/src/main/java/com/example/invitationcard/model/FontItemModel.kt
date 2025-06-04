package com.example.invitationcard.model

import android.graphics.Typeface

data class FontItem(
    val name: String,
    val family: String,
    val category: String = "",
    val variants: List<String> = listOf("regular"),
    var typeface: Typeface? = null,
    val isSystemFont: Boolean = false,
    val googleFontName: String? = null
) {
    val displayName: String
        get() = name.replace("+", " ")
}