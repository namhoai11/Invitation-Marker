package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SvgBaseImageExtractor(private val context: Context) {
    companion object {
        private const val TAG = "SvgBaseImageExtractor"
        private const val IMAGES_DIR = "svg_images"
    }

    private var lastExtractedImagePath: String? = null

    // Thêm biến để lưu thông tin về kích thước ảnh lớn nhất
    private var largestImageSize: Long = 0

    fun getLastExtractedImagePath(): String? = lastExtractedImagePath

    // Trong SvgBaseImageExtractor
    fun extractImagesToFiles(svgContent: String): String {
        try {
            if (!svgContent.contains("data:image/")) {
                Log.d(TAG, "No Base64 images found in SVG")
                return svgContent
            }

            val imagesDir = File(context.filesDir, IMAGES_DIR)
            if (!imagesDir.exists()) imagesDir.mkdirs()

            var modifiedSvg = svgContent
            val base64Pattern = "xlink:href=\"(data:image/[^;]+;base64,[^\"]+)\"".toRegex()
            val matches = base64Pattern.findAll(svgContent)

            for (match in matches) {
                val fullBase64Url = match.groupValues[1]
                val base64Data = fullBase64Url.substringAfter("base64,")
                val mimeType = fullBase64Url.substringAfter("data:").substringBefore(";")
                val fileExtension = when (mimeType) {
                    "image/png" -> ".png"
                    "image/jpeg" -> ".jpg"
                    else -> ".bin"
                }

                val fileName = "img_${UUID.randomUUID()}$fileExtension"
                val imageFile = File(imagesDir, fileName)
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

                FileOutputStream(imageFile).use { it.write(imageBytes) }

                // Kiểm tra file sau khi lưu
                if (imageFile.exists() && imageFile.canRead()) {
                    Log.d(TAG, "Image saved successfully: ${imageFile.absolutePath}")
                    val fileUri = Uri.fromFile(imageFile).toString()
                    modifiedSvg = modifiedSvg.replace(fullBase64Url, fileUri)
                    lastExtractedImagePath = imageFile.absolutePath
                } else {
                    Log.e(TAG, "Failed to save or read image file: ${imageFile.absolutePath}")
                }
            }
            return modifiedSvg
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Base64 images: ${e.message}", e)
            return svgContent
        }
    }

    /**
     * Tải hình ảnh từ đường dẫn
     */
    fun loadImageFromPath(path: String?): Bitmap? {
        if (path == null) return null

        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                Log.e(TAG, "Image file does not exist: $path")
                return null
            }

            return BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image from path: $path", e)
            return null
        }
    }
}