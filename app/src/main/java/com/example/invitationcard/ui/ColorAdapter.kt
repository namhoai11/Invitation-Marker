package com.example.invitationcard.ui


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.invitationcard.R
import com.example.invitationcard.model.ColorItem

class ColorAdapter(
    private var colors: List<ColorItem>,
    private val onColorSelected: (ColorItem) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedPosition = -1

    class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorView: View = itemView.findViewById(R.id.color_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val colorItem = colors[position]

        // Set color
        holder.colorView.backgroundTintList =
            android.content.res.ColorStateList.valueOf(colorItem.colorValue)

        // Set selection state
        holder.colorView.isSelected = position == selectedPosition

        // Handle click
        holder.colorView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position

            // Update UI
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            // Callback
            onColorSelected(colorItem)
        }
    }

    override fun getItemCount(): Int = colors.size

//    fun updateColors(newColors: List<ColorItem>) {
//        colors = newColors
//        notifyDataSetChanged()
//    }

    fun setSelectedColor(colorValue: Int) {
        val newPosition = colors.indexOfFirst { it.colorValue == colorValue }
        if (newPosition != -1) {
            val previousPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
        }
    }
}
