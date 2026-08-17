package com.example.safenest.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.AccessRequestItem
import com.example.safenest.network.AlertOut
import com.example.safenest.repository.ParentInboxData
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.ParentInboxViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.Locale

class ParentInboxFragment : Fragment() {
    private val viewModel: ParentInboxViewModel by viewModels()
    private var progress: ProgressBar? = null
    private var countText: TextView? = null
    private var emptyText: TextView? = null
    private var errorText: TextView? = null
    private var requestsContainer: LinearLayout? = null
    private var alertsContainer: LinearLayout? = null
    private var continueButton: MaterialButton? = null
    private var loadedOnce = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_parent_inbox, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress = view.findViewById(R.id.inboxProgress)
        countText = view.findViewById(R.id.inboxCount)
        emptyText = view.findViewById(R.id.inboxEmptyText)
        errorText = view.findViewById(R.id.inboxError)
        requestsContainer = view.findViewById(R.id.requestsContainer)
        alertsContainer = view.findViewById(R.id.alertsContainer)
        continueButton = view.findViewById(R.id.continueHomeButton)

        view.findViewById<MaterialButton>(R.id.closeInboxButton).setOnClickListener { goHome() }
        continueButton?.setOnClickListener { goHome() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state -> renderState(state) }
                }
                launch {
                    viewModel.decisionState.collect { state -> renderDecisionState(state) }
                }
                launch {
                    viewModel.alertResolutionState.collect { state -> renderAlertResolutionState(state) }
                }
            }
        }
        viewModel.load()
    }

    private fun renderState(state: Result<ParentInboxData>?) {
        when (state) {
            Result.Loading -> {
                progress?.visibility = View.VISIBLE
                emptyText?.visibility = View.GONE
                errorText?.visibility = View.GONE
            }
            is Result.Success -> {
                loadedOnce = true
                progress?.visibility = View.GONE
                errorText?.visibility = View.GONE
                renderData(state.data)
                if (state.data.actionableCount == 0) {
                    goHome()
                }
            }
            is Result.Error -> {
                progress?.visibility = View.GONE
                errorText?.text = "تعذر تحميل الإشعارات والطلبات. يمكنك إعادة المحاولة أو المتابعة إلى الرئيسية."
                errorText?.visibility = View.VISIBLE
                continueButton?.text = "إعادة المحاولة"
                continueButton?.setOnClickListener { viewModel.load() }
            }
            null -> Unit
        }
    }

    private fun renderData(data: ParentInboxData) {
        countText?.text = "${data.actionableCount} عناصر تحتاج مراجعتك"
        requestsContainer?.removeAllViews()
        alertsContainer?.removeAllViews()
        data.requests.forEach { addRequestCard(it) }
        data.alerts.forEach { addAlertCard(it) }
        emptyText?.visibility = if (data.alerts.isEmpty() && data.requests.isEmpty()) View.VISIBLE else View.GONE
        errorText?.visibility = if (data.alertsUnavailable || data.requestsUnavailable) View.VISIBLE else View.GONE
        if (data.alertsUnavailable || data.requestsUnavailable) {
            errorText?.text = "بعض البيانات غير متاحة الآن. اسحب للمحاولة مرة أخرى أو تابع إلى الرئيسية."
        }
    }

    private fun addRequestCard(request: AccessRequestItem) {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 12) }
            radius = 18f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#DDEBE9")
            strokeWidth = 1
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
        }
        content.addView(label("${request.childName ?: "الطفل"} يطلب السماح بالتطبيق", 16f, Color.parseColor("#15385F"), true))
        content.addView(label("${request.scopeValue} · ${minutes(request.requestedSeconds)}", 14f, Color.DKGRAY, false))
        request.reason?.takeIf { it.isNotBlank() }?.let { content.addView(label(it, 13f, Color.GRAY, false)) }
        content.addView(label(request.requestedAt, 11f, Color.GRAY, false))

        val actions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, 14, 0, 0)
        }
        val allow = actionButton("السماح", Color.parseColor("#2CA39D"))
        val reject = actionButton("رفض", Color.parseColor("#E8796A"))
        allow.setOnClickListener {
            allow.isEnabled = false
            reject.isEnabled = false
            viewModel.approve(request.requestId, request.requestedSeconds)
        }
        reject.setOnClickListener {
            allow.isEnabled = false
            reject.isEnabled = false
            viewModel.reject(request.requestId)
        }
        actions.addView(allow)
        actions.addView(reject)
        content.addView(actions)
        card.addView(content)
        requestsContainer?.addView(card)
    }

    private fun addAlertCard(alert: AlertOut) {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 12) }
            radius = 18f
            cardElevation = 1f
            setCardBackgroundColor(Color.WHITE)
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
        }
        content.addView(label(alert.alertType, 12f, Color.parseColor("#2CA39D"), true))
        content.addView(label(alert.message, 14f, Color.parseColor("#333333"), false))
        content.addView(label(alert.timestamp ?: "", 11f, Color.GRAY, false))
        if (!alert.isResolved) {
            val resolve = actionButton("تمت المراجعة", Color.parseColor("#15385F"))
            resolve.setOnClickListener {
                resolve.isEnabled = false
                viewModel.resolveAlert(alert.alertId)
            }
            content.addView(resolve)
        }
        card.addView(content)
        alertsContainer?.addView(card)
    }

    private fun renderAlertResolutionState(state: Result<AlertOut>?) {
        when (state) {
            is Result.Success -> {
                Toast.makeText(requireContext(), "تم تعليم الإشعار كمقروء", Toast.LENGTH_SHORT).show()
                viewModel.load()
            }
            is Result.Error -> Toast.makeText(requireContext(), "تعذر تحديث الإشعار", Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    private fun renderDecisionState(state: Result<AccessRequestItem>?) {
        when (state) {
            is Result.Success -> {
                Toast.makeText(requireContext(), "تم تحديث الطلب بنجاح", Toast.LENGTH_SHORT).show()
                viewModel.clearDecisionState()
                viewModel.load()
            }
            is Result.Error -> {
                Toast.makeText(requireContext(), "تعذر تحديث الطلب، حاول مرة أخرى", Toast.LENGTH_LONG).show()
                viewModel.clearDecisionState()
                viewModel.load()
            }
            else -> Unit
        }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 4, 0, 4)
    }

    private fun actionButton(text: String, color: Int): MaterialButton = MaterialButton(requireContext()).apply {
        this.text = text
        isAllCaps = false
        minHeight = 48
        setTextColor(Color.WHITE)
        backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = 8 }
    }

    private fun minutes(seconds: Int): String =
        String.format(Locale.getDefault(), "%d دقيقة", (seconds / 60).coerceAtLeast(1))

    private fun goHome() {
        (activity as? MainActivity)?.showHomeFromInbox()
    }
}
