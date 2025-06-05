package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.TextSticker
import java.lang.reflect.Field

class FlexibleTextSticker(context: Context) : TextSticker(context) {

    private var customTextSizeSp: Int = 18  // Bắt đầu với kích thước nhỏ hơn
    private val context: Context = context

    init {
        // Ngay từ đầu, hãy tắt cơ chế tự động thay đổi kích thước của TextSticker
        try {
            val minTextSizePixelsField = TextSticker::class.java.getDeclaredField("minTextSizePixels")
            minTextSizePixelsField.isAccessible = true
            minTextSizePixelsField.setFloat(this, 0f)

            val maxTextSizePixelsField = TextSticker::class.java.getDeclaredField("maxTextSizePixels")
            maxTextSizePixelsField.isAccessible = true
            maxTextSizePixelsField.setFloat(this, Float.MAX_VALUE)
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error disabling auto-resize", e)
        }

        // Đặt kích thước ban đầu
        setTextSizeSp(customTextSizeSp)
    }

    fun setTextSizeSp(sizeInSp: Int) {
        try {
            customTextSizeSp = sizeInSp

            // Tính kích thước pixel từ SP
            val sizeInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sizeInSp.toFloat(),
                context.resources.displayMetrics
            )

            Log.d("FlexibleTextSticker", "Setting text size to $sizeInSp SP ($sizeInPixels pixels)")

            // Cách 1: Đặt kích thước trực tiếp trong TextPaint
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint
            textPaint.textSize = sizeInPixels

            // Cách 2: Cũng đặt min/max size để tránh TextSticker tự điều chỉnh
            val minTextSizePixelsField = TextSticker::class.java.getDeclaredField("minTextSizePixels")
            minTextSizePixelsField.isAccessible = true
            minTextSizePixelsField.setFloat(this, sizeInPixels)

            val maxTextSizePixelsField = TextSticker::class.java.getDeclaredField("maxTextSizePixels")
            maxTextSizePixelsField.isAccessible = true
            maxTextSizePixelsField.setFloat(this, sizeInPixels)

            // Cách 3: Vô hiệu hóa các biến có thể ảnh hưởng đến resize
            val currentTextSizePixelsField = TextSticker::class.java.getDeclaredField("currentTextSizePixels")
            if (currentTextSizePixelsField != null) {
                currentTextSizePixelsField.isAccessible = true
                currentTextSizePixelsField.setFloat(this, sizeInPixels)
            }

            // Vẽ lại và cập nhật border
            updateBoundsToFitText()
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error setting text size", e)
        }
    }

    fun getTextSizeSp(): Int = customTextSizeSp

    override fun setText(text: String?): TextSticker {
        // Gọi phương thức của lớp cha
        val result = super.setText(text)

        // Cập nhật lại kích thước đã cài đặt để đảm bảo không bị ghi đè
        try {
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            val sizeInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                customTextSizeSp.toFloat(),
                context.resources.displayMetrics
            )

            // Đảm bảo kích thước text không thay đổi
            if (textPaint.textSize != sizeInPixels) {
                textPaint.textSize = sizeInPixels
                Log.d("FlexibleTextSticker", "Re-applied text size after setText()")
            }

            // Cập nhật border
            updateBoundsToFitText()
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error in setText", e)
        }

        return result
    }

    private fun updateBoundsToFitText() {
        try {
            val text = getText() ?: ""
            if (text.isEmpty()) return

            // Lấy TextPaint
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            // Log kích thước thực tế để kiểm tra
            Log.d("FlexibleTextSticker", "Current textPaint size: ${textPaint.textSize} pixels")

            // ĐO CHÍNH XÁC chiều rộng text
            val textWidth = textPaint.measureText(text).toInt()

            // Log chiều rộng text để kiểm tra
            Log.d("FlexibleTextSticker", "Text width: $textWidth pixels for text: '$text'")

            // Thêm padding phù hợp
            val paddingHorizontal = 40
            val paddingVertical = 30

            // Tính chiều cao của text
            val metrics = textPaint.fontMetrics
            val lineHeight = (metrics.descent - metrics.ascent).toInt()

            // Tính toán dựa trên số dòng (nếu có xuống dòng)
            val lineCount = Math.max(text.count { it == '\n' } + 1, 1)
            val textHeight = lineHeight * lineCount

            // Đảm bảo kích thước tối thiểu
            val minWidth = Math.max(textWidth + paddingHorizontal * 2, 150)
            val minHeight = Math.max(textHeight + paddingVertical * 2, 60)

            // Cập nhật cả realBounds và textRect
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect
            realBounds.set(0, 0, minWidth, minHeight)

            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as android.graphics.Rect
            textRect.set(0, 0, minWidth, minHeight)

            // Cập nhật drawable nếu có
            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.setBounds(0, 0, minWidth, minHeight)

            // Tạo StaticLayout mới với chiều rộng thích hợp
            val alignmentField = TextSticker::class.java.getDeclaredField("alignment")
            alignmentField.isAccessible = true
            val alignment = alignmentField.get(this) as Layout.Alignment

            // Tạo layout với chiều rộng phù hợp cho text
            val layoutWidth = minWidth - paddingHorizontal
            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(alignment)
                .setIncludePad(true)
                .setLineSpacing(0f, 1.0f)
                .build()

            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            staticLayoutField.set(this, staticLayout)

            Log.d("FlexibleTextSticker", "Updated border: $minWidth x $minHeight")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating bounds", e)
        }
    }

    // Ghi đè phương thức vẽ để căn giữa text
    override fun draw(canvas: Canvas) {
        try {
            canvas.save()
            canvas.concat(matrix)

            // Lấy staticLayout và realBounds
            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            val staticLayout = staticLayoutField.get(this) as StaticLayout

            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect

            // Vẽ background nếu có
            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.draw(canvas)

            // Tính toán vị trí căn giữa
            val horizontalCenter = (realBounds.width() - staticLayout.width) / 2f
            val verticalCenter = (realBounds.height() - staticLayout.height) / 2f

            // Di chuyển để căn giữa và vẽ text
            canvas.translate(horizontalCenter, verticalCenter)
            staticLayout.draw(canvas)

            canvas.restore()
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error in draw", e)
            super.draw(canvas) // Fallback
        }
    }

    // Vô hiệu hóa hoàn toàn phương thức resizeText()
    override fun resizeText(): TextSticker {
        // Không làm gì cả, vô hiệu hóa tính năng resize tự động
        return this
    }
}