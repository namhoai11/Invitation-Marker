package com.example.invitationcard.ui.invitation_edit.edit_text.lineheight

import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.invitationcard.R

class LineHeightController(
    private val controlView: View,
    private val onLineHeightChanged: (Int) -> Unit
) {
    private val lineHeightValue: TextView = controlView.findViewById(R.id.line_height_value)
    private val seekBar: SeekBar = controlView.findViewById(R.id.seekbar_line_height)
    private val btnDecrease: ImageButton = controlView.findViewById(R.id.btn_decrease_line_height)
    private val btnIncrease: ImageButton = controlView.findViewById(R.id.btn_increase_line_height)

    private val minLineHeight = 75
    private val maxLineHeight = 400
    private val defaultLineHeight = 120

    init {
        // Seekbar progress = lineHeight - minLineHeight
        // Max progress = maxLineHeight - minLineHeight
        seekBar.max = maxLineHeight - minLineHeight
        seekBar.progress = defaultLineHeight - minLineHeight
        updateValueDisplay(defaultLineHeight)

        setupListeners()
    }

    private fun setupListeners() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val lineHeight = progress + minLineHeight
                updateValueDisplay(lineHeight)
                if (fromUser) {
                    onLineHeightChanged(lineHeight)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDecrease.setOnClickListener {
            if (seekBar.progress > 0) {
                seekBar.progress -= 5
                val lineHeight = seekBar.progress + minLineHeight
                updateValueDisplay(lineHeight)
                onLineHeightChanged(lineHeight)
            }
        }

        btnIncrease.setOnClickListener {
            if (seekBar.progress < seekBar.max) {
                seekBar.progress += 5
                val lineHeight = seekBar.progress + minLineHeight
                updateValueDisplay(lineHeight)
                onLineHeightChanged(lineHeight)
            }
        }
    }

    private fun updateValueDisplay(lineHeight: Int) {
        lineHeightValue.text = lineHeight.toString()
    }

    fun setLineHeight(lineHeight: Int) {
        val boundedLineHeight = lineHeight.coerceIn(minLineHeight, maxLineHeight)
        seekBar.progress = boundedLineHeight - minLineHeight
        updateValueDisplay(boundedLineHeight)
        onLineHeightChanged(boundedLineHeight)
    }

    fun setLineHeightWithoutCallback(lineHeight: Int) {
        val boundedLineHeight = lineHeight.coerceIn(minLineHeight, maxLineHeight)
        seekBar.progress = boundedLineHeight - minLineHeight
        updateValueDisplay(boundedLineHeight)
    }

    fun show() {
        controlView.visibility = View.VISIBLE
    }

    fun hide() {
        controlView.visibility = View.GONE
    }
}