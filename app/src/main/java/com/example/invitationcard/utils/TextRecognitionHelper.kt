package com.example.invitationcard.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.text.Layout
import android.util.Log
import com.example.invitationcard.model.TemplateElement
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TextRecognitionHelper {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val TAG = "TextRecognitionHelper"

        // Danh sách các từ khóa phổ biến trong thiệp mời để gợi ý font
        private val CURSIVE_KEYWORDS = setOf("congratulations", "wedding", "shower", "welcome", "baby", "invitation")
        private val BOLD_KEYWORDS = setOf("rsvp", "date", "time", "when", "where", "location")
    }

    //Phát hiện text trong bitmap và trả về danh sách TextElement
    suspend fun detectText(bitmap: Bitmap): List<TemplateElement.TextElement> = suspendCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val textElements = mutableListOf<TemplateElement.TextElement>()
                    var zIndex = 10

                    // Phân tích từng text block được phát hiện
                    for (block in visionText.textBlocks) {
                        // Xử lý từng dòng
                        for (line in block.lines) {
                            val boundingBox = line.boundingBox
                            if (boundingBox != null) {
                                val bounds = RectF(
                                    boundingBox.left.toFloat(),
                                    boundingBox.top.toFloat(),
                                    boundingBox.right.toFloat(),
                                    boundingBox.bottom.toFloat()
                                )

                                // Phân tích text để xác định thuộc tính
                                val text = line.text
                                val fontSize = bounds.height() * 0.7f

                                // Xác định font dựa trên nội dung text
                                val fontName = suggestFontName(text)

                                // Xác định bold/italic dựa trên nội dung
                                val isBold = shouldBeBold(text)
                                val isItalic = shouldBeItalic(text)

                                // Xác định alignment dựa trên vị trí
                                val alignment = determineAlignment(bounds, bitmap.width)

                                // Xác định màu (mặc định là đen)
                                val color = suggestColor(text, bounds, bitmap)

                                textElements.add(TemplateElement.TextElement(
                                    id = "ocr_text_${zIndex}",
                                    zIndex = zIndex++,
                                    bounds = bounds,
                                    isEditable = true,
                                    isVisible = true,
                                    rotation = 0f,
                                    scaleX = 1f,
                                    scaleY = 1f,
                                    pivotX = 0.5f,
                                    pivotY = 0.5f,
                                    text = text,
                                    fontName = fontName,
                                    fontSize = fontSize,
                                    color = color,
                                    alignment = alignment,
                                    bold = isBold,
                                    italic = isItalic,
                                    uppercase = text.all { it.isUpperCase() || !it.isLetter() }
                                ))

                                Log.d(TAG, "OCR detected: '$text' at $bounds with font $fontName, size $fontSize")
                            }
                        }
                    }

                    // Tinh chỉnh các bounding box nếu chồng lấp
                    val optimizedElements = optimizeTextElements(textElements)

                    // Resume coroutine với kết quả
                    continuation.resume(optimizedElements)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                    continuation.resume(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in text recognition", e)
            continuation.resumeWithException(e)
        }
    }


    private fun suggestFontName(text: String): String {
        val lowerText = text.lowercase()

        // Kiểm tra các từ khóa để gợi ý font kiểu cursive/script
        if (CURSIVE_KEYWORDS.any { keyword -> lowerText.contains(keyword) }) {
            return "cursive"
        }

        // Một số từ ngắn thường là tiêu đề, dùng font serif
        if (text.length < 12 && text.all { it.isLetterOrDigit() || it.isWhitespace() }) {
            return "serif"
        }

        // Mặc định là sans-serif
        return "sans-serif"
    }


    private fun shouldBeBold(text: String): Boolean {
        val lowerText = text.lowercase()

        // Kiểm tra từ khóa thường được in đậm
        if (BOLD_KEYWORDS.any { keyword -> lowerText.contains(keyword) }) {
            return true
        }

        // Text ngắn và viết hoa thường là tiêu đề được in đậm
        if (text.length < 10 && text == text.uppercase() && text.any { it.isLetter() }) {
            return true
        }

        return false
    }


    private fun shouldBeItalic(text: String): Boolean {
        // Tên người, ngày tháng đặc biệt thường được in nghiêng
        val lowerText = text.lowercase()

        if (lowerText.contains("at") && text.length < 15) {
            return true
        }

        return false
    }


    private fun determineAlignment(bounds: RectF, bitmapWidth: Int): Layout.Alignment {
        val centerX = bounds.centerX()
        val relativePos = centerX / bitmapWidth

        return when {
            relativePos < 0.4 -> Layout.Alignment.ALIGN_NORMAL // Left
            relativePos > 0.6 -> Layout.Alignment.ALIGN_OPPOSITE // Right
            else -> Layout.Alignment.ALIGN_CENTER // Center
        }
    }


    private fun suggestColor(text: String, bounds: RectF, bitmap: Bitmap): Int {
        // Đây là ví dụ đơn giản, bạn có thể phát triển phức tạp hơn
        val lowerText = text.lowercase()

        // Các từ liên quan đến baby shower thường màu hồng
        if (lowerText.contains("baby") || lowerText.contains("shower")) {
            return Color.parseColor("#FF66AA")
        }

        // Các từ liên quan đến đám cưới thường màu xanh nhạt hoặc vàng gold
        if (lowerText.contains("wedding") || lowerText.contains("invite")) {
            return Color.parseColor("#7B68EE")
        }

        // Thông tin ngày giờ thường màu nâu
        if (lowerText.contains("date") || lowerText.matches("\\d+\\s*[a-z]+.*".toRegex())) {
            return Color.parseColor("#8B4513")
        }

        // Mặc định là đen
        return Color.BLACK
    }


    private fun optimizeTextElements(elements: List<TemplateElement.TextElement>): List<TemplateElement.TextElement> {
        // Sắp xếp các element theo y để nhóm các dòng gần nhau
        val sortedByY = elements.sortedBy { it.bounds.top }
        val result = mutableListOf<TemplateElement.TextElement>()

        var currentGroup = mutableListOf<TemplateElement.TextElement>()
        var lastBottom = 0f

        // Nhóm các text gần nhau theo chiều dọc
        for (element in sortedByY) {
            if (currentGroup.isEmpty() || element.bounds.top - lastBottom < element.fontSize * 0.5f) {
                // Thêm vào nhóm hiện tại
                currentGroup.add(element)
                lastBottom = maxOf(lastBottom, element.bounds.bottom)
            } else {
                // Xử lý nhóm hiện tại trước khi bắt đầu nhóm mới
                if (currentGroup.size > 1) {
                    // Nếu có nhiều element gần nhau, xem xét gộp lại
                    val mergedElement = mergeTextElements(currentGroup)
                    result.add(mergedElement)
                } else {
                    // Nếu chỉ có 1 element, thêm vào kết quả
                    result.add(currentGroup[0])
                }

                // Bắt đầu nhóm mới
                currentGroup = mutableListOf(element)
                lastBottom = element.bounds.bottom
            }
        }

        // Xử lý nhóm cuối cùng
        if (currentGroup.isNotEmpty()) {
            if (currentGroup.size > 1) {
                result.add(mergeTextElements(currentGroup))
            } else {
                result.add(currentGroup[0])
            }
        }

        return result
    }


    private fun mergeTextElements(elements: List<TemplateElement.TextElement>): TemplateElement.TextElement {
        // Sắp xếp theo x để đảm bảo thứ tự đọc đúng
        val sortedByX = elements.sortedBy { it.bounds.left }

        // Tính toán bounds mới bao quanh tất cả elements
        val left = sortedByX.minOf { it.bounds.left }
        val top = sortedByX.minOf { it.bounds.top }
        val right = sortedByX.maxOf { it.bounds.right }
        val bottom = sortedByX.maxOf { it.bounds.bottom }

        // Gộp text
        val mergedText = sortedByX.joinToString(" ") { it.text }

        // Sử dụng thuộc tính từ element lớn nhất
        val largestElement = sortedByX.maxByOrNull { it.bounds.width() * it.bounds.height() } ?: sortedByX.first()

        return TemplateElement.TextElement(
            id = "merged_${largestElement.id}",
            zIndex = largestElement.zIndex,
            bounds = RectF(left, top, right, bottom),
            isEditable = true,
            isVisible = true,
            rotation = 0f,
            scaleX = 1f,
            scaleY = 1f,
            pivotX = 0.5f,
            pivotY = 0.5f,
            text = mergedText,
            fontName = largestElement.fontName,
            fontSize = largestElement.fontSize,
            color = largestElement.color,
            alignment = largestElement.alignment,
            bold = largestElement.bold,
            italic = largestElement.italic,
            uppercase = mergedText.all { it.isUpperCase() || !it.isLetter() },
            lineHeight = largestElement.lineHeight,
            letterSpacing = largestElement.letterSpacing
        )
    }


    fun close() {
        textRecognizer.close()
    }
}