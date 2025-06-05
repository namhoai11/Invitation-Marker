package com.example.invitationcard.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Layout
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.invitationcard.R
import com.example.invitationcard.model.FontItem
import com.example.invitationcard.utils.FlexibleTextSticker
import com.example.invitationcard.utils.FontManager
import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.TextSticker
import kotlinx.coroutines.launch
import java.lang.reflect.Method

class InvitationEditActivity : AppCompatActivity() {

    private lateinit var stickerView: StickerView
    private lateinit var mainContainer: View

    private lateinit var fontManager: FontManager
    private var currentSelectedFont: FontItem? = null


    private lateinit var fontSizeController: FontSizeController
    private var isSizeControlVisible = false


    // Store current text properties
    private data class TextProperties(
        val text: String,
        val color: Int,
        val alignment: Layout.Alignment,
        val sizeInSp: Int = 30
    )

    private var baseTextSize = 30f
    private var currentScale = 1.0f

    private var currentDisplayedSize = 30
    private var debugDone = false
    private var textSizeMethod: Method? = null
    private var currentTextContent = "Enter text..."
    private var currentTextColor = android.graphics.Color.GRAY

    private var currentTextProperties: TextProperties? = null

    companion object {
        private const val REQUEST_EDIT_TEXT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_invitation_edit)

        stickerView = findViewById(R.id.sticker_view)
        mainContainer = findViewById(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Xử lý click nút Add (+)
        findViewById<View>(R.id.btn_add).setOnClickListener {
            showAddItemDialog()
        }

        setupStickerViewListeners()
        stickerView.setLocked(false)

        // QUAN TRỌNG: Đặt showBorder = true bằng phản chiếu (reflection)
        try {
            val field = StickerView::class.java.getDeclaredField("showBorder")
            field.isAccessible = true
            field.setBoolean(stickerView, true)

            val borderPaintField = StickerView::class.java.getDeclaredField("borderPaint")
            borderPaintField.isAccessible = true
            val borderPaint = borderPaintField.get(stickerView) as android.graphics.Paint
            borderPaint.color = Color.GREEN
            borderPaint.alpha = 255
            borderPaint.strokeWidth = 8f
        } catch (e: Exception) {
            e.printStackTrace()
        }

        fontManager = FontManager(this)
        setupFontSizeController()
        setupBackgroundTouchListener()
        setupTextEditingTools()
    }

    private fun setupDynamicBorder() {
        try {
            // Enable border display
            val showBorderField = StickerView::class.java.getDeclaredField("showBorder")
            showBorderField.isAccessible = true
            showBorderField.setBoolean(stickerView, true)

            // Customize border paint for better visibility
            val borderPaintField = StickerView::class.java.getDeclaredField("borderPaint")
            borderPaintField.isAccessible = true
            val borderPaint = borderPaintField.get(stickerView) as android.graphics.Paint

            borderPaint.apply {
                color = Color.parseColor("#2196F3") // Blue color
                strokeWidth = 2f
                style = android.graphics.Paint.Style.STROKE
                alpha = 180
                // Remove dash effect for cleaner look
                pathEffect = null
            }

            // Customize icon paint if exists
            try {
                val iconPaintField = StickerView::class.java.getDeclaredField("iconPaint")
                iconPaintField.isAccessible = true
                val iconPaint = iconPaintField.get(stickerView) as android.graphics.Paint
                iconPaint.alpha = 200
            } catch (e: Exception) {
                Log.d("InvitationEditActivity", "No iconPaint field found")
            }

            Log.d("InvitationEditActivity", "Dynamic border setup completed")
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error setting up dynamic border", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackgroundTouchListener() {
        mainContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Kiểm tra xem tap có vào khoảng trống không
                val wasHandled = stickerView.dispatchTouchEvent(event)
                if (!wasHandled) {
                    // Nếu StickerView không xử lý event, bỏ chọn sticker hiện tại
                    unselectCurrentSticker()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    // Phương thức để bỏ chọn sticker hiện tại
    private fun unselectCurrentSticker() {
        try {
            val field = StickerView::class.java.getDeclaredField("handlingSticker")
            field.isAccessible = true
            field.set(stickerView, null)

            hideAllEditTools()
            stickerView.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupStickerViewListeners() {
        stickerView.setOnStickerOperationListener(object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                if (sticker is TextSticker) {
                    showTextEditTools()
                }
                stickerView.invalidate()
            }

            override fun onStickerClicked(sticker: Sticker) {
                if (sticker is TextSticker) {
                    showTextEditTools()
                    // Update size controller with current size
                    updateSizeControllerFromSticker(sticker)
                }
                stickerView.invalidate()
            }

            override fun onStickerDeleted(sticker: Sticker) {
                if (stickerView.stickerCount == 0) {
                    hideAllEditTools()
                }
            }

            override fun onStickerDragFinished(sticker: Sticker) {
                // Handle drag finished
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                // Handle touch down
            }

            override fun onStickerZoomFinished(sticker: Sticker) {
                // QUAN TRỌNG: Update size controller khi zoom finished
                if (sticker is FlexibleTextSticker && isSizeControlVisible) {
                    updateSizeControllerFromSticker(sticker)
                }
            }

            override fun onStickerFlipped(sticker: Sticker) {
                // Handle flip
            }

            override fun onStickerDoubleTapped(sticker: Sticker) {
                if (sticker is TextSticker) {
                    showTextEditor(sticker)
                }
            }
        })
    }

    private fun setupTextEditingTools() {
        findViewById<TextView>(R.id.btn_edit_text)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is TextSticker) {
                showTextEditor(currentSticker)
            }
        }

        findViewById<ImageButton>(R.id.btn_delete_text)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null) {
                stickerView.remove(currentSticker)
                hideAllEditTools()
            }
        }

