package com.adika.learnable.view

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.BottomSheetSelectRoleBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectRoleBottomSheet(
    private val onRoleSelected: (String) -> Unit,
    private val isFromLogin: Boolean = false,
    private val onCancel: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSelectRoleBinding? = null
    private val binding get() = _binding!!

    companion object {
        object UserRole {
            const val STUDENT = "student"
            const val TEACHER = "teacher"
            const val PARENT = "parent"
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCancelable(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectRoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStudent.setOnClickListener {
            onRoleSelected(UserRole.STUDENT)
            dismiss()
        }

        binding.btnTeacher.setOnClickListener {
            onRoleSelected(UserRole.TEACHER)
            dismiss()
        }

        binding.btnParent.setOnClickListener {
            onRoleSelected(UserRole.PARENT)
            dismiss()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (!isFromLogin) {
            findNavController().navigate(R.id.action_signup_to_login)
        } else {
            onCancel?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}