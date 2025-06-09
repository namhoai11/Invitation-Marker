package com.example.invitationcard.model

data class ColorItem(
    val colorValue: Int,
    val colorName: String,
    val colorType: ColorType,
    val isSelected: Boolean = false
)

enum class ColorType {
    DESIGN,
    DEFAULT,
    FOIL,
    GLITTER
}
