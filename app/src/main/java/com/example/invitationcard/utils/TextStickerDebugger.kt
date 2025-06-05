package com.example.invitationcard.utils

import android.util.Log
import com.xiaopo.flying.sticker.TextSticker
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object TextStickerDebugger {

    private const val TAG = "TextStickerDebug"

    fun debugTextSticker(textSticker: TextSticker) {
        Log.d(TAG, "==========================================")
        Log.d(TAG, "=== TextSticker Debug Info ===")
        Log.d(TAG, "==========================================")

        // Debug class info
        Log.d(TAG, "Class: ${textSticker.javaClass.name}")
        Log.d(TAG, "Superclass: ${textSticker.javaClass.superclass?.name}")

        // Debug all methods
        val methods = textSticker.javaClass.declaredMethods
        Log.d(TAG, "--- Declared Methods (${methods.size}) ---")
        for (method in methods) {
            val modifiers = Modifier.toString(method.modifiers)
            val returnType = method.returnType.simpleName
            val params = method.parameterTypes.joinToString { it.simpleName }
            Log.d(TAG, "$modifiers $returnType ${method.name}($params)")
        }

        // Debug all fields
        val fields = textSticker.javaClass.declaredFields
        Log.d(TAG, "--- Declared Fields (${fields.size}) ---")
        for (field in fields) {
            val modifiers = Modifier.toString(field.modifiers)
            Log.d(TAG, "$modifiers ${field.type.simpleName} ${field.name}")

            // Try to get field value
            if (!Modifier.isPrivate(field.modifiers)) {
                try {
                    val value = field.get(textSticker)
                    Log.d(TAG, "  Value: $value")
                } catch (e: Exception) {
                    Log.d(TAG, "  Cannot access value: ${e.message}")
                }
            }
        }

        // Find text size related methods
        Log.d(TAG, "--- Text Size Related Methods ---")
        val allMethods = textSticker.javaClass.methods
        for (method in allMethods) {
            if (method.name.contains("size", ignoreCase = true) ||
                method.name.contains("text", ignoreCase = true) ||
                method.name.contains("font", ignoreCase = true)) {

                val modifiers = Modifier.toString(method.modifiers)
                val returnType = method.returnType.simpleName
                val params = method.parameterTypes.joinToString { it.simpleName }
                Log.d(TAG, "$modifiers $returnType ${method.name}($params)")

                // Try to invoke getter methods
                if (method.parameterCount == 0 &&
                    !method.name.startsWith("set") &&
                    !Modifier.isPrivate(method.modifiers)) {
                    try {
                        val result = method.invoke(textSticker)
                        Log.d(TAG, "  Result: $result")
                    } catch (e: Exception) {
                        Log.d(TAG, "  Cannot invoke: ${e.message}")
                    }
                }
            }
        }

        // Try to access text paint
        Log.d(TAG, "--- Trying to access TextPaint ---")
        try {
            val textPaintField = findFieldRecursively(textSticker.javaClass, "textPaint")
            if (textPaintField != null) {
                textPaintField.isAccessible = true
                val textPaint = textPaintField.get(textSticker)
                Log.d(TAG, "Found textPaint: $textPaint")

                // Try to get text size from paint
                val textSizeMethod = textPaint.javaClass.getMethod("getTextSize")
                val textSize = textSizeMethod.invoke(textPaint)
                Log.d(TAG, "Text size from paint: $textSize")
            } else {
                Log.d(TAG, "TextPaint field not found")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error accessing TextPaint: ${e.message}")
        }

        Log.d(TAG, "==========================================")
        Log.d(TAG, "=== End Debug ===")
        Log.d(TAG, "==========================================")
    }

    private fun findFieldRecursively(clazz: Class<*>?, fieldName: String): java.lang.reflect.Field? {
        if (clazz == null || clazz == Any::class.java) return null

        try {
            return clazz.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            // Try in superclass
            return findFieldRecursively(clazz.superclass, fieldName)
        }
    }

    fun findTextSizeMethod(textSticker: TextSticker): Method? {
        val possibleMethods = listOf(
            "setTextSize",
            "setSize",
            "setFontSize",
            "changeTextSize",
            "updateTextSize"
        )

        for (methodName in possibleMethods) {
            try {
                // Try with float parameter
                val method = textSticker.javaClass.getMethod(methodName, Float::class.java)
                Log.d(TAG, "Found method: $methodName(Float)")
                return method
            } catch (e: Exception) {
                try {
                    // Try with int parameter
                    val method = textSticker.javaClass.getMethod(methodName, Int::class.java)
                    Log.d(TAG, "Found method: $methodName(Int)")
                    return method
                } catch (e2: Exception) {
                    // Method not found with this signature
                }
            }
        }

        return null
    }
}