package com.safenest.kids.util

import com.safenest.kids.service.CandidateRegion
import com.safenest.kids.service.NodeRole
import com.safenest.kids.service.RegionBounds

/** A serializable description extracted by the Android shell from an AccessibilityNodeInfo. */
data class PlacementNodeDescriptor(
    val packageName: String,
    val className: String?,
    val contentDescription: String?,
    val viewId: String?,
    val bounds: RegionBounds,
    val isVisible: Boolean,
    val childIndex: Int,
)

object PlacementNodeClassifier {
    fun classify(node: PlacementNodeDescriptor): CandidateRegion? {
        if (!node.isVisible || node.bounds.isEmpty()) return null
        val className = node.className.orEmpty()
        val description = node.contentDescription.orEmpty()
        val viewId = node.viewId.orEmpty()
        val role = when {
            className.containsAny("Video", "TextureView", "SurfaceView", "Player", "ExoPlayer") ||
                description.containsAny("video", "فيديو") ||
                viewId.containsAny("video", "player", "reel", "story") -> NodeRole.VIDEO
            className.containsAny("ImageView", "PhotoView", "Image") ||
                description.containsAny("image", "photo", "صورة") ||
                viewId.containsAny("image", "photo", "thumbnail", "media") -> NodeRole.IMAGE
            else -> return null
        }
        val stableKey = node.viewId?.takeIf { it.isNotBlank() }
            ?: "${node.packageName}:${node.className}:${node.childIndex}:${node.bounds.left}:${node.bounds.top}"
        val treeHash = listOf(node.className, node.contentDescription, node.viewId, node.childIndex).joinToString("|").hashCode().toString()
        return CandidateRegion(node.packageName, stableKey, treeHash, node.bounds, role)
    }

    private fun String.containsAny(vararg values: String): Boolean =
        values.any { value -> contains(value, ignoreCase = true) }
}
