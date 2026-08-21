package com.example.safenest.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.example.safenest.util.InboxAlertAction
import com.example.safenest.util.InboxAlertKind
import com.example.safenest.util.InboxRequestKind
import com.example.safenest.util.ParentInboxPresentation
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.ParentInboxViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/** Calm parent decision inbox: pending requests first, then today's informational alerts. */
class ParentInboxFragment : Fragment() {
    private val viewModel: ParentInboxViewModel by viewModels()

    private var progress: ProgressBar? = null
    private var countText: TextView? = null
    private var countBadge: TextView? = null
    private var emptyText: TextView? = null
    private var errorText: TextView? = null
    private var requestsContainer: LinearLayout? = null
    private var alertsContainer: LinearLayout? = null
    private var requestsHeading: TextView? = null
    private var requestsHelper: TextView? = null
    private var alertsHeading: TextView? = null
    private var alertsHelper: TextView? = null
    private var endOfList: View? = null
    private var markAllReadButton: MaterialButton? = null
    private var continueButton: MaterialButton? = null
    private var latestAlerts: List<AlertOut> = emptyList()
    private val pendingAlertResolutionIds = ArrayDeque<String>()
    private var isMarkingAllAlertsRead = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_parent_inbox, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress = view.findViewById(R.id.inboxProgress)
        countText = view.findViewById(R.id.inboxCount)
        countBadge = view.findViewById(R.id.inboxCountBadge)
        emptyText = view.findViewById(R.id.inboxEmptyText)
        errorText = view.findViewById(R.id.inboxError)
        requestsContainer = view.findViewById(R.id.requestsContainer)
        alertsContainer = view.findViewById(R.id.alertsContainer)
        requestsHeading = view.findViewById(R.id.requestsHeading)
        requestsHelper = view.findViewById(R.id.requestsHelper)
        alertsHeading = view.findViewById(R.id.alertsHeading)
        alertsHelper = view.findViewById(R.id.alertsHelper)
        endOfList = view.findViewById(R.id.inboxEndOfList)
        markAllReadButton = view.findViewById(R.id.markAllReadButton)
        continueButton = view.findViewById(R.id.continueHomeButton)

