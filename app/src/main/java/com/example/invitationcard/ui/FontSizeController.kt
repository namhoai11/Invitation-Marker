package com.example.invitationcard.ui

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.example.invitationcard.R

class FontSizeController(
    private val controlView: View,
    private val onSizeChanged: (Int) -> Unit
) {
    private val seekBar: SeekBar = controlView.findViewById(R.id.seekbar_font_size)
    private val btnMinus: ImageButton = controlView.findViewById(R.id.btn_decrease_size)
    private val btnPlus: ImageButton = controlView.findViewById(R.id.btn_increase_size)
    private val tvSize: TextView = controlView.findViewById(R.id.font_size_value)

    private val minSize = 8
    private val maxSize = 200

    private var isCallbackEnabled = true

    init {
        setupSeekBar()
        setupButtons()
    }

    private fun setupSeekBar() {
        seekBar.max = maxSize - minSize
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = minSize + progress
                tvSize.text = "$size"

                if (fromUser && isCallbackEnabled) {
                    onSizeChanged(size)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        btnMinus.setOnClickListener {
            val currentProgress = seekBar.progress
            if (currentProgress > 0) {
                seekBar.progress = currentProgress - 1
                if (isCallbackEnabled) {
                    onSizeChanged(minSize + seekBar.progress)
                }
            }
        }

        btnPlus.setOnClickListener {
            val currentProgress = seekBar.progress
            if (currentProgress < seekBar.max) {
                seekBar.progress = currentProgress + 1
                if (isCallbackEnabled) {
                    onSizeChanged(minSize + seekBar.progress)
                }
            }
        }
    }

    fun setSize(size: Int) {
        val clampedSize = size.coerceIn(minSize, maxSize)
        seekBar.progress = clampedSize - minSize
        tvSize.text = "${clampedSize}"

        if (isCallbackEnabled) {
            onSizeChanged(clampedSize)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setSizeWithoutCallback(size: Int) {
        isCallbackEnabled = false
        val clampedSize = size.coerceIn(minSize, maxSize)
        seekBar.progress = clampedSize - minSize
        tvSize.text = "$clampedSize"
        isCallbackEnabled = true
    }

    fun show() {
        controlView.visibility = View.VISIBLE
    }

    fun hide() {
        controlView.visibility = View.GONE
    }
}