package com.example.safenest.fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.VideoHistoryViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class VideoHistoryFragment : Fragment() {

    companion object {
        private const val TAG = "VideoHistoryFragment"
    }

    private val viewModel: VideoHistoryViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var videoHistoryCard: MaterialCardView? = null
    private var videoHistoryList: LinearLayout? = null
    private var clearHistoryBtn: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video_history, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        videoHistoryCard = view.findViewById(R.id.videoHistoryCard)
        videoHistoryList = view.findViewById(R.id.videoHistoryList)
        clearHistoryBtn = view.findViewById(R.id.clearHistoryBtn)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        clearHistoryBtn?.setOnClickListener {
            val childId = viewModel.getSelectedChildId() ?: return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("مسح السجل")
                .setMessage("هل تريد مسح سجل الفيديوهات؟")
                .setPositiveButton("مسح") { _, _ ->
                    viewModel.clearVideoHistory(childId)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe video history fetch state
                launch {
                    viewModel.videoHistoryState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                emptyText?.visibility = View.GONE
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                val items = state.data
                                if (items.isEmpty()) {
                                    emptyText?.text = "لا يوجد سجل فيديوهات"
                                    emptyText?.visibility = View.VISIBLE
                                    videoHistoryCard?.visibility = View.GONE
                                } else {
                                    emptyText?.visibility = View.GONE
                                    videoHistoryCard?.visibility = View.VISIBLE
                                    renderList(items)
                                }
                                viewModel.clearVideoHistoryState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, getString(R.string.error_loading_video_history), Toast.LENGTH_SHORT).show()
                                viewModel.clearVideoHistoryState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Observe clear video history state
                launch {
                    viewModel.clearVideoHistoryState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                clearHistoryBtn?.isEnabled = false
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                clearHistoryBtn?.isEnabled = true
                                Toast.makeText(context, "تم مسح السجل بنجاح", Toast.LENGTH_SHORT).show()
                                // Re-fetch to confirm empty state from server
                                val childId = viewModel.getSelectedChildId()
                                if (childId != null) viewModel.getVideoHistory(childId)
                                viewModel.clearClearVideoHistoryState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                clearHistoryBtn?.isEnabled = true
                                Toast.makeText(context, getString(R.string.error_clear_video_history, state.message), Toast.LENGTH_LONG).show()
                                viewModel.clearClearVideoHistoryState()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }

        val childId = viewModel.getSelectedChildId()
        if (childId != null) {
            viewModel.getVideoHistory(childId)
        } else {
            emptyText?.text = getString(R.string.error_no_child)
            emptyText?.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        val childId = viewModel.getSelectedChildId() ?: return
        viewModel.getVideoHistory(childId)
    }

    private fun renderList(items: List<Map<String, Any>>) {
        val ctx = requireContext()
        videoHistoryList?.removeAllViews()

        items.forEach { item ->
            val appPkg = item["app"]?.toString() ?: ""
            val videoTitle = item["videoTitle"]?.toString()
                ?: item["title"]?.toString()
                ?: ""
            val videoUrl = item["videoUrl"]?.toString()
                ?: item["url"]?.toString()
                ?: ""

            // Row: title+app column | open-link icon
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }

            // Left column: title + app package
            val textCol = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(ctx).apply {
                text = videoTitle.ifBlank { videoUrl }
                textSize = 14f
                setTextColor(android.graphics.Color.BLACK)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            if (appPkg.isNotBlank()) {
                textCol.addView(TextView(ctx).apply {
                    text = appPkg
                    textSize = 11f
                    setTextColor(android.graphics.Color.GRAY)
                    setPadding(0, 4, 0, 0)
                })
            }

            // Open-link icon
            val linkIcon = ImageView(ctx).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                layoutParams = LinearLayout.LayoutParams(72, 72).apply {
                    setMargins(8, 0, 0, 0)
                }
                setPadding(8, 8, 8, 8)
                setColorFilter(android.graphics.Color.parseColor("#692AC8")) // purple_dark approx
                setOnClickListener {
                    if (videoUrl.isNotBlank()) {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "لا يمكن فتح الرابط", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(ctx, "لا يمكن فتح الرابط", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            row.addView(textCol)
            row.addView(linkIcon)
            videoHistoryList?.addView(row)

            // Divider
            videoHistoryList?.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(0, 0, 0, 8) }
                setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            })
        }
    }
}
