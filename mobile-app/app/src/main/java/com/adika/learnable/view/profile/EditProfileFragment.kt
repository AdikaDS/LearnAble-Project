package com.adika.learnable.view.profile

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentEditProfileBinding
import com.adika.learnable.model.StudentData
import com.adika.learnable.model.User
import com.adika.learnable.util.LanguageUtils
import com.adika.learnable.util.NormalizeFirestore
import com.adika.learnable.util.loadAvatar
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.ProfileViewModel
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class EditProfileFragment : BaseFragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var originalUser: User? = null
    private var isInitializing = true
    private var currentRole: String? = null

    private companion object {
        private const val STUDENT = "student"
        private const val TEACHER = "teacher"
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri = result.data?.data!!
                val file = uriToFile(uri)
                if (file != null) {
                    viewModel.uploadProfilePicture(file)
                } else {
                    showToast(getString(R.string.fail_up_picture))
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(
                    requireContext(),
                    ImagePicker.getError(result.data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageCode = LanguageUtils.getLanguagePreference(requireContext())
        val config = resources.configuration
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        binding.btnSave.isEnabled = false

        observeViewModel()
        setupGenderDropdown()
        observeRegency()
        setupProvinceDropdown()
        setupCityDropdown()
        setupClickListeners()
        loadProfileData()
        viewModel.loadProvinces()

        setupTextScaling()
    }

    private fun setupGenderDropdown() {
        val genders = resources.getStringArray(R.array.genders).toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, genders)
        binding.genderAutoComplete.setAdapter(adapter)
        binding.genderAutoComplete.setOnClickListener { binding.genderAutoComplete.showDropDown() }
        binding.genderTeacherAutoComplete.setAdapter(adapter)
        binding.genderTeacherAutoComplete.setOnClickListener { binding.genderTeacherAutoComplete.showDropDown() }
    }

    private fun setupProvinceDropdown() {
        binding.provinceEditText.setOnClickListener { binding.provinceEditText.showDropDown() }

        binding.provinceEditText.doAfterTextChanged { text ->
            val list = (viewModel.provinces.value ?: emptyList())
            val selected = list.find { it.name == text.toString() }
            if (selected != null) {
                binding.cityEditText.setText("")
                viewModel.loadRegencies(selected.id)
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
        }
    }

    private fun setupCityDropdown() {
        binding.cityEditText.setOnClickListener { binding.cityEditText.showDropDown() }
        binding.cityEditText.setOnItemClickListener { _, _, _, _ ->
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
    }

    private fun observeRegency() {
        viewModel.provinces.observe(viewLifecycleOwner) { list ->
            val names = list.map { it.name }
            binding.provinceEditText.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            )
            originalUser?.let { user ->
                val idx = names.indexOfFirst { it.equals(user.studentData.provinceAddress, true) }
                if (idx >= 0) {
                    binding.provinceEditText.setText(names[idx], false)
                    viewModel.loadRegencies(list[idx].id)
                }
            }
        }
        viewModel.regencies.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                val names = list.map { it.name }
                val adapter =
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.cityEditText.setAdapter(adapter)

                binding.cityEditText.setText("", false)

                originalUser?.let { user ->
                    val idx = names.indexOfFirst { it.equals(user.studentData.cityAddress, true) }
                    if (idx >= 0) binding.cityEditText.setText(names[idx], false)
                }

                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            } else {
                binding.cityEditText.setAdapter(null)
                binding.cityEditText.setText("")
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
        }
    }


    private fun observeViewModel() {
        viewModel.userState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }

        viewModel.uploadState.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is ProfileViewModel.UserState.Loading,
            is ProfileViewModel.UploadState.Loading -> {
                showLoading(true)
            }

            is ProfileViewModel.UserState.Success -> {
                showLoading(false)
                state.user?.let { updateUI(it) }
            }

            is ProfileViewModel.UploadState.Success -> {
                showLoading(false)
                Glide.with(requireContext())
                    .load(state.imageUrl)
                    .circleCrop()
                    .into(binding.profileImage)
            }

            is ProfileViewModel.UserState.Error,
            is ProfileViewModel.UploadState.Error -> {
                showLoading(false)
                showToast(
                    (state as? ProfileViewModel.UploadState.Error)?.message
                        ?: (state as? ProfileViewModel.UserState.Error)?.message
                        ?: getString(R.string.unknown_error)
                )
            }
        }

    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            val user = collectUserData()
            if (validateChangeUserData(user)) {
                viewModel.updateUserProfile(user)
                findNavController().navigateUp()
            }
        }

        binding.btnEditProfile.setOnClickListener {
            openImagePicker()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

    }

    private fun validateChangeUserData(user: User): Boolean {
        if (!hasChanges()) return false
        if (user.name.isBlank()) {
            showToast(getString(R.string.name) + " " + getString(R.string.must_be_filled))
            return false
        }
        if (user.phoneNumber.isNullOrBlank()) {
            showToast(getString(R.string.number) + " " + getString(R.string.must_be_filled))
            return false
        }

        if (user.gender.isNullOrBlank()) {
            showToast(getString(R.string.gender) + " " + getString(R.string.must_be_filled))
            return false
        }

        return when (currentRole) {
            TEACHER -> {
                if (user.idNumber.isNullOrBlank()) {
                    showToast(getString(R.string.nip) + " " + getString(R.string.must_be_filled))
                    false
                } else {
                    true
                }
            }

            STUDENT -> {
                when {
                    user.studentData.provinceAddress.isBlank() -> {
                        showToast(getString(R.string.province) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    user.studentData.cityAddress.isBlank() -> {
                        showToast(getString(R.string.city) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    user.studentData.address.isBlank() -> {
                        showToast(getString(R.string.address) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    user.studentData.nameParent.isBlank() -> {
                        showToast(getString(R.string.parent_name) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    user.studentData.phoneNumberParent.isBlank() -> {
                        showToast(getString(R.string.parent_number) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    else -> true
                }
            }

            else -> {
                when {
                    user.studentData.provinceAddress.isBlank() -> {
                        showToast(getString(R.string.province) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    user.studentData.cityAddress.isBlank() -> {
                        showToast(getString(R.string.city) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    user.studentData.address.isBlank() -> {
                        showToast(getString(R.string.address) + " " + getString(R.string.must_be_filled))
                        false
                    }

                    else -> true
                }
            }
        }
    }

    private fun updateUI(user: User) {
        originalUser = user
        currentRole = user.role?.lowercase(Locale.ROOT)

        val isTeacher = currentRole == TEACHER
        val showStudentCard = !isTeacher
        val showParentInfoCard = currentRole == STUDENT

        binding.apply {
            profileImage.loadAvatar(
                user.name,
                user.profilePicture
            )

            val roleText =
                user.role?.let { NormalizeFirestore.unormalizeRole(requireContext(), it) }
            if (isTeacher) {
                cardTeacherProfile.isVisible = true
                cardStudentProfile.isVisible = false
                cardParentInformation.isVisible = false

                fullNameTeacherEditText.setText(user.name)
                roleTeacherEditText.setText(roleText)
                genderTeacherAutoComplete.setText(user.gender, false)
                emailTeacherEditText.setText(user.email)
                numberTeacherPhoneEditText.setText(user.phoneNumber)
                NIPTeacherEditText.setText(user.idNumber ?: "")
            } else {
                cardTeacherProfile.isVisible = false
                cardStudentProfile.isVisible = showStudentCard
                cardParentInformation.isVisible = showParentInfoCard

                fullNameEditText.setText(user.name)
                roleEditText.setText(roleText)
                genderAutoComplete.setText(user.gender, false)
                emailEditText.setText(user.email)
                numberPhoneEditText.setText(user.phoneNumber)
                fullNameParentEditText.setText(user.studentData.nameParent)
                phoneNumberParentEditText.setText(user.studentData.phoneNumberParent)
            }

            provinceEditText.setText(user.studentData.provinceAddress, false)
            cityEditText.setText(user.studentData.cityAddress, false)
            addressEditText.setText(user.studentData.address)
        }

        setupFieldChangeListeners()
        isInitializing = false

        binding.btnSave.isEnabled = hasChanges()
    }

    private fun loadProfileData() {
        viewModel.loadUserProfile()
    }


    private fun setupFieldChangeListeners() {
        binding.fullNameEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.fullNameTeacherEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.genderAutoComplete.apply {
            setOnItemClickListener { _, _, _, _ ->
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
            doAfterTextChanged {
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
        }
        binding.genderTeacherAutoComplete.apply {
            setOnItemClickListener { _, _, _, _ ->
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
            doAfterTextChanged {
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
        }
        binding.numberPhoneEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.numberTeacherPhoneEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.provinceEditText.apply {
            setOnItemClickListener { _, _, _, _ ->
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
            doAfterTextChanged {
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
        }
        binding.cityEditText.apply {
            setOnItemClickListener { _, _, _, _ ->
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
            doAfterTextChanged {
                if (!isInitializing) {
                    binding.btnSave.isEnabled = hasChanges()
                }
            }
        }
        binding.addressEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.fullNameParentEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.phoneNumberParentEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
        binding.NIPTeacherEditText.doAfterTextChanged {
            if (!isInitializing) {
                binding.btnSave.isEnabled = hasChanges()
            }
        }
    }

    private fun hasChanges(): Boolean {
        val base = originalUser ?: return false
        val isTeacher = currentRole == TEACHER
        val currentName = if (isTeacher) {
            binding.fullNameTeacherEditText.text?.toString()?.trim().orEmpty()
        } else {
            binding.fullNameEditText.text?.toString()?.trim().orEmpty()
        }
        val currentGender = if (isTeacher) {
            binding.genderTeacherAutoComplete.text?.toString()?.trim().orEmpty()
        } else {
            binding.genderAutoComplete.text?.toString()?.trim().orEmpty()
        }
        val currentPhone = if (isTeacher) {
            binding.numberTeacherPhoneEditText.text?.toString()?.trim().orEmpty()
        } else {
            binding.numberPhoneEditText.text?.toString()?.trim().orEmpty()
        }
        val currentProvince = binding.provinceEditText.text?.toString()?.trim().orEmpty()
        val currentCity = binding.cityEditText.text?.toString()?.trim().orEmpty()
        val currentAddress = binding.addressEditText.text?.toString()?.trim().orEmpty()
        val currentParentName = binding.fullNameParentEditText.text?.toString()?.trim().orEmpty()
        val currentParentPhone =
            binding.phoneNumberParentEditText.text?.toString()?.trim().orEmpty()
        val currentNip = binding.NIPTeacherEditText.text?.toString()?.trim().orEmpty()

        if (currentName != base.name) return true
        if (currentGender != (base.gender ?: "")) return true
        if (currentPhone != (base.phoneNumber ?: "")) return true
        if (currentProvince != base.studentData.provinceAddress) return true
        if (currentCity != base.studentData.cityAddress) return true
        if (currentAddress != base.studentData.address) return true
        if (currentRole == STUDENT) {
            if (currentParentName != base.studentData.nameParent) return true
            if (currentParentPhone != base.studentData.phoneNumberParent) return true
        }
        if (currentRole == TEACHER && currentNip != (base.idNumber ?: "")) return true
        return false
    }


    private fun openImagePicker() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val type = contentResolver.getType(uri)
            val extension = when (type) {
                "image/jpeg" -> ".jpg"
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                else -> ".jpg" // default
            }
            val tempFile = File.createTempFile("profile_", extension, requireContext().cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun collectUserData(): User {
        val base = originalUser
        val isTeacher = currentRole == TEACHER
        val name = if (isTeacher) {
            binding.fullNameTeacherEditText.text?.toString()?.trim().orEmpty()
        } else {
            binding.fullNameEditText.text?.toString()?.trim().orEmpty()
        }
        val gender = if (isTeacher) {
            binding.genderTeacherAutoComplete.text?.toString()?.trim().orEmpty()
        } else {
            binding.genderAutoComplete.text?.toString()?.trim().orEmpty()
        }
        val email = if (isTeacher) {
            binding.emailTeacherEditText.text?.toString()?.trim().orEmpty()
        } else {
            binding.emailEditText.text?.toString()?.trim().orEmpty()
        }
        val roleText = if (isTeacher) {
            binding.roleTeacherEditText.text?.toString()?.trim().orEmpty()
        } else {
            binding.roleEditText.text?.toString()?.trim().orEmpty()
        }
        val role = if (base?.role != null) base.role else roleText
        val phone = if (isTeacher) {
            binding.numberTeacherPhoneEditText.text?.toString()?.trim().orEmpty()
        } else {
            binding.numberPhoneEditText.text?.toString()?.trim().orEmpty()
        }
        val province = binding.provinceEditText.text?.toString()?.trim().orEmpty()
        val city = binding.cityEditText.text?.toString()?.trim().orEmpty()
        val address = binding.addressEditText.text?.toString()?.trim().orEmpty()
        val parentName = binding.fullNameParentEditText.text?.toString()?.trim().orEmpty()
        val parentPhone = binding.phoneNumberParentEditText.text?.toString()?.trim().orEmpty()
        val nip = binding.NIPTeacherEditText.text?.toString()?.trim().orEmpty()

        val normalizedRole =
            base?.role ?: role.let { NormalizeFirestore.normalizeRole(requireContext(), it) }
        val updatedStudentData = when {
            isTeacher -> {
                val currentStudentData = base?.studentData ?: StudentData()
                currentStudentData.copy(
                    provinceAddress = province,
                    cityAddress = city,
                    address = address
                )
            }

            else -> {
                val currentStudentData = base?.studentData ?: StudentData()
                currentStudentData.copy(
                    provinceAddress = province,
                    cityAddress = city,
                    address = address,
                    nameParent = parentName,
                    phoneNumberParent = parentPhone
                )
            }
        }

        return base?.copy(
            name = name,
            phoneNumber = phone,
            gender = gender,
            idNumber = if (isTeacher) nip else base.idNumber,
            studentData = updatedStudentData
        )
            ?: User(
                name = name,
                email = email,
                role = normalizedRole,
                phoneNumber = phone,
                gender = gender,
                idNumber = if (isTeacher) nip else null,
                studentData = StudentData(
                    provinceAddress = province,
                    cityAddress = city,
                    address = address,
                    nameParent = parentName,
                    phoneNumberParent = parentPhone
                )
            )
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}