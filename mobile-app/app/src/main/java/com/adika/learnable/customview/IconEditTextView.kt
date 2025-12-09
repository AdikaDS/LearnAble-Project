package com.adika.learnable.customview

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.adika.learnable.R
import com.adika.learnable.util.ValidationResult

class IconEditTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val iconImageView: ImageView
    private val toggleIcon: ImageView
    private val editText: EditText
    private val inputContainer: LinearLayout
    private val tvError: TextView
    private val floatingHint: TextView


    // Strength meter views
    private val strengthGroup: LinearLayout
    private val bar1: View
    private val bar2: View
    private val bar3: View
    private val tvStrength: TextView

    private var isPasswordVisible = false
    private var isDropdown = false
    private var isPasswordField = false
    private var enableFloatingHint: Boolean = true

    // Dropdown State
    private var dropdownItems: List<String> = emptyList()
    private var onItemSelected: ((String, Int) -> Unit)? = null
    private var selectedIndex: Int = -1

    // Strength meter config
    private var enableStrengthMeter = false
    private var minAcceptableStrength = PasswordStrength.MEDIUM
    private var attachedSubmitButton: View? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_icon_edittext, this, true)
        orientation = VERTICAL

        iconImageView = findViewById(R.id.iconImageView)
        toggleIcon = findViewById(R.id.toggleIcon)
        editText = findViewById(R.id.editText)
        inputContainer = findViewById(R.id.inputContainer)
        tvError = findViewById(R.id.tvErrorInput)
        floatingHint = findViewById(R.id.floatingHint)


        // Strength views
        strengthGroup = findViewById(R.id.strengthGroup)
        bar1 = findViewById(R.id.bar1)
        bar2 = findViewById(R.id.bar2)
        bar3 = findViewById(R.id.bar3)
        tvStrength = findViewById(R.id.tvStrength)

        context.withStyledAttributes(attrs, R.styleable.IconEditTextView) {
            val hint = getString(R.styleable.IconEditTextView_hintText)
            val icon = getDrawable(R.styleable.IconEditTextView_iconDrawable)
            val inputType = getInt(
                R.styleable.IconEditTextView_android_inputType,
                InputType.TYPE_CLASS_TEXT
            )
            val showToggle = getBoolean(R.styleable.IconEditTextView_enableToggle, false)
            enableFloatingHint = getBoolean(R.styleable.IconEditTextView_enableFloatingHint, true)

            // strength meter flag
            enableStrengthMeter = getBoolean(R.styleable.IconEditTextView_enableStrengthMeter, false)

            icon?.let { iconImageView.setImageDrawable(it) }
            floatingHint.text = hint
            editText.hint = hint

            // Dropdown Config
            isDropdown = getBoolean(R.styleable.IconEditTextView_isDropdown, false)
            val entriesRes = getResourceId(R.styleable.IconEditTextView_dropdownEntries, 0)
            if (entriesRes != 0) dropdownItems = resources.getStringArray(entriesRes).toList()


            if (isDropdown) {
                setInputTypeWithTypeface(InputType.TYPE_CLASS_TEXT)
                toggleIcon.visibility = VISIBLE
                toggleIcon.setImageResource(R.drawable.ic_caret_down)
                setupAsDropdown()
                strengthGroup.visibility = View.GONE
            } else if (showToggle && isPasswordInputType(inputType)) {
                toggleIcon.visibility = VISIBLE
                isPasswordField = true
                setInputTypeWithTypeface(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                toggleIcon.setOnClickListener { togglePasswordVisibility() }
                updateStrengthUI(calculateStrength(editText.text?.toString().orEmpty()))
            } else {
                toggleIcon.visibility = GONE
                setInputTypeWithTypeface(inputType)
                isPasswordField = isPasswordInputType(inputType)
                updateStrengthUI(calculateStrength(editText.text?.toString().orEmpty()))
            }
        }

        // Floating hint + clear error while typing/focus
        editText.addTextChangedListener { text ->
            if (!text.isNullOrEmpty()) showFloatingHint() else hideFloatingHint()
            if (!text.isNullOrEmpty()) setError(null)

            // Update strength meter & policy in real time
            if (isPasswordField && enableStrengthMeter) {
                val strength = calculateStrength(text?.toString().orEmpty())
                updateStrengthUI(strength)
                applyPolicy(strength)
            }
        }

        // Auto clear error saat focus
        editText.setOnFocusChangeListener { _, hasFocus ->
            val colorNormal = ContextCompat.getColor(context, R.color.grey)
            val colorError = ContextCompat.getColor(context, R.color.error)

            // jika sedang error, pertahankan warna merah
            if (tvError.isVisible) {
                floatingHint.setTextColor(colorError)
            } else {
                floatingHint.setTextColor(colorNormal)
            }

            if (hasFocus || !editText.text.isNullOrEmpty()) showFloatingHint() else hideFloatingHint()
        }

        // Jika sudah ada text dari luar sebelum init selesai
        if (!editText.text.isNullOrEmpty()) {
            showFloatingHint(immediate = true)
        }

        if (!enableFloatingHint) {
            floatingHint.visibility = View.GONE
            editText.hint = floatingHint.text
        }

    }

    /*Public*/

    // Untuk validasi pakai lambda + ValidationResult
    fun validateWith(validator: () -> ValidationResult): Boolean {
        return when (val result = validator()) {
            is ValidationResult.Valid -> {
                setError(null)
                true
            }

            is ValidationResult.Invalid -> {
                setError(result.message)
                false
            }
        }
    }

    fun getText(): String = editText.text?.toString().orEmpty()
    fun setText(text: String) = editText.setText(text)


    fun reset() {
        editText.text.clear()
        editText.clearFocus()
        setError(null)
        updateStrengthUI(PasswordStrength.EMPTY)
        applyPolicy(PasswordStrength.EMPTY)
    }

    fun setIcon(@androidx.annotation.DrawableRes resId: Int) {
        if (resId != 0) iconImageView.setImageResource(resId)
    }

    fun setError(message: String?) {
        val colorError = ContextCompat.getColor(context, R.color.error)
        val colorNormal = ContextCompat.getColor(context, R.color.grey)

        if (message != null) {
            inputContainer.setBackgroundResource(R.drawable.bg_rounded_border_error)
            tvError.text = message
            tvError.visibility = VISIBLE

            editText.setHintTextColor(colorError)
            floatingHint.setTextColor(colorError)
        } else {
            inputContainer.setBackgroundResource(R.drawable.bg_rounded_border)
            tvError.visibility = GONE

            editText.setHintTextColor(colorNormal)
            floatingHint.setTextColor(colorNormal)
        }
    }

    fun setDropdownItems(items: List<String>) {
        dropdownItems = items
        // sync selectedIndex dengan text saat ini
        val current = editText.text?.toString()
        val norm = { s: String? -> s?.trim()?.lowercase()?.replace(" ", "") ?: "" }
        selectedIndex = dropdownItems.indexOfFirst { norm(it) == norm(current) }
    }

    fun setOnItemSelectedListener(listener: (String, Int) -> Unit) { onItemSelected = listener }

    // Strength Meter Policy API
    fun setMinAcceptableStrength(min: PasswordStrength) {
        minAcceptableStrength = min
        val strength = calculateStrength(editText.text?.toString().orEmpty())
        applyPolicy(strength)
    }

    private fun getCurrentStrength(): PasswordStrength =
        calculateStrength(editText.text?.toString().orEmpty())

    fun addTextChangedListener(afterTextChanged: (Editable?) -> Unit): TextWatcher {
        return editText.addTextChangedListener(afterTextChanged = afterTextChanged)
    }

    fun addTextChangedListener(watcher: TextWatcher?) {
        editText.addTextChangedListener(watcher)
    }

    fun removeTextChangedListener(watcher: TextWatcher) {
        editText.removeTextChangedListener(watcher)
    }

    /*Internal*/

    private fun setInputTypeWithTypeface(type: Int) {
        editText.inputType = type
        editText.typeface = ResourcesCompat.getFont(context, R.font.ltsaeada_regular)
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_visibility_off)
        }
        editText.typeface = ResourcesCompat.getFont(context, R.font.ltsaeada_regular)
        editText.setSelection(editText.text.length)
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        return inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
                inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
    }

    private fun setupAsDropdown() {
        // Non-editable, tampilkan caret di kanan (pakai toggleIcon yang sudah ada atau drawable lain)
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        editText.isCursorVisible = false
        editText.isClickable = true
        editText.isLongClickable = false
        editText.inputType = InputType.TYPE_NULL

        toggleIcon.visibility = VISIBLE
        toggleIcon.setImageResource(R.drawable.ic_caret_down) // siapkan drawable panah bawah

        // Buka dropdown saat klik di mana saja pada container
        val open = { showDropdown() }
        editText.setOnClickListener { open() }
        toggleIcon.setOnClickListener { open() }
        inputContainer.setOnClickListener { open() }
        this.setOnClickListener { open() }
    }

    private fun showDropdown() {
        // ListPopupWindow biar nempel ke field & lebarnya sama
        val popup = androidx.appcompat.widget.ListPopupWindow(context)

        val adapter = object : ArrayAdapter<String>(context, 0, dropdownItems) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_dropdown_role, parent, false)

                val textView = view.findViewById<TextView>(R.id.tvText)

                // state selected untuk background hijau
                val activated = position == selectedIndex
                textView.isActivated = activated
                textView.text = getItem(position)

                // ganti warna teks saat selected (putih), selain itu abu-abu
                textView.setTextColor(
                    if (activated) Color.WHITE
                    else ContextCompat.getColor(context, R.color.grey)
                )
                return view
            }
        }

        popup.setAdapter(adapter)
        popup.anchorView = inputContainer
        popup.isModal = true
        popup.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_dropdown_panel))

        // Lebar = lebar field; posisikan tepat di bawah
        inputContainer.post {
            popup.width = inputContainer.width
            popup.verticalOffset = 8
            popup.show()
        }

        popup.setOnItemClickListener { _, _, position, _ ->
            val value = dropdownItems[position]
            editText.setText(value)
            selectedIndex = position // simpan index terpilih -> item hijau saat buka lagi
            setError(null)
            adapter.notifyDataSetChanged()

            onItemSelected?.invoke(value, position)
            popup.dismiss()
        }
    }

    // ===== Floating hint animation =====

    private fun showFloatingHint(immediate: Boolean = false) {
        // kosongkan placeholder di EditText biar tidak dobel
        if (!editText.hint.isNullOrEmpty()) editText.hint = ""

        if (immediate) {
            floatingHint.alpha = 1f
            floatingHint.translationY = 0f
            floatingHint.scaleX = 0.9f
            floatingHint.scaleY = 0.9f
            return
        }

        floatingHint.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(180)
            .start()
    }

    private fun hideFloatingHint() {
        // kembalikan hint ke dalam EditText jika kosong & tidak dropdown (untuk dropdown tetap floating)
        if (!isDropdown && editText.text.isNullOrEmpty()) {
            editText.hint = floatingHint.text
        }

        floatingHint.animate()
            .alpha(0f)
            .translationY(10f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(160)
            .start()
    }

    /* ======= Password Strength ======= */

    enum class PasswordStrength(val label: String, val segments: Int, val colorRes: Int) {
        EMPTY("", 0, android.R.color.transparent),
        WEAK("Lemah", 1, R.color.strength_weak),
        MEDIUM("Sedang", 2, R.color.strength_medium),
        STRONG("Kuat", 3, R.color.strength_strong)
    }

    private fun calculateStrength(pw: String): PasswordStrength {
        if (pw.isBlank()) return PasswordStrength.EMPTY

        var score = 0
        val length = pw.length
        val hasLower = pw.any { it.isLowerCase() }
        val hasUpper = pw.any { it.isUpperCase() }
        val hasDigit = pw.any { it.isDigit() }
        val hasSymbol = pw.any { !it.isLetterOrDigit() }

        // Panjang
        if (length >= 8) score++
        if (length >= 12) score++

        // Variasi
        if (hasLower) score++
        if (hasUpper) score++
        if (hasDigit) score++
        if (hasSymbol) score++

        // Penalti pola lemah
        val allSame = pw.toSet().size == 1
        val onlyLetters = pw.all { it.isLetter() }
        val onlyDigits = pw.all { it.isDigit() }
        if (allSame || onlyLetters || onlyDigits) score = maxOf(1, score - 2)

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score in 3..4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    private fun updateStrengthUI(strength: PasswordStrength) {
        if (!enableStrengthMeter || !isPasswordField) {
            strengthGroup.visibility = View.GONE
            return
        }
        if (strength == PasswordStrength.EMPTY) {
            strengthGroup.visibility = View.GONE
            return
        }
        strengthGroup.visibility = View.VISIBLE

        val color = ContextCompat.getColor(context, strength.colorRes)
        val gray = ContextCompat.getColor(context, android.R.color.darker_gray)

        bar1.setBackgroundColor(if (strength.segments >= 1) color else gray)
        bar2.setBackgroundColor(if (strength.segments >= 2) color else gray)
        bar3.setBackgroundColor(if (strength.segments >= 3) color else gray)

        tvStrength.text = strength.label
        tvStrength.setTextColor(color)
    }

    private fun applyPolicy(strength: PasswordStrength) {
        if (strength == PasswordStrength.EMPTY) {
            // kosong: tidak munculin error otomatis
            attachedSubmitButton?.isEnabled = false
            return
        }
        val pass = strength.ordinal >= minAcceptableStrength.ordinal
        if (!pass) {
            setError(context.getString(R.string.password_too_weak))
        } else {
            setError(null)
        }
        attachedSubmitButton?.isEnabled = pass
    }
}