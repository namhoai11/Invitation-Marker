package com.example.invitationcard.ui.invitation_edit.edit_text.font

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.invitationcard.R
import com.example.invitationcard.model.FontItem

class FontAdapter(
    private val fonts: List<FontItem>,
    private var selectedFontName: String = "",
    private val onFontSelected: (FontItem) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fontName: TextView = itemView.findViewById(R.id.font_name)
        val selectedIcon: ImageView = itemView.findViewById(R.id.selected_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_font, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val font = fonts[position]

        holder.fontName.text = font.displayName

        // Apply font to the name text if available
        font.typeface?.let { typeface ->
            holder.fontName.typeface = typeface
        }

        // Show/hide selected icon
        if (font.name == selectedFontName) {
            holder.selectedIcon.visibility = View.VISIBLE
        } else {
            holder.selectedIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val oldSelectedPosition = fonts.indexOfFirst { it.name == selectedFontName }
            selectedFontName = font.name

            // Notify changes for selection state
            if (oldSelectedPosition != -1) {
                notifyItemChanged(oldSelectedPosition)
            }
            notifyItemChanged(position)

            onFontSelected(font)
        }
    }

    override fun getItemCount(): Int = fonts.size

    fun updateSelectedFont(fontName: String) {
        val oldSelectedPosition = fonts.indexOfFirst { it.name == selectedFontName }
        val newSelectedPosition = fonts.indexOfFirst { it.name == fontName }

        selectedFontName = fontName

        if (oldSelectedPosition != -1) {
            notifyItemChanged(oldSelectedPosition)
        }
        if (newSelectedPosition != -1) {
            notifyItemChanged(newSelectedPosition)
        }
    }
}