        findViewById<TextView>(R.id.btn_font)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is TextSticker) {
                showFontSelectionBottomSheet(currentSticker)
            }
        }

        findViewById<TextView>(R.id.btn_size)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is TextSticker) {
                toggleSizeControl(currentSticker)
            }
        }

    }

    private fun showAddItemDialog() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.btn_add_text).setOnClickListener {
            dialog.dismiss()
            addText()
        }

        view.findViewById<LinearLayout>(R.id.btn_add_sticker).setOnClickListener {
            dialog.dismiss()
            // TODO: Hiển thị giao diện chọn sticker
        }

        view.findViewById<LinearLayout>(R.id.btn_add_image).setOnClickListener {
            dialog.dismiss()
            // TODO: Hiển thị giao diện chọn ảnh
        }

        dialog.show()
    }

    private fun addText() {
        createTextSticker("Enter text...")
    }

    private fun showTextEditor(textSticker: TextSticker) {
        val intent = Intent(this, TextEditorActivity::class.java).apply {
            putExtra(TextEditorActivity.EXTRA_TEXT, textSticker.text)
        }
        startActivityForResult(intent, REQUEST_EDIT_TEXT)
    }

    // Xử lý kết quả trả về từ TextEditorActivity
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_TEXT && resultCode == Activity.RESULT_OK) {
            val resultText = data?.getStringExtra(TextEditorActivity.RESULT_TEXT)
            Log.d("InvitationEditActivity", "Result text: $resultText")
            if (resultText != null) {
                val currentSticker = getCurrentSticker()
                if (currentSticker is TextSticker) {
                    currentSticker.setText(resultText)
                    currentSticker.resizeText()
                    Log.d("InvitationEditActivity", "Updated text: ${currentSticker.text}")
                    stickerView.invalidate()
                }
            }
        }
    }

    // Phương thức lấy sticker hiện tại đang được chọn
    private fun getCurrentSticker(): Sticker? {
        try {
            val field = StickerView::class.java.getDeclaredField("handlingSticker")
            field.isAccessible = true
            return field.get(stickerView) as? Sticker
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun createTextSticker(text: String) {
//        createTextStickerWithSize(text, 8, Color.GRAY)
        createTextStickerFirst(text)
    }

    private fun createTextStickerFirst(text: String) {
        val textSticker = FlexibleTextSticker(this).apply {
            setText(text)
            setTextAlign(Layout.Alignment.ALIGN_CENTER)
            setTypeface(Typeface.DEFAULT)
            setTextColor(Color.GRAY)
            setTextSizeSp(18) // Giảm kích thước mặc định xuống 18sp
        }

        // Thêm sticker vào StickerView
        stickerView.addSticker(textSticker)

        // Hiện toolbar chỉnh sửa text và ẩn toolbar ảnh
        showTextEditTools()
    }

    private fun showTextEditTools() {
        val editToolsContainer = findViewById<LinearLayout>(R.id.edit_tools_container)
        val textToolsContainer = findViewById<HorizontalScrollView>(R.id.text_tools_container)
        val imageToolsContainer = findViewById<HorizontalScrollView>(R.id.image_tools_container)

        editToolsContainer.visibility = View.VISIBLE
        textToolsContainer.visibility = View.VISIBLE
        imageToolsContainer.visibility = View.GONE

        // Hide size control when switching tools
        if (isSizeControlVisible) {
            hideSizeControl()
        }
    }

    private fun hideAllEditTools() {
        val editToolsContainer = findViewById<LinearLayout>(R.id.edit_tools_container)
        editToolsContainer.visibility = View.GONE
        // Hide size control
        if (isSizeControlVisible) {
            hideSizeControl()
        }
    }
    private fun showFontSelectionBottomSheet(textSticker: TextSticker) {
        // Get current font of the text sticker
        val currentFont = currentSelectedFont ?: FontItem("Default", "default", "System", isSystemFont = true)

        val bottomSheet = FontSelectionBottomSheet.newInstance(currentFont)
        bottomSheet.setOnFontSelectedListener { selectedFont ->
            applyFontToSticker(textSticker, selectedFont)
        }
        bottomSheet.show(supportFragmentManager, "FontSelectionBottomSheet")
    }

    private fun applyFontToSticker(textSticker: TextSticker, fontItem: FontItem) {
        lifecycleScope.launch {
            // Load font if needed
            val typeface = if (fontItem.typeface != null) {
                fontItem.typeface
            } else {
                fontManager.loadFont(fontItem)
            }

            // Apply font on main thread
            runOnUiThread {
                typeface?.let { tf ->
                    textSticker.setTypeface(tf)
                    textSticker.resizeText()
                    stickerView.invalidate()
                    currentSelectedFont = fontItem
                }
            }
        }
    }

    private fun setupFontSizeController() {
        val fontSizeControlView = findViewById<View>(R.id.font_size_control)
        fontSizeController = FontSizeController(fontSizeControlView) { newSize ->
            applyFontSizeToCurrentSticker(newSize)
        }
    }

    private fun toggleSizeControl(textSticker: TextSticker) {
        if (isSizeControlVisible) {
            hideSizeControl()
        } else {
            showSizeControl(textSticker)
        }
    }
    private fun showSizeControl(textSticker: TextSticker) {
        // Get current text size from sticker
        val currentSize = getCurrentTextSize(textSticker)
        fontSizeController.setSize(currentSize)
        fontSizeController.show()
        isSizeControlVisible = true

        // Update Size button appearance
        updateSizeButtonState(true)
    }

    private fun hideSizeControl() {
        fontSizeController.hide()
        isSizeControlVisible = false

        // Update Size button appearance
        updateSizeButtonState(false)
    }

    private fun updateSizeButtonState(isActive: Boolean) {
        val btnSize = findViewById<TextView>(R.id.btn_size)
        if (isActive) {
            btnSize?.setTextColor(resources.getColor(R.color.green, null))
        } else {
            btnSize?.setTextColor(resources.getColor(android.R.color.black, null))
        }
    }

    private fun updateSizeControllerFromSticker(sticker: Sticker) {
        try {
            if (sticker is FlexibleTextSticker) {
                val currentSize = sticker.getTextSizeSp()
                fontSizeController.setSizeWithoutCallback(currentSize)
                currentDisplayedSize = currentSize
                Log.d("InvitationEditActivity", "Updated size controller to: $currentSize sp")
            }
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error updating size controller", e)
        }
    }

    private fun getCurrentTextSize(textSticker: TextSticker): Int {
        return if (textSticker is FlexibleTextSticker) {
            textSticker.getTextSizeSp()
        } else {
            currentDisplayedSize // Giá trị mặc định nếu không phải FlexibleTextSticker
        }
    }

    private fun applyFontSizeToCurrentSticker(size: Int) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying font size to FlexibleTextSticker: $size sp")
                currentSticker.setTextSizeSp(size)
                stickerView.invalidate()
                currentDisplayedSize = size
                Log.d("InvitationEditActivity", "Font size applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying font size: ${e.message}")
            }
        }
    }

    private fun createTextStickerWithSize(
        text: String,
        sizeInSp: Int,
        color: Int = Color.BLACK
    ) {
        // Use FlexibleTextSticker
        val textSticker = FlexibleTextSticker(this)

        textSticker.apply {
            setText(text)
            setTextAlign(Layout.Alignment.ALIGN_CENTER)
            setTextColor(color)
            setTextSizeSp(sizeInSp)
        }

        // Add to StickerView
        stickerView.addSticker(textSticker)

        // Update stored properties
        currentTextProperties = TextProperties(
            text = text,
            color = color,
            alignment = Layout.Alignment.ALIGN_CENTER,
            sizeInSp = sizeInSp
        )

        currentDisplayedSize = sizeInSp

        // Show text tools
        showTextEditTools()

        Log.d("InvitationEditActivity", "Created flexible text sticker with size: $sizeInSp sp")
    }
}