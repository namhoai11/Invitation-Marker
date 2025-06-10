package com.example.invitationcard.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.invitationcard.R

class ColorPickerDialog(
    private val context: Context,
    private val onColorSelected: (Int) -> Unit
) {

    private var selectedColor: Int = Color.RED

    fun show() {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // THÊM DÒNG NÀY: Thiết lập chiều rộng bằng 90% chiều rộng màn hình
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        setupColorButtons(view, dialog)
        setupActionButtons(view, dialog)

        dialog.show()
    }

    private fun setupColorButtons(view: View, dialog: Dialog) {
        val colors = mapOf(
            R.id.dialog_color_red to Color.RED,
            R.id.dialog_color_cyan to Color.CYAN,
            R.id.dialog_color_blue to Color.BLUE,
            R.id.dialog_color_green to Color.GREEN,
            R.id.dialog_color_magenta to Color.MAGENTA,
            R.id.dialog_color_yellow to Color.YELLOW,
            R.id.dialog_color_black to Color.BLACK,
            R.id.dialog_color_white to Color.WHITE
        )

        colors.forEach { (viewId, color) ->
            view.findViewById<View>(viewId)?.setOnClickListener {
                selectedColor = color
                updateColorSelection(view, viewId)
            }
        }
    }

    private fun updateColorSelection(view: View, selectedViewId: Int) {
        // Reset all selections
        val colorIds = listOf(
            R.id.dialog_color_red, R.id.dialog_color_cyan, R.id.dialog_color_blue,
            R.id.dialog_color_green, R.id.dialog_color_magenta, R.id.dialog_color_yellow,
            R.id.dialog_color_black, R.id.dialog_color_white
        )

        colorIds.forEach { id ->
            view.findViewById<View>(id)?.isSelected = (id == selectedViewId)
        }
    }

    private fun setupActionButtons(view: View, dialog: Dialog) {
        // Customize button - Show HSV picker
        view.findViewById<TextView>(R.id.btn_customize)?.setOnClickListener {
            dialog.dismiss()
            showHSVColorPicker()
        }

        // Selected colors button
        view.findViewById<TextView>(R.id.btn_selected_colors)?.setOnClickListener {
            // TODO: Show recently selected colors
        }

        // Cancel button
        view.findViewById<TextView>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }

        // Apply button
        view.findViewById<TextView>(R.id.btn_apply)?.setOnClickListener {
            onColorSelected(selectedColor)
            dialog.dismiss()
        }
    }

    private fun showHSVColorPicker() {
        val hsvDialog = HSVColorPickerDialog(context, onColorSelected)
        hsvDialog.show()
    }
}
