package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log
import com.caverock.androidsvg.SVG
import com.example.invitationcard.model.TemplateElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lớp tải và xử lý template SVG
 */
class SvgTemplateLoader(
    private val context: Context,
    private val templateRenderer: TemplateRenderer,
    private val fontManager: FontManager
) {
    companion object {
        private const val TAG = "SvgTemplateLoader"
    }

    private val textRecognitionHelper = TextRecognitionHelper()

    suspend fun loadSvgTemplateFromAssets(fileName: String): Triple<List<TemplateElement>, Bitmap?, Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            val elements = mutableListOf<TemplateElement>()
            var background: Bitmap? = null
            var dimensions = Pair(0, 0)

            try {
                Log.d(TAG, "Loading SVG template: $fileName")

                // Đọc nội dung SVG
                val svgContent = getSvgContentFromAssets(fileName)

                // Log thông tin SVG để debug
                if (svgContent != null) {
                    Log.d(TAG, "SVG content preview (first 500 chars): ${svgContent.take(500)}...")
                    Log.d(TAG, "SVG contains <text> elements: ${svgContent.contains("<text")}")
                    Log.d(TAG, "SVG contains <image> elements: ${svgContent.contains("<image")}")
                    Log.d(TAG, "SVG contains <g> elements: ${svgContent.contains("<g ")}")
                    Log.d(TAG, "SVG contains <path> elements: ${svgContent.contains("<path")}")
                }

                // Tải SVG để render
                val inputStream = context.assets.open(fileName)
                val svg = SVG.getFromInputStream(inputStream)

                // Lấy kích thước SVG
                val width = svg.documentWidth.toInt()
                val height = svg.documentHeight.toInt()
                dimensions = Pair(width, height)
                Log.d(TAG, "SVG dimensions: ${width}x${height}")

                // Tạo background bitmap từ SVG
                background = renderSvgToBackground(svg)

                // Thêm background SVG như một phần tử không chỉnh sửa được
                elements.add(TemplateElement.SvgElement(
                    id = "background",
                    zIndex = 0,
                    bounds = RectF(0f, 0f, svg.documentWidth, svg.documentHeight),
                    isEditable = false,
                    isVisible = true,
                    svgContent = svgContent ?: "",
                    assetPath = fileName,
                    isInteractive = false
                ))

                // Phát hiện text trong bitmap bằng ML Kit OCR
                background?.let { bitmap ->
                    try {
                        val textElements = textRecognitionHelper.detectText(bitmap)
                        Log.d(TAG, "OCR detected ${textElements.size} text elements")
                        elements.addAll(textElements)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during text recognition", e)
                    }
                }

                // Đóng input stream
                inputStream.close()

                Log.d(TAG, "Parsed SVG successfully: ${elements.size} elements found")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SVG template: ${e.message}", e)
                // Khi có lỗi, trả về danh sách elements rỗng, background null
            }

            Triple(elements, background, dimensions)
        }

    /**
     * Đọc nội dung file SVG từ assets
     */
    fun getSvgContentFromAssets(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SVG file: $fileName", e)
            null
        }
    }

    /**
     * Render toàn bộ SVG thành bitmap background
     */
    private fun renderSvgToBackground(svg: SVG): Bitmap? {
        try {
            val width = svg.documentWidth.toInt()
            val height = svg.documentHeight.toInt()

            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Invalid SVG dimensions: ${width}x${height}")
                return null
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Render SVG vào bitmap
            svg.renderToCanvas(canvas)

            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering SVG to background", e)
            return null
        }
    }

    /**
     * Giải phóng tài nguyên
     */
    fun destroy() {
        textRecognitionHelper.close()
    }
}