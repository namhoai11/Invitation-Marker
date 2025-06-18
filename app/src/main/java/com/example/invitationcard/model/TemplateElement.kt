package com.example.invitationcard.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.text.Layout
import android.util.Log

sealed class TemplateElement {
    // Thuộc tính chung giữ nguyên
    abstract val id: String
    abstract val zIndex: Int
    abstract val bounds: RectF
    abstract val isEditable: Boolean
    abstract val isVisible: Boolean


    abstract val rotation: Float    // Góc xoay (degrees)
    abstract val scaleX: Float      // Tỷ lệ theo chiều ngang
    abstract val scaleY: Float      // Tỷ lệ theo chiều dọc
    abstract val pivotX: Float      // Điểm neo X (0.5f = giữa)
    abstract val pivotY: Float      // Điểm neo Y (0.5f = giữa)

    // THÊM: Phương thức tạo ma trận biến đổi
    fun createMatrix(): Matrix {
        val matrix = Matrix()

        // LƯU Ý: Đảm bảo tính toán pivot point đúng
        val centerX = bounds.left + bounds.width() * pivotX
        val centerY = bounds.top + bounds.height() * pivotY

        // THÊM: Dịch chuyển đến vị trí ban đầu (bounds.left, bounds.top)
        matrix.postTranslate(bounds.left, bounds.top)

        // Scale quanh điểm neo
        if (scaleX != 1f || scaleY != 1f) {
            // Dịch chuyển về gốc tọa độ
            matrix.postTranslate(-centerX, -centerY)
            // Áp dụng tỷ lệ
            matrix.postScale(scaleX, scaleY)
            // Dịch chuyển về vị trí cũ
            matrix.postTranslate(centerX, centerY)
        }

        // Xoay quanh điểm neo
        if (rotation != 0f) {
            matrix.postRotate(rotation, centerX, centerY)
        }

        return matrix
    }

    // Text element
    data class TextElement(
        override val id: String,
        override val zIndex: Int,
        override val bounds: RectF,
        override val isEditable: Boolean = true,
        override val isVisible: Boolean = true,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val pivotX: Float = 0.5f,
        override val pivotY: Float = 0.5f,
        val text: String,
        val fontName: String? = null,
        val fontSize: Float = 14f,
        val color: Int,
        val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val lineHeight: Int = 120,
        val letterSpacing: Float = 0f,
        val uppercase: Boolean = false,
        val curveAngle: Float = 0f,
        val shadow: Shadow? = null,
        val stroke: Stroke? = null,
        val fixedPosition: Boolean = false
    ) : TemplateElement()

    // Image element
    data class ImageElement(
        override val id: String,
        override val zIndex: Int,
        override val bounds: RectF,
        override val isEditable: Boolean = true,
        override val isVisible: Boolean = true,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val pivotX: Float = 0.5f,
        override val pivotY: Float = 0.5f,
        val bitmap: Bitmap? = null,
        val placeholderColor: Int = Color.LTGRAY,
        val isUserReplaceable: Boolean = false,
        val maskShape: MaskShape = MaskShape.NONE,
        val cornerRadius: Float = 0f,
        val localImagePath: String? = null
    ) : TemplateElement()

    // Vector element
    data class VectorElement(
        override val id: String,
        override val zIndex: Int,
        override val bounds: RectF,
        override val isEditable: Boolean = false,
        override val isVisible: Boolean = true,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val pivotX: Float = 0.5f,
        override val pivotY: Float = 0.5f,
        val vectorDrawableResId: Int,
        val tintColor: Int? = null
    ) : TemplateElement()

    // Group element
    data class GroupElement(
        override val id: String,
        override val zIndex: Int,
        override val bounds: RectF,
        override val isEditable: Boolean = true,
        override val isVisible: Boolean = true,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val pivotX: Float = 0.5f,
        override val pivotY: Float = 0.5f,
        val elements: List<TemplateElement>
    ) : TemplateElement()

    // THÊM: SVG Element để hỗ trợ SVG
    data class SvgElement(
        override val id: String,
        override val zIndex: Int,
        override val bounds: RectF,
        override val isEditable: Boolean = false,
        override val isVisible: Boolean = true,
        override val rotation: Float = 0f,
        override val scaleX: Float = 1f,
        override val scaleY: Float = 1f,
        override val pivotX: Float = 0.5f,
        override val pivotY: Float = 0.5f,
        val svgContent: String = "", // Đổi thành rỗng mặc định
        val assetPath: String? = null,
        val tintColor: Int? = null,
        val isInteractive: Boolean = false,
        val isFixedPosition: Boolean = false
    ) : TemplateElement() {

        companion object {
            private const val TAG = "SvgElement"
        }

        /**
         * Tải nội dung SVG từ asset path thay vì lưu trữ trong đối tượng
         */
        fun loadSvgContent(context: Context): String? {
            if (assetPath == null) {
                Log.w(TAG, "Cannot load SVG content: assetPath is null")
                return null
            }

            return try {
                context.assets.open(assetPath).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading SVG content from $assetPath: ${e.message}", e)
                null
            }
        }
    }
}
// Các class hỗ trợ
data class Shadow(
    val radius: Float,
    val dx: Float,
    val dy: Float,
    val color: Int
)

data class Stroke(
    val width: Float,
    val color: Int,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f
)

enum class MaskShape {
    NONE, RECTANGLE, OVAL, ROUNDED_RECTANGLE, CUSTOM_PATH
}