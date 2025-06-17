package com.adika.learnable.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adika.learnable.databinding.BottomSheetSelectRoleBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet dialog untuk memilih peran pengguna.
 * Menyediakan pilihan peran: Student, Teacher, Parent, dan Admin.
 */
class SelectRoleBottomSheet(private val onRoleSelected: (String) -> Unit) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSelectRoleBinding? = null
    private val binding get() = _binding!!

    companion object {
        /**
         * Konstanta untuk peran pengguna yang tersedia
         */
        object UserRole {
            const val STUDENT = "student"
            const val TEACHER = "teacher"
            const val PARENT = "parent"

            /**
             * Memeriksa apakah role yang diberikan valid
             * @param role role yang akan dicek
             * @return true jika role valid, false jika tidak
             */
            fun isValidRole(role: String): Boolean {
                return role in listOf(STUDENT, TEACHER, PARENT)
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}