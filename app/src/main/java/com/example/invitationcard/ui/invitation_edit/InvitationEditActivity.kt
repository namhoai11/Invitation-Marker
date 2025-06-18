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
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.TextPaint
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
import com.example.invitationcard.model.TemplateElement
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
import com.example.invitationcard.utils.LockableDrawableSticker
import com.example.invitationcard.utils.PsdParser
import com.example.invitationcard.utils.SvgTemplateLoader
import com.example.invitationcard.utils.TemplateRenderer
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

    private lateinit var psdParser: PsdParser
    private lateinit var templateRenderer: TemplateRenderer
    private var backgroundStickerRef: LockableDrawableSticker? = null

    private val lockedStickers = HashMap<Int, Boolean>()

    private lateinit var svgTemplateLoader: SvgTemplateLoader

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

        fontManager = FontManager(this)
        templateRenderer = TemplateRenderer(this, fontManager)

        svgTemplateLoader = SvgTemplateLoader(this, templateRenderer, fontManager)

        Log.d("InvitationEditActivity", "Starting template loading process")

        stickerView.post {
            val viewWidth = stickerView.width
            val viewHeight = stickerView.height
            Log.d("InvitationEditActivity", "Initial StickerView dimensions: ${viewWidth}x${viewHeight}")

            if (viewWidth > 0 && viewHeight > 0) {
                val templateFile = "invitation_figma.svg"
                try {
                    val files = assets.list("")
                    if (files?.contains(templateFile) == true) {
                        loadSvgTemplate(templateFile)
                    } else {
                        Log.d("InvitationEditActivity", "Template file không tồn tại, tạo template trống")
                        loadTestSvgTemplate(viewWidth, viewHeight)
                    }
                } catch (e: Exception) {
                    Log.e("InvitationEditActivity", "Lỗi khi kiểm tra file template", e)
                    // Tạo template trống nếu có lỗi
                    loadTestSvgTemplate(viewWidth, viewHeight)
                }
            }
        }

        stickerView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val width = right - left
            val height = bottom - top

            if (width > 0 && height > 0 && (width != oldRight - oldLeft || height != oldBottom - oldTop)) {
                Log.d("InvitationEditActivity", "StickerView layout changed: ${width}x${height}")

                // Chỉ tải template trống nếu chưa có sticker nào
                if (stickerView.stickerCount == 0) {
                    loadTestSvgTemplate(width, height)
                }
            }
        }

        // Thêm vào cuối onCreate của InvitationEditActivity
        fontManager.checkFontsAvailability()

        // Thiết lập các controller (không thay đổi)
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

    private fun loadSvgTemplate(templatePath: String) {
        lifecycleScope.launch {
            try {
                // Xóa tất cả stickers hiện tại
                stickerView.removeAllStickers()

                // Tải template SVG
                val (elements, background, dimensions) = svgTemplateLoader.loadSvgTemplateFromAssets(templatePath)

                Log.d("TemplateLoading", "SVG loaded with ${elements.size} elements")

                // Xử lý trường hợp không có element và không có background
                if (elements.isEmpty() && background == null) {
                    Log.d("TemplateLoading", "Template rỗng, tạo template trống thay thế")
                    loadTestSvgTemplate(stickerView.width, stickerView.height)
                    return@launch
                }

                // Thêm background nếu có
                background?.let { bitmap ->
                    val backgroundDrawable = BitmapDrawable(resources, bitmap)
                    val backgroundSticker = LockableDrawableSticker(backgroundDrawable)

                    // Đặt vị trí phù hợp với view
                    val matrix = Matrix()
                    val viewWidth = stickerView.width.toFloat()
                    val viewHeight = stickerView.height.toFloat()

                    // Căn giữa
                    val translateX = (viewWidth - bitmap.width) / 2f
                    val translateY = (viewHeight - bitmap.height) / 2f
                    matrix.postTranslate(translateX, translateY)

                    backgroundSticker.setMatrix(matrix)
                    backgroundSticker.isLocked = true

                    // Thêm background vào index 0
                    stickerView.addSticker(backgroundSticker, 0)
                    Log.d("TemplateLoading", "Background added from SVG")
                }

                // QUAN TRỌNG: Sắp xếp elements theo zIndex TĂNG dần
                val sortedElements = elements.sortedBy { it.zIndex }

                // Thêm các element khác
                var successCount = 0
                for (element in sortedElements.filter { it.id != "background" }) {
                    try {
                        Log.d("TemplateLoading", "Creating sticker for element: ${element.id}, z: ${element.zIndex}")
                        val sticker = templateRenderer.createStickerFromElement(element)

                        if (sticker != null) {
                            stickerView.addSticker(sticker)
                            successCount++
                            Log.d("TemplateLoading", "Successfully added sticker for element: ${element.id}")
                        }
                    } catch (e: Exception) {
                        Log.e("TemplateLoading", "Error creating sticker for element ${element.id}", e)
                    }
                }

                Log.d("TemplateLoading", "SVG template loaded with $successCount stickers")

                // Nếu không có sticker nào được tạo và không có background, hiển thị template trống
                if (successCount == 0 && background == null) {
                    Log.d("TemplateLoading", "Không tạo được sticker nào, tạo template trống thay thế")
                    loadTestSvgTemplate(stickerView.width, stickerView.height)
                }

            } catch (e: Exception) {
                Log.e("TemplateLoading", "Error loading SVG template", e)
                Toast.makeText(this@InvitationEditActivity,
                    "Không thể tải template SVG: ${e.message}", Toast.LENGTH_SHORT).show()

                // Tạo template trống nếu có lỗi
                loadTestSvgTemplate(stickerView.width, stickerView.height)
            }
        }
    }

    private fun loadTestSvgTemplate(viewWidth: Int, viewHeight: Int) {
        lifecycleScope.launch {
            try {
                // Xóa stickers hiện tại
                stickerView.removeAllStickers()

                Log.d("TemplateLoading", "Không có template mẫu, tạo template trống với kích thước: ${viewWidth}x${viewHeight}")

                // Tạo background màu trắng đơn giản
                val backgroundBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                backgroundBitmap.eraseColor(Color.WHITE) // Đổi sang màu trắng đơn giản
                val backgroundDrawable = BitmapDrawable(resources, backgroundBitmap)
                val backgroundSticker = LockableDrawableSticker(backgroundDrawable)
                backgroundSticker.isLocked = true

                // Thêm background vào sticker view
                stickerView.addSticker(backgroundSticker, 0)
                Log.d("TemplateLoading", "Đã thêm background trống")

//                // Thêm text hướng dẫn đơn giản
//                val helpText = FlexibleTextSticker(this@InvitationEditActivity).apply {
//                    setText("Nhấn nút + để thêm văn bản hoặc hình ảnh")
//                    setTextAlign(Layout.Alignment.ALIGN_CENTER)
//                    setTypeface(Typeface.DEFAULT)
//                    setCustomTextColor(Color.GRAY)
//                    setTextSizeSp(16)
//                }
//
//                // Định vị ở giữa màn hình
//                val matrix = Matrix()
//                matrix.postTranslate(
//                    (viewWidth / 2 - helpText.width / 2).toFloat(),
//                    (viewHeight / 2 - helpText.height / 2).toFloat()
//                )
//                helpText.setMatrix(matrix)
//
//                // Thêm text vào StickerView
//                stickerView.addSticker(helpText)

                // Force redraw
                stickerView.invalidate()

            } catch (e: Exception) {
                Log.e("TemplateLoading", "Lỗi khi tạo template trống", e)
                Toast.makeText(this@InvitationEditActivity,
                    "Không thể tạo template: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPsdTemplate(templatePath: String) {
        lifecycleScope.launch {
            try {
                // Xóa tất cả stickers hiện tại
                stickerView.removeAllStickers()

                // Phân tích file PSD
                val (elements, background, dimensions) = psdParser.parsePsdFromAssets(templatePath)

                Log.d("InvitationEditActivity", "Loaded template with ${elements.size} elements")

                // QUAN TRỌNG: Sắp xếp elements theo zIndex trước khi tạo sticker
                val sortedElements = elements.sortedBy { it.zIndex }

                // Tạo và thêm stickers từ elements đã sắp xếp
                val stickers = mutableListOf<Sticker>()
                for (element in sortedElements) {
                    try {
                        val sticker = templateRenderer.createStickerFromElement(element)
                        if (sticker != null) {
                            // Khóa các phần tử không thể chỉnh sửa
                            if (!element.isEditable && sticker is LockableDrawableSticker) {
                                sticker.isLocked = true
                            }

                            stickers.add(sticker)
                            stickerView.addSticker(sticker)

                            // Log để debug
                            when (element) {
                                is TemplateElement.TextElement -> {
                                    Log.d("InvitationEditActivity", "Added text sticker: '${element.text}', bounds: ${element.bounds}")
                                }
                                is TemplateElement.ImageElement -> {
                                    Log.d("InvitationEditActivity", "Added image sticker: ${element.id}, bounds: ${element.bounds}")
                                }
                                else -> {
                                    Log.d("InvitationEditActivity", "Added other sticker: ${element.id}, bounds: ${element.bounds}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("InvitationEditActivity", "Error creating sticker for element ${element.id}", e)
                    }
                }

                // Đặt background nếu có và không đã được thêm vào elements
                if (background != null && !elements.any { it.id == "background" }) {
                    val backgroundDrawable = BitmapDrawable(resources, background)
                    val backgroundSticker = LockableDrawableSticker(backgroundDrawable)

                    // Căn chỉnh để phủ toàn màn hình
                    val matrix = Matrix()
                    val viewWidth = stickerView.width.toFloat()
                    val viewHeight = stickerView.height.toFloat()
                    val translateX = (viewWidth - background.width) / 2
                    val translateY = (viewHeight - background.height) / 2
                    matrix.setTranslate(translateX, translateY)

                    backgroundSticker.setMatrix(matrix)
                    backgroundSticker.isLocked = true

                    // Thêm vào đầu tiên để ở dưới cùng
                    stickerView.addSticker(backgroundSticker, 0)
                    backgroundStickerRef = backgroundSticker
                }

                Log.d("InvitationEditActivity", "Template loaded successfully with ${stickers.size} stickers")

            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error loading PSD template", e)
                Toast.makeText(this@InvitationEditActivity,
                    "Failed to load template: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun hideAllStickerBorders() {
        try {
            val stickersField = StickerView::class.java.getDeclaredField("stickers")
            stickersField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val stickers = stickersField.get(stickerView) as? List<Sticker>

            stickers?.forEach { sticker ->
                if (sticker is FlexibleTextSticker) {
                    sticker.setShowBorder(false)
                }
            }

            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error hiding all sticker borders", e)
        }
    }

    private fun unselectCurrentSticker() {
        try {
            val field = StickerView::class.java.getDeclaredField("handlingSticker")
            field.isAccessible = true
            field.set(stickerView, null)

            hideAllStickerBorders()

            hideAllEditTools()

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

                    if (isStickerLocked(sticker)) {
                        return
                    }

                    if (sticker is FlexibleTextSticker) {
                        sticker.setShowBorder(true)
                        showTextEditTools()
                        updateSizeControllerFromSticker(sticker)

                        val matrix = sticker.matrix
                        val values = FloatArray(9)
                        matrix.getValues(values)

                        Log.d("TextPosition", "============= TEXT CLICKED POSITION =============")
                        Log.d("TextPosition", "Text content: '${sticker.getText()}'")
                        Log.d("TextPosition", "Matrix values - translation: (${values[Matrix.MTRANS_X]}, ${values[Matrix.MTRANS_Y]})")
                        Log.d("TextPosition", "Matrix values - scale: (${values[Matrix.MSCALE_X]}, ${values[Matrix.MSCALE_Y]})")
                        Log.d("TextPosition", "Matrix values - rotation/skew: " +
                                "(${values[Matrix.MSKEW_X]}, ${values[Matrix.MSKEW_Y]}, ${values[Matrix.MPERSP_0]})")

                        try {
                            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
                            realBoundsField.isAccessible = true
                            val realBounds = realBoundsField.get(sticker) as Rect
                            Log.d("TextPosition", "Text bounds: $realBounds")
                            Log.d("TextPosition", "Text size in SP: ${sticker.getTextSizeSp()}")

                            val centerX = values[Matrix.MTRANS_X] + realBounds.exactCenterX() * values[Matrix.MSCALE_X]
                            val centerY = values[Matrix.MTRANS_Y] + realBounds.exactCenterY() * values[Matrix.MSCALE_Y]
                            Log.d("TextPosition", "Calculated center: ($centerX, $centerY)")

                            val viewWidth = stickerView.width
                            val viewHeight = stickerView.height
                            Log.d("TextPosition", "View dimensions: $viewWidth x $viewHeight")

                            val stickerHashCode = sticker.hashCode()
                            Log.d("TextPosition", "Sticker identity: #$stickerHashCode")

                            Log.d("TextPosition", "Total stickers in view: ${stickerView.stickerCount}")
                        } catch (e: Exception) {
                            Log.e("TextPosition", "Error getting bounds: ${e.message}")
                        }

                        Log.d("TextPosition", "=============================================")

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
                        Log.d("ImageSticker", "============= IMAGE CLICKED =============")

                        showImageEditTools()

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
                lockedStickers.remove(sticker.hashCode())

                if (stickerView.stickerCount == 0) {
                    hideAllEditTools()
                }
            }

            override fun onStickerDragFinished(sticker: Sticker) {
                if (isStickerLocked(sticker)) return
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                if (isStickerLocked(sticker)) return
            }

            override fun onStickerZoomFinished(sticker: Sticker) {
                if (isStickerLocked(sticker)) return
                if (sticker is FlexibleTextSticker) {
                    val scale = sticker.getCurrentScale()
                    val newSize = (sticker.getTextSizeSp() * scale).toInt().coerceIn(8, 200)
                    sticker.setTextSizeSp(newSize)
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

        findViewById<ImageButton>(R.id.btn_color)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is TextSticker) {
                toggleColorControl(currentSticker)
            }
        }

        findViewById<TextView>(R.id.btn_Bold)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker is FlexibleTextSticker) {
                val isBold = currentSticker.toggleBold()
                updateBoldButtonState(isBold)
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
            addImage()
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
            data?.data?.let { uri ->
                try {
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
            setCustomTextColor(Color.GRAY)
            setTextSizeSp(18)
        }

        val viewWidth = stickerView.width.toFloat()
        val viewHeight = stickerView.height.toFloat()

        val matrix = Matrix()
        matrix.postTranslate(
            viewWidth / 2 - textSticker.width / 2,
            viewHeight / 2 - textSticker.height / 2
        )
        textSticker.setMatrix(matrix)

        stickerView.addSticker(textSticker)

        showTextEditTools()
    }

    private fun showTextEditTools() {
        val editToolsContainer = findViewById<LinearLayout>(R.id.edit_tools_container)
        val textToolsContainer = findViewById<HorizontalScrollView>(R.id.text_tools_container)
        val imageToolsContainer = findViewById<HorizontalScrollView>(R.id.image_tools_container)

        editToolsContainer.visibility = View.VISIBLE
        textToolsContainer.visibility = View.VISIBLE
        imageToolsContainer.visibility = View.GONE

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

    private fun showFontSelectionBottomSheet(textSticker: TextSticker) {
        val currentFont = currentSelectedFont ?: FontItem("Default", "default", "System", isSystemFont = true)

        val bottomSheet = FontSelectionBottomSheet.newInstance(currentFont)
        bottomSheet.setOnFontSelectedListener { selectedFont ->
            applyFontToSticker(textSticker, selectedFont)
        }
        bottomSheet.show(supportFragmentManager, "FontSelectionBottomSheet")
    }

    private fun applyFontToSticker(textSticker: TextSticker, fontItem: FontItem) {
        lifecycleScope.launch {
            val typeface = if (fontItem.typeface != null) {
                fontItem.typeface
            } else {
                fontManager.loadFont(fontItem)
            }

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
        val currentSize = getCurrentTextSize(textSticker)
        fontSizeController.setSize(currentSize)
        fontSizeController.show()
        isSizeControlVisible = true

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
            18
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTextColorController() {
        val textColorControlView = findViewById<View>(R.id.text_color_control)


        textColorController = TextColorController(textColorControlView) { newColor ->
            applyTextColorToCurrentSticker(newColor)
        }
    }

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
            Layout.Alignment.ALIGN_CENTER
        }

        textAlignmentController.setAlignmentWithoutCallback(currentAlignment)
        textAlignmentController.show()
        isAlignmentControlVisible = true

        updateAlignmentButtonState(true)

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
                currentSticker.setTextAlign(alignment)

                currentSticker.refreshLayout()

                stickerView.invalidate()

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
                textSticker.checkLineHeightApplied()
            }
            showLineHeightControl(textSticker)
        }
    }

    private fun showLineHeightControl(textSticker: TextSticker) {
        val currentLineHeight = getCurrentLineHeight(textSticker)
        lineHeightController.setLineHeightWithoutCallback(currentLineHeight)
        lineHeightController.show()
        isLineHeightControlVisible = true

        updateLineHeightButtonState(true)

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
            120
        }
    }

    private fun applyLineHeightToCurrentSticker(lineHeight: Int) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying line height: $lineHeight%")

                currentSticker.setLineHeightPercent(lineHeight)

                stickerView.invalidate()

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
        val currentSpacing = getCurrentLetterSpacing(textSticker)
        letterSpacingController.setLetterSpacingWithoutCallback(currentSpacing)
        letterSpacingController.show()
        isLetterSpacingControlVisible = true

        updateLetterSpacingButtonState(true)

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
            0f
        }
    }

    private fun applyLetterSpacingToCurrentSticker(spacing: Float) {
        val currentSticker = getCurrentSticker()
        if (currentSticker is FlexibleTextSticker) {
            try {
                Log.d("InvitationEditActivity", "Applying letter spacing: $spacing")

                currentSticker.setLetterSpacing(spacing)

                stickerView.invalidate()

                Handler(Looper.getMainLooper()).postDelayed({
                    stickerView.invalidate()
                }, 50)

                Log.d("InvitationEditActivity", "Letter spacing applied successfully")
            } catch (e: Exception) {
                Log.e("InvitationEditActivity", "Error applying letter spacing: ${e.message}", e)
            }
        }
    }

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
            0f
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

    private fun duplicateCurrentTextSticker(originalSticker: FlexibleTextSticker) {
        try {
            Log.d("InvitationEditActivity", "Starting text duplicate process")

            val offsetX = 20f
            val offsetY = 20f
            val duplicateSticker = originalSticker.createDuplicate(offsetX, offsetY)

            val duplicateMatrix = Matrix(duplicateSticker.matrix)

            stickerView.addSticker(duplicateSticker)

            stickerView.post {
                duplicateSticker.setMatrix(duplicateMatrix)

                hideAllStickerBorders()
                focusOnDuplicateSticker(duplicateSticker)
                Toast.makeText(this, "Text duplicate - offset đã được áp dụng", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error duplicating text: ${e.message}", e)
        }
    }

    private fun calculateSmartOffset(sticker: FlexibleTextSticker): Pair<Float, Float> {
        try {
            val realBoundsField = TextSticker::class.java.getDeclaredField("realBounds")
            realBoundsField.isAccessible = true
            val realBounds = realBoundsField.get(sticker) as Rect

            val matrix = sticker.matrix
            val values = FloatArray(9)
            matrix.getValues(values)
            val scaleX = values[Matrix.MSCALE_X]
            val scaleY = values[Matrix.MSCALE_Y]

            val actualWidth = realBounds.width() * Math.abs(scaleX)
            val actualHeight = realBounds.height() * Math.abs(scaleY)

            val offsetX = Math.max(actualWidth * 0.3f, 40f)
            val offsetY = Math.max(actualHeight * 0.3f, 40f)

            Log.d("InvitationEditActivity", "Smart offset calculated: ($offsetX, $offsetY) for size: ${actualWidth}x${actualHeight}")

            return Pair(offsetX, offsetY)

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error calculating smart offset: ${e.message}")
            return Pair(40f, 40f)
        }
    }


    private fun duplicateCurrentTextStickerWithAnimation(originalSticker: FlexibleTextSticker) {
        try {
            val duplicateSticker = originalSticker.createDuplicate(80f, 80f)

            stickerView.addSticker(duplicateSticker)

            stickerView.post {
                hideAllStickerBorders()

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
            val handlingStickerField = StickerView::class.java.getDeclaredField("handlingSticker")
            handlingStickerField.isAccessible = true
            handlingStickerField.set(stickerView, duplicateSticker)

            duplicateSticker.setShowBorder(true)

            updateUIControlsFromSticker(duplicateSticker)

            showTextEditTools()

            stickerView.invalidate()

            Log.d("InvitationEditActivity", "Duplicate sticker focused successfully")

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error in focusOnDuplicateSticker: ${e.message}", e)
        }
    }

    private fun updateUIControlsFromSticker(sticker: FlexibleTextSticker) {
        try {
            if (isSizeControlVisible) {
                updateSizeControllerFromSticker(sticker)
            }

            if (isCurvedTextControlVisible) {
                curvedTextController.setCurveAngleWithoutCallback(sticker.getCurveAngle())
            }

            if (isAlignmentControlVisible) {
                textAlignmentController.setAlignmentWithoutCallback(sticker.getTextAlignment())
            }

            updateBoldButtonState(sticker.isBold())
            updateItalicButtonState(sticker.isItalic())
            updateUppercaseButtonState(sticker.isUppercase())

            Log.d("InvitationEditActivity", "UI controls updated from duplicate sticker")

        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error updating UI controls: ${e.message}", e)
        }
    }

    private fun addImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_STORAGE_PERMISSION)
                return
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                return
            }
        }

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }
    private fun addImageSticker(bitmap: Bitmap) {
        try {
            val drawable = BitmapDrawable(resources, bitmap)

            val imageElement = TemplateElement.ImageElement(
                id = "user_added_image_${System.currentTimeMillis()}",
                zIndex = stickerView.stickerCount + 1,
                bounds = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                isEditable = true,
                isVisible = true,
                rotation = 0f,
                scaleX = 1f,
                scaleY = 1f,
                pivotX = 0.5f,
                pivotY = 0.5f,
                bitmap = bitmap,
                isUserReplaceable = true
            )

            val sticker = LockableDrawableSticker(drawable)

            val viewWidth = stickerView.width.toFloat()
            val viewHeight = stickerView.height.toFloat()

            val scale = Math.min(
                viewWidth * 0.7f / bitmap.width,
                viewHeight * 0.7f / bitmap.height
            )

            val matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(
                (viewWidth - bitmap.width * scale) / 2,
                (viewHeight - bitmap.height * scale) / 2
            )

            sticker.setMatrix(matrix)

            stickerView.addSticker(sticker)

            showImageEditTools()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error adding image sticker: ${e.message}")
        }
    }

    private fun showImageEditTools() {
        val editToolsContainer = findViewById<LinearLayout>(R.id.edit_tools_container)
        val textToolsContainer = findViewById<HorizontalScrollView>(R.id.text_tools_container)
        val imageToolsContainer = findViewById<HorizontalScrollView>(R.id.image_tools_container)

        editToolsContainer.visibility = View.VISIBLE
        textToolsContainer.visibility = View.GONE
        imageToolsContainer.visibility = View.VISIBLE

        isImageLocked = false
        try {
            findViewById<ImageButton>(R.id.btn_lockImage)?.let {
                updateLockButtonState(false)
            }
        } catch (e: Exception) {
            Log.e("ImageEditing", "Error updating lock button: ${e.message}")
        }

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
        findViewById<ImageButton>(R.id.btn_delete_image)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                stickerView.remove(currentSticker)
                hideAllEditTools()
            }
        }

        findViewById<ImageButton>(R.id.btn_flipImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                flipImageSticker(currentSticker)
            }
        }

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

        findViewById<TextView>(R.id.btn_removeBackgroundImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                Toast.makeText(this, "Chức năng xóa nền đang được phát triển", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<ImageButton>(R.id.btn_lockImage)?.setOnClickListener {
            val currentSticker = getCurrentSticker()
            if (currentSticker != null && currentSticker !is TextSticker) {
                isImageLocked = !isImageLocked

                lockSticker(currentSticker, isImageLocked)

                updateLockButtonState(isImageLocked)

                val message = if (isImageLocked)
                    "Đã khóa zoom và xoay ảnh"
                else
                    "Đã mở khóa zoom và xoay ảnh"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun flipImageSticker(sticker: Sticker) {
        try {
            val matrix = Matrix(sticker.matrix)
            matrix.preScale(-1f, 1f, sticker.width / 2f, sticker.height / 2f)
            sticker.setMatrix(matrix)
            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error flipping image: ${e.message}")
        }
    }

    private fun rotateImageSticker(sticker: Sticker, degrees: Float) {
        try {
            val mappedBoundPoints = sticker.getMappedBoundPoints()


            var sumX = 0f
            var sumY = 0f
            for (i in 0 until mappedBoundPoints.size step 2) {
                sumX += mappedBoundPoints[i]
                sumY += mappedBoundPoints[i + 1]
            }
            val centerX = sumX / 4
            val centerY = sumY / 4

            Log.d("ImageRotation", "Rotating around calculated center: ($centerX, $centerY)")

            val matrix = Matrix(sticker.matrix)
            matrix.postRotate(degrees, centerX, centerY)

            sticker.setMatrix(matrix)

            stickerView.invalidate()
        } catch (e: Exception) {
            Log.e("InvitationEditActivity", "Error rotating image: ${e.message}", e)
        }
    }



    private fun lockSticker(sticker: Sticker, locked: Boolean) {
        when (sticker) {
            is LockableDrawableSticker -> sticker.isLocked = locked
            else -> {
                val stickerId = sticker.hashCode()
                if (locked) {
                    lockedStickers[stickerId] = true
                } else {
                    lockedStickers.remove(stickerId)
                }
            }
        }

        stickerView.invalidate()
    }


    private fun isStickerLocked(sticker: Sticker): Boolean {
        return when {
            sticker is LockableDrawableSticker && sticker.isLocked -> true
            lockedStickers.containsKey(sticker.hashCode()) -> true
            else -> false
        }
    }

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

    override fun onDestroy() {
        super.onDestroy()
        svgTemplateLoader.destroy()
    }
}