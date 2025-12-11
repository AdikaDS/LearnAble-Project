package com.adika.learnable.util

import android.annotation.SuppressLint
import android.view.View
import androidx.fragment.app.Fragment
import com.adika.learnable.R
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.view.dashboard.student.dialog.TextScaleDialogFragment

fun Fragment.setupAccessibilityButton(accessibilityButton: View) {
    accessibilityButton.setOnClickListener {
        TextScaleDialogFragment().show(parentFragmentManager, "TextScaleDialog")
    }

    if (accessibilityButton.id == R.id.btnAccessibility) {
        setupFloatingAccessibilityButton(accessibilityButton)
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun Fragment.setupFloatingAccessibilityButton(button: View) {
    var dX = 0f
    var dY = 0f
    var startX = 0f
    var startY = 0f
    var isDragging = false
    val touchSlop = 8 * resources.displayMetrics.density

    button.setOnTouchListener { v, event ->
        val parent = v.parent as View
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                v.animate().alpha(1f).setDuration(100).start()
                dX = v.x - event.rawX
                dY = v.y - event.rawY
                startX = v.x
                startY = v.y
                isDragging = false
                false
            }

            android.view.MotionEvent.ACTION_MOVE -> {
                val newX = event.rawX + dX
                val newY = event.rawY + dY
                if (!isDragging && (kotlin.math.abs(newX - startX) > touchSlop || kotlin.math.abs(
                        newY - startY
                    ) > touchSlop)
                ) {
                    isDragging = true
                }
                if (isDragging) {

                    val maxX = (parent.width - v.width).toFloat()
                    val maxY = (parent.height - v.height).toFloat()
                    v.x = newX.coerceIn(0f, maxX)
                    v.y = newY.coerceIn(0f, maxY)
                    true
                } else {
                    false
                }
            }

            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                v.animate().alpha(0.75f).setDuration(200).start()
                if (isDragging) {

                    val inset = 6 * resources.displayMetrics.density
                    val snapToLeft = v.x + v.width / 2f < parent.width / 2f
                    val targetX = if (snapToLeft) -inset else (parent.width - v.width + inset)
                    val targetY = v.y.coerceIn(0f, (parent.height - v.height).toFloat())
                    v.animate()
                        .x(targetX)
                        .y(targetY)
                        .setDuration(180)
                        .start()
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }
}

fun Fragment.setupTextScaling() {
    parentFragmentManager.setFragmentResultListener(
        "text_scale_changed",
        viewLifecycleOwner
    ) { _, _ ->

        view?.let { rootView ->
            val textScaleRepository = TextScaleRepository(requireContext())
            val scale = textScaleRepository.getScale()
            TextScaleUtils.applyScaleToHierarchy(rootView, scale)
        }
    }
}