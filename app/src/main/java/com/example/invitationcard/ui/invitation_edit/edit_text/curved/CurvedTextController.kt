package com.example.invitationcard.ui.invitation_edit.edit_text.curved

import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.invitationcard.R

class CurvedTextController(
    private val controlView: View,
    private val onCurveAngleChanged: (Float) -> Unit
) {
    private val curvedTextValue: TextView = controlView.findViewById(R.id.curved_text_value)
    private val seekBar: SeekBar = controlView.findViewById(R.id.seekbar_curved_text)
    private val btnDecrease: ImageButton = controlView.findViewById(R.id.btn_decrease_curve)
    private val btnIncrease: ImageButton = controlView.findViewById(R.id.btn_increase_curve)

    // *** THAY ĐỔI: Tăng phạm vi góc lên ±360 độ ***
    private val minAngle = -360f
    private val maxAngle = 360f
    private val defaultAngle = 0f

    init {
        // Seekbar có giá trị từ 0 đến 200, giá trị giữa (100) tương ứng với góc 0°
        seekBar.max = 720
        seekBar.progress = 360
        updateValueDisplay(defaultAngle)

        setupListeners()
    }

    private fun setupListeners() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val angle = calculateAngle(progress)
                updateValueDisplay(angle)
                if (fromUser) {
                    onCurveAngleChanged(angle)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDecrease.setOnClickListener {
            if (seekBar.progress > 0) {
                seekBar.progress -= 1 // Tăng bước nhảy để điều chỉnh nhanh hơn
                val angle = calculateAngle(seekBar.progress)
                updateValueDisplay(angle)
                onCurveAngleChanged(angle)
            }
        }

        btnIncrease.setOnClickListener {
            if (seekBar.progress < seekBar.max) {
                seekBar.progress += 1 // Tăng bước nhảy
                val angle = calculateAngle(seekBar.progress)
                updateValueDisplay(angle)
                onCurveAngleChanged(angle)
            }
        }
    }

    private fun calculateAngle(progress: Int): Float {
        // *** THAY ĐỔI: Chuyển đổi progress (0-720) thành góc (-360° đến 360°) ***
        return minAngle + (progress / 720f) * (maxAngle - minAngle)
    }

    private fun calculateProgress(angle: Float): Int {
        // Chuyển đổi góc thành progress
        return ((angle - minAngle) / (maxAngle - minAngle) * 720).toInt()
            .coerceIn(0, 720)
    }

    private fun updateValueDisplay(angle: Float) {
        curvedTextValue.text = "${angle.toInt()}°"
    }

    fun setCurveAngle(angle: Float) {
        val boundedAngle = angle.coerceIn(minAngle, maxAngle)
        seekBar.progress = calculateProgress(boundedAngle)
        updateValueDisplay(boundedAngle)
        onCurveAngleChanged(boundedAngle)
    }

    fun setCurveAngleWithoutCallback(angle: Float) {
        val boundedAngle = angle.coerceIn(minAngle, maxAngle)
        seekBar.progress = calculateProgress(boundedAngle)
        updateValueDisplay(boundedAngle)
    }

    fun show() {
        controlView.visibility = View.VISIBLE
    }

    fun hide() {
        controlView.visibility = View.GONE
    }
}