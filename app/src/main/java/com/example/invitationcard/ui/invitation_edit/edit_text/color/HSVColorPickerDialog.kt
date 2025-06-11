package com.example.invitationcard.ui.invitation_edit.edit_text.color

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import com.example.invitationcard.R

class HSVColorPickerDialog(
    private val context: Context,
    initialColor: Int? = null,
    private val onColorSelected: (Int) -> Unit
) {

    private var hue: Float = 0f
    private var saturation: Float = 1f
    private var value: Float = 1f

    private lateinit var colorPreview: View
    private lateinit var hueThumb: ImageView
    private lateinit var saturationThumb: ImageView
    private lateinit var valueThumb: ImageView
    private lateinit var hueGradient: View
    private lateinit var saturationGradient: View
    private lateinit var valueGradient: View


    init {
        if (initialColor != null) {
            setInitialColor(initialColor)
        }
    }

    // Thêm phương thức để đặt màu từ bên ngoài
    private fun setInitialColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_hsv_color_picker, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.setCanceledOnTouchOutside(false)  // Ngăn đóng dialog khi chạm ra ngoài
        dialog.setCancelable(false)  // Ngăn đóng dialog khi nhấn nút Back

        // Thiết lập chiều rộng bằng 90% chiều rộng màn hình
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        view.post {
            // Đặt vị trí của hue thumb dựa trên hue (0-360)
            val huePosition = (hue / 360f) * hueGradient.width
            updateHueThumb(huePosition)

            // Đặt vị trí của saturation thumb dựa trên saturation (0-1)
            val saturationPosition = saturation * saturationGradient.width
            updateSaturationThumb(saturationPosition)

            // Đặt vị trí của value thumb dựa trên value (0-1)
            val valuePosition = value * valueGradient.width
            updateValueThumb(valuePosition)

            // Cập nhật gradients và preview
            updateSaturationGradient()
            updateValueGradient()
            updateColorPreview()
        }


        initViews(view)
        setupSliders(view)
        setupActionButtons(view, dialog)
        updateColorPreview()

        dialog.show()
    }

    private fun initViews(view: View) {
        colorPreview = view.findViewById(R.id.color_preview)
        hueThumb = view.findViewById(R.id.hue_thumb)
        saturationThumb = view.findViewById(R.id.saturation_thumb)
        valueThumb = view.findViewById(R.id.value_thumb)
        hueGradient = view.findViewById(R.id.hue_gradient)
        saturationGradient = view.findViewById(R.id.saturation_gradient)
        valueGradient = view.findViewById(R.id.value_gradient)

        // Thiết lập gradient hue
        setupHueGradient()

        // Thiết lập vị trí ban đầu cho các thumb
        view.post {
            // Hue slider - đỏ ban đầu (hue = 0)
            updateHueThumb(0f)

            // Saturation slider - mặc định saturation = 1 (ở cuối thanh trượt)
            updateSaturationThumb(saturationGradient.width.toFloat())

            // Value slider - mặc định value = 1 (ở cuối thanh trượt)
            updateValueThumb(valueGradient.width.toFloat())
        }
    }

