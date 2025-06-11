package com.example.invitationcard.ui.invitation_edit

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import com.example.invitationcard.ui.invitation_edit.edit_text.font.FontSelectionBottomSheet
import com.example.invitationcard.ui.invitation_edit.edit_text.font.FontSizeController
import com.example.invitationcard.ui.invitation_edit.edit_text.alignment.TextAlignmentController
import com.example.invitationcard.ui.invitation_edit.edit_text.color.TextColorController
import com.example.invitationcard.ui.invitation_edit.edit_text.TextEditorActivity
import com.example.invitationcard.ui.invitation_edit.edit_text.lineheight.LineHeightController
import com.example.invitationcard.utils.FlexibleTextSticker
import com.example.invitationcard.utils.FontManager
import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.TextSticker
import kotlinx.coroutines.launch

class InvitationEditActivity : AppCompatActivity() {

    private lateinit var stickerView: StickerView
    private lateinit var mainContainer: View

    private lateinit var fontManager: FontManager
    private var currentSelectedFont: FontItem? = null

    private lateinit var fontSizeController: FontSizeController
    private var isSizeControlVisible = false

    companion object {
        private const val REQUEST_EDIT_TEXT = 1001
    }

    private lateinit var textColorController: TextColorController
    private var isColorControlVisible = false

    private lateinit var textAlignmentController: TextAlignmentController
    private var isAlignmentControlVisible = false

    private lateinit var lineHeightController: LineHeightController
    private var isLineHeightControlVisible = false

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
//        setupBorderAppearance()

