package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.text.Layout
import android.util.Log
import com.caverock.androidsvg.SVG
import com.example.invitationcard.model.TemplateElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

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
    private val pathTextExtractor = SvgPathTextExtractor(context)

    suspend fun loadSvgTemplateFromAssets(fileName: String): Triple<List<TemplateElement>, Bitmap?, Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            val elements = mutableListOf<TemplateElement>()
            var background: Bitmap? = null
            var dimensions = Pair(0, 0)

            try {
                val svgContent = getSvgContentFromAssets(fileName)
                val extractor = SvgBaseImageExtractor(context)
                val processedSvg = svgContent?.let { extractor.extractImagesToFiles(it) } ?: ""
                val inputStream = File(context.cacheDir, "temp_${System.currentTimeMillis()}.svg").apply {
                    writeText(processedSvg)
                }.inputStream()

                val svg = SVG.getFromInputStream(inputStream)
                dimensions = Pair(svg.documentWidth.toInt(), svg.documentHeight.toInt())

                // Thử render SVG
                background = renderSvgToBackground(svg)

                // Nếu SVG render thất bại, dùng hình ảnh Base64 trực tiếp
                if (background == null || background.getPixel(0, 0) == Color.TRANSPARENT) {
                    Log.w(TAG, "SVG render failed, falling back to Base64 image")
                    val imagePath = extractor.getLastExtractedImagePath()
                    background = imagePath?.let { BitmapFactory.decodeFile(it) }
                }

                background?.let { bitmap ->
                    elements.add(TemplateElement.ImageElement(
                        id = "background",
                        zIndex = 0,
                        bounds = RectF(0f, 0f, svg.documentWidth, svg.documentHeight),
                        isEditable = false,
                        isVisible = true,
                        bitmap = bitmap,
                        isUserReplaceable = false,
                        localImagePath = extractor.getLastExtractedImagePath()
                    ))
                }

                inputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading SVG template: ${e.message}", e)
            }

            Triple(elements, background, dimensions)
        }

    /**
     * Kiểm tra xem SVG có thể tách thành các thành phần riêng biệt hay không
     */
    private fun canExtractSvgComponents(svgContent: String, containsBase64: Boolean): Boolean {
        // Nếu SVG chứa Base64 quá lớn, không nên tách
        if (containsBase64 && svgContent.length > 100000) {
            Log.d(TAG, "SVG with large Base64 content - not suitable for component extraction")
            return false
        }

        // SVG có phần tử text rõ ràng
        val hasText = svgContent.contains("<text")

        // SVG có đủ path có thể trích xuất thành text
        val pathCount = "<path".toRegex().findAll(svgContent).count()
        val potentialTextPaths = pathCount > 2 && pathCount < 100

        // SVG có nhóm (group) riêng biệt
        val hasGroups = "<g ".toRegex().findAll(svgContent).count() > 1

        // Kết luận
        return hasText || potentialTextPaths || hasGroups
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
            svg.renderToCanvas(canvas)

            // Kiểm tra bitmap có dữ liệu không
            if (bitmap.getPixel(0, 0) == Color.TRANSPARENT) {
                Log.w(TAG, "Rendered bitmap is transparent at (0,0) - possible rendering issue")
            } else {
                Log.d(TAG, "SVG rendered successfully to bitmap")
            }
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