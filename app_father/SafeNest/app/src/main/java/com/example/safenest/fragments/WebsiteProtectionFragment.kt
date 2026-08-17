package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.WebsitePolicyResponse
import com.example.safenest.network.WebsiteRuleResponse
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.WebsiteProtectionViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class WebsiteProtectionFragment : Fragment() {
    private val viewModel: WebsiteProtectionViewModel by viewModels()
    private lateinit var modeGroup: RadioGroup
    private lateinit var modeSummary: TextView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var rulesContainer: LinearLayout
    private lateinit var hostInput: EditText
    private lateinit var statusText: TextView
    private var policy: WebsitePolicyResponse? = null
    private var selectedCategories = mutableSetOf<String>()
    private var restoring = false
    private val categories = listOf(
        "adult" to "المحتوى الإباحي والعري",
        "gambling" to "المقامرة",
        "drugs" to "المخدرات",
        "violence" to "العنف",
        "self_harm" to "إيذاء النفس",
        "dangerous_downloads" to "التنزيلات الخطرة"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_website_protection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        modeGroup = view.findViewById(R.id.websiteModeGroup)
        modeSummary = view.findViewById(R.id.websiteModeSummary)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        rulesContainer = view.findViewById(R.id.websiteRulesContainer)
        hostInput = view.findViewById(R.id.websiteHostInput)
        statusText = view.findViewById(R.id.websiteProtectionStatus)

        view.findViewById<MaterialButton>(R.id.websiteBackButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<MaterialButton>(R.id.addWebsiteBlockButton).setOnClickListener { addRule("block") }
        view.findViewById<MaterialButton>(R.id.addWebsiteAllowButton).setOnClickListener { addRule("allow") }
        view.findViewById<MaterialButton>(R.id.publishWebsiteButton).setOnClickListener { publishAndAssign() }

        modeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (restoring) return@setOnCheckedChangeListener
            val mode = if (checkedId == R.id.websiteAllowlistMode) "allowlist" else "blocklist"
            updateSummary(mode)
            policy?.let { viewModel.updateMode(it.policyId, mode, selectedCategories.toList()) }
        }
        buildCategoryChecks()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.policyState.collect { state ->
                        when (state) {
                            is Result.Loading -> statusText.text = "جاري تحميل سياسة المواقع..."
                            is Result.Success -> showPolicy(state.data)
                            is Result.Error -> statusText.text = "تعذر تحميل سياسة المواقع: ${state.message}"
                            null -> Unit
                        }
                    }
                }
                launch {
                    viewModel.rulesState.collect { state ->
                        when (state) {
                            is Result.Loading -> Unit
                            is Result.Success -> renderRules(state.data.items)
                            is Result.Error -> statusText.text = "تعذر تحميل المواقع المضافة: ${state.message}"
                            null -> Unit
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Result.Loading -> statusText.text = "جاري حفظ سياسة المواقع..."
                            is Result.Success<*> -> {
                                statusText.text = "تم حفظ التغيير محليًا على الخادم. اضغط نشر لتطبيقه على جهاز الطفل."
                                policy?.let { viewModel.loadRules(it.policyId) }
                            }
                            is Result.Error -> statusText.text = "فشل حفظ سياسة المواقع: ${state.message}"
                            null -> Unit
                        }
                    }
                }
            }
        }
        viewModel.load()
    }

    private fun showPolicy(value: WebsitePolicyResponse) {
        policy = value
        selectedCategories = value.mandatoryBlockedCategories.toMutableSet()
        restoring = true
        modeGroup.check(if (value.websiteControlMode == "allowlist") R.id.websiteAllowlistMode else R.id.websiteBlocklistMode)
        restoring = false
        updateSummary(value.websiteControlMode)
        refreshCategoryChecks()
        viewModel.loadRules(value.policyId)
        statusText.text = if (value.status == "published") {
            "السياسة منشورة بالإصدار ${value.currentVersion}. أي تعديل جديد يحتاج حفظًا ثم نشرًا مرة أخرى."
        } else {
            "السياسة مسودة ولم تُنشر بعد."
        }
    }

    private fun updateSummary(mode: String) {
        modeSummary.text = if (mode == "allowlist") {
            "المواقع التي تسمح بها فقط ستعمل، وأي موقع جديد سيتم حجبه تلقائيًا."
        } else {
            "المواقع التي تحظرها فقط سيتم منعها، والمواقع الجديدة ستظل متاحة."
        }
    }

    private fun buildCategoryChecks() {
        categoryContainer.removeAllViews()
        categories.forEach { (key, label) ->
            val check = CheckBox(requireContext()).apply {
                text = label
                tag = key
                minHeight = 52
                setOnCheckedChangeListener { _, checked ->
                    if (restoring) return@setOnCheckedChangeListener
                    if (checked) selectedCategories.add(key) else selectedCategories.remove(key)
                    policy?.let {
                        viewModel.updateMode(it.policyId, currentMode(), selectedCategories.toList())
                    }
                }
            }
            categoryContainer.addView(check)
        }
    }

    private fun refreshCategoryChecks() {
        restoring = true
        for (index in 0 until categoryContainer.childCount) {
            val check = categoryContainer.getChildAt(index) as? CheckBox ?: continue
            check.isChecked = selectedCategories.contains(check.tag as String)
        }
        restoring = false
    }

    private fun currentMode(): String = if (modeGroup.checkedRadioButtonId == R.id.websiteAllowlistMode) "allowlist" else "blocklist"

    private fun addRule(action: String) {
        val host = hostInput.text.toString().trim()
        if (host.isBlank()) {
            hostInput.error = "اكتب اسم الموقع مثل example.com"
            return
        }
        val current = policy ?: run {
            Toast.makeText(context, "انتظر تحميل السياسة أولًا", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.addHost(current.policyId, host, action)
        hostInput.text?.clear()
    }

    private fun renderRules(rules: List<WebsiteRuleResponse>) {
        rulesContainer.removeAllViews()
        if (rules.isEmpty()) {
            statusText.text = "لا توجد مواقع مخصصة بعد."
            return
        }
        rules.forEach { rule ->
            val card = MaterialCardView(requireContext()).apply {
                radius = 14f
                cardElevation = 1f
                setContentPadding(16, 8, 16, 8)
            }
            val text = TextView(requireContext()).apply {
                text = "${rule.normalizedPattern} — ${if (rule.action == "block") "محظور" else "مسموح"}"
                textSize = 15f
                setTextColor(resources.getColor(R.color.navy_brand, null))
            }
            card.addView(text)
            rulesContainer.addView(card)
        }
    }

    private fun publishAndAssign() {
        val current = policy ?: return
        if (currentMode() == "allowlist" && selectedCategories.isEmpty()) {
            Toast.makeText(context, "وضع السماح سيحظر المواقع غير المضافة. راجع المواقع قبل النشر.", Toast.LENGTH_LONG).show()
        }
        statusText.text = "جاري نشر سياسة المواقع..."
        viewModel.publish(current.policyId)
        val childId = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
            .getString("selected_child_id", null)
        if (childId != null) viewModel.assignSelectedChild(current.policyId, childId)
    }
}
