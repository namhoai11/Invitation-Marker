package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.invitationcard.model.TemplateElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parser cho file PSD
 * Chỉ trích xuất hình nền mà không tạo các phần tử mẫu
 */
class PsdParser(private val context: Context) {

    /**
     * Phân tích file PSD từ assets
     * @param fileName Tên của file PSD trong thư mục assets
     * @return Triple(List<TemplateElement>, Bitmap? background, Pair<Int, Int> dimensions)
     */
    suspend fun parsePsdFromAssets(fileName: String): Triple<List<TemplateElement>, Bitmap?, Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            val elements = mutableListOf<TemplateElement>()
            var background: Bitmap? = null
            var dimensions = Pair(0, 0)

            try {
                Log.d("PsdParser", "Đang mở file PSD: $fileName")
                val inputStream = context.assets.open(fileName)

                // Đọc file PSD dưới dạng bitmap tổng hợp
                // Lưu ý: Cách này chỉ lấy được composite image (hình tổng hợp)
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                background = BitmapFactory.decodeStream(inputStream, null, options)

                // Lấy kích thước document
                background?.let {
                    dimensions = Pair(it.width, it.height)
                    Log.d("PsdParser", "Kích thước PSD: ${dimensions.first}x${dimensions.second}")
                }

                // Đóng input stream
                inputStream.close()

                Log.d("PsdParser", "Phân tích PSD thành công")

            } catch (e: Exception) {
                Log.e("PsdParser", "Lỗi khi phân tích file PSD: ${e.message}", e)
                // Khi có lỗi, trả về danh sách elements rỗng, background null, và kích thước 0
            }

            // Trả về kết quả với danh sách elements rỗng (không có phần tử mẫu)
            Triple(elements, background, dimensions)
        }

    /**
     * Các triển khai trong tương lai có thể bổ sung:
     * - Trích xuất layers từ file PSD
     * - Nhận diện các phần tử văn bản và hình ảnh
     * - Phân tích thuộc tính của layer
     */
}