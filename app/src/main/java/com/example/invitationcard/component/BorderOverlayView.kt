package com.example.invitationcard.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.xiaopo.flying.sticker.Sticker

class BorderOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GREEN
        alpha = 255
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val borderPath = Path()
    private var stickerPoints: FloatArray? = null

    init {
        // Đảm bảo view này không chặn các sự kiện chạm
        isClickable = false
        isFocusable = false
    }

    // Cập nhật điểm cho sticker và vẽ lại
    fun updateStickerPoints(points: FloatArray?) {
        stickerPoints = points?.clone()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Vẽ viền nếu có điểm
        val points = stickerPoints ?: return
        if (points.size < 8) return

        // Sử dụng Path để vẽ viền
        borderPath.reset()
        borderPath.moveTo(points[0], points[1])
        borderPath.lineTo(points[2], points[3])
        borderPath.lineTo(points[4], points[5])
        borderPath.lineTo(points[6], points[7])
        borderPath.close()

        canvas.drawPath(borderPath, borderPaint)
    }

    // Đảm bảo tất cả các sự kiện chạm đều được chuyển tiếp
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return false // Không xử lý bất kỳ sự kiện chạm nào
    }
}