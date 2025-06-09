package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import com.xiaopo.flying.sticker.TextSticker

class FlexibleTextSticker(context: Context) : TextSticker(context) {

    private var customTextSizeSp: Int = 18  // Bắt đầu với kích thước nhỏ hơn
    private val context: Context = context

    // Thêm biến để theo dõi trạng thái khởi tạo
    private var isInitialSetup = true

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

    // Thêm biến lưu trữ màu
    private var currentTextColor: Int = Color.BLACK

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
            val text = text ?: ""
            if (text.isEmpty()) return

            // Lấy TextPaint
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            // Tính toán kích thước border dựa trên kích thước text
            val scale = currentScale
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

            // Cập nhật cả realBounds và textRect
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect

            // Tính kích thước mới
            val minWidth = Math.max(textWidth + paddingHorizontal * 2, 150)
            val minHeight = Math.max(textHeight + paddingVertical * 2, 60)

            // Xử lý đặc biệt cho lần đầu tiên
            if (isInitialSetup) {
                // Đặt bounds ban đầu với điểm neo ở trung tâm (0,0)
                realBounds.set(-minWidth/2, -minHeight/2, minWidth/2, minHeight/2)
                isInitialSetup = false
                Log.d("FlexibleTextSticker", "Initial setup with centered bounds")
            } else {
                // Lưu lại kích thước cũ
                val oldWidth = realBounds.width()
                val oldHeight = realBounds.height()

                // Tính độ chênh lệch
                val widthDiff = (minWidth - oldWidth) / 2
                val heightDiff = (minHeight - oldHeight) / 2

                // Mở rộng bounds theo cả 4 hướng từ trung tâm
                realBounds.left -= widthDiff
                realBounds.top -= heightDiff
                realBounds.right += widthDiff
                realBounds.bottom += heightDiff
            }

            // Cập nhật textRect và drawable
            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as android.graphics.Rect
            textRect.set(realBounds)

            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.setBounds(
                realBounds.left,
                realBounds.top,
                realBounds.right,
                realBounds.bottom
            )

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

            Log.d("FlexibleTextSticker", "Updated bounds: left=${realBounds.left}, top=${realBounds.top}, right=${realBounds.right}, bottom=${realBounds.bottom}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating bounds", e)
        }
    }

    private fun updateRealBoundsToText() {
        try {
            // Lưu kích thước cũ
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect
            val oldWidth = realBounds.width()
            val oldHeight = realBounds.height()

            // Gọi updateBoundsToFitText() để cập nhật StaticLayout
            updateBoundsToFitText()

            // Phần còn lại không cần thiết vì updateBoundsToFitText đã cập nhật tất cả
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

            // Vẽ text căn giữa trong bounds
            val offsetX = realBounds.left + (realBounds.width() - staticLayout.width) / 2f
            val offsetY = realBounds.top + (realBounds.height() - staticLayout.height) / 2f
            canvas.translate(offsetX, offsetY)
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

//    // Hàm để lấy scale hiện tại
//    fun getCurrentScale(): Float {
//        val values = FloatArray(9)
//        matrix.getValues(values)
//        // Scale là giá trị đầu tiên trong ma trận
//        return values[Matrix.MSCALE_X]
//    }
//
//    // Hàm để lấy góc xoay hiện tại
//    fun getCurrentAngle(): Float {
//        val values = FloatArray(9)
//        matrix.getValues(values)
//        // Tính góc xoay từ ma trận
//        return Math.toDegrees(Math.atan2(values[Matrix.MSKEW_X].toDouble(),
//            values[Matrix.MSCALE_X].toDouble())).toFloat()
//    }

    // Phương thức custom để set màu
    fun setCustomTextColor(color: Int) {
        currentTextColor = color
        super.setTextColor(color)
    }

    // Phương thức custom để get màu
    fun getCustomTextColor(): Int {
        return currentTextColor
    }
}