        fontManager = FontManager(this)
        setupFontSizeController()
        setupTextColorController()
        setupTextAlignmentController()
        setupLineHeightController()
        setupBackgroundTouchListener()
        setupTextEditingTools()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackgroundTouchListener() {
        mainContainer.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val wasHandled = stickerView.dispatchTouchEvent(event)
                if (!wasHandled) {
                    unselectCurrentSticker()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    // Thêm phương thức mới vào InvitationEditActivity
    private fun hideAllStickerBorders() {
        try {
            // Lấy danh sách tất cả sticker từ StickerView
            val stickersField = StickerView::class.java.getDeclaredField("stickers")
            stickersField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val stickers = stickersField.get(stickerView) as? List<Sticker>

            // Nếu danh sách sticker tồn tại, ẩn border cho tất cả
            stickers?.forEach { sticker ->
                if (sticker is FlexibleTextSticker) {
                    sticker.setShowBorder(false)
                }
            }

            // Buộc vẽ lại
            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error hiding all sticker borders", e)
        }
    }

    private fun unselectCurrentSticker() {
        try {
            // Đặt handlingSticker = null
            val field = StickerView::class.java.getDeclaredField("handlingSticker")
            field.isAccessible = true
            field.set(stickerView, null)

            // Ẩn tất cả border
            hideAllStickerBorders()

            // Ẩn các công cụ
            hideAllEditTools()

            // Buộc vẽ lại
            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error in unselectCurrentSticker", e)
        }
    }

    private fun setupStickerViewListeners() {
        stickerView.setOnStickerOperationListener(object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                try {
                    hideAllStickerBorders()
                    if (sticker is FlexibleTextSticker) {
                        // Hiển thị border khi thêm mới
                        sticker.setShowBorder(true)
                        showTextEditTools()
                    }
                    stickerView.invalidate()
                }catch (e: Exception) {
                    Log.e("InvitationEditActivity", "Error in onStickerAdded", e)
                }

            }

            override fun onStickerClicked(sticker: Sticker) {
                try {
                    hideAllStickerBorders()
                    if (sticker is FlexibleTextSticker) {
                        sticker.setShowBorder(true)
                        showTextEditTools()
                        updateSizeControllerFromSticker(sticker)

                        // Thêm: Cập nhật trạng thái các nút theo sticker được chọn
                        updateBoldButtonState(sticker.isBold())
                        updateItalicButtonState(sticker.isItalic())
                    }

                    stickerView.invalidate()
                } catch (e: Exception) {
                    Log.e("InvitationEditActivity", "Error in onStickerClicked", e)
                }
            }


            override fun onStickerDeleted(sticker: Sticker) {
                if (stickerView.stickerCount == 0) {
                    hideAllEditTools()
                }
            }

            override fun onStickerDragFinished(sticker: Sticker) {}

            override fun onStickerTouchedDown(sticker: Sticker) {}

            override fun onStickerZoomFinished(sticker: Sticker) {
                if (sticker is FlexibleTextSticker) {
                    val scale = sticker.getCurrentScale()
                    val newSize = (sticker.getTextSizeSp() * scale).toInt().coerceIn(8, 200)
                    sticker.setTextSizeSp(newSize)
                    // Reset scale nhưng giữ lại vị trí/góc xoay
                    sticker.resetScaleKeepPosition()
                    if (isSizeControlVisible) {
                        updateSizeControllerFromSticker(sticker)
                    }
                    stickerView.invalidate()
                }
            }
            override fun onStickerFlipped(sticker: Sticker) {}

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

        // THÊM: Xử lý click cho nút color - NHẤT QUÁN với size
        findViewById<ImageButton>(R.id.btn_color)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is TextSticker) {
                toggleColorControl(currentSticker)
            }
        }

        findViewById<TextView>(R.id.btn_Bold)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                // Toggle trạng thái bold
                val isBold = currentSticker.toggleBold()
                // Cập nhật giao diện nút
                updateBoldButtonState(isBold)
                // Redraw sticker
                stickerView.invalidate()
            }
        }

        // Xử lý nút Italic
        findViewById<TextView>(R.id.btn_Italic)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                // Toggle trạng thái italic
                val isItalic = currentSticker.toggleItalic()
                // Cập nhật giao diện nút
                updateItalicButtonState(isItalic)
                // Redraw sticker
                stickerView.invalidate()
            }
        }

        findViewById<ImageButton>(R.id.btn_gravityHorizontal)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is TextSticker) {
                toggleAlignmentControl(currentSticker)
            }
        }

        findViewById<ImageButton>(R.id.btn_editTextHeight)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                toggleLineHeightControl(currentSticker)
            }
        }
    }

    @SuppressLint("InflateParams")
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_TEXT && resultCode == Activity.RESULT_OK) {
            val resultText = data?.getStringExtra(TextEditorActivity.RESULT_TEXT)
            if (resultText != null) {
                val currentSticker = getCurrentSticker()
                if (currentSticker is TextSticker) {
                    currentSticker.setText(resultText)
                    currentSticker.resizeText()
                    stickerView.invalidate()
                }
            }
        }
    }

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
        createTextStickerFirst(text)
    }

    private fun createTextStickerFirst(text: String) {
        val textSticker = FlexibleTextSticker(this).apply {
            setText(text)
            setTextAlign(Layout.Alignment.ALIGN_CENTER)
            setTypeface(Typeface.DEFAULT)
            setCustomTextColor(Color.GRAY) // SỬA: Dùng setCustomTextColor
            setTextSizeSp(18) // Kích thước mặc định 18sp
        }

        // Thêm sticker vào StickerView
        stickerView.addSticker(textSticker)

        // Hiện toolbar chỉnh sửa text
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
        if (isColorControlVisible) {
            hideColorControl()
        }
    }

    private fun hideAllEditTools() {
        val editToolsContainer = findViewById<LinearLayout>(R.id.edit_tools_container)
        editToolsContainer.visibility = View.GONE
        // Hide size control
        if (isSizeControlVisible) {
            hideSizeControl()
        }
        if (isColorControlVisible) {
            hideColorControl()
        }
        if (isAlignmentControlVisible) { // Thêm điều kiện này
            hideAlignmentControl()
        }

        if (isLineHeightControlVisible) {
            hideLineHeightControl()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFontSizeController() {
        val fontSizeControlView = findViewById<View>(R.id.font_size_control)

        fontSizeControlView.setOnTouchListener { _, _ ->
            // Luôn trả về true để chặn sự kiện chạm
            true
        }

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
        // Lấy kích thước hiện tại của text
        val currentSize = getCurrentTextSize(textSticker)
        fontSizeController.setSize(currentSize)
        fontSizeController.show()
        isSizeControlVisible = true

        // Cập nhật trạng thái nút Size
        updateSizeButtonState(true)
    }

    private fun hideSizeControl() {
        fontSizeController.hide()
        isSizeControlVisible = false
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
            18 // Giá trị mặc định nếu không phải FlexibleTextSticker
        }
    }

    private fun applyFontSizeToCurrentSticker(size: Int) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying font size: $size sp")
                currentSticker.setTextSizeSp(size)
                stickerView.invalidate()
                Log.d("InvitationEditActivity", "Font size applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying font size: ${e.message}")
            }
        }
    }

    // Thêm phương thức setupTextColorController()
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTextColorController() {
        val textColorControlView = findViewById<View>(R.id.text_color_control)

        // THAY ĐỔI: Không chặn mọi sự kiện chạm nữa
        // Để onTouchListener xử lý trong TextColorController

        textColorController = TextColorController(textColorControlView) { newColor ->
            applyTextColorToCurrentSticker(newColor)
        }
    }

    // Thêm phương thức toggleColorControl()
    private fun toggleColorControl(textSticker: TextSticker) {
        if (isColorControlVisible) {
            hideColorControl()
        } else {
            showColorControl(textSticker)
        }
    }

    private fun showColorControl(textSticker: TextSticker) {
        // Kiểm tra type và lấy màu hiện tại
        val currentColor = getCurrentTextColor(textSticker)
        textColorController.setColor(currentColor)

        // Đặt chiều cao TRƯỚC khi hiển thị
        val textColorControlView = findViewById<View>(R.id.text_color_control)
        val params = textColorControlView.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        textColorControlView.layoutParams = params

        textColorController.show()
        isColorControlVisible = true

        updateColorButtonState(true)

        // Ẩn size control nếu đang hiển thị
        if (isSizeControlVisible) {
            hideSizeControl()
        }
    }

    // Thêm phương thức hideColorControl()
    private fun hideColorControl() {
        textColorController.hide()
        isColorControlVisible = false
        updateColorButtonState(false)
    }

    // Thêm phương thức updateColorButtonState()
    private fun updateColorButtonState(isActive: Boolean) {
        val btnColor = findViewById<ImageButton>(R.id.btn_color)
        if (isActive) {
            btnColor?.setColorFilter(resources.getColor(R.color.green, null))
        } else {
            btnColor?.clearColorFilter()
        }
    }


    // NHẤT QUÁN: Chấp nhận TextSticker và kiểm tra type bên trong
    private fun getCurrentTextColor(textSticker: TextSticker): Int {
        return if (textSticker is FlexibleTextSticker) {
            textSticker.getCustomTextColor()
        } else {
            Color.BLACK // Màu mặc định cho TextSticker thường
        }
    }

    // THÊM: Phương thức updateColorControllerFromSticker
