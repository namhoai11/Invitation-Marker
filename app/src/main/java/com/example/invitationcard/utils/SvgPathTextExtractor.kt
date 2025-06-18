package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.util.Log
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.example.invitationcard.model.TemplateElement
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.resume


class SvgPathTextExtractor(private val context: Context) {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val TAG = "SvgPathTextExtractor"
        private const val PATH_RENDER_SCALE = 4.0f
    }


    suspend fun extractTextFromSvg(svgFileName: String): List<TemplateElement.TextElement> = withContext(Dispatchers.IO) {
        val textElements = mutableListOf<TemplateElement.TextElement>()
        var zIndex = 10

        try {
            // Đọc file SVG từ assets
            val svgContent = context.assets.open(svgFileName).bufferedReader().use { it.readText() }

            // Đọc SVG để lấy kích thước
            val svg = SVG.getFromString(svgContent)
            val svgWidth = svg.documentWidth
            val svgHeight = svg.documentHeight

            Log.d(TAG, "Parsing SVG: ${svgFileName}, size: ${svgWidth}x${svgHeight}")

            // Parse SVG dưới dạng XML để truy cập các phần tử path
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputStream = context.assets.open(svgFileName)
            val document = builder.parse(inputStream)
            inputStream.close()

            // Lấy tất cả các phần tử path
            val pathElements = document.getElementsByTagName("path")
            Log.d(TAG, "Found ${pathElements.length} path elements in SVG")

            // Xử lý từng path
            for (i in 0 until pathElements.length) {
                val pathElement = pathElements.item(i) as Element
                val pathData = pathElement.getAttribute("d")

                // Kiểm tra xem path có thuộc tính phù hợp với text không
                if (isLikelyTextPath(pathData)) {
                    // Lấy bounding box của path
                    val pathBounds = getPathBounds(pathData)

                    // Render path thành bitmap
                    val pathBitmap = renderPathToBitmap(
                        pathData,
                        pathBounds.width() * PATH_RENDER_SCALE,
                        pathBounds.height() * PATH_RENDER_SCALE
                    )

                    // Nhận dạng text từ bitmap
                    val recognizedText = recognizeTextFromBitmap(pathBitmap)

                    if (recognizedText.isNotEmpty()) {
                        Log.d(TAG, "Path ${i} recognized as text: '${recognizedText}'")

                        // Tạo text element
                        val fontSize = pathBounds.height() * 0.8f
                        textElements.add(TemplateElement.TextElement(
                            id = "path_text_${zIndex}",
                            zIndex = zIndex++,
                            bounds = pathBounds,
                            isEditable = true,
                            isVisible = true,
                            rotation = 0f,
                            scaleX = 1f,
                            scaleY = 1f,
                            pivotX = 0.5f,
                            pivotY = 0.5f,
                            text = recognizedText,
                            fontName = "sans-serif",
                            fontSize = fontSize,
                            color = parsePathColor(pathElement),
                            alignment = Layout.Alignment.ALIGN_NORMAL,
                            bold = false,
                            italic = false
                        ))
                    }
                }
            }

            Log.d(TAG, "Extracted ${textElements.size} text elements from paths")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from SVG: ${e.message}", e)
        }

        textElements
    }

    /**
     * Kiểm tra xem một path có khả năng là text không
     * Dựa trên các đặc điểm thường thấy ở text path
     */
    private fun isLikelyTextPath(pathData: String): Boolean {
        // Các heuristic đơn giản để xác định khả năng path là text

        // Path quá dài thường không phải text
        if (pathData.length > 1000) return false

        // Text thường có nhiều lệnh M (move to) - mỗi ký tự thường bắt đầu bằng M
        val moveToCommands = pathData.split('M').size - 1
        if (moveToCommands < 2) return false

        // Text thường có tỷ lệ lệnh đường cong trên tổng số lệnh cao
        val curveCommands = pathData.count { it == 'C' || it == 'c' || it == 'S' || it == 's' || it == 'Q' || it == 'q' }
        val totalCommands = pathData.count { it.isLetter() }
        val curvesRatio = curveCommands.toFloat() / totalCommands
        if (curvesRatio < 0.3f) return false

        return true
    }

    /**
     * Lấy bounding box của path dựa trên dữ liệu path
     */
    private fun getPathBounds(pathData: String): RectF {
        // Phân tích path để tìm bounding box
        // Đây là triển khai đơn giản, chỉ phân tích các lệnh M (moveto)
        val bounds = RectF(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

        // Regex để tìm các lệnh M và tọa độ theo sau
        val moveToPattern = "[Mm]\\s*([\\d.-]+)\\s*,?\\s*([\\d.-]+)".toRegex()
        val matches = moveToPattern.findAll(pathData)

        for (match in matches) {
            val x = match.groupValues[1].toFloat()
            val y = match.groupValues[2].toFloat()

            bounds.left = minOf(bounds.left, x)
            bounds.top = minOf(bounds.top, y)
            bounds.right = maxOf(bounds.right, x)
            bounds.bottom = maxOf(bounds.bottom, y)
        }

        // Mở rộng bounds để đảm bảo không bỏ sót các phần của path
        bounds.left -= 5
        bounds.top -= 5
        bounds.right += 5
        bounds.bottom += 5

        return bounds
    }

    /**
     * Render path thành bitmap
     */
    private fun renderPathToBitmap(pathData: String, width: Float, height: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Đặt nền trắng
        canvas.drawColor(Color.WHITE)

        try {
            // Tạo Path từ SVG path data
            val androidPath = Path()
            SVGPathParser().parsePathString(pathData, androidPath)

            // Scale path để fit vào bitmap
            val pathBounds = RectF()
            androidPath.computeBounds(pathBounds, true)
            val scaleX = width / pathBounds.width()
            val scaleY = height / pathBounds.height()
            val scale = minOf(scaleX, scaleY)

            // Tạo ma trận biến đổi
            val translateX = (width - pathBounds.width() * scale) / 2 - pathBounds.left * scale
            val translateY = (height - pathBounds.height() * scale) / 2 - pathBounds.top * scale

            // Vẽ path
            val paint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
                strokeWidth = 2f
            }

            canvas.save()
            canvas.translate(translateX, translateY)
            canvas.scale(scale, scale)
            canvas.drawPath(androidPath, paint)
            canvas.restore()

        } catch (e: Exception) {
            Log.e(TAG, "Error rendering path to bitmap: ${e.message}", e)
        }

        return bitmap
    }

    /**
     * Nhận dạng text từ bitmap bằng ML Kit OCR
     */
    private suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed: ${e.message}")
                    continuation.resume("")
                }

        } catch (e: Exception) {
            Log.e(TAG, "Error in text recognition: ${e.message}", e)
            continuation.resume("")
        }
    }

    /**
     * Parse màu từ phần tử path
     */
    private fun parsePathColor(pathElement: Element): Int {
        // Lấy thuộc tính fill hoặc style từ path
        val fill = pathElement.getAttribute("fill")
        val style = pathElement.getAttribute("style")

        // Kiểm tra fill trước
        if (fill.isNotEmpty() && fill != "none") {
            try {
                return Color.parseColor(fill)
            } catch (e: Exception) {
                // Bỏ qua lỗi parse color
            }
        }

        // Kiểm tra style nếu không có fill
        if (style.isNotEmpty()) {
            val fillPattern = "fill:\\s*([^;]+)".toRegex()
            val match = fillPattern.find(style)

            if (match != null) {
                val fillColor = match.groupValues[1].trim()
                if (fillColor != "none") {
                    try {
                        return Color.parseColor(fillColor)
                    } catch (e: Exception) {
                        // Bỏ qua lỗi parse color
                    }
                }
            }
        }

        // Mặc định là đen
        return Color.BLACK
    }

    /**
     * Class để parse path data
     */
    private inner class SVGPathParser {
        fun parsePathString(pathData: String, path: Path) {
            // Đây là triển khai đơn giản chỉ hỗ trợ một số lệnh path cơ bản
            // Trong thực tế, cần triển khai đầy đủ theo đặc tả SVG

            var i = 0
            var currentX = 0f
            var currentY = 0f

            while (i < pathData.length) {
                val c = pathData[i]
                if (c.isLetter()) {
                    i++

                    // Bỏ qua khoảng trắng sau lệnh
                    while (i < pathData.length && pathData[i].isWhitespace()) i++

                    when (c) {
                        'M', 'm' -> {
                            // Move to
                            val coords = parseCoordinates(pathData, i, 2)
                            i = coords.second

                            if (c == 'M') {
                                path.moveTo(coords.first[0], coords.first[1])
                                currentX = coords.first[0]
                                currentY = coords.first[1]
                            } else {
                                path.rMoveTo(coords.first[0], coords.first[1])
                                currentX += coords.first[0]
                                currentY += coords.first[1]
                            }
                        }
                        'L', 'l' -> {
                            // Line to
                            val coords = parseCoordinates(pathData, i, 2)
                            i = coords.second

                            if (c == 'L') {
                                path.lineTo(coords.first[0], coords.first[1])
                                currentX = coords.first[0]
                                currentY = coords.first[1]
                            } else {
                                path.rLineTo(coords.first[0], coords.first[1])
                                currentX += coords.first[0]
                                currentY += coords.first[1]
                            }
                        }
                        // Thêm xử lý cho các lệnh khác (C, Q, A, Z, ...)
                        else -> {
                            // Tìm vị trí lệnh tiếp theo
                            while (i < pathData.length && !pathData[i].isLetter()) i++
                        }
                    }
                } else {
                    i++
                }
            }
        }

        private fun parseCoordinates(pathData: String, startIndex: Int, count: Int): Pair<FloatArray, Int> {
            val coords = FloatArray(count)
            var i = startIndex

            for (j in 0 until count) {
                // Bỏ qua khoảng trắng và dấu phẩy
                while (i < pathData.length && (pathData[i].isWhitespace() || pathData[i] == ',')) i++

                // Đọc số
                val numStart = i
                while (i < pathData.length && (pathData[i].isDigit() || pathData[i] == '.' || pathData[i] == '-' || pathData[i] == 'e' || pathData[i] == 'E')) i++

                if (numStart < i) {
                    coords[j] = pathData.substring(numStart, i).toFloat()
                }
            }

            return Pair(coords, i)
        }
    }


    fun close() {
        textRecognizer.close()
    }
}