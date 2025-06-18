package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import com.xiaopo.flying.sticker.TextSticker
//import kotlinx.coroutines.InternalCoroutinesApi
//import kotlinx.coroutines.NonDisposableHandle.parent
import kotlin.math.ceil

class FlexibleTextSticker(private val context: Context) : TextSticker(context) {

    private var customTextSizeSp: Int = 18

    private var isInitialSetup = true

    private var showCustomBorder: Boolean = false

    private val borderPaint = Paint().apply {
        color = Color.GREEN
        alpha = 255
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var borderPadding = 20f
    private var borderCornerRadius = 10f

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

    private var curveAngle: Float = 0f
    fun getCurveAngle(): Float = curveAngle


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

            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            val lineSpacingMultiplier = lineSpacingMultiplierField.get(this) as Float

            val lineSpacingExtraField = TextSticker::class.java.getDeclaredField("lineSpacingExtra")
            lineSpacingExtraField.isAccessible = true
            val lineSpacingExtra = lineSpacingExtraField.get(this) as Float

            val lines = text.split("\n")

            var maxLineWidth = 0f
            for (line in lines) {
                val lineWidth = calculateTextWidthWithLetterSpacing(line, textPaint)
                maxLineWidth = maxOf(maxLineWidth, lineWidth)
            }

            val fontMetrics = textPaint.fontMetrics
            val lineHeight = (fontMetrics.descent - fontMetrics.ascent)
            val totalLineHeight = lineHeight * lineSpacingMultiplier
            val textHeight = totalLineHeight * lines.size + lineSpacingExtra * (lines.size - 1)

            val paddingHorizontal = 20
            val paddingVertical = 20

            val minWidth = ceil(maxLineWidth + paddingHorizontal * 2).toInt().coerceAtLeast(100)
            val minHeight = ceil(textHeight + paddingVertical * 2).toInt().coerceAtLeast(40)

            Log.d("FlexibleTextSticker", "Text: '$text', Lines: ${lines.size}, Max width: $maxLineWidth, Height: $textHeight")
            Log.d("FlexibleTextSticker", "Required width: $minWidth, height: $minHeight")

            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as Rect

            if (isInitialSetup) {
                realBounds.set(-minWidth/2, -minHeight/2, minWidth/2, minHeight/2)
                isInitialSetup = false
            } else {
                val centerX = realBounds.exactCenterX()
                val centerY = realBounds.exactCenterY()

                realBounds.set(
                    (centerX - minWidth / 2).toInt(),
                    (centerY - minHeight / 2).toInt(),
                    (centerX + minWidth / 2).toInt(),
                    (centerY + minHeight / 2).toInt()
                )
            }

            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as Rect
            textRect.set(realBounds)

            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.setBounds(realBounds)

            val layoutWidth = minWidth - paddingHorizontal
            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(currentAlignment)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(true)
                .build()

            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            staticLayoutField.set(this, staticLayout)

            Log.d("FlexibleTextSticker", "Updated bounds: left=${realBounds.left}, top=${realBounds.top}, right=${realBounds.right}, bottom=${realBounds.bottom}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating bounds", e)
        }
    }

    private fun calculateTextWidthWithLetterSpacing(text: String, textPaint: TextPaint): Float {
        if (text.isEmpty()) return 0f

        val baseWidth = textPaint.measureText(text)

        return if (letterSpacing != 0f) {
            if (letterSpacing > 0) {
                val expandRatio = 1f + letterSpacing * (text.length - 1f) / text.length
                baseWidth * expandRatio * 1.02f
            } else {
                baseWidth * 1.01f
            }
        } else {
            baseWidth * 1.01f
        }
    }

    private fun updateRealBoundsToText() {
        try {
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(this) as Rect
            val oldWidth = realBounds.width()
            val oldHeight = realBounds.height()

            updateBoundsToFitText()

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
            val realBounds = realBoundsField.get(this) as Rect

            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            // Vẽ border nếu cần
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

            val text = getText() ?: ""
            if (text.isNotEmpty()) {
                if (text.contains("\n")) {
                    // Chỉ sử dụng StaticLayout cho text nhiều dòng
                    val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
                    staticLayoutField.isAccessible = true
                    val staticLayout = staticLayoutField.get(this) as? StaticLayout

                    if (staticLayout != null) {
                        val offsetX = realBounds.left + (realBounds.width() - staticLayout.width) / 2f

                        val textHeight = staticLayout.height
                        val boundsHeight = realBounds.height()
                        val offsetY = realBounds.top + (boundsHeight - textHeight) / 2f

                        canvas.save()
                        canvas.translate(offsetX, offsetY)
                        staticLayout.draw(canvas)
                        canvas.restore()
                    }
                } else {
                    drawCurvedText(canvas, text, textPaint, realBounds)
                }
            }

            canvas.restore()
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error in draw", e)
            super.draw(canvas)
        }
    }

    override fun contains(point: FloatArray): Boolean {
        val inverse = Matrix()
        matrix.invert(inverse)
        val mapped = FloatArray(2)
        inverse.mapPoints(mapped, point)

        val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
        realBoundsField.isAccessible = true
        val realBounds = realBoundsField.get(this) as Rect

        return mapped[0] >= realBounds.left && mapped[0] <= realBounds.right &&
                mapped[1] >= realBounds.top && mapped[1] <= realBounds.bottom
    }

    override fun resizeText(): TextSticker {
        updateBoundsToFitText()
        updateRealBoundsToText()
        return this
    }

    fun resetScaleKeepPosition() {
        val oldCenter = FloatArray(2)
        val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
        realBoundsField.isAccessible = true
        val realBounds = realBoundsField.get(this) as Rect
        oldCenter[0] = realBounds.exactCenterX()
        oldCenter[1] = realBounds.exactCenterY()
        this.matrix.mapPoints(oldCenter)

        val angle = super.getCurrentAngle()

        this.matrix.reset()

        val newCenter = FloatArray(2)
        newCenter[0] = realBounds.exactCenterX()
        newCenter[1] = realBounds.exactCenterY()

        val dx = oldCenter[0] - newCenter[0]
        val dy = oldCenter[1] - newCenter[1]
        this.matrix.postTranslate(dx, dy)

        this.matrix.postRotate(angle, oldCenter[0], oldCenter[1])
    }

    fun resetScale() {
        this.matrix.reset() // Reset về identity matrix
    }


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

    private fun updateTypeface() {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        if (currentTypeface != Typeface.DEFAULT) {
            try {
                val newTypeface = Typeface.create(currentTypeface, style)
                super.setTypeface(newTypeface)
            } catch (e: Exception) {
                Log.e("FlexibleTextSticker", "Error updating typeface style", e)
                super.setTypeface(Typeface.create(Typeface.DEFAULT, style))
            }
        } else {
            super.setTypeface(Typeface.create(Typeface.DEFAULT, style))
        }

        updateBoundsToFitText()
        updateRealBoundsToText()
    }

    override fun setTypeface(typeface: Typeface?): TextSticker {
        if (typeface != null) {
            currentTypeface = typeface

            val style = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            val styledTypeface = Typeface.create(typeface, style)
            return super.setTypeface(styledTypeface)
        }
        return super.setTypeface(typeface)
    }

    override fun setTextAlign(alignment: Layout.Alignment): TextSticker {
        currentAlignment = alignment

        val result = super.setTextAlign(alignment)

        refreshLayout()

        return result
    }

    fun refreshLayout() {
        try {
            val text = getText() ?: ""
            if (text.isEmpty()) return

            Log.d("FlexibleTextSticker", "Refreshing layout - Line height: $lineHeightPercent%, Letter spacing: $letterSpacing")

            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            textPaint.letterSpacing = letterSpacing

            updateBoundsToFitText()

            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as Rect

            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            val lineSpacingMultiplier = lineSpacingMultiplierField.get(this) as Float

            val lineSpacingExtraField = TextSticker::class.java.getDeclaredField("lineSpacingExtra")
            lineSpacingExtraField.isAccessible = true
            val lineSpacingExtra = lineSpacingExtraField.get(this) as Float

            val layoutWidth = textRect.width() - 40

            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, layoutWidth)
                .setAlignment(currentAlignment)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(true)
                .build()

            val staticLayoutField = TextSticker::class.java.getDeclaredField("staticLayout")
            staticLayoutField.isAccessible = true
            staticLayoutField.set(this, staticLayout)

            val alignmentField = TextSticker::class.java.getDeclaredField("alignment")
            alignmentField.isAccessible = true
            alignmentField.set(this, currentAlignment)

            Log.d("FlexibleTextSticker", "Layout refreshed successfully - Layout width: ${staticLayout.width}, Height: ${staticLayout.height}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error refreshing layout", e)
        }
    }

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

            val lineSpacingMultiplierField = TextSticker::class.java.getDeclaredField("lineSpacingMultiplier")
            lineSpacingMultiplierField.isAccessible = true
            lineSpacingMultiplierField.set(this, lineSpacingMultiplier)

            val lineSpacingExtraField = TextSticker::class.java.getDeclaredField("lineSpacingExtra")
            lineSpacingExtraField.isAccessible = true
            val lineSpacingExtra = lineSpacingExtraField.get(this) as Float

            Log.d("FlexibleTextSticker", "Current lineSpacingExtra: $lineSpacingExtra")

            val text = getText() ?: ""
            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            val textRectField = TextSticker::class.java.getDeclaredField("textRect")
            textRectField.isAccessible = true
            val textRect = textRectField.get(this) as Rect

            val alignmentField = TextSticker::class.java.getDeclaredField("alignment")
            alignmentField.isAccessible = true
            val alignment = alignmentField.get(this) as Layout.Alignment

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

            refreshLayout()

            updateBoundsToFitText()


            Log.d("FlexibleTextSticker", "Static layout updated with new line height")



        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error setting line height: ${e.message}", e)
        }
    }

    override fun setLineSpacing(add: Float, multiplier: Float): TextSticker {
        lineHeightPercent = (multiplier * 100).toInt()
        return super.setLineSpacing(add, multiplier)
    }

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

            val layoutHeight = staticLayout.height
            Log.d("FlexibleTextSticker", "StaticLayout height: $layoutHeight pixels")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error checking line height: ${e.message}")
        }
    }


    fun setLetterSpacing(spacing: Float) {
        try {
            letterSpacing = spacing

            val textPaintField = TextSticker::class.java.getDeclaredField("textPaint")
            textPaintField.isAccessible = true
            val textPaint = textPaintField.get(this) as TextPaint

            textPaint.letterSpacing = spacing

            Log.d("FlexibleTextSticker", "Set letter spacing to $spacing")

            refreshLayout()

        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error setting letter spacing: ${e.message}", e)
        }
    }

    fun toggleUppercase(): Boolean {
        isUppercase = !isUppercase
        updateTextCase()
        return isUppercase
    }

    fun setUppercase(uppercase: Boolean): Boolean {
        if (isUppercase != uppercase) {
            isUppercase = uppercase
            updateTextCase()
        }
        return isUppercase
    }

    private fun updateTextCase() {
        try {
            val currentText = text ?: ""
            if (currentText.isEmpty()) return

            val textField = TextSticker::class.java.getDeclaredField("text")
            textField.isAccessible = true

            val newText = if (isUppercase) {
                currentText.uppercase()
            } else {
                currentText.lowercase()
            }

            textField.set(this, newText)

            // Cập nhật layout
            refreshLayout()

            Log.d("FlexibleTextSticker", "Text case updated to ${if (isUppercase) "UPPERCASE" else "lowercase"}")
        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error updating text case: ${e.message}", e)
        }
    }
    private var originalText: String = ""


    private fun drawCurvedText(canvas: Canvas, text: String, paint: TextPaint, bounds: Rect) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val absAngle = Math.abs(curveAngle)

        try {
            canvas.save()

            if (absAngle < 3f) {
                paint.textAlign = Paint.Align.CENTER
                val yOffset = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
                canvas.drawText(text, centerX, yOffset, paint)

            } else {
                drawCurvedTextCanvas(canvas, text, paint, bounds)
            }

            canvas.restore()

        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error in drawCurvedText", e)
            // Fallback: simple straight text
            canvas.restore()
            canvas.save()
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, centerX, centerY, paint)
            canvas.restore()
        }
    }

    private fun drawCurvedTextCanvas(canvas: Canvas, text: String, paint: TextPaint, bounds: Rect) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val absAngle = Math.abs(curveAngle)

        val textWidth = paint.measureText(text)
        val boundsWidth = bounds.width().toFloat()
        val boundsHeight = bounds.height().toFloat()

        val path = Path()

        when {
            absAngle <= 90f -> {
                createQuadraticPath(path, centerX, centerY, textWidth, boundsWidth, boundsHeight)
            }

            absAngle <= 180f -> {
                createCubicPath(path, centerX, centerY, textWidth, boundsWidth, boundsHeight)
            }

            else -> {
                createArcPath(path, centerX, centerY, textWidth, boundsWidth, boundsHeight)
            }
        }

        paint.textAlign = Paint.Align.CENTER

        val spacing = calculateLetterSpacing(absAngle, paint.textSize)
        val yOffset = calculateYOffset(absAngle, paint.fontMetrics)

        canvas.drawTextOnPath(text, path, spacing, yOffset, paint)
    }

    private fun createQuadraticPath(path: Path, centerX: Float, centerY: Float,
                                    textWidth: Float, boundsWidth: Float, boundsHeight: Float) {
        val pathWidth = Math.min(textWidth * 1.3f, boundsWidth * 0.85f)
        val startX = centerX - pathWidth / 2f
        val endX = centerX + pathWidth / 2f

        val absAngle = Math.abs(curveAngle)
        val bendRatio = absAngle / 90f
        val maxBend = Math.min(boundsHeight * 0.4f, pathWidth * 0.6f)
        val bendAmount = maxBend * bendRatio * (if (curveAngle < 0) -1f else 1f)

        path.moveTo(startX, centerY)
        path.quadTo(centerX, centerY + bendAmount, endX, centerY)
    }

    private fun createCubicPath(path: Path, centerX: Float, centerY: Float,
                                textWidth: Float, boundsWidth: Float, boundsHeight: Float) {
        val pathWidth = Math.min(textWidth * 1.4f, boundsWidth * 0.9f)
        val startX = centerX - pathWidth / 2f
        val endX = centerX + pathWidth / 2f

        val absAngle = Math.abs(curveAngle)
        val bendRatio = (absAngle - 90f) / 90f
        val maxBend = Math.min(boundsHeight * 0.5f, pathWidth * 0.8f)
        val bendAmount = maxBend * (0.5f + bendRatio * 0.5f) * (if (curveAngle < 0) -1f else 1f)

        // Two control points for smoother S-curve
        val control1X = centerX - pathWidth * 0.25f
        val control2X = centerX + pathWidth * 0.25f
        val controlY = centerY + bendAmount

        path.moveTo(startX, centerY)
        path.cubicTo(control1X, controlY, control2X, controlY, endX, centerY)
    }

    private fun createArcPath(path: Path, centerX: Float, centerY: Float,
                              textWidth: Float, boundsWidth: Float, boundsHeight: Float) {
        val absAngle = Math.abs(curveAngle)

        val radiusFactor = when {
            absAngle >= 300f -> 0.6f
            absAngle >= 240f -> 0.7f
            else -> 0.8f
        }

        val radius = Math.min(boundsWidth, boundsHeight) * radiusFactor

        val centerOffset = radius * 0.2f
        val arcCenterX = centerX
        val arcCenterY = centerY + (if (curveAngle < 0) -centerOffset else centerOffset)

        val ovalRect = RectF(
            arcCenterX - radius,
            arcCenterY - radius,
            arcCenterX + radius,
            arcCenterY + radius
        )

        val gap = Math.max(20f, absAngle * 0.1f)
        val sweepAngle = Math.min(absAngle - gap, 320f)

        val startAngle = if (curveAngle < 0) {
            270f - sweepAngle / 2f
        } else {
            90f - sweepAngle / 2f
        }

        path.addArc(ovalRect, startAngle, sweepAngle)
    }

    private fun calculateLetterSpacing(absAngle: Float, textSize: Float): Float {
        return when {
            absAngle < 1f -> textSize * 0.001f   // Minimal spacing ngay từ 0°
            absAngle < 30f -> textSize * 0.005f  // Tăng dần từ 0°
            absAngle < 90f -> textSize * 0.01f * (absAngle / 90f)
            absAngle < 180f -> textSize * 0.02f
            else -> textSize * 0.03f * Math.min(absAngle / 270f, 1f)
        }
    }

    private fun calculateYOffset(absAngle: Float, fontMetrics: Paint.FontMetrics): Float {
        val baseOffset = -fontMetrics.ascent / 2f

        return when {
            absAngle < 45f -> baseOffset
            absAngle < 90f -> baseOffset * 0.8f
            absAngle < 180f -> baseOffset * 0.6f
            else -> baseOffset * 0.4f
        }
    }

    fun invalidateSticker() {
        try {
            val currentMatrix = Matrix(this.matrix)
            currentMatrix.postTranslate(0.01f, 0.01f) // Tiny movement
            currentMatrix.postTranslate(-0.01f, -0.01f) // Move back
            this.setMatrix(currentMatrix) // Use setter method

            Log.d("FlexibleTextSticker", "Sticker invalidated via matrix update")

        } catch (e: Exception) {
            Log.d("FlexibleTextSticker", "Could not invalidate sticker: ${e.message}")
        }
    }

    private fun forceRedraw() {
        try {
            val drawableField = TextSticker::class.java.getDeclaredField("drawable")
            drawableField.isAccessible = true
            val drawable = drawableField.get(this) as? android.graphics.drawable.Drawable
            drawable?.invalidateSelf()

            if (getText()?.contains("\n") != true) {
                refreshLayout()
            }

            Log.d("FlexibleTextSticker", "Force redraw completed")

        } catch (e: Exception) {
            Log.d("FlexibleTextSticker", "Could not force redraw: ${e.message}")
        }
    }

    fun setCurveAngle(angle: Float) {
        val oldAngle = curveAngle
        curveAngle = angle

        Log.d("FlexibleTextSticker", "Curve angle set to: $angle° (was: $oldAngle°)")

        if (oldAngle != angle) {
            forceRedraw()
        }
    }




    fun createDuplicate(offsetX: Float = 50f, offsetY: Float = 50f): FlexibleTextSticker {
        try {
            val duplicateSticker = FlexibleTextSticker(context)

            // Copy properties as before
            duplicateSticker.isInitialSetup = false
            duplicateSticker.originalText = this.originalText
            duplicateSticker.customTextSizeSp = this.customTextSizeSp
            duplicateSticker.currentTextColor = this.currentTextColor
            duplicateSticker.isBold = this.isBold
            duplicateSticker.isItalic = this.isItalic
            duplicateSticker.currentTypeface = this.currentTypeface
            duplicateSticker.currentAlignment = this.currentAlignment
            duplicateSticker.lineHeightPercent = this.lineHeightPercent
            duplicateSticker.letterSpacing = this.letterSpacing
            duplicateSticker.isUppercase = this.isUppercase
            duplicateSticker.curveAngle = this.curveAngle
            duplicateSticker.showCustomBorder = this.showCustomBorder

            duplicateSticker.setTextSizeSp(this.customTextSizeSp)

            val finalText = if (this.isUppercase) this.originalText.uppercase() else this.originalText
            duplicateSticker.setText(finalText)


            val duplicateMatrix = Matrix(this.matrix)

            duplicateMatrix.postTranslate(offsetX, offsetY)

            duplicateSticker.setMatrix(duplicateMatrix)

            // Debug
            val values = FloatArray(9)
            this.matrix.getValues(values)
            val newValues = FloatArray(9)
            duplicateMatrix.getValues(newValues)

            Log.d("FlexibleTextSticker", "=== MATRIX COPY DEBUG ===")
            Log.d("FlexibleTextSticker", "Original position: (${values[Matrix.MTRANS_X]}, ${values[Matrix.MTRANS_Y]})")
            Log.d("FlexibleTextSticker", "Duplicate position: (${newValues[Matrix.MTRANS_X]}, ${newValues[Matrix.MTRANS_Y]})")
            Log.d("FlexibleTextSticker", "Offset applied: ($offsetX, $offsetY)")
            Log.d("FlexibleTextSticker", "Scale preserved: ${newValues[Matrix.MSCALE_X]}")

            return duplicateSticker

        } catch (e: Exception) {
            Log.e("FlexibleTextSticker", "Error creating duplicate: ${e.message}", e)
            throw e
        }
    }


    fun debugMatrix() {
        val values = FloatArray(9)
        matrix.getValues(values)
        Log.d("FlexibleTextSticker", "=== MATRIX DEBUG ===")
        Log.d("FlexibleTextSticker", "ScaleX: ${values[Matrix.MSCALE_X]}")
        Log.d("FlexibleTextSticker", "ScaleY: ${values[Matrix.MSCALE_Y]}")
        Log.d("FlexibleTextSticker", "TransX: ${values[Matrix.MTRANS_X]}")
        Log.d("FlexibleTextSticker", "TransY: ${values[Matrix.MTRANS_Y]}")
        Log.d("FlexibleTextSticker", "Size: ${customTextSizeSp}sp")
        Log.d("FlexibleTextSticker", "===================")
    }


}
