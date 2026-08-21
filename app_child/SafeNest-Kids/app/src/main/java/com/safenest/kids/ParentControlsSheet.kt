package com.safenest.kids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ParentControlsSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.sheet_parent_controls, container, false).also { view ->
        view.findViewById<View>(R.id.action_sign_out).setOnClickListener {
            sendAction(ParentControlAction.SIGN_OUT)
        }
        view.findViewById<View>(R.id.action_suspend_protection).setOnClickListener {
            sendAction(ParentControlAction.SUSPEND_PROTECTION)
        }
    }

    private fun sendAction(action: ParentControlAction) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(BUNDLE_ACTION to action.name),
        )
        dismiss()
    }

    companion object {
        const val TAG = "parent_controls_sheet"
        const val REQUEST_KEY = "parent_controls_action"
        const val BUNDLE_ACTION = "parent_controls_action_name"
    }
}
