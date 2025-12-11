package com.adika.learnable.view.core

import androidx.fragment.app.Fragment
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.TextScaleUtils
import com.adika.learnable.util.VibrationHelper

abstract class BaseFragment : Fragment() {

    private var textScaleRepository: TextScaleRepository? = null
    private var isTextScalingSetup = false
    protected val vibrationHelper: VibrationHelper by lazy {
        VibrationHelper(requireContext())
    }

    override fun onResume() {
        super.onResume()

        if (isTextScalingSetup) {
            applyTextScale()
        }
    }

    protected fun setupTextScaling() {
        if (isTextScalingSetup) return

        textScaleRepository = TextScaleRepository(requireContext())

        applyTextScale()

        parentFragmentManager.setFragmentResultListener(
            "text_scale_changed",
            viewLifecycleOwner
        ) { _, _ ->
            applyTextScale()
        }

        childFragmentManager.setFragmentResultListener(
            "text_scale_changed",
            viewLifecycleOwner
        ) { _, _ ->
            applyTextScale()
        }

        isTextScalingSetup = true
    }

    open fun applyTextScale() {
        view?.let { rootView ->
            val scale = textScaleRepository?.getScale() ?: 1.0f
            TextScaleUtils.applyScaleToHierarchy(rootView, scale)
        }
    }

    private fun getCurrentTextScale(): Float = textScaleRepository?.getScale() ?: 1.0f
}
