package com.example.invitationcard.utils

import android.graphics.drawable.Drawable
import com.xiaopo.flying.sticker.DrawableSticker

class LockableDrawableSticker(drawable: Drawable) : DrawableSticker(drawable) {

    // Thêm thuộc tính isLocked
    var isLocked: Boolean = false

}