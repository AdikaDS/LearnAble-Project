package com.adika.learnable.view.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adika.learnable.databinding.FragmentSettingBinding
import com.adika.learnable.service.NotificationScheduler
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.settings.LanguageViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingFragment : BaseFragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private val languageViewModel: LanguageViewModel by viewModels()

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    private val prefsName = "learnable_settings"
    private val keyVibration = "vibration_enabled"
    private val keyNotification = "notification_enabled"
    private val prefs by lazy {
        requireContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    }

    private var isProgrammaticChange = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            val enabled = granted
            prefs.edit { putBoolean(keyNotification, enabled) }
            isProgrammaticChange = true
            binding.switchNotification.isChecked = enabled
            isProgrammaticChange = false
            if (enabled) {

                notificationScheduler.scheduleDailyLearningCheck()
                notificationScheduler.scheduleEveningReminder()
                vibrationHelper.vibrateClick(binding.switchNotification)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTextScaling() // Setup text scaling from BaseFragment
        setOnClickListener()
    }

    private fun setOnClickListener() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        initialSwitch()
        checkChangesSwitch()

        binding.btnVibration.setOnClickListener {
            binding.switchVibration.toggle()
        }

        binding.btnNotification.setOnClickListener {
            binding.switchNotification.toggle()
        }

        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLanguageDialog() {
        val currentCode = languageViewModel.languageCode.value ?: "id"
        val currentOptions = if (currentCode == "en") {
            LanguageDialog.ChooseLanguageOptions(LanguageDialog.ChooseLanguageOptions.LanguageBy.ENGLISH)
        } else {
            LanguageDialog.ChooseLanguageOptions(LanguageDialog.ChooseLanguageOptions.LanguageBy.INDONESIAN)
        }

        val dialog = LanguageDialog.newInstance(currentOptions) { options ->
            val code = when (options.languageBy) {
                LanguageDialog.ChooseLanguageOptions.LanguageBy.INDONESIAN -> "id"
                LanguageDialog.ChooseLanguageOptions.LanguageBy.ENGLISH -> "en"
            }
            languageViewModel.setLanguage(code)

            safeRecreateActivity()
        }
        dialog.show(parentFragmentManager, "LanguageDialog")
    }

    private fun safeRecreateActivity() {
        try {

            requireActivity().window.decorView.post {
                try {
                    requireActivity().recreate()
                } catch (e: Exception) {

                    Log.e(
                        "SettingFragment",
                        "Failed to recreate activity: ${e.message}"
                    )
                    restartApp()
                }
            }
        } catch (e: Exception) {
            Log.e(
                "SettingFragment",
                "Failed to schedule activity recreation: ${e.message}"
            )
            restartApp()
        }
    }

    private fun restartApp() {
        try {
            val intent =
                requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent != null) {
                startActivity(intent)
            }
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e("SettingFragment", "Failed to restart app: ${e.message}")
        }
    }

    private fun initialSwitch() {
        val initialVibration = prefs.getBoolean(keyVibration, false)
        val initialNotification = prefs.getBoolean(keyNotification, false)

        binding.switchVibration.isChecked = initialVibration
        binding.switchNotification.isChecked = initialNotification

        if (initialNotification) {
            notificationScheduler.scheduleDailyLearningCheck()
            notificationScheduler.scheduleEveningReminder()
        }
    }

    private fun checkChangesSwitch() {
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit() { putBoolean(keyVibration, isChecked) }
            if (isChecked) {
                vibrationHelper.vibrateClick(binding.switchVibration)
            }
        }
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isProgrammaticChange) return@setOnCheckedChangeListener

            if (isChecked) {
                if (needsNotificationPermission()) {

                    isProgrammaticChange = true
                    binding.switchNotification.isChecked = false
                    isProgrammaticChange = false
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    prefs.edit { putBoolean(keyNotification, true) }

                    notificationScheduler.scheduleDailyLearningCheck()
                    notificationScheduler.scheduleEveningReminder()
                    vibrationHelper.vibrateClick(binding.switchNotification)
                }
            } else {
                prefs.edit { putBoolean(keyNotification, false) }

                notificationScheduler.cancelAllNotifications()
            }
        }
    }


    private fun needsNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
    }
}