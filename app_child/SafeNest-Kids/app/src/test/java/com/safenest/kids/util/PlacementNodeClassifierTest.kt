package com.safenest.kids.util

import com.safenest.kids.service.NodeRole
import com.safenest.kids.service.RegionBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlacementNodeClassifierTest {
    @Test
    fun detectsCommonVideoSurfacesAndLocalizedDescriptions() {
        val surfaceCandidate = PlacementNodeClassifier.classify(
            descriptor(className = "android.view.TextureView"),
        )
        val arabicCandidate = PlacementNodeClassifier.classify(
            descriptor(className = "android.view.View", contentDescription = "تشغيل فيديو"),
        )

        assertNotNull(surfaceCandidate)
        assertNotNull(arabicCandidate)
        assertEquals(NodeRole.VIDEO, surfaceCandidate?.role)
        assertEquals(NodeRole.VIDEO, arabicCandidate?.role)
    }

    @Test
    fun detectsMediaIdentifiersWithoutMakingUnrelatedViewsCandidates() {
        val thumbnailCandidate = PlacementNodeClassifier.classify(
            descriptor(className = "android.view.View", viewId = "id/media_thumbnail"),
        )
        val unrelatedCandidate = PlacementNodeClassifier.classify(
            descriptor(className = "android.widget.TextView", contentDescription = "إعدادات"),
        )

        assertNotNull(thumbnailCandidate)
        assertEquals(NodeRole.IMAGE, thumbnailCandidate?.role)
        assertNull(unrelatedCandidate)
    }

    private fun descriptor(
        className: String,
        contentDescription: String? = null,
        viewId: String? = null,
    ) = PlacementNodeDescriptor(
        packageName = "com.example.target",
        className = className,
        contentDescription = contentDescription,
        viewId = viewId,
        bounds = RegionBounds(0, 0, 100, 100),
        isVisible = true,
        childIndex = 0,
    )
}
