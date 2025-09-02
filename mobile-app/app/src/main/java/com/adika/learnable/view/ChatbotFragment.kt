package com.adika.learnable.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentChatbotBinding
import com.adika.learnable.model.Chip
import com.adika.learnable.ui.ChatMessage
import com.adika.learnable.ui.ChatbotAdapter
import com.adika.learnable.viewmodel.ChatbotViewModel
import com.adika.learnable.viewmodel.ChatbotViewModel.ChatBotState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotFragment : Fragment() {
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatbotViewModel by viewModels()
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatbotAdapter
    private var userText = ""
    private var isWaitingForCustomQuestion = false
    private var lastErrorMessage = ""
    private var lastTimeoutMessage = ""

    private val projectId = "learnable-22a3b"
    private val sessionId = "user-123"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSendButton()
        setupRetryButtons()
        observeViewModel()
        setInputGone(true)
        viewModel.fetchToken()
    }

    private fun setupRecyclerView() {
        adapter = ChatbotAdapter(messages) { chipOption ->
            viewModel.clearPolling()

            val chipText = chipOption.text ?: ""
            addUserMessage(chipText)

            if (chipText == "\uD83D\uDCAC Tanya Lagi ke AI") {
                isWaitingForCustomQuestion = true
                setInputGone(false)
                viewModel.sendMessage(chipText, projectId, sessionId)
                return@ChatbotAdapter
            }

            userText = chipText
            viewModel.sendMessage(chipText, projectId, sessionId)
        }

        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = adapter
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun setupRetryButtons() {
        binding.btnRetry.setOnClickListener {
            hideErrorState()
            viewModel.fetchToken()
        }

        binding.btnRetryTimeout.setOnClickListener {
            hideTimeoutState()
            // Retry dengan pesan terakhir
            if (lastTimeoutMessage.isNotEmpty()) {
                viewModel.sendMessage(lastTimeoutMessage, projectId, sessionId)
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            addUserMessage(text)
            binding.etMessage.text?.clear()
            // Kirim ke backend jika ini pertanyaan custom
            if (isWaitingForCustomQuestion) {
                isWaitingForCustomQuestion = false
                userText = text
                // Kirim dengan intent "Custom Pertanyaan"
                viewModel.sendMessage(text, projectId, sessionId)
            }
        }
    }

    private fun addUserMessage(text: String) {
        adapter.addMessage(ChatMessage.UserMessage(text))
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String) {
        adapter.addMessage(ChatMessage.BotMessage(text))
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun addBotChips(chips: List<Chip>) {
        adapter.addMessage(ChatMessage.BotChips(chips))
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun observeViewModel() {
        viewModel.token.observe(viewLifecycleOwner) { token ->
            handleToken(token)
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            handleState(state)
        }
    }

    private fun handleState(state: Any) {
        when (state) {
            is ChatBotState.Loading -> showLoadingState()
            is ChatBotState.SuccessDialogflow -> {
                hideAllStates()
                // Ambil pesan bot dari fulfillmentText, fallback ke fulfillmentMessages jika perlu
                val text = state.response?.queryResult?.fulfillmentText
                    ?.takeIf { it.isNotBlank() }
                    ?: state.response?.queryResult?.fulfillmentMessages
                        ?.firstOrNull { it.text?.text?.isNotEmpty() == true }
                        ?.text?.text?.firstOrNull()
                    ?: ""

                if (text.isNotBlank()) addBotMessage(text)

                // Tampilkan chips jika ada
                state.response?.queryResult?.fulfillmentMessages?.forEach { msg ->
                    msg.payload?.richContent?.forEach { chipList ->
                        chipList.forEach { chipOption ->
                            if (chipOption.type == "chips" && chipOption.options != null) {
                                if (chipOption.options.isNotEmpty()) {
                                    addBotChips(chipOption.options)
                                }
                            }
                        }
                    }
                }

                val fullfilmentText = state.response?.queryResult?.fulfillmentText ?: ""

                // Ambil cache key dari backend ketika fullfilmenttext berisi 🤖 Jawaban sedang diproses... Mohon tunggu sebentar.
                if (fullfilmentText.contains("\uD83E\uDD16 Jawaban sedang diproses... Mohon tunggu sebentar.")) {
                    val cacheKey = state.response?.queryResult?.outputContexts
                        ?.find { it.name.contains("waiting_custom_answer") || it.name.contains("waiting_theory_answer") }
                        ?.parameters?.get("cache_key") as? String

                    Log.d("ChatbotFragment", "Extracted cacheKey: $cacheKey")

                    if (!cacheKey.isNullOrBlank()) {
                        viewModel.startPolling(cacheKey)
                    }
                }

                handleInputVisibility(userText)
            }

            is ChatBotState.SuccessGemini -> {
                hideAllStates()
                // Ambil pesan bot dari fulfillmentMessages
                val text = state.response?.fulfillmentMessages
                    ?.firstOrNull { it.text?.text?.isNotEmpty() == true }
                    ?.text?.text?.firstOrNull()
                    ?: ""

                if (text.isNotBlank()) addBotMessage(text)

                // Tampilkan chips jika ada
                state.response?.fulfillmentMessages?.forEach { msg ->
                    msg.payload?.richContent?.forEach { chipList ->
                        chipList.forEach { chipOption ->
                            if (chipOption.type == "chips" && chipOption.options != null) {
                                if (chipOption.options.isNotEmpty()) {
                                    addBotChips(chipOption.options)
                                }
                            }
                        }
                    }
                }
            }

            is ChatBotState.Timeout -> {
                hideAllStates()
                showTimeoutState(state.message)
                lastTimeoutMessage = userText
            }

            ChatBotState.Idle -> hideAllStates()

            is ChatBotState.Error -> {
                hideAllStates()
                showErrorState(state.message)
                lastErrorMessage = state.message
            }
        }
    }

    private fun handleInputVisibility(currentUserText: String) {
        when {
            currentUserText.contains("Tanya Lagi ke AI") -> {
                setInputGone(false)
                userText = ""
            }

            currentUserText.contains("Menu Utama") -> {
                setInputGone(true)
                isWaitingForCustomQuestion = false
                userText = ""
            }
        }
    }

    private fun handleToken(token: String?) {
        val tokenReady = !token.isNullOrBlank()
        binding.btnSend.isEnabled = tokenReady
        binding.etMessage.isEnabled = tokenReady
        if (!token.isNullOrBlank() && messages.isEmpty()) {
            viewModel.sendMessage("Mulai", projectId, sessionId)
        }
    }

    private fun showLoadingState() {
        hideAllStates()
        binding.loadingContainer.isVisible = true
        startLoadingAnimation()
    }

    private fun showErrorState(message: String) {
        hideAllStates()
        binding.errorContainer.isVisible = true
        binding.tvErrorMessage.text = getErrorMessage(message)
    }

    private fun showTimeoutState(message: String) {
        hideAllStates()
        binding.timeoutContainer.isVisible = true
        // Update pesan timeout jika diperlukan
        binding.timeoutContainer.findViewById<TextView>(R.id.tvTimeoutMessage)?.text = message
    }

    private fun hideAllStates() {
        binding.loadingContainer.isGone = true
        binding.errorContainer.isGone = true
        binding.timeoutContainer.isGone = true
        binding.progressBar.isGone = true
        stopLoadingAnimation()
    }

    private fun hideErrorState() {
        binding.errorContainer.isGone = true
    }

    private fun hideTimeoutState() {
        binding.timeoutContainer.isGone = true
    }

    private fun startLoadingAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEachIndexed { index, dot ->
            val animator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f)
            animator.duration = 1000
            animator.repeatCount = ObjectAnimator.INFINITE
            animator.startDelay = (index * 200).toLong()
            animator.start()
        }
    }

    private fun stopLoadingAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        dots.forEach { dot ->
            dot.animate().cancel()
            dot.alpha = 1f
        }
    }

    private fun getErrorMessage(error: String): String {
        return when {
            error.contains("Token") -> "Gagal mendapatkan akses. Silakan coba lagi."
            error.contains("server") -> "Gagal terhubung ke server. Periksa koneksi internet Anda."
            error.contains("timeout") -> "Koneksi timeout. Silakan coba lagi."
            else -> "Terjadi kesalahan: $error"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setInputGone(gone: Boolean) {
        binding.messageInputLayout.visibility = if (gone) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearPolling()
        _binding = null
    }
}