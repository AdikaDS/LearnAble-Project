package com.adika.learnable.view.core

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.adika.learnable.NavGraphDirections
import com.adika.learnable.R
import com.adika.learnable.databinding.ActivityMainBinding
import com.adika.learnable.service.NotificationScheduler
import com.adika.learnable.util.LanguageUtils
import com.adika.learnable.util.TextScaleManager
import com.adika.learnable.util.VibrationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var isUpdatingBottomSelection: Boolean = false
    private val vibrationHelper by lazy { VibrationHelper(this) }
    private var globalFocusChangeListener: ViewTreeObserver.OnGlobalFocusChangeListener? = null

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var textScaleManager: TextScaleManager

    companion object {
        private const val STUDENT_DASHBOARD = "student_dashboard"
        private const val TEACHER_DASHBOARD = "teacher_dashboard"
        private const val ADMIN_DASHBOARD = "admin_dashboard"
        private const val ADMIN_CONFIRMATION = "admin_confirmation"
        private const val LOGIN = "login"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        handleBottomNavigation()

        initializeNotificationSystem()

        setupTextScaling()
        setupGlobalVibrationFeedback()

        val cameFromDeeplink = intent.getBooleanExtra("from_deeplink", false)
        handleEmailActionDeepLink(intent)
        if (!cameFromDeeplink) {
            handleDestination()
        }

        if (intent.getBooleanExtra("open_notification", false)) {
            try {

                if (intent.getStringExtra("destination") == STUDENT_DASHBOARD) {
                    navController.navigate(R.id.studentDashboardFragment)
                }
                navController.navigate(R.id.notificationFragment)
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error (open_notification): ${e.message}")
            }

            intent.removeExtra("open_notification")
        }

        binding.root.post {
            applyInitialPadding()
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            binding.root.post {
                applyInitialPadding()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume: ${e.message}")

        }
    }

    override fun onDestroy() {
        removeGlobalFocusListener()
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            handleTouchForVibration(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun attachBaseContext(newBase: Context) {
        try {
            val languageCode = LanguageUtils.getLanguagePreference(newBase)
            val context = LanguageUtils.changeLanguage(newBase, languageCode)
            super.attachBaseContext(context)
        } catch (e: Exception) {
            super.attachBaseContext(newBase)
        }
    }

    /** Baca extras dari DeepLinkActivity dan arahkan ke ResetPasswordFragment */
    private fun handleEmailActionDeepLink(intent: Intent) {
        val fromDeeplink = intent.getBooleanExtra("from_deeplink", false)
        if (!fromDeeplink) return

        val mode = intent.getStringExtra("email_action_mode")
        val oob = intent.getStringExtra("oobCode")

        if (mode == "resetPassword" && !oob.isNullOrEmpty()) {

            try {
                val action = NavGraphDirections.actionGlobalResetPasswordFragment(oob)
                navController.navigate(action)
            } catch (e: Exception) {

                Log.e("MainActivity", "DeepLink nav error: ${e.message}")
            }
        }

        intent.removeExtra("from_deeplink")
        intent.removeExtra("email_action_mode")
        intent.removeExtra("oobCode")
    }

    private fun applyInitialPadding() {
        val currentDestination = navController.currentDestination?.id
        val showOnly = setOf(
            R.id.studentDashboardFragment,
            R.id.subjectListFragment,
            R.id.progressFragment,
            R.id.moreFragment,
            R.id.materialBookmarkFragment,
            R.id.accountProfileFragment,
            R.id.settingFragment,
            R.id.aboutFragment,
        )

        val shouldShow = currentDestination in showOnly
        Log.d(
            "MainActivity",
            "Applying initial padding for destination: $currentDestination, shouldShow: $shouldShow"
        )

        adjustNavHostPadding(shouldShow)
    }

    private fun adjustNavHostPadding(shouldShow: Boolean) {
        val navHost = binding.navHostFragment

        val systemBottomInset = ViewCompat.getRootWindowInsets(navHost)
            ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0

        val targetPadding = if (!shouldShow) {
            systemBottomInset
        } else {
            val barH = binding.bottomAppBar.height
            val contentSpacing = (8 * resources.displayMetrics.density).toInt() // 8dp spacing
            barH + contentSpacing + systemBottomInset
        }

        val start = navHost.paddingBottom
        if (start == targetPadding) return

        Log.d(
            "MainActivity",
            "Adjusting padding from $start to $targetPadding, shouldShow: $shouldShow"
        )

        val animator = ValueAnimator.ofInt(start, targetPadding)
        animator.duration = 200
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            navHost.updatePadding(bottom = value)
        }

        navHost.post { animator.start() }
    }

    private fun handleBottomNavigation() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showOnly = setOf(
                R.id.studentDashboardFragment,
                R.id.subjectListFragment,
                R.id.progressFragment,
                R.id.moreFragment,
                R.id.materialBookmarkFragment,
                R.id.accountProfileFragment,
                R.id.settingFragment,
                R.id.aboutFragment

            )

            val shouldShow = destination.id in showOnly
            Log.d(
                "MainActivity",
                "Destination changed to: ${destination.id}, shouldShow: $shouldShow"
            )

            binding.bottomAppBar.isVisible = shouldShow
            binding.fabLearnBot.isVisible = shouldShow

            binding.root.post {
                adjustNavHostPadding(shouldShow)
            }

            if (shouldShow) {
                val targetSelectedId = when (destination.id) {
                    R.id.studentDashboardFragment -> R.id.homeFragment
                    R.id.subjectListFragment -> R.id.studyFragment
                    R.id.progressFragment -> R.id.progressFragment
                    R.id.moreFragment, R.id.accountProfileFragment -> R.id.moreFragment
                    else -> null
                }

                if (targetSelectedId != null && binding.bottomNav.selectedItemId != targetSelectedId) {
                    isUpdatingBottomSelection = true
                    binding.bottomNav.selectedItemId = targetSelectedId

                    binding.bottomNav.post { isUpdatingBottomSelection = false }
                }
            }
        }

        binding.bottomNav.apply {
            background = null
            setOnApplyWindowInsetsListener(null)
            menu[2].isEnabled = false

            setOnItemReselectedListener { /* no-op */ }

            setOnItemSelectedListener {
                if (isUpdatingBottomSelection) return@setOnItemSelectedListener true
                when (it.itemId) {
                    R.id.homeFragment -> {
                        val target = R.id.studentDashboardFragment
                        if (navController.currentDestination?.id != target) {
                            navController.navigate(target)
                        }
                        true
                    }

                    R.id.studyFragment -> {
                        val target = R.id.subjectListFragment
                        if (navController.currentDestination?.id != target) {
                            navController.navigate(target)
                        }
                        true
                    }

                    R.id.progressFragment -> {
                        val target = R.id.progressFragment
                        if (navController.currentDestination?.id != target) {
                            navController.navigate(target)
                        }
                        true
                    }

                    R.id.moreFragment -> {
                        val target = R.id.moreFragment
                        if (navController.currentDestination?.id != target) {
                            navController.navigate(target)
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        binding.fabLearnBot.setOnClickListener {
            navController.navigate(R.id.chatbotFragment)
        }
    }

    private fun setupGlobalVibrationFeedback() {
        setupEditTextFocusVibration()
    }

    private fun setupEditTextFocusVibration() {
        val contentView = findViewById<ViewGroup>(android.R.id.content) ?: return
        val listener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus is EditText && newFocus.isShown && newFocus.isEnabled) {
                vibrationHelper.vibrateClick(newFocus)
            }
        }
        contentView.viewTreeObserver.addOnGlobalFocusChangeListener(listener)
        globalFocusChangeListener = listener
    }

    private fun removeGlobalFocusListener() {
        val listener = globalFocusChangeListener ?: return
        val contentView = findViewById<ViewGroup>(android.R.id.content) ?: return
        val observer = contentView.viewTreeObserver
        if (observer.isAlive) {
            observer.removeOnGlobalFocusChangeListener(listener)
        }
        globalFocusChangeListener = null
    }

    private fun handleTouchForVibration(ev: MotionEvent) {
        val contentView = findViewById<ViewGroup>(android.R.id.content) ?: return
        val touchedView =
            findViewAtPosition(contentView, ev.rawX.toInt(), ev.rawY.toInt()) ?: return
        val clickableTarget = findClickableAncestor(touchedView) ?: return
        if (shouldVibrateForClick(clickableTarget)) {
            vibrationHelper.vibrateClick(clickableTarget)
        }
    }

    private fun findViewAtPosition(view: View, rawX: Int, rawY: Int): View? {
        if (!view.isShown) return null
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        val inside = rawX >= left && rawX <= right && rawY >= top && rawY <= bottom
        if (!inside) return null
        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(i)
                val target = findViewAtPosition(child, rawX, rawY)
                if (target != null) return target
            }
        }
        return view
    }

    private fun findClickableAncestor(view: View?): View? {
        var current: View? = view
        while (current != null) {
            if (shouldVibrateForClick(current)) return current
            val parent = current.parent
            current = if (parent is View) parent else null
        }
        return null
    }

    private fun shouldVibrateForClick(view: View): Boolean {
        val isInteractive =
            view.isClickable || view.isLongClickable || view.isContextClickable
        return view.isShown &&
                view.isEnabled &&
                isInteractive &&
                view !is EditText
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            try {
                supportActionBar?.title = destination.label
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in navigation: ${e.message}")
            }
        }
    }

    private fun handleDestination() {
        val destination = intent.getStringExtra("destination")
        if (destination != null) {
            try {
                when (destination) {
                    ADMIN_CONFIRMATION -> {
                        navController.navigate(R.id.adminConfirmationFragment)
                    }

                    ADMIN_DASHBOARD -> {
                        navController.navigate(R.id.adminDashboardFragment)
                    }

                    STUDENT_DASHBOARD -> {
                        navController.navigate(R.id.studentDashboardFragment)

                        binding.root.post {
                            applyInitialPadding()
                        }
                    }

                    TEACHER_DASHBOARD -> {
                        navController.navigate(R.id.teacherDashboardFragment)
                    }

                    LOGIN -> {
                        navController.navigate(R.id.loginFragment)
                    }

                    else -> {
                        Log.w("MainActivity", "Invalid destination: $destination")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error: ${e.message}")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (navController.currentDestination?.id) {
            R.id.studentDashboardFragment,
            R.id.teacherDashboardFragment,
            R.id.adminConfirmationFragment,
            R.id.adminDashboardFragment -> {
                showExitConfirmationDialog()
            }

            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.exit_app))
            .setMessage(getString(R.string.confirm_exit_app))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                finish()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            super.onSupportNavigateUp()
        }
    }

    private fun initializeNotificationSystem() {
        try {
            val dest = intent.getStringExtra("destination")
            if (dest == STUDENT_DASHBOARD) {

                notificationScheduler.scheduleDailyLearningCheck()
                notificationScheduler.scheduleEveningReminder()
            } else {

                notificationScheduler.cancelAllNotifications()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing notification system", e)
        }
    }

    private fun setupTextScaling() {
        try {

            supportFragmentManager.registerFragmentLifecycleCallbacks(
                object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(
                        fm: androidx.fragment.app.FragmentManager,
                        f: androidx.fragment.app.Fragment,
                        v: View,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentViewCreated(fm, f, v, savedInstanceState)

                        textScaleManager.applyTextScaleToView(v)
                    }
                },
                true
            )

            supportFragmentManager.setFragmentResultListener(
                "text_scale_changed",
                this
            ) { _, _ ->
                textScaleManager.applyTextScaleToAllFragments(supportFragmentManager)
            }

            Log.d("MainActivity", "Text scaling system initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing text scaling system", e)
        }
    }
}