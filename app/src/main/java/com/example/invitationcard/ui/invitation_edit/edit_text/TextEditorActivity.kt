package com.example.invitationcard.ui.invitation_edit.edit_text

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.invitationcard.R

class TextEditorActivity : AppCompatActivity() {

    private lateinit var textEditor: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnClear: Button
    private lateinit var btnUpdate: Button
    private lateinit var rootView: View

    companion object {
        const val EXTRA_TEXT = "text"
        const val RESULT_TEXT = "result_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_text_editor)

        // Thiết lập chế độ hiển thị bàn phím
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        rootView = findViewById(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textEditor = findViewById(R.id.text_editor)
        btnCancel = findViewById(R.id.btn_cancel)
        btnClear = findViewById(R.id.btn_clear)
        btnUpdate = findViewById(R.id.btn_update)

//        // Đặt background mặc định (tùy chọn)
//        textEditor.background = ContextCompat.getDrawable(this, R.drawable.text_editor_background)

        textEditor.setOnFocusChangeListener { v, hasFocus ->
            val drawableRes = if (hasFocus) R.drawable.text_editor_background_selected else R.drawable.text_editor_background
            v.background = ContextCompat.getDrawable(this, drawableRes)
        }

        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        if (text == "enter text...") {
            textEditor.hint = text
        } else {
            textEditor.setText(text)
            textEditor.selectAll()
        }

        textEditor.requestFocus()

        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnClear.setOnClickListener {
            textEditor.text.clear()
        }

        btnUpdate.setOnClickListener {
            val resultText = textEditor.text.toString()
            val resultIntent = Intent().apply {
                putExtra(RESULT_TEXT, resultText)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        setupTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (isTouchOutsideEditText(event.rawX.toInt(), event.rawY.toInt())) {
                    clearFocusAndHideKeyboard()
                    return@setOnTouchListener true
                }
            }
            false
        }

        textEditor.setOnTouchListener { v, event ->
            v.performClick()
            v.onTouchEvent(event)
        }
    }

    private fun isTouchOutsideEditText(x: Int, y: Int): Boolean {
        val location = IntArray(2)
        textEditor.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + textEditor.width
        val bottom = top + textEditor.height
        return x < left || x > right || y < top || y > bottom
    }

    private fun clearFocusAndHideKeyboard() {
        textEditor.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(textEditor.windowToken, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}