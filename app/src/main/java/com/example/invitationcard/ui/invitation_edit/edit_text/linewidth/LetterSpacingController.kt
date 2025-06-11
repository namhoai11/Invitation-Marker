package com.example.invitationcard.ui.invitation_edit.edit_text.linewidth

import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.invitationcard.R

class LetterSpacingController(
    private val controlView: View,
    private val onLetterSpacingChanged: (Float) -> Unit
) {
    private val letterSpacingValue: TextView = controlView.findViewById(R.id.letter_spacing_value)
    private val seekBar: SeekBar = controlView.findViewById(R.id.seekbar_letter_spacing)
    private val btnDecrease: ImageButton = controlView.findViewById(R.id.btn_decrease_letter_spacing)
    private val btnIncrease: ImageButton = controlView.findViewById(R.id.btn_increase_letter_spacing)

    // Giá trị letter spacing từ -0.25 đến 1.0
    private val minLetterSpacing = -0.25f
    private val maxLetterSpacing = 1.0f
    private val defaultLetterSpacing = 0.0f

    // Tổng range là 1.25, mỗi mức trong seekbar là 0.01
    private val totalSteps = 125

    init {
        // Thiết lập giá trị mặc định
        seekBar.max = totalSteps
        val defaultProgress = ((defaultLetterSpacing - minLetterSpacing) / (maxLetterSpacing - minLetterSpacing) * totalSteps).toInt()
        seekBar.progress = defaultProgress
        updateValueDisplay(defaultLetterSpacing)

        setupListeners()
    }

    private fun setupListeners() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val letterSpacing = calculateLetterSpacing(progress)
                updateValueDisplay(letterSpacing)
                if (fromUser) {
                    onLetterSpacingChanged(letterSpacing)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDecrease.setOnClickListener {
            if (seekBar.progress > 0) {
                seekBar.progress -= 5
                val letterSpacing = calculateLetterSpacing(seekBar.progress)
                updateValueDisplay(letterSpacing)
                onLetterSpacingChanged(letterSpacing)
            }
        }

        btnIncrease.setOnClickListener {
            if (seekBar.progress < seekBar.max) {
                seekBar.progress += 5
                val letterSpacing = calculateLetterSpacing(seekBar.progress)
                updateValueDisplay(letterSpacing)
                onLetterSpacingChanged(letterSpacing)
            }
        }
    }

    private fun calculateLetterSpacing(progress: Int): Float {
        return minLetterSpacing + (progress.toFloat() / totalSteps) * (maxLetterSpacing - minLetterSpacing)
    }

    private fun calculateProgress(letterSpacing: Float): Int {
        return ((letterSpacing - minLetterSpacing) / (maxLetterSpacing - minLetterSpacing) * totalSteps).toInt()
            .coerceIn(0, totalSteps)
    }

    private fun updateValueDisplay(letterSpacing: Float) {
        // Hiển thị giá trị với hai chữ số thập phân
        letterSpacingValue.text = String.format("%.2f", letterSpacing)
    }

    fun setLetterSpacing(letterSpacing: Float) {
        val boundedValue = letterSpacing.coerceIn(minLetterSpacing, maxLetterSpacing)
        seekBar.progress = calculateProgress(boundedValue)
        updateValueDisplay(boundedValue)
        onLetterSpacingChanged(boundedValue)
    }

    fun setLetterSpacingWithoutCallback(letterSpacing: Float) {
        val boundedValue = letterSpacing.coerceIn(minLetterSpacing, maxLetterSpacing)
        seekBar.progress = calculateProgress(boundedValue)
        updateValueDisplay(boundedValue)
    }

    fun show() {
        controlView.visibility = View.VISIBLE
    }

    fun hide() {
        controlView.visibility = View.GONE
    }
}