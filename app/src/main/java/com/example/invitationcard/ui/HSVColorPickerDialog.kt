package com.example.invitationcard.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.invitationcard.R

class HSVColorPickerDialog(
    private val context: Context,
    private val onColorSelected: (Int) -> Unit
) {

    private var hue: Float = 0f
    private var saturation: Float = 1f
    private var value: Float = 1f

    private lateinit var colorPreview: View
    private lateinit var hueThumb: ImageView
    private lateinit var saturationThumb: ImageView
    private lateinit var valueThumb: ImageView
    private lateinit var hueGradient: View
    private lateinit var saturationGradient: View
    private lateinit var valueGradient: View

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_hsv_color_picker, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // THÊM DÒNG NÀY: Thiết lập chiều rộng bằng 90% chiều rộng màn hình
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        initViews(view)
        setupSliders(view)
        setupActionButtons(view, dialog)
        updateColorPreview()

        dialog.show()
    }

    private fun initViews(view: View) {
        colorPreview = view.findViewById(R.id.color_preview)
        hueThumb = view.findViewById(R.id.hue_thumb)
        saturationThumb = view.findViewById(R.id.saturation_thumb)
        valueThumb = view.findViewById(R.id.value_thumb)
        hueGradient = view.findViewById(R.id.hue_gradient)
        saturationGradient = view.findViewById(R.id.saturation_gradient)
        valueGradient = view.findViewById(R.id.value_gradient)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSliders(view: View) {
        // Hue slider
        hueGradient.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x.coerceIn(0f, v.width.toFloat())
                    hue = (x / v.width) * 360f
                    updateHueThumb(x)
                    updateSaturationGradient()
                    updateValueGradient()
                    updateColorPreview()
                    true
                }
                else -> false
            }
        }

        // Saturation slider
        saturationGradient.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x.coerceIn(0f, v.width.toFloat())
                    saturation = x / v.width
                    updateSaturationThumb(x)
                    updateValueGradient()
                    updateColorPreview()
                    true
                }
                else -> false
            }
        }

        // Value slider
        valueGradient.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x.coerceIn(0f, v.width.toFloat())
                    value = x / v.width
                    updateValueThumb(x)
                    updateColorPreview()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateHueThumb(x: Float) {
        hueThumb.x = x - hueThumb.width / 2
    }

    private fun updateSaturationThumb(x: Float) {
        saturationThumb.x = x - saturationThumb.width / 2
    }

    private fun updateValueThumb(x: Float) {
        valueThumb.x = x - valueThumb.width / 2
    }

    private fun updateSaturationGradient() {
        val hsvColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.WHITE, hsvColor)
        )
        gradient.cornerRadius = 10f
        saturationGradient.background = gradient
    }

    private fun updateValueGradient() {
        val hsvColor = Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.BLACK, hsvColor)
        )
        gradient.cornerRadius = 10f
        valueGradient.background = gradient
    }

    private fun updateColorPreview() {
        val color = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        colorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun setupActionButtons(view: View, dialog: Dialog) {
        // Suggest button
        view.findViewById<TextView>(R.id.btn_suggest)?.setOnClickListener {
            // TODO: Show color suggestions
        }

        // Selected colors button
        view.findViewById<TextView>(R.id.btn_selected_colors_hsv)?.setOnClickListener {
            // TODO: Show recently selected colors
        }

        // Cancel button
        view.findViewById<TextView>(R.id.btn_cancel_hsv)?.setOnClickListener {
            dialog.dismiss()
        }

        // Apply button
        view.findViewById<TextView>(R.id.btn_apply_hsv)?.setOnClickListener {
            val selectedColor = Color.HSVToColor(floatArrayOf(hue, saturation, value))
            onColorSelected(selectedColor)
            dialog.dismiss()
        }
    }
}
