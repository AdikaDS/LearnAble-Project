package com.adika.learnable.util

import android.view.View
import androidx.fragment.app.FragmentManager
import com.adika.learnable.repository.TextScaleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextScaleManager @Inject constructor(
    private val textScaleRepository: TextScaleRepository
) {
    fun applyTextScaleToView(view: View) {
        val scale = textScaleRepository.getScale()
        TextScaleUtils.applyScaleToHierarchy(view, scale)
    }

    fun applyTextScaleToAllFragments(fragmentManager: FragmentManager) {
        val fragments = fragmentManager.fragments
        fragments.forEach { fragment ->
            if (fragment.isVisible && fragment.view != null) {
                applyTextScaleToView(fragment.requireView())
            }
        }
    }
}