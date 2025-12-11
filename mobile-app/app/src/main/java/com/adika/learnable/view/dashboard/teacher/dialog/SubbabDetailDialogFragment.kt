package com.adika.learnable.view.dashboard.teacher.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogSubbabDetailBinding
import com.adika.learnable.model.SubBab
import com.bumptech.glide.Glide

class SubbabDetailDialogFragment : DialogFragment() {
    private var _binding: DialogSubbabDetailBinding? = null
    private val binding get() = _binding!!

    private var subBab: SubBab? = null
    private var lessonTitle: String? = null
    private var onViewMateriClick: ((String) -> Unit)? = null
    private var onViewVideoClick: ((String) -> Unit)? = null
    private var onViewQuizClick: ((SubBab) -> Unit)? = null

    companion object {
        fun newInstance(
            subBab: SubBab,
            lessonTitle: String? = null,
            onViewMateriClick: ((String) -> Unit)? = null,
            onViewVideoClick: ((String) -> Unit)? = null,
            onViewQuizClick: ((SubBab) -> Unit)? = null
        ): SubbabDetailDialogFragment {
            return SubbabDetailDialogFragment().apply {
                this.subBab = subBab
                this.lessonTitle = lessonTitle
                this.onViewMateriClick = onViewMateriClick
                this.onViewVideoClick = onViewVideoClick
                this.onViewQuizClick = onViewQuizClick
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSubbabDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        subBab?.let { subBab ->
            binding.apply {

                tvSubbabTitle.text = subBab.title

                if (!lessonTitle.isNullOrBlank()) {
                    tvLessonTitle.text = lessonTitle
                    tvLessonTitle.visibility = View.VISIBLE
                } else {
                    tvLessonTitle.visibility = View.GONE
                }

                if (subBab.coverImage.isNotBlank()) {
                    Glide.with(requireContext())
                        .load(subBab.coverImage)
                        .placeholder(R.drawable.icon_dummy_subject)
                        .error(R.drawable.icon_dummy_subject)
                        .into(ivIllustration)
                } else {
                    ivIllustration.setImageResource(R.drawable.icon_dummy_subject)
                }

                val pdfUrl = subBab.mediaUrls["pdfLesson"]
                if (!pdfUrl.isNullOrBlank()) {
                    layoutMateri.visibility = View.VISIBLE

                    val fileName = pdfUrl.substringAfterLast("/").substringBefore("?")
                    val buttonText = if (fileName.isNotEmpty() && fileName.contains(".")) {
                        "${getString(R.string.lihat_materi)}$fileName"
                    } else {
                        getString(R.string.lihat_materi)
                    }
                    btnViewMateri.text = buttonText
                    btnViewMateri.setOnClickListener {
                        onViewMateriClick?.invoke(pdfUrl)
                        dismiss()
                    }
                } else {
                    layoutMateri.visibility = View.GONE
                }

                val videoUrl = subBab.mediaUrls["video"]
                if (!videoUrl.isNullOrBlank()) {
                    layoutVideo.visibility = View.VISIBLE

                    val fileName = videoUrl.substringAfterLast("/").substringBefore("?")
                    val buttonText = if (fileName.isNotEmpty() && fileName.contains(".")) {
                        "${getString(R.string.lihat_video)}$fileName"
                    } else {
                        getString(R.string.lihat_video)
                    }
                    btnViewVideo.text = buttonText
                    btnViewVideo.setOnClickListener {
                        onViewVideoClick?.invoke(videoUrl)
                        dismiss()
                    }
                } else {
                    layoutVideo.visibility = View.GONE
                }

                btnDetailKuis.setOnClickListener {
                    onViewQuizClick?.invoke(subBab)
                    dismiss()
                }

                btnBackArrow.setOnClickListener {
                    dismiss()
                }

                btnBack.setOnClickListener {
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}