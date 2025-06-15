package com.example.invitationcard.ui.invitation_edit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import android.widget.Toast
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
import com.example.invitationcard.ui.invitation_edit.edit_text.curved.CurvedTextController
import com.example.invitationcard.ui.invitation_edit.edit_text.lineheight.LineHeightController
import com.example.invitationcard.ui.invitation_edit.edit_text.linewidth.LetterSpacingController
import com.example.invitationcard.utils.FlexibleTextSticker
import com.example.invitationcard.utils.FontManager
import com.xiaopo.flying.sticker.DrawableSticker
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
        private const val REQUEST_PICK_IMAGE = 1002
        private const val REQUEST_STORAGE_PERMISSION = 1003
    }

    private lateinit var textColorController: TextColorController
    private var isColorControlVisible = false

    private lateinit var textAlignmentController: TextAlignmentController
    private var isAlignmentControlVisible = false

    private lateinit var lineHeightController: LineHeightController
    private var isLineHeightControlVisible = false

    private lateinit var letterSpacingController: LetterSpacingController
    private var isLetterSpacingControlVisible = false

    private lateinit var curvedTextController: CurvedTextController
    private var isCurvedTextControlVisible = false

    private var isImageLocked = false

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
        setupLetterSpacingController()
        setupCurvedTextController()
        setupBackgroundTouchListener()
        setupTextEditingTools()
        setupImageEditingTools()
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
                // Không cần làm gì cho DrawableSticker vì nó không có phương thức setShowBorder
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

                        // *** LOGGING NÂNG CAO CHO VỊ TRÍ TEXT ***
                        val matrix = sticker.matrix
                        val values = FloatArray(9)
                        matrix.getValues(values)

                        // 1. Log thông tin cơ bản
                        Log.d("TextPosition", "============= TEXT CLICKED POSITION =============")
                        Log.d("TextPosition", "Text content: '${sticker.getText()}'")
                        Log.d("TextPosition", "Matrix values - translation: (${values[Matrix.MTRANS_X]}, ${values[Matrix.MTRANS_Y]})")
                        Log.d("TextPosition", "Matrix values - scale: (${values[Matrix.MSCALE_X]}, ${values[Matrix.MSCALE_Y]})")
                        Log.d("TextPosition", "Matrix values - rotation/skew: " +
                                "(${values[Matrix.MSKEW_X]}, ${values[Matrix.MSKEW_Y]}, ${values[Matrix.MPERSP_0]})")

                        // 2. Lấy thông tin bounds
                        try {
                            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
                            realBoundsField.isAccessible = true
                            val realBounds = realBoundsField.get(sticker) as Rect
                            Log.d("TextPosition", "Text bounds: $realBounds")
                            Log.d("TextPosition", "Text size in SP: ${sticker.getTextSizeSp()}")

                            // 3. Tính toán vị trí thực tế
                            val centerX = values[Matrix.MTRANS_X] + realBounds.exactCenterX() * values[Matrix.MSCALE_X]
                            val centerY = values[Matrix.MTRANS_Y] + realBounds.exactCenterY() * values[Matrix.MSCALE_Y]
                            Log.d("TextPosition", "Calculated center: ($centerX, $centerY)")

                            // 4. Thông tin về view
                            val viewWidth = stickerView.width
                            val viewHeight = stickerView.height
                            Log.d("TextPosition", "View dimensions: $viewWidth x $viewHeight")

                            // 5. Lấy ID của sticker nếu có
                            val stickerHashCode = sticker.hashCode()
                            Log.d("TextPosition", "Sticker identity: #$stickerHashCode")

                            // 6. Số lượng sticker trong view
                            Log.d("TextPosition", "Total stickers in view: ${stickerView.stickerCount}")
                        } catch (e: Exception) {
                            Log.e("TextPosition", "Error getting bounds: ${e.message}")
                        }

                        // 7. Tổng hợp
                        Log.d("TextPosition", "=============================================")

                        // Cập nhật trạng thái UI
                        updateBoldButtonState(sticker.isBold())
                        updateItalicButtonState(sticker.isItalic())
                        updateUppercaseButtonState(sticker.isUppercase())

                        if (isCurvedTextControlVisible) {
                            curvedTextController.setCurveAngleWithoutCallback(sticker.getCurveAngle())
                        }

                        if (isAlignmentControlVisible) {
                            textAlignmentController.setAlignmentWithoutCallback(sticker.getTextAlignment())
                        }
                    }
                    else if (sticker is DrawableSticker) {
                        // XỬ LÝ KHI NHẤN VÀO ẢNH
                        Log.d("ImageSticker", "============= IMAGE CLICKED =============")

                        // Hiển thị công cụ chỉnh sửa ảnh
                        showImageEditTools()

                        // Log thông tin về ảnh để debug
                        val matrix = sticker.matrix
                        val values = FloatArray(9)
                        matrix.getValues(values)

                        Log.d("ImageSticker", "Image position: (${values[Matrix.MTRANS_X]}, ${values[Matrix.MTRANS_Y]})")
                        Log.d("ImageSticker", "Image scale: (${values[Matrix.MSCALE_X]}, ${values[Matrix.MSCALE_Y]})")
                        Log.d("ImageSticker", "Image rotation: ${Math.toDegrees(Math.atan2(values[Matrix.MSKEW_X].toDouble(), values[Matrix.MSCALE_X].toDouble()))} degrees")
                        Log.d("ImageSticker", "Image size: ${sticker.width} x ${sticker.height}")
                        Log.d("ImageSticker", "Image identity: #${sticker.hashCode()}")
                        Log.d("ImageSticker", "Total stickers in view: ${stickerView.stickerCount}")
                        Log.d("ImageSticker", "=============================================")

                        // Reset trạng thái khóa ảnh nếu có
                        isImageLocked = false
                        try {
                            findViewById<ImageButton>(R.id.btn_lockImage)?.let {
                                updateLockButtonState(false)
                            }
                        } catch (e: Exception) {
                            Log.e("ImageSticker", "Error updating lock button: ${e.message}")
                        }
                    }

                    stickerView.invalidate()
                } catch (e: Exception) {
                    Log.e("StickerView", "Error in onStickerClicked: ${e.message}")
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
        findViewById<ImageButton>(R.id.btn_editTextWidth)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                toggleLetterSpacingControl(currentSticker)
            }
        }

        findViewById<TextView>(R.id.btn_Uppercase)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                // Toggle trạng thái uppercase
                val isUppercase = currentSticker.toggleUppercase()
                // Cập nhật giao diện nút
                updateUppercaseButtonState(isUppercase)
                // Redraw sticker
                stickerView.invalidate()
            }
        }
        findViewById<ImageButton>(R.id.btn_editCurvedText)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                toggleCurvedTextControl(currentSticker)
            }
        }
        findViewById<ImageButton>(R.id.btn_duplicateText)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                duplicateCurrentTextSticker(currentSticker)
            } else {
                // Toast thông báo nếu không có text được chọn
                android.widget.Toast.makeText(this, "Không có text nào được chọn để duplicate", android.widget.Toast.LENGTH_SHORT).show()
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
            addImage() // Thêm phương thức addImage
        }

        dialog.show()
    }

    private fun addText() {
        createTextSticker("enter text...")
    }

    private fun showTextEditor(textSticker: TextSticker) {
        val intent = Intent(this,  TextEditorActivity::class.java).apply {
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
        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            // Xử lý kết quả chọn ảnh
            data?.data?.let { uri ->
                try {
                    // Tạo bitmap từ URI
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }

                    // Thêm bitmap vào StickerView
                    addImageSticker(bitmap)
                } catch (e: Exception) {
                    Log.e("InvitationEditActivity", "Error loading image: ${e.message}")
                    Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show()
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

        if (isLetterSpacingControlVisible) {
            hideLetterSpacingControl()
        }
        if (isCurvedTextControlVisible) {
            hideCurvedTextControl()
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupLetterSpacingController() {
        val letterSpacingControlView = findViewById<View>(R.id.letter_spacing_control)

        letterSpacingControlView.setOnTouchListener { _, _ ->
            // Chặn sự kiện chạm
            true
        }

        letterSpacingController = LetterSpacingController(letterSpacingControlView) { newSpacing ->
            applyLetterSpacingToCurrentSticker(newSpacing)
        }
    }

    private fun toggleLetterSpacingControl(textSticker: TextSticker) {
        if (isLetterSpacingControlVisible) {
            hideLetterSpacingControl()
        } else {
            showLetterSpacingControl(textSticker)
        }
    }

    private fun showLetterSpacingControl(textSticker: TextSticker) {
        // Lấy letter spacing hiện tại
        val currentSpacing = getCurrentLetterSpacing(textSticker)
        letterSpacingController.setLetterSpacingWithoutCallback(currentSpacing)
        letterSpacingController.show()
        isLetterSpacingControlVisible = true

        // Cập nhật trạng thái nút
        updateLetterSpacingButtonState(true)

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
        if (isLineHeightControlVisible) {
            hideLineHeightControl()
        }
    }

    private fun hideLetterSpacingControl() {
        letterSpacingController.hide()
        isLetterSpacingControlVisible = false
        updateLetterSpacingButtonState(false)
    }

    private fun updateLetterSpacingButtonState(isActive: Boolean) {
        val btnLetterSpacing = findViewById<ImageButton>(R.id.btn_editTextWidth)
        if (isActive) {
            btnLetterSpacing?.setColorFilter(resources.getColor(R.color.green, null))
        } else {
            btnLetterSpacing?.clearColorFilter()
        }
    }

    private fun getCurrentLetterSpacing(textSticker: TextSticker): Float {
        return if (textSticker is FlexibleTextSticker) {
            textSticker.getLetterSpacing()
        } else {
            0f // Giá trị mặc định
        }
    }

    private fun applyLetterSpacingToCurrentSticker(spacing: Float) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying letter spacing: $spacing")

                // Đặt spacing mới
                currentSticker.setLetterSpacing(spacing)

                // Force redraw
                stickerView.invalidate()

                // Đặt một handler để vẽ lại sau một khoảng thời gian nhỏ (đề phòng)
                Handler(Looper.getMainLooper()).postDelayed({
                    stickerView.invalidate()
                }, 50)

                Log.d("InvitationEditActivity", "Letter spacing applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying letter spacing: ${e.message}", e)
            }
        }
    }

    // Thêm phương thức cập nhật trạng thái nút Uppercase
    private fun updateUppercaseButtonState(isActive: Boolean) {
        val btnUppercase = findViewById<TextView>(R.id.btn_Uppercase)
        if (isActive) {
            btnUppercase?.setTextColor(resources.getColor(R.color.green, null))
            btnUppercase?.typeface = Typeface.DEFAULT_BOLD
        } else {
            btnUppercase?.setTextColor(resources.getColor(android.R.color.black, null))
            btnUppercase?.typeface = Typeface.DEFAULT
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCurvedTextController() {
        val curvedTextControlView = findViewById<View>(R.id.curved_text_control)

        curvedTextControlView.setOnTouchListener { _, _ ->
            // Chặn sự kiện chạm để không truyền đến các view bên dưới
            true
        }

        curvedTextController = CurvedTextController(curvedTextControlView) { newAngle ->
            applyCurveAngleToCurrentSticker(newAngle)
        }
    }

    private fun toggleCurvedTextControl(textSticker: TextSticker) {
        if (isCurvedTextControlVisible) {
            hideCurvedTextControl()
        } else {
            showCurvedTextControl(textSticker)
        }
    }

    private fun showCurvedTextControl(textSticker: TextSticker) {
        // Lấy góc cong hiện tại
        val currentAngle = getCurrentCurveAngle(textSticker)
        curvedTextController.setCurveAngleWithoutCallback(currentAngle)
        curvedTextController.show()
        isCurvedTextControlVisible = true

        // Cập nhật trạng thái nút
        updateCurvedTextButtonState(true)

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
        if (isLetterSpacingControlVisible) {
            hideLetterSpacingControl()
        }
        if (isLineHeightControlVisible) {
            hideLineHeightControl()
        }
    }

    private fun hideCurvedTextControl() {
        curvedTextController.hide()
        isCurvedTextControlVisible = false
        updateCurvedTextButtonState(false)
    }

    private fun updateCurvedTextButtonState(isActive: Boolean) {
        val btnCurvedText = findViewById<ImageButton>(R.id.btn_editCurvedText)
        if (isActive) {
            btnCurvedText?.setColorFilter(resources.getColor(R.color.green, null))
        } else {
            btnCurvedText?.clearColorFilter()
        }
    }

    private fun getCurrentCurveAngle(textSticker: TextSticker): Float {
        return if (textSticker is FlexibleTextSticker) {
            textSticker.getCurveAngle()
        } else {
            0f // Giá trị mặc định
        }
    }

    private fun applyCurveAngleToCurrentSticker(angle: Float) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying curve angle: $angle°")
                currentSticker.setCurveAngle(angle)
                stickerView.invalidate()
                Log.d("InvitationEditActivity", "Curve angle applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying curve angle: ${e.message}", e)
            }
        }
    }

    // Trong InvitationEditActivity
    private fun duplicateCurrentTextSticker(originalSticker: FlexibleTextSticker) {
        try {
            Log.d("InvitationEditActivity", "Starting text duplicate process")

            // Chỉ tạo duplicate với thuộc tính copy
            val offsetX = 20f
            val offsetY = 20f
            val duplicateSticker = originalSticker.createDuplicate(offsetX, offsetY)

            // Lưu lại matrix dùng cho duplicate
            val duplicateMatrix = Matrix(duplicateSticker.matrix)

            // Thêm vào StickerView (có thể sẽ thay đổi matrix)
            stickerView.addSticker(duplicateSticker)

            // *** QUAN TRỌNG: ÁP DỤNG LẠI MATRIX SAU KHI THÊM ***
            stickerView.post {
                // Áp dụng lại matrix sau khi sticker đã được thêm vào view
                duplicateSticker.setMatrix(duplicateMatrix)

                hideAllStickerBorders()
                focusOnDuplicateSticker(duplicateSticker)
                Toast.makeText(this, "Text duplicate - offset đã được áp dụng", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error duplicating text: ${e.message}", e)
        }
    }

    // *** THÊM METHOD TÍNH SMART OFFSET ***
    private fun calculateSmartOffset(sticker: FlexibleTextSticker): Pair<Float, Float> {
        try {
            // Lấy bounds của sticker hiện tại
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(sticker) as Rect

            // Lấy matrix để tính kích thước thực tế sau transform
            val matrix = sticker.matrix
            val values = FloatArray(9)
            matrix.getValues(values)
            val scaleX = values[Matrix.MSCALE_X]
            val scaleY = values[Matrix.MSCALE_Y]

            // Tính kích thước thực tế
            val actualWidth = realBounds.width() * Math.abs(scaleX)
            val actualHeight = realBounds.height() * Math.abs(scaleY)

            // *** OFFSET THÔNG MINH: 30% kích thước sticker + minimum 40px ***
            val offsetX = Math.max(actualWidth * 0.3f, 40f)
            val offsetY = Math.max(actualHeight * 0.3f, 40f)

            Log.d("InvitationEditActivity", "Smart offset calculated: ($offsetX, $offsetY) for size: ${actualWidth}x${actualHeight}")

            return Pair(offsetX, offsetY)

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error calculating smart offset: ${e.message}")
            // Fallback to smaller default offset
            return Pair(40f, 40f)
        }
    }

    // Version có animation smooth hơn:

    private fun duplicateCurrentTextStickerWithAnimation(originalSticker: FlexibleTextSticker) {
        try {
            // Tạo duplicate
            val duplicateSticker = originalSticker.createDuplicate(80f, 80f)

            // Add với animation
            stickerView.addSticker(duplicateSticker)

            // Animation focus smooth
            stickerView.post {
                // Fade out current border
                hideAllStickerBorders()

                // Delay một chút rồi focus vào duplicate
                Handler(Looper.getMainLooper()).postDelayed({
                    focusOnDuplicateSticker(duplicateSticker)
                }, 100)
            }

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error in animated duplicate: ${e.message}", e)
        }
    }

    private fun focusOnDuplicateSticker(duplicateSticker: FlexibleTextSticker) {
        try {
            // *** SET DUPLICATE STICKER LÀM CURRENT STICKER ***
            val handlingStickerField = StickerView::class.java.getDeclaredField("handlingSticker")
            handlingStickerField.isAccessible = true
            handlingStickerField.set(stickerView, duplicateSticker)

            // *** HIỂN THỊ BORDER CHO DUPLICATE ***
            duplicateSticker.setShowBorder(true)

            // *** UPDATE UI CONTROLS THEO DUPLICATE ***
            updateUIControlsFromSticker(duplicateSticker)

            // *** SHOW TEXT EDIT TOOLS ***
            showTextEditTools()

            // *** FORCE REDRAW ***
            stickerView.invalidate()

            Log.d("InvitationEditActivity", "Duplicate sticker focused successfully")

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error in focusOnDuplicateSticker: ${e.message}", e)
        }
    }

    private fun updateUIControlsFromSticker(sticker: FlexibleTextSticker) {
        try {
            // *** UPDATE SIZE CONTROLLER NẾU ĐANG HIỂN THỊ ***
            if (isSizeControlVisible) {
                updateSizeControllerFromSticker(sticker)
            }

            // *** UPDATE CURVED TEXT CONTROLLER NẾU ĐANG HIỂN THỊ ***
            if (isCurvedTextControlVisible) {
                curvedTextController.setCurveAngleWithoutCallback(sticker.getCurveAngle())
            }

            // *** UPDATE ALIGNMENT CONTROLLER NẾU ĐANG HIỂN THỊ ***
            if (isAlignmentControlVisible) {
                textAlignmentController.setAlignmentWithoutCallback(sticker.getTextAlignment())
            }

            // *** UPDATE BUTTON STATES ***
            updateBoldButtonState(sticker.isBold())
            updateItalicButtonState(sticker.isItalic())
            updateUppercaseButtonState(sticker.isUppercase())

            Log.d("InvitationEditActivity", "UI controls updated from duplicate sticker")

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error updating UI controls: ${e.message}", e)
        }
    }

    // 1. Triển khai phương thức addImage()
    private fun addImage() {
        // Kiểm tra quyền dựa trên phiên bản Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ sử dụng quyền đặc biệt cho ảnh
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_STORAGE_PERMISSION)
                return
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 sử dụng quyền storage chung
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                return
            }
        }

        // Nếu đã có quyền, mở trình chọn ảnh
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }
    // 3. Thêm phương thức tạo và thêm image sticker
    private fun addImageSticker(bitmap: Bitmap) {
        try {
            // Tạo drawable từ bitmap
            val drawable = BitmapDrawable(resources, bitmap)

            // Tạo image sticker
            val sticker = DrawableSticker(drawable)

            // Thêm vào StickerView
            stickerView.addSticker(sticker)

            // Hiển thị công cụ chỉnh sửa ảnh
            showImageEditTools()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error adding image sticker: ${e.message}")
        }
    }

    // 4. Phương thức hiển thị công cụ chỉnh sửa ảnh
    private fun showImageEditTools() {
        val editToolsContainer = findViewById<LinearLayout>(R.id.edit_tools_container)
        val textToolsContainer = findViewById<HorizontalScrollView>(R.id.text_tools_container)
        val imageToolsContainer = findViewById<HorizontalScrollView>(R.id.image_tools_container)

        // Hiển thị container chính và container công cụ ảnh
        editToolsContainer.visibility = View.VISIBLE
        textToolsContainer.visibility = View.GONE
        imageToolsContainer.visibility = View.VISIBLE

        // Reset trạng thái khóa khi hiển thị công cụ ảnh
        isImageLocked = false
        try {
            findViewById<ImageButton>(R.id.btn_lockImage)?.let {
                updateLockButtonState(false)
            }
        } catch (e: Exception) {
            Log.e("ImageEditing", "Error updating lock button: ${e.message}")
        }

        // Ẩn các control khác nếu đang hiển thị
        if (isSizeControlVisible) {
            hideSizeControl()
        }
        if (isColorControlVisible) {
            hideColorControl()
        }
        if (isAlignmentControlVisible) {
            hideAlignmentControl()
        }
        if (isLineHeightControlVisible) {
            hideLineHeightControl()
        }
        if (isLetterSpacingControlVisible) {
            hideLetterSpacingControl()
        }
        if (isCurvedTextControlVisible) {
            hideCurvedTextControl()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp, mở trình chọn ảnh
                addImage()
            } else {
                Toast.makeText(this, "Cần cấp quyền truy cập thư viện ảnh để thêm ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupImageEditingTools() {
        // Xử lý nút xóa ảnh
        findViewById<ImageButton>(R.id.btn_delete_image)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                stickerView.remove(currentSticker)
                hideAllEditTools()
            }
        }

        // Xử lý nút lật ảnh
        findViewById<ImageButton>(R.id.btn_flipImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                flipImageSticker(currentSticker)
            }
        }

        // Xử lý nút xoay ảnh
        findViewById<ImageButton>(R.id.btn_rotateImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                rotateImageSticker(currentSticker, 90f)
            }
        }

        // Xử lý nút zoom
        findViewById<TextView>(R.id.btn_zoomImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                // Đây chỉ là demo, bạn có thể thay thế bằng một slider hoặc control riêng
                Toast.makeText(this, "Chức năng zoom ảnh đang được phát triển", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý nút filter
        findViewById<TextView>(R.id.btn_filterImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                Toast.makeText(this, "Chức năng filter ảnh đang được phát triển", Toast.LENGTH_SHORT).show()
            }
        }

        // Xử lý nút remove background
        findViewById<TextView>(R.id.btn_removeBackgroundImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                Toast.makeText(this, "Chức năng xóa nền đang được phát triển", Toast.LENGTH_SHORT).show()
            }
        }