//    private fun updateColorControllerFromSticker(sticker: Sticker) {
//        try {
//            if (sticker is FlexibleTextSticker && isColorControlVisible) {
//                val currentColor = sticker.getCustomTextColor()
//                textColorController.setColor(currentColor)
//                Log.d("InvitationEditActivity", "Updated color controller to: #${Integer.toHexString(currentColor)}")
//            }
//        } catch (e: Exception) {
//            Log.e("InvitationEditActivity", "Error updating color controller", e)
//        }
//    }

    private fun applyTextColorToCurrentSticker(color: Int) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying text color: #${Integer.toHexString(color)}")
                currentSticker.setCustomTextColor(color)
                stickerView.invalidate()
                Log.d("InvitationEditActivity", "Text color applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying text color: ${e.message}")
            }
        }
    }

    // Cập nhật trạng thái nút Bold
    private fun updateBoldButtonState(isActive: Boolean) {
        val btnBold = findViewById<TextView>(R.id.btn_Bold)
        if (isActive) {
            btnBold?.setTextColor(resources.getColor(R.color.green, null))
            btnBold?.typeface = Typeface.DEFAULT_BOLD
        } else {
            btnBold?.setTextColor(resources.getColor(android.R.color.black, null))
            btnBold?.typeface = Typeface.DEFAULT
        }
    }

    // Cập nhật trạng thái nút Italic
    private fun updateItalicButtonState(isActive: Boolean) {
        val btnItalic = findViewById<TextView>(R.id.btn_Italic)
        if (isActive) {
            btnItalic?.setTextColor(resources.getColor(R.color.green, null))
            btnItalic?.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        } else {
            btnItalic?.setTextColor(resources.getColor(android.R.color.black, null))
            btnItalic?.typeface = Typeface.DEFAULT
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTextAlignmentController() {
        val textAlignmentControlView = findViewById<View>(R.id.text_alignment_control)

        textAlignmentController = TextAlignmentController(textAlignmentControlView) { newAlignment ->
            applyTextAlignmentToCurrentSticker(newAlignment)
        }
    }

    private fun toggleAlignmentControl(textSticker: TextSticker) {
        if (isAlignmentControlVisible) {
            hideAlignmentControl()
        } else {
            showAlignmentControl(textSticker)
        }
    }

    private fun showAlignmentControl(textSticker: TextSticker) {
        // Lấy căn lề hiện tại
        val currentAlignment = if (textSticker is FlexibleTextSticker) {
            textSticker.getTextAlignment()
        } else {
            Layout.Alignment.ALIGN_CENTER // Giá trị mặc định nếu không phải FlexibleTextSticker
        }

        textAlignmentController.setAlignmentWithoutCallback(currentAlignment)
        textAlignmentController.show()
        isAlignmentControlVisible = true

        // Cập nhật trạng thái nút
        updateAlignmentButtonState(true)

        // Ẩn các control khác
        if (isSizeControlVisible) {
            hideSizeControl()
        }
        if (isColorControlVisible) {
            hideColorControl()
        }
    }

    private fun hideAlignmentControl() {
        textAlignmentController.hide()
        isAlignmentControlVisible = false
        updateAlignmentButtonState(false)
    }

    private fun updateAlignmentButtonState(isActive: Boolean) {
        val btnAlign = findViewById<ImageButton>(R.id.btn_gravityHorizontal)
        if (isActive) {
            btnAlign?.setColorFilter(resources.getColor(R.color.green, null))
        } else {
            btnAlign?.clearColorFilter()
        }
    }

    private fun applyTextAlignmentToCurrentSticker(alignment: Layout.Alignment) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                // Đặt alignment mới
                currentSticker.setTextAlign(alignment)

                // Đảm bảo refreshLayout được gọi
                currentSticker.refreshLayout()

                // Force redraw sticker ngay lập tức
                stickerView.invalidate()

                // Thêm log
                Log.d("InvitationEditActivity", "Text alignment applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying text alignment", e)
            }
        } else if (currentSticker is TextSticker) {
            try {
                currentSticker.setTextAlign(alignment)
                stickerView.invalidate()
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying text alignment", e)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupLineHeightController() {
        val lineHeightControlView = findViewById<View>(R.id.line_height_control)

        lineHeightControlView.setOnTouchListener { _, _ ->
            // Chặn sự kiện chạm để không truyền đến các view bên dưới
            true
        }

        lineHeightController = LineHeightController(lineHeightControlView) { newLineHeight ->
            applyLineHeightToCurrentSticker(newLineHeight)
        }
    }

    private fun toggleLineHeightControl(textSticker: TextSticker) {
        if (isLineHeightControlVisible) {
            hideLineHeightControl()
        } else {
            if (textSticker is FlexibleTextSticker) {
                // Log kiểm tra
                textSticker.checkLineHeightApplied()
            }
            showLineHeightControl(textSticker)
        }
    }

    private fun showLineHeightControl(textSticker: TextSticker) {
        // Lấy line height hiện tại
        val currentLineHeight = getCurrentLineHeight(textSticker)
        lineHeightController.setLineHeightWithoutCallback(currentLineHeight)
        lineHeightController.show()
        isLineHeightControlVisible = true

        // Cập nhật trạng thái nút Line Height
        updateLineHeightButtonState(true)

        // Ẩn các control khác
        if (isSizeControlVisible) {
            hideSizeControl()
        }
        if (isColorControlVisible) {
            hideColorControl()
        }
        if (isAlignmentControlVisible) {
            hideAlignmentControl()
        }
    }

    private fun hideLineHeightControl() {
        lineHeightController.hide()
        isLineHeightControlVisible = false
        updateLineHeightButtonState(false)
    }

    private fun updateLineHeightButtonState(isActive: Boolean) {
        val btnLineHeight = findViewById<ImageButton>(R.id.btn_editTextHeight)
        if (isActive) {
            btnLineHeight?.setColorFilter(resources.getColor(R.color.green, null))
        } else {
            btnLineHeight?.clearColorFilter()
        }
    }

    private fun getCurrentLineHeight(textSticker: TextSticker): Int {
        return if (textSticker is FlexibleTextSticker) {
            textSticker.getLineHeightPercent()
        } else {
            120 // Giá trị mặc định
        }
    }

    private fun applyLineHeightToCurrentSticker(lineHeight: Int) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying line height: $lineHeight%")

                // Đặt line height mới
                currentSticker.setLineHeightPercent(lineHeight)

                // Force redraw sticker
                stickerView.invalidate()

                // Thêm delay redraw để đảm bảo UI được cập nhật
                Handler(Looper.getMainLooper()).postDelayed({
                    stickerView.invalidate()
                    Log.d("InvitationEditActivity", "Redraw after delay")
                }, 50)

                Log.d("InvitationEditActivity", "Line height applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying line height: ${e.message}", e)
            }
        } else {
            Log.e("InvitationEditActivity", "Current sticker is not FlexibleTextSticker")
        }
    }

}