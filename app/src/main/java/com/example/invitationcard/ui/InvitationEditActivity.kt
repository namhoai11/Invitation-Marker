package com.example.invitationcard.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Layout
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.invitationcard.R
import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.TextSticker

class InvitationEditActivity : AppCompatActivity() {

    private lateinit var stickerView: StickerView
    private lateinit var mainContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_invitation_edit)

        stickerView = findViewById(R.id.sticker_view)
        mainContainer = findViewById(R.id.main)

        // Áp dụng padding cho hệ thống insets (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Xử lý click nút Add (+)
        findViewById<View>(R.id.btn_add).setOnClickListener {
            showAddItemDialog()
        }

        // Thiết lập listener cho StickerView
        setupStickerViewListeners()

        // Đảm bảo StickerView không bị khóa và hiển thị viền
        stickerView.setLocked(false)

        // QUAN TRỌNG: Đặt showBorder = true bằng phản chiếu (reflection)
        try {
            val field = StickerView::class.java.getDeclaredField("showBorder")
            field.isAccessible = true
            field.setBoolean(stickerView, true)

            // Tùy chọn: Thay đổi màu viền nếu muốn
            val borderPaintField = StickerView::class.java.getDeclaredField("borderPaint")
            borderPaintField.isAccessible = true
            val borderPaint = borderPaintField.get(stickerView) as android.graphics.Paint
            borderPaint.color = Color.GREEN
            borderPaint.alpha = 255
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Xử lý sự kiện khi tap vào không gian trống
        setupBackgroundTouchListener()
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
            // Sử dụng reflection để đặt handlingSticker = null
            val field = StickerView::class.java.getDeclaredField("handlingSticker")
            field.isAccessible = true
            field.set(stickerView, null)

            // Ẩn edit tools
            hideAllEditTools()

            // Vẽ lại view
            stickerView.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupStickerViewListeners() {
        stickerView.setOnStickerOperationListener(object : StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                // Hiện toolbar tương ứng khi thêm sticker
                if (sticker is TextSticker) {
                    showTextEditTools()
                }
                // Đảm bảo sticker được vẽ với viền
                stickerView.invalidate()
            }

            override fun onStickerClicked(sticker: Sticker) {
                // Hiện toolbar tương ứng khi chọn sticker
                if (sticker is TextSticker) {
                    showTextEditTools()
                }
                // Đảm bảo sticker được vẽ với viền
                stickerView.invalidate()
            }

            override fun onStickerDeleted(sticker: Sticker) {
                // Ẩn toolbar nếu không còn sticker nào
                if (stickerView.stickerCount == 0) {
                    hideAllEditTools()
                }
            }

            override fun onStickerDragFinished(sticker: Sticker) {
                // Xử lý khi kéo sticker xong
            }

            override fun onStickerTouchedDown(sticker: Sticker) {
                // Xử lý khi bắt đầu chạm vào sticker
            }

            override fun onStickerZoomFinished(sticker: Sticker) {
                // Xử lý khi zoom sticker xong
            }

            override fun onStickerFlipped(sticker: Sticker) {
                // Xử lý khi flip sticker
            }

            override fun onStickerDoubleTapped(sticker: Sticker) {
                // Khi double tap vào text sticker, hiện dialog chỉnh sửa
                if (sticker is TextSticker) {
                    showEditTextDialog(sticker)
                }
            }
        })
    }

    private fun showAddItemDialog() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Sự kiện nút đóng dialog (X)
        view.findViewById<View>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        // Sự kiện chọn Text
        view.findViewById<LinearLayout>(R.id.btn_add_text).setOnClickListener {
            dialog.dismiss()
            addText()
        }

        // Sự kiện chọn Sticker (bổ sung sau)
        view.findViewById<LinearLayout>(R.id.btn_add_sticker).setOnClickListener {
            dialog.dismiss()
            // TODO: Hiển thị giao diện chọn sticker
        }

        // Sự kiện chọn Image (bổ sung sau)
        view.findViewById<LinearLayout>(R.id.btn_add_image).setOnClickListener {
            dialog.dismiss()
            // TODO: Hiển thị giao diện chọn ảnh
        }

        dialog.show()
    }

    private fun addText() {
        // Tạo trực tiếp text sticker với nội dung placeholder
        createTextSticker("Enter text...")
    }

    private fun showEditTextDialog(textSticker: TextSticker) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chỉnh sửa văn bản")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(textSticker.text)
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val text = input.text.toString()
            if (text.isNotEmpty()) {
                textSticker.setText(text)
                textSticker.setTextColor(Color.BLACK)
                stickerView.invalidate() // Cập nhật lại view
            }
        }

        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun createTextSticker(text: String) {
        val textSticker = TextSticker(this)
        textSticker.apply {
            setText(text)
            setTextAlign(Layout.Alignment.ALIGN_CENTER)
            setTypeface(Typeface.DEFAULT)
            setTextColor(Color.GRAY)
            resizeText()
        }

        // Thêm sticker vào StickerView
        stickerView.addSticker(textSticker)

        // Hiện toolbar chỉnh sửa text và ẩn toolbar ảnh
        showTextEditTools()
    }

    private fun showTextEditTools() {
        val editToolsContainer = findViewById<FrameLayout>(R.id.edit_tools_container)
        val textToolsContainer = findViewById<HorizontalScrollView>(R.id.text_tools_container)
        val imageToolsContainer = findViewById<HorizontalScrollView>(R.id.image_tools_container)

        editToolsContainer.visibility = View.VISIBLE
        textToolsContainer.visibility = View.VISIBLE
        imageToolsContainer.visibility = View.GONE
    }

    private fun hideAllEditTools() {
        val editToolsContainer = findViewById<FrameLayout>(R.id.edit_tools_container)
        editToolsContainer.visibility = View.GONE
    }
}