// Trong setupImageEditingTools() thêm:
        findViewById<ImageButton>(R.id.btn_lockImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                // Toggle trạng thái khóa
                isImageLocked = !isImageLocked

                // Đặt trạng thái khóa/mở khóa
                setImageManipulationLocked(isImageLocked)

                // Cập nhật giao diện nút
                updateLockButtonState(isImageLocked)

                // Thông báo cho người dùng
                val message = if (isImageLocked)
                    "Đã khóa zoom và xoay ảnh"
                else
                    "Đã mở khóa zoom và xoay ảnh"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    // Phương thức lật ảnh
    private fun flipImageSticker(sticker: Sticker) {
        try {
            // Lật ảnh theo chiều ngang
            val matrix = Matrix(sticker.matrix)
            matrix.preScale(-1f, 1f, sticker.width / 2f, sticker.height / 2f)
            sticker.setMatrix(matrix)
            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error flipping image: ${e.message}")
        }
    }

    // Phương thức xoay ảnh với trục xoay chính xác
    private fun rotateImageSticker(sticker: Sticker, degrees: Float) {
        try {
            // Lấy các điểm góc của sticker
            val mappedBoundPoints = sticker.getMappedBoundPoints()

            // Tính toán tâm thực sự từ các điểm góc sau các biến đổi
            var sumX = 0f
            var sumY = 0f
            for (i in 0 until mappedBoundPoints.size step 2) {
                sumX += mappedBoundPoints[i]
                sumY += mappedBoundPoints[i + 1]
            }
            val centerX = sumX / 4  // Chia cho 4 vì có 4 điểm góc
            val centerY = sumY / 4

            Log.d("ImageRotation", "Rotating around calculated center: ($centerX, $centerY)")

            // Xoay matrix quanh tâm thực tế
            val matrix = Matrix(sticker.matrix)
            matrix.postRotate(degrees, centerX, centerY)

            // Áp dụng matrix mới
            sticker.setMatrix(matrix)

            // Vẽ lại
            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error rotating image: ${e.message}", e)
        }
    }

    // Phương thức đặt trạng thái khóa cho ảnh
    private fun setImageManipulationLocked(locked: Boolean) {
        val currentSticker = getCurrentSticker()
        if (currentSticker != null && currentSticker !is TextSticker) {
            // Cài đặt trạng thái khóa cho sticker hiện tại
            // Lưu ý: Đây là phương pháp đơn giản, bạn có thể triển khai phức tạp hơn
            // bằng cách sử dụng reflection để can thiệp vào controller của StickerView

            // Trong tình huống thực tế, có thể cần triển khai lớp StickerView tùy chỉnh
            // để hỗ trợ tính năng này tốt hơn
            if (locked) {
                Toast.makeText(this, "Chỉ có thể di chuyển ảnh, không thể zoom/xoay", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cập nhật trạng thái nút khóa
    private fun updateLockButtonState(isLocked: Boolean) {
        val btnLock = findViewById<ImageButton>(R.id.btn_lockImage)
        if (isLocked) {
            btnLock?.setImageResource(R.drawable.ic_lock) // Cần tạo resource này
            btnLock?.setColorFilter(resources.getColor(R.color.green, null))
        } else {
            btnLock?.setImageResource(R.drawable.ic_unlock) // Cần tạo resource này
            btnLock?.clearColorFilter()
        }
    }
}