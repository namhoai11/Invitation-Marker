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
}