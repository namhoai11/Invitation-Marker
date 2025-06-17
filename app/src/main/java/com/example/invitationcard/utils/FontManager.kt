package com.example.invitationcard.utils

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.example.invitationcard.model.FontItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class FontManager(private val context: Context) {

    private val httpClient = OkHttpClient()
    private val fontCache = mutableMapOf<String, Typeface>()

    companion object {
        private const val TAG = "FontManager"
        private const val GOOGLE_FONTS_API = "https://fonts.googleapis.com/css2?family="
    }

    suspend fun loadFont(fontItem: FontItem): Typeface? {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                fontCache[fontItem.name]?.let { return@withContext it }

                // System fonts
                if (fontItem.isSystemFont) {
                    val typeface = when (fontItem.name) {
                        "Default" -> Typeface.DEFAULT
                        "Sans Serif" -> Typeface.SANS_SERIF
                        "Serif" -> Typeface.SERIF
                        "Monospace" -> Typeface.MONOSPACE
                        else -> Typeface.DEFAULT
                    }
                    fontCache[fontItem.name] = typeface
                    return@withContext typeface
                }

                // Google Fonts
                fontItem.googleFontName?.let { googleName ->
                    val typeface = downloadGoogleFont(googleName)
                    typeface?.let {
                        fontCache[fontItem.name] = it
                        fontItem.typeface = it
                    }
                    return@withContext typeface
                }

                null
            } catch (e: Exception) {
                Log.e(TAG, "Error loading font: ${fontItem.name}", e)
                null
            }
        }
    }

    private suspend fun downloadGoogleFont(fontFamily: String): Typeface? {
        return try {
            val fontDir = File(context.cacheDir, "fonts")
            if (!fontDir.exists()) {
                fontDir.mkdirs()
            }

            val fontFile = File(fontDir, "$fontFamily.ttf")

            // Check if font already exists
            if (fontFile.exists()) {
                return Typeface.createFromFile(fontFile)
            }

            // Download font
            val url = "https://fonts.gstatic.com/s/a/v1/fonts/$fontFamily/regular.ttf"
            val request = Request.Builder().url(url).build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.let { body ->
                    FileOutputStream(fontFile).use { output ->
                        body.byteStream().copyTo(output)
                    }
                    Typeface.createFromFile(fontFile)
                }
            } else {
                Log.e(TAG, "Failed to download font: $fontFamily")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading font: $fontFamily", e)
            null
        }
    }

    fun getPopularFonts(): List<FontItem> {
        return listOf(
            // System fonts
            FontItem("Default", "default", "System", isSystemFont = true),
            FontItem("Sans Serif", "sans-serif", "System", isSystemFont = true),
            FontItem("Serif", "serif", "System", isSystemFont = true),
            FontItem("Monospace", "monospace", "System", isSystemFont = true),

            // Popular Google Fonts
            FontItem("Roboto", "roboto", "Sans Serif", googleFontName = "roboto"),
            FontItem("Open Sans", "opensans", "Sans Serif", googleFontName = "opensans"),
            FontItem("Lato", "lato", "Sans Serif", googleFontName = "lato"),
            FontItem("Montserrat", "montserrat", "Sans Serif", googleFontName = "montserrat"),
            FontItem("Oswald", "oswald", "Sans Serif", googleFontName = "oswald"),
            FontItem("Source Sans Pro", "sourcesanspro", "Sans Serif", googleFontName = "sourcesanspro"),
            FontItem("Raleway", "raleway", "Sans Serif", googleFontName = "raleway"),
            FontItem("PT Sans", "ptsans", "Sans Serif", googleFontName = "ptsans"),
            FontItem("Lora", "lora", "Serif", googleFontName = "lora"),
            FontItem("Playfair Display", "playfairdisplay", "Serif", googleFontName = "playfairdisplay"),
            FontItem("Merriweather", "merriweather", "Serif", googleFontName = "merriweather"),
            FontItem("PT Serif", "ptserif", "Serif", googleFontName = "ptserif"),
            FontItem("Dancing Script", "dancingscript", "Handwriting", googleFontName = "dancingscript"),
            FontItem("Pacifico", "pacifico", "Handwriting", googleFontName = "pacifico"),
            FontItem("Great Vibes", "greatvibes", "Handwriting", googleFontName = "greatvibes"),
            FontItem("Amatic SC", "amaticsc", "Handwriting", googleFontName = "amaticsc"),
            FontItem("Lobster", "lobster", "Display", googleFontName = "lobster"),
            FontItem("Bebas Neue", "bebasneue", "Display", googleFontName = "bebasneue"),
            FontItem("Anton", "anton", "Display", googleFontName = "anton"),
            FontItem("Righteous", "righteous", "Display", googleFontName = "righteous")
        )
    }

    /**
     * Tìm font theo tên, hỗ trợ tìm kiếm không phân biệt chữ hoa/thường và tìm gần đúng
     */
    suspend fun findFontByName(fontName: String): FontItem? {
        val allFonts = getPopularFonts()

        // Xử lý tên font để tăng khả năng khớp
        val processedName = fontName.lowercase()
            .replace("-", " ")
            .replace("_", " ")
            .trim()

        // Thử tìm khớp chính xác (không phân biệt chữ hoa/thường)
        allFonts.find { it.name.lowercase() == processedName }?.let {
            // Đảm bảo font đã được load
            if (it.typeface == null && !it.isSystemFont) {
                loadFont(it)
            }
            return it
        }

        // Tìm font có chứa tên gần đúng
        val fontsByName = allFonts.filter {
            it.name.lowercase().contains(processedName) ||
                    processedName.contains(it.name.lowercase())
        }

        if (fontsByName.isNotEmpty()) {
            val bestMatch = fontsByName.first()
            // Đảm bảo font đã được load
            if (bestMatch.typeface == null && !bestMatch.isSystemFont) {
                loadFont(bestMatch)
            }
            return bestMatch
        }

        // Thử tìm kiếm theo từng phần của tên
        val nameParts = processedName.split(" ")
        for (part in nameParts) {
            if (part.length < 3) continue // Bỏ qua từ quá ngắn

            allFonts.find { it.name.lowercase().contains(part) }?.let {
                // Đảm bảo font đã được load
                if (it.typeface == null && !it.isSystemFont) {
                    loadFont(it)
                }
                return it
            }
        }

        // Nếu không tìm thấy, trả về font mặc định
        val defaultFont = allFonts.first()
        if (defaultFont.typeface == null && !defaultFont.isSystemFont) {
            loadFont(defaultFont)
        }
        return defaultFont
    }

    // Thêm vào FontManager
    fun checkFontsAvailability() {
        val fonts = listOf("sans-serif", "sans-serif-bold", "serif", "monospace", "cursive")
        for (fontName in fonts) {
            val typeface = Typeface.create(fontName, Typeface.NORMAL)
            Log.d("FontManager", "System font '$fontName' available: ${typeface != null}")
        }

        // Kiểm tra font từ assets (nếu có)
        try {
            val assetManager = context.assets
            val fontFiles = assetManager.list("fonts") ?: emptyArray()
            Log.d("FontManager", "Font files in assets/fonts/: ${fontFiles.joinToString(", ")}")
        } catch (e: Exception) {
            Log.e("FontManager", "Error checking fonts in assets", e)
        }
    }

}