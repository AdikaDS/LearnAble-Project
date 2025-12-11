package com.adika.learnable.view

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentChatbotBinding
import com.adika.learnable.model.Chip
import com.adika.learnable.ui.ChatMessage
import com.adika.learnable.ui.ChatbotAdapter
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.others.ChatbotViewModel
import com.adika.learnable.viewmodel.others.ChatbotViewModel.ChatBotState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatbotFragment : BaseFragment() {
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatbotViewModel by viewModels()
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatbotAdapter
    private var userText = ""
    private var isWaitingForCustomQuestion = false
    private var lastErrorMessage = ""
    private var lastTimeoutMessage = ""
    private var lastCacheKey: String? = null

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

        setupTextScaling()
    }

    private fun setupRecyclerView() {
        adapter = ChatbotAdapter(messages) { chipOption ->
            viewModel.clearPolling()

            val chipText = chipOption.text ?: ""
            addUserMessage(chipText)

            if (chipText == getString(R.string.ask_ai)) {
                isWaitingForCustomQuestion = true
                setInputGone(false)
                lastCacheKey = null 
                viewModel.sendMessage(chipText, projectId, sessionId)
                return@ChatbotAdapter
            }

            userText = chipText
            lastCacheKey = null 
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

            if (!lastCacheKey.isNullOrBlank()) {
                viewModel.startPolling(lastCacheKey!!)
            } else if (lastTimeoutMessage.isNotEmpty()) {

                viewModel.sendMessage(lastTimeoutMessage, projectId, sessionId)
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            addUserMessage(text)
            binding.etMessage.text?.clear()

            if (isWaitingForCustomQuestion) {
                isWaitingForCustomQuestion = false
                userText = text
                lastCacheKey = null

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

                val text = state.response?.queryResult?.fulfillmentText
                    ?.takeIf { it.isNotBlank() }
                    ?: state.response?.queryResult?.fulfillmentMessages
                        ?.firstOrNull { it.text?.text?.isNotEmpty() == true }
                        ?.text?.text?.firstOrNull()
                    ?: ""

                if (text.isNotBlank()) addBotMessage(text)

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

                if (fullfilmentText.contains(getString(R.string.processing_answer_ai))) {
                    val cacheKey = state.response?.queryResult?.outputContexts
                        ?.find { it.name.contains("waiting_custom_answer") || it.name.contains("waiting_theory_answer") }
                        ?.parameters?.get("cache_key") as? String

                    Log.d("ChatbotFragment", "Extracted cacheKey: $cacheKey")

                    if (!cacheKey.isNullOrBlank()) {
                        lastCacheKey = cacheKey
                        viewModel.startPolling(cacheKey)
                    }
                }

                handleInputVisibility(userText)
            }

            is ChatBotState.SuccessGemini -> {
                hideAllStates()

                val text = state.response?.fulfillmentMessages
                    ?.firstOrNull { it.text?.text?.isNotEmpty() == true }
                    ?.text?.text?.firstOrNull()
                    ?: ""

                if (text.isNotBlank()) addBotMessage(text)

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
            currentUserText.contains(getString(R.string.ask_ai)) -> {
                setInputGone(false)
                userText = ""
            }

            currentUserText.contains(getString(R.string.main_menu)) -> {
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
            viewModel.sendMessage(getString(R.string.start), projectId, sessionId)
        }
    }

    private fun showLoadingState() {
        hideAllStates()
        binding.loadingContainer.isVisible = true

        binding.rvMessages.isEnabled = false
        binding.rvMessages.isClickable = false
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

        binding.timeoutContainer.findViewById<TextView>(R.id.tvTimeoutMessage)?.text = message
    }

    private fun hideAllStates() {
        binding.loadingContainer.isGone = true
        binding.errorContainer.isGone = true
        binding.timeoutContainer.isGone = true
        binding.progressBar.isGone = true

        binding.rvMessages.isEnabled = true
        binding.rvMessages.isClickable = true
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
            error.contains("Token") -> getString(R.string.failed_get_ai_access)
            error.contains("server") -> getString(R.string.failed_connect_ai_server)
            error.contains("timeout") -> getString(R.string.timeout_ai)
            else -> getString(R.string.error_ai, error)
        }
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