//    private fun setupHueGradient() {
//        // Chỉ thiết lập gradient sau khi view đã được đo kích thước
//        hueGradient.post {
//            // Lúc này view đã được vẽ và có kích thước
//            val width = hueGradient.width.toFloat()
//            if (width > 0) {
//                val hueColors = intArrayOf(
//                    Color.RED,         // 0°
//                    Color.YELLOW,      // 60°
//                    Color.GREEN,       // 120°
//                    Color.CYAN,        // 180°
//                    Color.BLUE,        // 240°
//                    Color.MAGENTA,     // 300°
//                    Color.RED          // 360°
//                )
//
//                val linearGradient = LinearGradient(
//                    0f, 0f, width, 0f,
//                    hueColors, null, Shader.TileMode.CLAMP
//                )
//
//                val paint = Paint()
//                paint.shader = linearGradient
//
//                hueGradient.background = ShapeDrawable().apply {
//                    shape = RectShape()
//                    this.paint.set(paint)
//                }
//            }
//        }
//    }

    private fun setupHueGradient() {
        hueGradient.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                hueGradient.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val width = hueGradient.width.toFloat()
                val hueColors = intArrayOf(
                    Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                    Color.BLUE, Color.MAGENTA, Color.RED
                )

                // Sử dụng phương thức tái sử dụng
                val gradientDrawable = createGradientWithRoundedCorners(hueColors, width)
                if (gradientDrawable != null) {
                    hueGradient.background = gradientDrawable

                    // Cập nhật gradient cho saturation và value với cùng kiểu bo góc
                    updateSaturationGradient()
                    updateValueGradient()
                }
            }
        })
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSliders(view: View) {
        // Hue slider
        hueGradient.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x.coerceIn(0f, v.width.toFloat())
                    hue = (x / v.width) * 360f
                    updateHueThumb(x)
                    updateSaturationGradient()
                    updateValueGradient()
                    updateColorPreview()
                    true
                }
                else -> false
            }
        }

        // Saturation slider
        saturationGradient.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x.coerceIn(0f, v.width.toFloat())
                    saturation = x / v.width
                    updateSaturationThumb(x)
                    updateValueGradient()
                    updateColorPreview()
                    true
                }
                else -> false
            }
        }

        // Value slider
        valueGradient.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val x = event.x.coerceIn(0f, v.width.toFloat())
                    value = x / v.width
                    updateValueThumb(x)
                    updateColorPreview()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateHueThumb(x: Float) {
        // Tính toán giới hạn để thumb luôn nằm trong phạm vi gradient
        val minX = 0f
        val maxX = hueGradient.width.toFloat() - hueThumb.width

        // Giới hạn vị trí x trong khoảng [minX, maxX]
        val constrainedX = x - hueThumb.width / 2
        hueThumb.x = constrainedX.coerceIn(minX, maxX)
    }

    private fun updateSaturationThumb(x: Float) {
        val minX = 0f
        val maxX = saturationGradient.width.toFloat() - saturationThumb.width

        val constrainedX = x - saturationThumb.width / 2
        saturationThumb.x = constrainedX.coerceIn(minX, maxX)
    }

    private fun updateValueThumb(x: Float) {
        val minX = 0f
        val maxX = valueGradient.width.toFloat() - valueThumb.width

        val constrainedX = x - valueThumb.width / 2
        valueThumb.x = constrainedX.coerceIn(minX, maxX)
    }

    private fun updateSaturationGradient() {
        val hsvColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val colors = intArrayOf(Color.WHITE, hsvColor)

        val width = saturationGradient.width.toFloat()
        val gradientDrawable = createGradientWithRoundedCorners(colors, width)

        if (gradientDrawable != null) {
            saturationGradient.background = gradientDrawable
        } else {
            // Fallback nếu width chưa sẵn sàng
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                colors
            )
            gradient.cornerRadius = 10f * context.resources.displayMetrics.density
            saturationGradient.background = gradient
        }
    }

    private fun updateValueGradient() {
        val hsvColor = Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
        val colors = intArrayOf(Color.BLACK, hsvColor)

        val width = valueGradient.width.toFloat()
        val gradientDrawable = createGradientWithRoundedCorners(colors, width)

        if (gradientDrawable != null) {
            valueGradient.background = gradientDrawable
        } else {
            // Fallback nếu width chưa sẵn sàng
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                colors
            )
            gradient.cornerRadius = 10f * context.resources.displayMetrics.density
            valueGradient.background = gradient
        }
    }

    private fun updateColorPreview() {
        val color = Color.HSVToColor(floatArrayOf(hue, saturation, value))
        colorPreview.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    // Thêm phương thức này để tái sử dụng logic bo góc
    private fun createGradientWithRoundedCorners(colors: IntArray, width: Float): ShapeDrawable? {
        if (width <= 0) return null

        // Tạo hình dạng bo góc
        val cornerRadius = 10f * context.resources.displayMetrics.density
        val outerRadii = floatArrayOf(
            cornerRadius, cornerRadius, cornerRadius, cornerRadius,
            cornerRadius, cornerRadius, cornerRadius, cornerRadius
        )

        val roundRectShape = android.graphics.drawable.shapes.RoundRectShape(
            outerRadii, null, null
        )

        // Tạo gradient
        val linearGradient = LinearGradient(
            0f, 0f, width, 0f,
            colors, null, Shader.TileMode.CLAMP
        )

        val paint = Paint()
        paint.shader = linearGradient
        paint.isAntiAlias = true

        return ShapeDrawable(roundRectShape).apply {
            this.paint.set(paint)
        }
    }

    private fun setupActionButtons(view: View, dialog: Dialog) {
        // Suggest button
        view.findViewById<TextView>(R.id.btn_suggest)?.setOnClickListener {
            // TODO: Show color suggestions
        }

        // Selected colors button
        view.findViewById<TextView>(R.id.btn_selected_colors_hsv)?.setOnClickListener {
            // TODO: Show recently selected colors
        }

        // Cancel button
        view.findViewById<TextView>(R.id.btn_cancel_hsv)?.setOnClickListener {
            dialog.dismiss()
        }

        // Apply button
        view.findViewById<TextView>(R.id.btn_apply_hsv)?.setOnClickListener {
            val selectedColor = Color.HSVToColor(floatArrayOf(hue, saturation, value))
            onColorSelected(selectedColor)
            dialog.dismiss()
        }
    }
}
