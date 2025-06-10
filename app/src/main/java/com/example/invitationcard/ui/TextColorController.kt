package com.example.invitationcard.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.invitationcard.R
import com.example.invitationcard.model.ColorItem
import com.example.invitationcard.model.ColorType

class TextColorController(
    private val controlView: View,
    private val onColorChanged: (Int) -> Unit
) {
    private val expandedContainer: View = controlView.findViewById(R.id.expanded_colors_container)
    private val btnMoreColors: TextView = controlView.findViewById(R.id.btn_more_colors)
    private val rootScrollView: ScrollView = controlView as ScrollView

    // Basic color views
    private val colorBlack: View = controlView.findViewById(R.id.color_black)
    private val colorOrange: View = controlView.findViewById(R.id.color_orange)
    private val colorPink: View = controlView.findViewById(R.id.color_pink)
    private val colorGreen: View = controlView.findViewById(R.id.color_green)
    private val colorRed: View = controlView.findViewById(R.id.color_red)

    // THÊM: Color picker wheel
    private val colorPickerWheel: ImageView = controlView.findViewById(R.id.color_picker_wheel)

    // RecyclerViews
    private val recyclerDefaultColors: RecyclerView = controlView.findViewById(R.id.recycler_default_colors)
    private val recyclerFoilColors: RecyclerView = controlView.findViewById(R.id.recycler_foil_colors)
    private val recyclerGlitterColors: RecyclerView = controlView.findViewById(R.id.recycler_glitter_colors)

    // Adapters
    private lateinit var defaultColorsAdapter: ColorAdapter
    private lateinit var foilColorsAdapter: ColorAdapter
    private lateinit var glitterColorsAdapter: ColorAdapter

    private var isExpanded = false
    private var selectedColor = Color.BLACK

    init {
        val params = rootScrollView.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        rootScrollView.layoutParams = params

        expandedContainer.visibility = View.GONE

        setupBasicColors()
        setupRecyclerViews()
        setupExpandButton()
        setupColorPickerWheel() // THÊM: Setup color picker wheel
        setupScrollView()
    }

    // THÊM: Setup color picker wheel click
    private fun setupColorPickerWheel() {
        colorPickerWheel.setOnClickListener {
            showColorPickerDialog()
        }
    }

    // THÊM: Show color picker dialog
    private fun showColorPickerDialog() {
        val colorPickerDialog = ColorPickerDialog(controlView.context) { selectedColor ->
            selectColor(selectedColor)
            // Reset basic color selection since this is a custom color
            resetBasicColorSelection()
            // Update adapters
            defaultColorsAdapter.setSelectedColor(selectedColor)
            foilColorsAdapter.setSelectedColor(selectedColor)
            glitterColorsAdapter.setSelectedColor(selectedColor)
        }
        colorPickerDialog.show()
    }

    // THÊM: Reset basic color selection
    private fun resetBasicColorSelection() {
        listOf(colorBlack, colorOrange, colorPink, colorGreen, colorRed).forEach {
            it.isSelected = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupScrollView() {
        rootScrollView.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            true
        }

        recyclerDefaultColors.isNestedScrollingEnabled = false
        recyclerFoilColors.isNestedScrollingEnabled = false
        recyclerGlitterColors.isNestedScrollingEnabled = false
    }

    private fun setupBasicColors() {
        val basicColors = mapOf(
            colorBlack to Color.BLACK,
            colorOrange to Color.parseColor("#FF9800"),
            colorPink to Color.parseColor("#E91E63"),
            colorGreen to Color.parseColor("#4CAF50"),
            colorRed to Color.parseColor("#F44336")
        )

        basicColors.forEach { (view, color) ->
            view.setOnClickListener {
                selectColor(color)
                updateBasicColorSelection(view)
            }
        }
    }

    private fun setupRecyclerViews() {
        recyclerDefaultColors.layoutManager = GridLayoutManager(controlView.context, 6)
        defaultColorsAdapter = ColorAdapter(getDefaultColors()) { colorItem ->
            selectColor(colorItem.colorValue)
        }
        recyclerDefaultColors.adapter = defaultColorsAdapter

        recyclerFoilColors.layoutManager = GridLayoutManager(controlView.context, 6)
        foilColorsAdapter = ColorAdapter(getFoilColors()) { colorItem ->
            selectColor(colorItem.colorValue)
        }
        recyclerFoilColors.adapter = foilColorsAdapter

        recyclerGlitterColors.layoutManager = GridLayoutManager(controlView.context, 6)
        glitterColorsAdapter = ColorAdapter(getGlitterColors()) { colorItem ->
            selectColor(colorItem.colorValue)
        }
        recyclerGlitterColors.adapter = glitterColorsAdapter
    }

    private fun setupExpandButton() {
        btnMoreColors.setOnClickListener {
            toggleExpanded()
        }
    }

    private fun toggleExpanded() {
        isExpanded = !isExpanded

        rootScrollView.post {
            val params = rootScrollView.layoutParams
            if (isExpanded) {
                params.height = dpToPx(300)
                rootScrollView.layoutParams = params
                expandedContainer.visibility = View.VISIBLE
                adjustRecyclerViewHeights()
            } else {
                expandedContainer.visibility = View.GONE
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                rootScrollView.layoutParams = params
            }
        }
    }

    private fun adjustRecyclerViewHeights() {
        val defaultColors = getDefaultColors()
        val defaultRowCount = Math.ceil(defaultColors.size / 6.0).toInt()
        val defaultHeight = defaultRowCount * dpToPx(52)
        recyclerDefaultColors.layoutParams.height = defaultHeight
        recyclerDefaultColors.requestLayout()

        val foilColors = getFoilColors()
        val foilRowCount = Math.ceil(foilColors.size / 6.0).toInt()
        val foilHeight = foilRowCount * dpToPx(52)
        recyclerFoilColors.layoutParams.height = foilHeight
        recyclerFoilColors.requestLayout()

        val glitterColors = getGlitterColors()
        val glitterRowCount = Math.ceil(glitterColors.size / 6.0).toInt()
        val glitterHeight = glitterRowCount * dpToPx(52)
        recyclerGlitterColors.layoutParams.height = glitterHeight
        recyclerGlitterColors.requestLayout()
    }

    private fun dpToPx(dp: Int): Int {
        val scale = controlView.context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    private fun selectColor(color: Int) {
        selectedColor = color
        onColorChanged(color)
    }

    private fun updateBasicColorSelection(selectedView: View) {
        listOf(colorBlack, colorOrange, colorPink, colorGreen, colorRed).forEach {
            it.isSelected = false
        }
        selectedView.isSelected = true
    }

    private fun getDefaultColors(): List<ColorItem> {
        return listOf(
            ColorItem(Color.parseColor("#FFC107"), "Yellow", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#2196F3"), "Blue", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#FFFFFF"), "White", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#9C27B0"), "Purple", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#673AB7"), "Deep Purple", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#FFCCCB"), "Light Red", ColorType.DEFAULT),

            ColorItem(Color.parseColor("#E91E63"), "Pink", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#F44336"), "Red", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#D32F2F"), "Dark Red", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#8D6E63"), "Brown", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#FF5722"), "Deep Orange", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#795548"), "Brown", ColorType.DEFAULT),

            ColorItem(Color.parseColor("#FDD835"), "Light Yellow", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#689F38"), "Light Green", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#7986CB"), "Indigo", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#9575CD"), "Medium Purple", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#512DA8"), "Deep Purple", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#1976D2"), "Blue", ColorType.DEFAULT),

            ColorItem(Color.parseColor("#0D47A1"), "Dark Blue", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#00ACC1"), "Cyan", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#616161"), "Grey", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#BDBDBD"), "Light Grey", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#FFEB3B"), "Bright Yellow", ColorType.DEFAULT),
            ColorItem(Color.parseColor("#4CAF50"), "Green", ColorType.DEFAULT)
        )
    }

    private fun getFoilColors(): List<ColorItem> {
        return listOf(
            ColorItem(Color.parseColor("#FFD700"), "Gold", ColorType.FOIL),
            ColorItem(Color.parseColor("#CD7F32"), "Bronze", ColorType.FOIL),
            ColorItem(Color.parseColor("#4682B4"), "Steel Blue", ColorType.FOIL),
            ColorItem(Color.parseColor("#DDA0DD"), "Plum", ColorType.FOIL),
            ColorItem(Color.parseColor("#DC143C"), "Crimson", ColorType.FOIL),
            ColorItem(Color.parseColor("#F0E68C"), "Khaki", ColorType.FOIL),
            ColorItem(Color.parseColor("#C0C0C0"), "Silver", ColorType.FOIL)
        )
    }

    private fun getGlitterColors(): List<ColorItem> {
        return listOf(
            ColorItem(Color.parseColor("#FFD700"), "Gold Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#C0C0C0"), "Silver Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#000000"), "Black Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#00CED1"), "Turquoise Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#228B22"), "Forest Green Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#FF69B4"), "Hot Pink Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#FF1493"), "Deep Pink Glitter", ColorType.GLITTER),
            ColorItem(Color.parseColor("#8A2BE2"), "Blue Violet Glitter", ColorType.GLITTER)
        )
    }

    fun setColor(color: Int) {
        selectedColor = color

        val basicColors = mapOf(
            colorBlack to Color.BLACK,
            colorOrange to Color.parseColor("#FF9800"),
            colorPink to Color.parseColor("#E91E63"),
            colorGreen to Color.parseColor("#4CAF50"),
            colorRed to Color.parseColor("#F44336")
        )

        val basicColorView = basicColors.entries.find { it.value == color }?.key
        if (basicColorView != null) {
            updateBasicColorSelection(basicColorView)
        } else {
            resetBasicColorSelection()
        }

        defaultColorsAdapter.setSelectedColor(color)
        foilColorsAdapter.setSelectedColor(color)
        glitterColorsAdapter.setSelectedColor(color)
    }

    fun show() {
        val params = rootScrollView.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        rootScrollView.layoutParams = params

        if (!isExpanded) {
            expandedContainer.visibility = View.GONE
        }

        controlView.visibility = View.VISIBLE
    }

    fun hide() {
        controlView.visibility = View.GONE
        isExpanded = false
        expandedContainer.visibility = View.GONE

        val params = rootScrollView.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        rootScrollView.layoutParams = params
    }

    fun getCurrentColor(): Int = selectedColor
}
