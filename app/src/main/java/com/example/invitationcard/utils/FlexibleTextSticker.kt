package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import com.xiaopo.flying.sticker.TextSticker
import kotlin.math.ceil

class FlexibleTextSticker(context: Context) : TextSticker(context) {

    private var customTextSizeSp: Int = 18  // Bắt đầu với kích thước nhỏ hơn
    private val context: Context = context

    // Thêm biến để theo dõi trạng thái khởi tạo
    private var isInitialSetup = true

    // Thêm biến này để kiểm soát hiển thị border
    private var showCustomBorder: Boolean = false

    private val borderPaint = Paint().apply {
        color = Color.GREEN
        alpha = 255
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var borderPadding = 20f // Padding cho border
    private var borderCornerRadius = 10f // Bo góc cho border

    // Thêm biến lưu trữ màu
    private var currentTextColor: Int = Color.BLACK

    private var isBold: Boolean = false
    private var isItalic: Boolean = false
    private var currentTypeface: Typeface = Typeface.DEFAULT

    fun isBold(): Boolean = isBold
    fun isItalic(): Boolean = isItalic

    private var currentAlignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER

    private var lineHeightPercent: Int = 120

    fun getLineHeightPercent(): Int = lineHeightPercent

    private var letterSpacing: Float = 0f
    fun getLetterSpacing(): Float = letterSpacing


    private var isUppercase: Boolean = false
    fun isUppercase(): Boolean = isUppercase

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

        currentAlignment = Layout.Alignment.ALIGN_CENTER
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
        originalText = text ?: ""

        // Áp dụng chữ hoa/thường nếu cần
        val processedText = if (isUppercase) originalText.uppercase() else originalText.lowercase()

        val result = super.setText(processedText)
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

            // Lấy giá trị lineSpacing
            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            val lineSpacingMultiplier = lineSpacingMultiplierField.get(this) as Float

            val lineSpacingExtraField = TextSticker::class.java.getDeclaredField("lineSpacingExtra")
            lineSpacingExtraField.isAccessible = true
            val lineSpacingExtra = lineSpacingExtraField.get(this) as Float

            // Tách text thành các dòng
            val lines = text.split("\n")

            // Tính chiều rộng dựa trên dòng dài nhất
            var maxLineWidth = 0f
            for (line in lines) {
                // Tính toán chiều rộng của dòng hiện tại với letter spacing
                val lineWidth = calculateTextWidthWithLetterSpacing(line, textPaint)
                maxLineWidth = maxOf(maxLineWidth, lineWidth)
            }

            // Tính chiều cao chính xác của text
            val fontMetrics = textPaint.fontMetrics
            val lineHeight = (fontMetrics.descent - fontMetrics.ascent)
            val totalLineHeight = lineHeight * lineSpacingMultiplier
            val textHeight = totalLineHeight * lines.size + lineSpacingExtra * (lines.size - 1)

            // Đặt padding vừa đủ quanh text
            val paddingHorizontal = 20
            val paddingVertical = 20

            // Tính kích thước tối thiểu cần thiết cho bounds
            val minWidth = ceil(maxLineWidth + paddingHorizontal * 2).toInt().coerceAtLeast(100)
            val minHeight = ceil(textHeight + paddingVertical * 2).toInt().coerceAtLeast(40)

            Log.d("FlexibleTextSticker", "Text: '$text', Lines: ${lines.size}, Max width: $maxLineWidth, Height: $textHeight")
            Log.d("FlexibleTextSticker", "Required width: $minWidth, height: $minHeight")

            // Cập nhật bounds
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as android.graphics.Rect

            if (isInitialSetup) {
                // Lần đầu setup: đặt bounds với điểm neo ở trung tâm
                realBounds.set(-minWidth/2, -minHeight/2, minWidth/2, minHeight/2)
                isInitialSetup = false
            } else {
                // Lưu vị trí trung tâm hiện tại
                val centerX = realBounds.exactCenterX()
                val centerY = realBounds.exactCenterY()

                // Cập nhật bounds với trung tâm giữ nguyên
                realBounds.set(
                    (centerX - minWidth / 2).toInt(),
                    (centerY - minHeight / 2).toInt(),
                    (centerX + minWidth / 2).toInt(),
                    (centerY + minHeight / 2).toInt()
                )
            }

            // Cập nhật textRect
            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as android.graphics.Rect
            textRect.set(realBounds)

            // Cập nhật drawable
            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.setBounds(realBounds)

            // Tạo StaticLayout với alignment hiện tại
            val layoutWidth = minWidth - paddingHorizontal
            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(currentAlignment)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(true)
                .build()

            // Cập nhật StaticLayout
            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            staticLayoutField.set(this, staticLayout)

            Log.d("FlexibleTextSticker", "Updated bounds: left=${realBounds.left}, top=${realBounds.top}, right=${realBounds.right}, bottom=${realBounds.bottom}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating bounds", e)
        }
    }

    // Hàm mới để tính chiều rộng text có xét đến letter spacing
    private fun calculateTextWidthWithLetterSpacing(text: String, textPaint: TextPaint): Float {
        if (text.isEmpty()) return 0f

        // Chiều rộng cơ bản của văn bản
        val baseWidth = textPaint.measureText(text)

        // Điều chỉnh theo letter spacing (nếu có)
        return if (letterSpacing != 0f) {
            // Công thức cải tiến cho độ chính xác hơn
            if (letterSpacing > 0) {
                // Với letter spacing dương (giãn chữ), ta cần thêm không gian
                val expandRatio = 1f + letterSpacing * (text.length - 1f) / text.length
                baseWidth * expandRatio * 1.02f  // Giảm từ 1.05f xuống 1.02f
            } else {
                // Với letter spacing âm (thu chữ), ta chỉ cần một chút padding
                baseWidth * 1.01f
            }
        } else {
            baseWidth * 1.01f  // Thêm 1% margin cho an toàn
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

            // Vẽ border bám sát theo bounds đã tính toán
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

    // Toggle bold
    fun toggleBold(): Boolean {
        isBold = !isBold
        updateTypeface()
        return isBold
    }

    // Toggle italic
    fun toggleItalic(): Boolean {
        isItalic = !isItalic
        updateTypeface()
        return isItalic
    }

    // Set bold
    fun setBold(bold: Boolean): Boolean {
        if (isBold != bold) {
            isBold = bold
            updateTypeface()
        }
        return isBold
    }

    // Set italic
    fun setItalic(italic: Boolean): Boolean {
        if (isItalic != italic) {
            isItalic = italic
            updateTypeface()
        }
        return isItalic
    }

    // Cập nhật typeface dựa trên trạng thái bold và italic
    private fun updateTypeface() {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        // Lưu ý: nếu đã có custom font, cần giữ lại font đó
        if (currentTypeface != Typeface.DEFAULT) {
            try {
                // Tạo typeface mới từ typeface hiện tại với style mới
                val newTypeface = Typeface.create(currentTypeface, style)
                super.setTypeface(newTypeface)
            } catch (e: Exception) {
                Log.e("FlexibleTextSticker", "Error updating typeface style", e)
                // Fallback
                super.setTypeface(Typeface.create(Typeface.DEFAULT, style))
            }
        } else {
            super.setTypeface(Typeface.create(Typeface.DEFAULT, style))
        }

        // Cập nhật lại kích thước và bounds
        updateBoundsToFitText()
        updateRealBoundsToText()
    }

    // Override phương thức setTypeface để cập nhật currentTypeface
    override fun setTypeface(typeface: Typeface?): TextSticker {
        if (typeface != null) {
            currentTypeface = typeface

            // Giữ lại style nếu đang bold hoặc italic
            val style = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            // Tạo typeface mới từ typeface được cung cấp với style hiện tại
            val styledTypeface = Typeface.create(typeface, style)
            return super.setTypeface(styledTypeface)
        }
        return super.setTypeface(typeface)
    }

    // Thay thế phương thức hiện tại
    override fun setTextAlign(alignment: Layout.Alignment): TextSticker {
        currentAlignment = alignment

        // Gọi phương thức của lớp cha
        val result = super.setTextAlign(alignment)

        // Cập nhật layout ngay lập tức
        refreshLayout()

        return result
    }

    fun refreshLayout() {
        try {
            val text = getText() ?: ""
            if (text.isEmpty()) return

            // Log thông tin debug
            Log.d("FlexibleTextSticker", "Refreshing layout - Line height: $lineHeightPercent%, Letter spacing: $letterSpacing")

            // Lấy TextPaint
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            // Đảm bảo letter spacing được áp dụng
            textPaint.letterSpacing = letterSpacing

            // Cập nhật bounds để bám sát text sau khi thay đổi letter spacing/line height
            updateBoundsToFitText()

            // Lấy bounds đã cập nhật
            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as android.graphics.Rect

            // Lấy giá trị lineSpacing
            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            val lineSpacingMultiplier = lineSpacingMultiplierField.get(this) as Float

            val lineSpacingExtraField = TextSticker::class.java.getDeclaredField("lineSpacingExtra")
            lineSpacingExtraField.isAccessible = true
            val lineSpacingExtra = lineSpacingExtraField.get(this) as Float

            // Tính toán chiều rộng cần thiết cho layout (với padding giảm xuống)
            val layoutWidth = textRect.width() - 40 // Giảm padding để tránh xuống dòng không cần thiết

            // Tạo StaticLayout với các tham số đã cập nhật
            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(currentAlignment)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(true)
                .build()

            // Cập nhật StaticLayout
            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            staticLayoutField.set(this, staticLayout)

            // Cập nhật alignment
            val alignmentField = TextSticker::class.java.getDeclaredField("alignment")
            alignmentField.isAccessible = true
            alignmentField.set(this, currentAlignment)

            Log.d("FlexibleTextSticker", "Layout refreshed successfully - Layout width: ${staticLayout.width}, Height: ${staticLayout.height}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error refreshing layout", e)
        }
    }

    // Cung cấp getter cho alignment hiện tại
    fun getTextAlignment(): Layout.Alignment {
        return currentAlignment
    }

    // Thêm setter
    fun setLineHeightPercent(percent: Int) {
        try {
            Log.d("FlexibleTextSticker", "Setting line height from $lineHeightPercent% to $percent%")
            lineHeightPercent = percent
            val lineSpacingMultiplier = percent / 100f

            Log.d("FlexibleTextSticker", "Calculated multiplier: $lineSpacingMultiplier")

            // Cập nhật lineSpacingMultiplier trong TextSticker
            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            lineSpacingMultiplierField.set(this, lineSpacingMultiplier)

            // Cập nhật lineSpacingExtra cũng cần thiết
            val lineSpacingExtraField = TextSticker::class.java.getDeclaredField("lineSpacingExtra")
            lineSpacingExtraField.isAccessible = true
            val lineSpacingExtra = lineSpacingExtraField.get(this) as Float

            Log.d("FlexibleTextSticker", "Current lineSpacingExtra: $lineSpacingExtra")

            // Điểm quan trọng: Tạo lại StaticLayout với các giá trị mới
            val text = getText() ?: ""
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as android.graphics.Rect

            val alignmentField = TextSticker::class.java.getDeclaredField("alignment")
            alignmentField.isAccessible = true
            val alignment = alignmentField.get(this) as Layout.Alignment

            // SỬA: Đảm bảo sử dụng giá trị lineSpacingMultiplier mới
            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, textRect.width() - 40)
                .setAlignment(alignment)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier) // Quan trọng!
                .setIncludePad(true)
                .build()

            // Cập nhật StaticLayout
            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            staticLayoutField.set(this, staticLayout)

            // Cập nhật layout
            refreshLayout()

            // Đảm bảo cập nhật cả bounds để border bám sát text mới
            updateBoundsToFitText()


            Log.d("FlexibleTextSticker", "Static layout updated with new line height")

            // Không cần gọi updateBounds vì chỉ thay đổi khoảng cách giữa các dòng
            // updateBoundsToFitText()
            // updateRealBoundsToText()

        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error setting line height: ${e.message}", e)
        }
    }

    // Override phương thức setLineSpacing để cập nhật biến lineHeightPercent
    override fun setLineSpacing(add: Float, multiplier: Float): TextSticker {
        lineHeightPercent = (multiplier * 100).toInt()
        return super.setLineSpacing(add, multiplier)
    }

    // Thêm phương thức này vào FlexibleTextSticker để kiểm tra xem line height thực sự được áp dụng không
    fun checkLineHeightApplied() {
        try {
            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            val actualMultiplier = lineSpacingMultiplierField.get(this) as Float

            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            val staticLayout = staticLayoutField.get(this) as StaticLayout

            Log.d("FlexibleTextSticker", "CHECK: lineHeightPercent=$lineHeightPercent, " +
                    "multiplier=$actualMultiplier, " +
                    "lineCount=${staticLayout.lineCount}")

            // Log chiều cao của StaticLayout
            val layoutHeight = staticLayout.height
            Log.d("FlexibleTextSticker", "StaticLayout height: $layoutHeight pixels")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error checking line height: ${e.message}")
        }
    }


    fun setLetterSpacing(spacing: Float) {
        try {
            letterSpacing = spacing

            // Lấy TextPaint
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            // Đặt letterSpacing
            textPaint.letterSpacing = spacing

            Log.d("FlexibleTextSticker", "Set letter spacing to $spacing")

            // Tạo lại layout với chiều rộng phù hợp
            refreshLayout()

            // Không thay đổi bounds hay border
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error setting letter spacing: ${e.message}", e)
        }
    }

    fun toggleUppercase(): Boolean {
        isUppercase = !isUppercase
        updateTextCase()
        return isUppercase
    }

    // Thêm phương thức để đặt trạng thái uppercase
    fun setUppercase(uppercase: Boolean): Boolean {
        if (isUppercase != uppercase) {
            isUppercase = uppercase
            updateTextCase()
        }
        return isUppercase
    }

    // Phương thức cập nhật text theo trạng thái chữ hoa/thường
    private fun updateTextCase() {
        try {
            val currentText = getText()?.toString() ?: ""
            if (currentText.isEmpty()) return

            // Lấy text field từ TextSticker
            val textField = TextSticker::class.java.getDeclaredField("text")
            textField.isAccessible = true

            // Chuyển đổi text dựa trên trạng thái uppercase
            val newText = if (isUppercase) {
                currentText.uppercase()
            } else {
                currentText.lowercase()
            }

            // Cập nhật text field trong TextSticker
            textField.set(this, newText)

            // Cập nhật layout
            refreshLayout()

            Log.d("FlexibleTextSticker", "Text case updated to ${if (isUppercase) "UPPERCASE" else "lowercase"}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating text case: ${e.message}", e)
        }
    }
    // Ghi đè phương thức setText để lưu text gốc
    private var originalText: String = ""

//    override fun setText(text: String?): TextSticker {
//        originalText = text ?: ""
//
//        // Áp dụng chữ hoa/thường nếu cần
//        val processedText = if (isUppercase) originalText.uppercase() else originalText.lowercase()
//
//        val result = super.setText(processedText)
//        updateRealBoundsToText()
//        return result
//    }


}