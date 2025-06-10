package com.example.invitationcard.ui

import android.graphics.Color
import android.text.Layout
import android.view.View
import android.widget.ImageButton
import com.example.invitationcard.R

class TextAlignmentController(
    private val controlView: View,
    private val onAlignmentChanged: (Layout.Alignment) -> Unit
) {
    private val btnAlignLeft: ImageButton = controlView.findViewById(R.id.btn_align_left)
    private val btnAlignCenter: ImageButton = controlView.findViewById(R.id.btn_align_center)
    private val btnAlignRight: ImageButton = controlView.findViewById(R.id.btn_align_right)

    private var currentAlignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL

    init {
        setupButtons()
    }

    private fun setupButtons() {
        btnAlignLeft.setOnClickListener {
            setAlignment(Layout.Alignment.ALIGN_NORMAL)
        }

        btnAlignCenter.setOnClickListener {
            setAlignment(Layout.Alignment.ALIGN_CENTER)
        }

        btnAlignRight.setOnClickListener {
            setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
        }
    }

    fun setAlignment(alignment: Layout.Alignment) {
        // Cập nhật trạng thái nút trước
        currentAlignment = alignment
        updateButtonStates()

        // Gọi callback để thông báo thay đổi
        onAlignmentChanged(alignment)

        // Force redraw controller
        controlView.invalidate()
    }

    fun setAlignmentWithoutCallback(alignment: Layout.Alignment) {
        currentAlignment = alignment
        updateButtonStates()
    }

    private fun updateButtonStates() {
        // Reset tất cả về trạng thái mặc định
        btnAlignLeft.colorFilter = null
        btnAlignCenter.colorFilter = null
        btnAlignRight.colorFilter = null

        // Highlight nút được chọn
        val activeColor = Color.parseColor("#4CAF50")
        when (currentAlignment) {
            Layout.Alignment.ALIGN_NORMAL -> btnAlignLeft.setColorFilter(activeColor)
            Layout.Alignment.ALIGN_CENTER -> btnAlignCenter.setColorFilter(activeColor)
            Layout.Alignment.ALIGN_OPPOSITE -> btnAlignRight.setColorFilter(activeColor)
        }
    }



    fun show() {
        controlView.visibility = View.VISIBLE
    }

    fun hide() {
        controlView.visibility = View.GONE
    }

    fun getCurrentAlignment(): Layout.Alignment = currentAlignment
}