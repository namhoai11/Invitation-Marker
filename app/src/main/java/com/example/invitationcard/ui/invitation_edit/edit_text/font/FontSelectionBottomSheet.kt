package com.example.invitationcard.ui.invitation_edit.edit_text.font

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.invitationcard.R
import com.example.invitationcard.model.FontItem
import com.example.invitationcard.utils.FontManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class FontSelectionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var recyclerFonts: RecyclerView
    private lateinit var currentFontName: TextView
    private lateinit var fontManager: FontManager
    private lateinit var fontAdapter: FontAdapter

    private var onFontSelectedListener: ((FontItem) -> Unit)? = null
    private var currentFont: FontItem? = null

    companion object {
        fun newInstance(currentFont: FontItem? = null): FontSelectionBottomSheet {
            val fragment = FontSelectionBottomSheet()
            fragment.currentFont = currentFont
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_font_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerFonts = view.findViewById(R.id.recycler_fonts)
        currentFontName = view.findViewById(R.id.current_font_name)

        fontManager = FontManager(requireContext())

        setupViews()
        setupFontList()
    }

    private fun setupViews() {
        // Display current font
        currentFont?.let { font ->
            currentFontName.text = font.displayName
            font.typeface?.let { typeface ->
                currentFontName.typeface = typeface
            }
        }

        // Handle current font item click
        view?.findViewById<View>(R.id.current_font_item)?.setOnClickListener {
            currentFont?.let { font ->
                onFontSelectedListener?.invoke(font)
                dismiss()
            }
        }
    }

    private fun setupFontList() {
        val fonts = fontManager.getPopularFonts()
        val currentFontName = currentFont?.name ?: ""

        fontAdapter = FontAdapter(fonts, currentFontName) { selectedFont ->
            loadAndApplyFont(selectedFont)
        }

        recyclerFonts.layoutManager = LinearLayoutManager(requireContext())
        recyclerFonts.adapter = fontAdapter

        // Load fonts asynchronously
        loadFontsAsync(fonts)
    }

    private fun loadFontsAsync(fonts: List<FontItem>) {
        lifecycleScope.launch {
            fonts.forEach { font ->
                if (!font.isSystemFont && font.typeface == null) {
                    fontManager.loadFont(font)
                    // Update the adapter when font is loaded
                    requireActivity().runOnUiThread {
                        val position = fonts.indexOf(font)
                        if (position != -1) {
                            fontAdapter.notifyItemChanged(position)
                        }
                    }
                }
            }
        }
    }

    private fun loadAndApplyFont(selectedFont: FontItem) {
        lifecycleScope.launch {
            // Load font if not already loaded
            if (selectedFont.typeface == null && !selectedFont.isSystemFont) {
                fontManager.loadFont(selectedFont)
            }

            // Apply font
            requireActivity().runOnUiThread {
                onFontSelectedListener?.invoke(selectedFont)
                dismiss()
            }
        }
    }

    fun setOnFontSelectedListener(listener: (FontItem) -> Unit) {
        onFontSelectedListener = listener
    }
}