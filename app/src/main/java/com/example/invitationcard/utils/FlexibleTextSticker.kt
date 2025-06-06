package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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


    // Thêm biến này để kiểm soát hiển thị border
    private var showCustomBorder: Boolean = false

    // Thêm các thuộc tính cho border
    private val borderPaint = Paint().apply {
        color = android.graphics.Color.GREEN
        alpha = 255
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var borderPadding = 20f // Padding cho border
    private var borderCornerRadius = 10f // Bo góc cho border

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

    // Thêm phương thức để cập nhật border theo kích thước text
    private fun updateBorderSize() {
        try {
            val text = getText() ?: ""
            if (text.isEmpty()) return

            // Lấy TextPaint
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            // Tính toán kích thước border dựa trên kích thước text
            val scale = getCurrentScale()
            val scaledPadding = borderPadding * scale
            val scaledCornerRadius = borderCornerRadius * scale
            borderPaint.strokeWidth = 8f * scale

            // Cập nhật borderPaint
            borderPaint.alpha = (255 * (1 - (scale - 1) * 0.3)).toInt().coerceIn(128, 255)

            // Cập nhật realBounds và textRect với padding mới
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect

            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as android.graphics.Rect

            // Tính toán kích thước mới cho border
            val borderWidth = realBounds.width() + (scaledPadding * 2)
            val borderHeight = realBounds.height() + (scaledPadding * 2)

            // Cập nhật bounds
            realBounds.set(0, 0, borderWidth.toInt(), borderHeight.toInt())
            textRect.set(0, 0, borderWidth.toInt(), borderHeight.toInt())

            // Cập nhật drawable nếu có
            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.setBounds(0, 0, borderWidth.toInt(), borderHeight.toInt())

        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating border size", e)
        }
    }



    // Cập nhật phương thức setTextSizeSp để cập nhật border
    fun setTextSizeSp(sizeInSp: Int) {
        try {
            customTextSizeSp = sizeInSp
            val sizeInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sizeInSp.toFloat(),
                context.resources.displayMetrics
            )
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint
            textPaint.textSize = sizeInPixels

            val minTextSizePixelsField = TextSticker::class.java.getDeclaredField("minTextSizePixels")
            minTextSizePixelsField.isAccessible = true
            minTextSizePixelsField.setFloat(this, sizeInPixels)
            val maxTextSizePixelsField = TextSticker::class.java.getDeclaredField("maxTextSizePixels")
            maxTextSizePixelsField.isAccessible = true
            maxTextSizePixelsField.setFloat(this, sizeInPixels)

            updateBoundsToFitText()
            updateRealBoundsToText()
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error setting text size", e)
        }
    }

    // Cập nhật phương thức calculateTextSizeFromScale để cập nhật border
    fun calculateTextSizeFromScale(): Int {
        try {
            // Lấy scale hiện tại từ matrix
            val scale = getCurrentScale()

            // Tính toán kích thước mới dựa trên scale
            val newSize = (customTextSizeSp * scale).toInt()

            // Giới hạn kích thước trong khoảng cho phép
            val finalSize = newSize.coerceIn(8, 200)

            // Cập nhật border
            updateBorderSize()

            return finalSize
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error calculating text size from scale", e)
            return customTextSizeSp
        }
    }

    fun getTextSizeSp(): Int = customTextSizeSp

    override fun setText(text: String?): TextSticker {
        val result = super.setText(text)
        updateRealBoundsToText()
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

    private fun updateRealBoundsToText() {
        try {
            updateBoundsToFitText() // Đảm bảo staticLayout đã được tạo
            val text = getText() ?: ""
            if (text.isEmpty()) return

            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            val staticLayout = staticLayoutField.get(this) as? StaticLayout ?: return

            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect

            val padding = 20
            val width = staticLayout.width + padding * 2
            val height = staticLayout.height + padding * 2

            realBounds.set(0, 0, width, height)
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating realBounds", e)
        }
    }

    fun setShowBorder(show: Boolean) {
        this.showCustomBorder = show
    }

    override fun draw(canvas: Canvas) {
        try {
            canvas.save()
            canvas.concat(matrix)

            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect

            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            val staticLayout = staticLayoutField.get(this) as? StaticLayout
            if (staticLayout == null) {
                canvas.restore()
                return
            }

            // Chỉ vẽ border khi showCustomBorder = true
            if (showCustomBorder) {
                canvas.drawRoundRect(
                    realBounds.left.toFloat(),
                    realBounds.top.toFloat(),
                    realBounds.right.toFloat(),
                    realBounds.bottom.toFloat(),
                    borderCornerRadius,
                    borderCornerRadius,
                    borderPaint
                )
            }

            val horizontalCenter = (realBounds.width() - staticLayout.width) / 2f
            val verticalCenter = (realBounds.height() - staticLayout.height) / 2f
            canvas.translate(horizontalCenter, verticalCenter)
            staticLayout.draw(canvas)

            canvas.restore()
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error in draw", e)
            super.draw(canvas)
        }
    }

    override fun contains(point: FloatArray): Boolean {
        // Chuyển điểm touch về local coordinate của sticker
        val inverse = Matrix()
        matrix.invert(inverse)
        val mapped = FloatArray(2)
        inverse.mapPoints(mapped, point)

        // Lấy realBounds đã được cập nhật theo border
        val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
        realBoundsField.isAccessible = true
        val realBounds = realBoundsField.get(this) as android.graphics.Rect

        // Kiểm tra điểm có nằm trong realBounds không
        return mapped[0] >= realBounds.left && mapped[0] <= realBounds.right &&
                mapped[1] >= realBounds.top && mapped[1] <= realBounds.bottom
    }

    // Vô hiệu hóa hoàn toàn phương thức resizeText()
    override fun resizeText(): TextSticker {
        updateBoundsToFitText()
        updateRealBoundsToText()
        return this
    }

    fun resetScaleKeepPosition() {
        // 1. Lưu lại vị trí trung tâm hiện tại trên canvas
        val oldCenter = FloatArray(2)
        val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
        realBoundsField.isAccessible = true
        val realBounds = realBoundsField.get(this) as android.graphics.Rect
        oldCenter[0] = realBounds.exactCenterX()
        oldCenter[1] = realBounds.exactCenterY()
        this.matrix.mapPoints(oldCenter)

        // 2. Lưu lại góc xoay hiện tại
        val angle = getCurrentAngle()

        // 3. Reset matrix
        this.matrix.reset()

        // 4. Tính lại vị trí trung tâm mới (sau khi reset)
        val newCenter = FloatArray(2)
        newCenter[0] = realBounds.exactCenterX()
        newCenter[1] = realBounds.exactCenterY()
        // (vì matrix đã reset nên newCenter là tâm của sticker mới tại (width/2, height/2))

        // 5. Tính delta và di chuyển sticker về đúng vị trí cũ
        val dx = oldCenter[0] - newCenter[0]
        val dy = oldCenter[1] - newCenter[1]
        this.matrix.postTranslate(dx, dy)

        // 6. Đặt lại góc xoay
        this.matrix.postRotate(angle, oldCenter[0], oldCenter[1])
    }

    fun resetScale() {
        this.matrix.reset() // Reset về identity matrix
    }

}