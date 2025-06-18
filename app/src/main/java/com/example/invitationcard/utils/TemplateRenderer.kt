package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.caverock.androidsvg.SVG
import com.example.invitationcard.model.TemplateElement
import com.example.invitationcard.model.MaskShape
import com.xiaopo.flying.sticker.DrawableSticker
import com.xiaopo.flying.sticker.Sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Chuyển đổi các TemplateElement thành Sticker để hiển thị trong StickerView
 */
class TemplateRenderer(
    private val context: Context,
    private val fontManager: FontManager
) {
    companion object {
        private const val TAG = "TemplateRenderer"
    }

    /**
     * Tạo sticker từ bất kỳ loại TemplateElement nào, bao gồm cả OCR text elements
     */
    suspend fun createStickerFromElement(element: TemplateElement): Sticker? {
        try {
            // Debug đầu vào
            Log.d(TAG, "Creating sticker for ${element.javaClass.simpleName}: id=${element.id}, z=${element.zIndex}")
            Log.d(TAG, "Bounds: ${element.bounds}, rotation: ${element.rotation}")

            // Tạo sticker dựa trên loại element
            val sticker = when (element) {
                is TemplateElement.TextElement -> createTextSticker(element)
                is TemplateElement.ImageElement -> createImageSticker(element)
                is TemplateElement.SvgElement -> createSvgSticker(element)
                is TemplateElement.VectorElement -> createVectorSticker(element)
                is TemplateElement.GroupElement -> createGroupSticker(element)
                else -> {
                    Log.e(TAG, "Unknown element type: ${element.javaClass.simpleName}")
                    return null
                }
            }

            sticker?.let {
                // Áp dụng ma trận biến đổi chính xác cho mọi loại sticker
                applyTransformMatrix(sticker, element)
            }

            return sticker
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sticker from element ${element.id}: ${e.message}", e)
            return null
        }
    }

    /**
     * Tạo text sticker từ TextElement (bao gồm OCR text)
     */
    private suspend fun createTextSticker(element: TemplateElement.TextElement): FlexibleTextSticker {
        // Log thông tin cho debug OCR
        if (element.id.startsWith("ocr_")) {
            Log.d(TAG, "Creating OCR text sticker: '${element.text}'")
            Log.d(TAG, "OCR text properties: font=${element.fontName}, size=${element.fontSize}, color=#${Integer.toHexString(element.color)}")
            Log.d(TAG, "OCR text style: bold=${element.bold}, italic=${element.italic}, uppercase=${element.uppercase}")
            Log.d(TAG, "OCR text position: ${element.bounds}")
        }

        // Tạo text sticker với các thuộc tính từ element
        val textSticker = FlexibleTextSticker(context).apply {
            setText(element.text)
            setTextSizeSp(element.fontSize.toInt())
            setCustomTextColor(element.color)
            setTextAlign(element.alignment)

            // Áp dụng các định dạng
            if (element.bold) setBold(true)
            if (element.italic) setItalic(true)
            if (element.uppercase) setUppercase(true)
            if (element.curveAngle != 0f) setCurveAngle(element.curveAngle)
            setLineHeightPercent(element.lineHeight)
            setLetterSpacing(element.letterSpacing)
        }

        // Tìm và áp dụng font
        element.fontName?.let { fontName ->
            try {
                // Xử lý font hệ thống
                when (fontName) {
                    "sans-serif", "serif", "monospace", "cursive" -> {
                        val style = when {
                            element.bold && element.italic -> android.graphics.Typeface.BOLD_ITALIC
                            element.bold -> android.graphics.Typeface.BOLD
                            element.italic -> android.graphics.Typeface.ITALIC
                            else -> android.graphics.Typeface.NORMAL
                        }
                        val typeface = android.graphics.Typeface.create(fontName, style)
                        textSticker.setTypeface(typeface)
                    }
                    else -> {
                        // Tìm font từ FontManager
                        fontManager.findFontByName(fontName)?.let { fontItem ->
                            fontItem.typeface?.let { textSticker.setTypeface(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading font: $fontName", e)
            }
        }

        return textSticker
    }


    private suspend fun createImageSticker(element: TemplateElement.ImageElement): Sticker {
        // Log thông tin
        Log.d(TAG, "Creating image sticker: ${element.id}")

        // Cố gắng tải ảnh chất lượng cao từ đường dẫn nếu có
        var finalBitmap = element.bitmap
        if (element.localImagePath != null) {
            try {
                val loadedBitmap = BitmapFactory.decodeFile(element.localImagePath)
                if (loadedBitmap != null) {
                    finalBitmap = loadedBitmap
                    Log.d(TAG, "Successfully loaded image from path: ${element.localImagePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image from path: ${e.message}", e)
            }
        }

        Log.d(TAG, "Image has bitmap: ${finalBitmap != null}")

        val drawable = finalBitmap?.let {
            BitmapDrawable(context.resources, it)
        } ?: ColorDrawable(element.placeholderColor)

        val finalDrawable = when (element.maskShape) {
            MaskShape.NONE -> drawable
            MaskShape.OVAL -> applyOvalMask(drawable)
            MaskShape.RECTANGLE, MaskShape.ROUNDED_RECTANGLE ->
                applyRoundedRectangleMask(drawable, element.cornerRadius)
            MaskShape.CUSTOM_PATH -> drawable // Chưa hỗ trợ
        }

        // Tạo sticker thích hợp dựa vào tính chất editable
        return if (!element.isEditable) {
            LockableDrawableSticker(finalDrawable).apply {
                isLocked = true
            }
        } else {
            DrawableSticker(finalDrawable)
        }
    }


    private suspend fun createSvgSticker(element: TemplateElement.SvgElement): DrawableSticker =
        withContext(Dispatchers.Default) {
            try {
                // Parse SVG từ assetPath hoặc svgContent
                val svg = if (element.assetPath != null) {
                    // Ưu tiên sử dụng loadSvgContent nếu có assetPath
                    val svgContent = element.loadSvgContent(context)
                    if (svgContent != null) {
                        SVG.getFromString(svgContent)
                    } else {
                        // Fallback: Đọc trực tiếp từ assets
                        val inputStream = context.assets.open(element.assetPath)
                        SVG.getFromInputStream(inputStream)
                    }
                } else if (element.svgContent.isNotEmpty()) {
                    // Dùng svgContent nếu đã có sẵn (cho các SVG nhỏ)
                    SVG.getFromString(element.svgContent)
                } else {
                    throw IllegalArgumentException("No SVG content or asset path provided")
                }

                // Lấy kích thước của element bounds
                val width = element.bounds.width().toInt()
                val height = element.bounds.height().toInt()

                // Render SVG thành bitmap
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Thiết lập viewport SVG
                svg.documentWidth = width.toFloat()
                svg.documentHeight = height.toFloat()

                // Render SVG
                svg.renderToCanvas(canvas)

                // Tạo drawable từ bitmap
                val drawable = BitmapDrawable(context.resources, bitmap)

                // Tạo sticker từ drawable
                DrawableSticker(drawable)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating SVG sticker: ${e.message}")
                // Trả về một drawable trống nếu bị lỗi
                val drawable = ColorDrawable(android.graphics.Color.TRANSPARENT)
                DrawableSticker(drawable)
            }
        }


    private suspend fun createVectorSticker(element: TemplateElement.VectorElement): DrawableSticker =
        withContext(Dispatchers.Default) {
            try {
                // Tải vector drawable từ resource
                val vectorDrawable = context.getDrawable(element.vectorDrawableResId)
                    ?: throw IllegalArgumentException("Invalid vector resource ID: ${element.vectorDrawableResId}")

                // Áp dụng tint color nếu được chỉ định
                element.tintColor?.let {
                    vectorDrawable.setTint(it)
                }

                // Tạo sticker từ drawable
                DrawableSticker(vectorDrawable)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating vector sticker: ${e.message}")
                val drawable = ColorDrawable(android.graphics.Color.TRANSPARENT)
                DrawableSticker(drawable)
            }
        }


    private suspend fun createGroupSticker(element: TemplateElement.GroupElement): Sticker? =
        withContext(Dispatchers.Default) {
            if (element.elements.isEmpty()) {
                return@withContext null
            }

            try {
                // Hiện tại chỉ hỗ trợ element đầu tiên trong nhóm
                createStickerFromElement(element.elements.first())
            } catch (e: Exception) {
                Log.e(TAG, "Error creating sticker from group", e)
                null
            }
        }


    private fun applyTransformMatrix(sticker: Sticker, element: TemplateElement) {
        try {
            val matrix = Matrix()

            // 1. Dịch chuyển đến vị trí
            matrix.postTranslate(element.bounds.left, element.bounds.top)

            // 2. Tính toán điểm trục để xoay và scale
            val pivotX = element.bounds.width() * element.pivotX
            val pivotY = element.bounds.height() * element.pivotY
            val absolutePivotX = element.bounds.left + pivotX
            val absolutePivotY = element.bounds.top + pivotY

            // 3. Áp dụng scale nếu có
            if (element.scaleX != 1f || element.scaleY != 1f) {
                matrix.postScale(element.scaleX, element.scaleY, absolutePivotX, absolutePivotY)
            }

            // 4. Áp dụng xoay nếu có
            if (element.rotation != 0f) {
                matrix.postRotate(element.rotation, absolutePivotX, absolutePivotY)
            }

            // Áp dụng ma trận
            sticker.setMatrix(matrix)

            // Debug ma trận
            val values = FloatArray(9)
            matrix.getValues(values)
            Log.d(TAG, "Matrix applied: translate(${values[Matrix.MTRANS_X]},${values[Matrix.MTRANS_Y]}), " +
                    "scale(${values[Matrix.MSCALE_X]},${values[Matrix.MSCALE_Y]})")

        } catch (e: Exception) {
            Log.e(TAG, "Error applying matrix to sticker: ${e.message}")
        }
    }

    private fun applyOvalMask(drawable: Drawable): Drawable {
        // Giữ stub đơn giản
        return drawable
    }

    private fun applyRoundedRectangleMask(drawable: Drawable, cornerRadius: Float): Drawable {
        // Giữ stub đơn giản
        return drawable
    }
}