        view.findViewById<MaterialButton>(R.id.closeInboxButton).setOnClickListener { goHome() }
        continueButton?.setOnClickListener { goHome() }
        markAllReadButton?.setOnClickListener { markCurrentAlertsRead() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::renderState) }
                launch { viewModel.decisionState.collect(::renderDecisionState) }
                launch { viewModel.alertResolutionState.collect(::renderAlertResolutionState) }
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
                progress?.visibility = View.GONE
                errorText?.visibility = View.GONE
                renderData(state.data)
            }
            is Result.Error -> {
                progress?.visibility = View.GONE
                errorText?.text = "تعذر تحميل التنبيهات والطلبات. يمكنك إعادة المحاولة أو المتابعة إلى الرئيسية."
                errorText?.visibility = View.VISIBLE
                continueButton?.text = "إعادة المحاولة"
                continueButton?.setOnClickListener { viewModel.load() }
            }
            null -> Unit
        }
    }

    private fun renderData(data: ParentInboxData) {
        latestAlerts = data.alerts
        countText?.text = ParentInboxPresentation.pendingSummary(data.requests.size)
        countBadge?.text = data.requests.size.toString()
        markAllReadButton?.visibility = if (data.alerts.isEmpty()) View.INVISIBLE else View.VISIBLE
        continueButton?.text = "المتابعة إلى الرئيسية"
        continueButton?.setOnClickListener { goHome() }

        requestsContainer?.removeAllViews()
        alertsContainer?.removeAllViews()
        data.requests.forEach(::addRequestCard)
        data.alerts.forEach(::addAlertCard)

        val hasRequests = data.requests.isNotEmpty()
        val hasAlerts = data.alerts.isNotEmpty()
        requestsHeading?.visibility = if (hasRequests) View.VISIBLE else View.GONE
        requestsHelper?.visibility = if (hasRequests) View.VISIBLE else View.GONE
        alertsHeading?.visibility = if (hasAlerts) View.VISIBLE else View.GONE
        alertsHelper?.visibility = if (hasAlerts) View.VISIBLE else View.GONE
        emptyText?.visibility = if (!hasRequests && !hasAlerts) View.VISIBLE else View.GONE
        endOfList?.visibility = if (hasAlerts) View.VISIBLE else View.GONE

        errorText?.visibility = if (data.alertsUnavailable || data.requestsUnavailable) View.VISIBLE else View.GONE
        if (data.alertsUnavailable || data.requestsUnavailable) {
            errorText?.text = "بعض البيانات غير متاحة الآن. أعد المحاولة لاحقًا أو تابع إلى الرئيسية."
        }
    }

    private fun addRequestCard(request: AccessRequestItem) {
        val presentation = ParentInboxPresentation.request(request)
        val card = baseCard(stroke = true)
        val content = verticalContent()
        content.addView(cardHeader(requestIcon(presentation.kind), presentation.title, presentation.timestamp))
        content.addView(bodyText(presentation.detail))

        val actions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, dp(14), 0, 0)
        }
        val primary = filledActionButton(presentation.primaryActionLabel)
        val secondary = outlinedActionButton(presentation.secondaryActionLabel)
        primary.setOnClickListener { submitDecision(request, approved = true, primary, secondary) }
        secondary.setOnClickListener { submitDecision(request, approved = false, primary, secondary) }
        actions.addView(primary)
        actions.addView(secondary)
        content.addView(actions)
        card.addView(content)
        requestsContainer?.addView(card)
    }

    private fun addAlertCard(alert: AlertOut) {
        val presentation = ParentInboxPresentation.alert(alert)
        val card = baseCard(stroke = presentation.kind == InboxAlertKind.BLOCKED_APP || presentation.kind == InboxAlertKind.RISK_PLACE_ENTRY)
        val content = verticalContent()
        content.addView(cardHeader(alertIcon(presentation.kind), presentation.title, presentation.timestamp))
        content.addView(bodyText(presentation.detail))
        presentation.statusLabel?.let { content.addView(statusText(it, presentation.kind)) }
        when (presentation.action) {
            InboxAlertAction.REVIEW_APP_RULES -> content.addView(textAction("مراجعة قاعدة التطبيق") { reviewAppRules(alert) })
            InboxAlertAction.VIEW_LOCATION -> content.addView(textAction("عرض الموقع") { viewLocation(alert) })
            InboxAlertAction.NONE -> Unit
        }
        card.addView(content)
        alertsContainer?.addView(card)
    }

    private fun submitDecision(
        request: AccessRequestItem,
        approved: Boolean,
        primary: MaterialButton,
        secondary: MaterialButton,
    ) {
        primary.isEnabled = false
        secondary.isEnabled = false
        primary.text = "جارٍ الحفظ..."
        if (approved) viewModel.approve(request.requestId, request.requestedSeconds) else viewModel.reject(request.requestId)
    }

    private fun markCurrentAlertsRead() {
        if (isMarkingAllAlertsRead) return
        pendingAlertResolutionIds.clear()
        latestAlerts.filter { !it.isResolved }.forEach { pendingAlertResolutionIds.addLast(it.alertId) }
        if (pendingAlertResolutionIds.isEmpty()) return
        isMarkingAllAlertsRead = true
        markAllReadButton?.isEnabled = false
        markAllReadButton?.text = "جارٍ التحديث..."
        resolveNextAlert()
    }

    private fun resolveNextAlert() {
        pendingAlertResolutionIds.firstOrNull()?.let(viewModel::resolveAlert)
    }

    private fun renderAlertResolutionState(state: Result<AlertOut>?) {
        when (state) {
            is Result.Success -> {
                if (isMarkingAllAlertsRead) {
                    pendingAlertResolutionIds.removeFirstOrNull()
                    if (pendingAlertResolutionIds.isNotEmpty()) {
                        resolveNextAlert()
                    } else {
                        completeMarkAllRead("تم تحديد التنبيهات كمقروءة")
                    }
                } else {
                    Toast.makeText(requireContext(), "تم تعليم الإشعار كمقروء", Toast.LENGTH_SHORT).show()
                    viewModel.clearAlertResolutionState()
                    viewModel.load()
                }
            }
            is Result.Error -> {
                val failureMessage = if (isMarkingAllAlertsRead) {
                    "تعذر تحديد بعض التنبيهات كمقروءة"
                } else {
                    "تعذر تحديث الإشعار"
                }
                isMarkingAllAlertsRead = false
                pendingAlertResolutionIds.clear()
                markAllReadButton?.isEnabled = true
                markAllReadButton?.text = "تحديد كمقروء"
                Toast.makeText(requireContext(), failureMessage, Toast.LENGTH_LONG).show()
                viewModel.clearAlertResolutionState()
                viewModel.load()
            }
            else -> Unit
        }
    }

    private fun completeMarkAllRead(message: String) {
        isMarkingAllAlertsRead = false
        pendingAlertResolutionIds.clear()
        markAllReadButton?.isEnabled = true
        markAllReadButton?.text = "تحديد كمقروء"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        viewModel.clearAlertResolutionState()
        viewModel.load()
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

    private fun reviewAppRules(alert: AlertOut) {
        if (alert.deviceId.isBlank()) {
            Toast.makeText(requireContext(), "لا تتوفر بيانات الجهاز لمراجعة القاعدة الآن", Toast.LENGTH_SHORT).show()
            return
        }
        (activity as? MainActivity)?.navigateToFragment(InstalledAppsFragment())
    }

    private fun viewLocation(alert: AlertOut) {
        if (alert.deviceId.isBlank()) {
            Toast.makeText(requireContext(), "لا تتوفر بيانات الجهاز لعرض الموقع الآن", Toast.LENGTH_SHORT).show()
            return
        }
        (activity as? MainActivity)?.navigateToFragment(GpsFragment())
    }

    private fun baseCard(stroke: Boolean): MaterialCardView = MaterialCardView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) }
        radius = dp(16).toFloat()
        cardElevation = dp(1).toFloat()
        setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        if (stroke) {
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(requireContext(), R.color.daily_usage_coral)
        }
    }

    private fun verticalContent(): LinearLayout = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    private fun cardHeader(iconRes: Int, title: String, timestamp: String): View = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        addView(ImageView(requireContext()).apply {
            setImageResource(iconRes)
            contentDescription = null
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(10) }
        })
        addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            addView(TextView(requireContext()).apply {
                text = title
                textSize = 15f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_brand))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(requireContext()).apply {
                text = timestamp
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
            })
        })
    }

    private fun bodyText(text: String): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 13f
        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
        setPadding(0, dp(8), 0, 0)
    }

    private fun statusText(text: String, kind: InboxAlertKind): TextView = TextView(requireContext()).apply {
        this.text = text
        textSize = 12f
        setTextColor(ContextCompat.getColor(requireContext(), if (kind == InboxAlertKind.BLOCKED_APP) R.color.daily_usage_coral else R.color.teal_brand))
        setPadding(0, dp(10), 0, 0)
    }

    private fun filledActionButton(text: String): MaterialButton = MaterialButton(requireContext()).apply {
        this.text = text
        isAllCaps = false
        minHeight = dp(48)
        setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_brand))
        cornerRadius = dp(12)
        layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) }
    }

    private fun outlinedActionButton(text: String): MaterialButton = MaterialButton(requireContext()).apply {
        this.text = text
        isAllCaps = false
        minHeight = dp(48)
        setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_brand))
        strokeWidth = dp(1)
        strokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.teal_brand))
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        cornerRadius = dp(12)
        layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
    }

    private fun textAction(text: String, listener: () -> Unit): MaterialButton = MaterialButton(requireContext()).apply {
        this.text = text
        isAllCaps = false
        minHeight = dp(48)
        setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_brand))
        layoutParams = LinearLayout.LayoutParams(-2, dp(48)).apply { topMargin = dp(8) }
        setOnClickListener { listener() }
    }

    private fun requestIcon(kind: InboxRequestKind): Int = when (kind) {
        InboxRequestKind.EXTRA_TIME -> R.drawable.ic_inbox_clock
        InboxRequestKind.APP_ACCESS, InboxRequestKind.GENERAL -> R.drawable.ic_inbox_app
    }

    private fun alertIcon(kind: InboxAlertKind): Int = when (kind) {
        InboxAlertKind.BLOCKED_APP -> R.drawable.ic_inbox_block
        InboxAlertKind.SAFE_PLACE_EXIT -> R.drawable.ic_inbox_pin
        InboxAlertKind.SAFE_PLACE_ARRIVAL -> R.drawable.ic_inbox_home
        InboxAlertKind.ATTENTION_PLACE_ENTRY -> R.drawable.ic_inbox_pin
        InboxAlertKind.RISK_PLACE_ENTRY -> R.drawable.ic_alert_circle
        InboxAlertKind.GENERAL -> R.drawable.ic_inbox_check
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun goHome() {
        (activity as? MainActivity)?.showHomeFromInbox()
    }
}
