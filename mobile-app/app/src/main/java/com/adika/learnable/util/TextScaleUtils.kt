package com.adika.learnable.util

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.adika.learnable.R

object TextScaleUtils {

    fun applyScaleToHierarchy(root: View, scaleFactor: Float) {
        applyScaleRecursively(root, scaleFactor)
    }

    private fun applyScaleRecursively(view: View, scaleFactor: Float) {
        if (view is TextView) {
            val tagKey = R.id.tag_original_text_size
            val originalSizePx = (view.getTag(tagKey) as? Float) ?: view.textSize.also {
                view.setTag(tagKey, it)
            }
            val newSizePx = originalSizePx * scaleFactor
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSizePx)
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyScaleRecursively(view.getChildAt(i), scaleFactor)
            }
        }
    }
}