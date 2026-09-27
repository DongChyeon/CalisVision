package com.calisvision.ui.result

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.calisvision.domain.model.BodySide
import com.calisvision.domain.model.Joint
import com.calisvision.domain.model.Landmark
import com.calisvision.domain.model.MIN_JOINT_VISIBILITY
import com.calisvision.ui.theme.CalisTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val BONES = listOf(
    Joint.WRIST to Joint.ELBOW,
    Joint.ELBOW to Joint.SHOULDER,
    Joint.SHOULDER to Joint.HIP,
    Joint.HIP to Joint.KNEE,
    Joint.KNEE to Joint.ANKLE,
    Joint.ANKLE to Joint.HEEL,
    Joint.HEEL to Joint.FOOT_INDEX,
    Joint.ANKLE to Joint.FOOT_INDEX,
)
private val DRAWN_JOINTS = listOf(Joint.WRIST, Joint.ELBOW, Joint.SHOULDER, Joint.HIP, Joint.KNEE, Joint.ANKLE)

/** Keeps the current sample and its neighbours decoded; one 640 px frame is ~1.6 MB. */
private const val CACHE_RADIUS = 2

private class FrameBitmapCache(private val path: (Int) -> File) {
    private val cache = LruCache<Int, ImageBitmap>(2 * CACHE_RADIUS + 1)

    fun get(sampleIndex: Int): ImageBitmap? = cache.get(sampleIndex) ?: load(sampleIndex)?.also { cache.put(sampleIndex, it) }

    private fun load(sampleIndex: Int): ImageBitmap? =
        path(sampleIndex).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.absolutePath) }?.asImageBitmap()
}

/**
 * The analysis JPEG of [sampleIndex] with the chosen side's skeleton on top. Landmarks are normalized to the JPEG
 * (same frame the detector saw), so the canvas only scales by its own size; the box keeps the JPEG [aspect].
 * Image and skeleton always come from the same sample: the previous pair stays on screen until the next frame decodes.
 */
@Composable
fun FrameWithSkeleton(
    sampleIndex: Int,
    aspect: Float,
    side: BodySide?,
    framePath: (Int) -> File,
    overlay: (Int) -> FrameOverlay,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val cache = remember(framePath) { FrameBitmapCache(framePath) }
    val loaded by produceState<Pair<Int, ImageBitmap?>?>(null, sampleIndex, cache) {
        value = sampleIndex to withContext(Dispatchers.IO) { cache.get(sampleIndex) }
        withContext(Dispatchers.IO) {
            for (d in 1..CACHE_RADIUS) {
                cache.get(sampleIndex + d)
                if (sampleIndex - d >= 0) cache.get(sampleIndex - d)
            }
        }
    }
    val shown = loaded
    val colors = CalisTheme.appColors
    val spacing = CalisTheme.spacing
    Box(
        modifier
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(CalisTheme.radius.component))
            .background(CalisTheme.colors.fillAlternative)
            .testTag("frame")
            .semantics { this.contentDescription = contentDescription },
    ) {
        shown?.second?.let { Image(it, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()) }
        if (shown != null && side != null) {
            val frame = overlay(shown.first)
            Canvas(Modifier.fillMaxSize()) {
                val points = frame.pose?.let { pose ->
                    Joint.entries.associateWith { pose[side, it] }.filterValues { it != null && it.visibility >= MIN_JOINT_VISIBILITY }
                }.orEmpty()
                fun at(joint: Joint): Offset? = points[joint]?.toOffset(this)
                val wrist = at(Joint.WRIST)
                val ankle = at(Joint.ANKLE)
                if (wrist != null && ankle != null) {
                    drawLine(
                        colors.skeletonLine, wrist, ankle,
                        strokeWidth = spacing.s2.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(spacing.s8.toPx(), spacing.s4.toPx())),
                    )
                }
                BONES.forEach { (a, b) ->
                    val pa = at(a) ?: return@forEach
                    val pb = at(b) ?: return@forEach
                    drawLine(colors.skeletonLine, pa, pb, strokeWidth = spacing.s4.toPx(), cap = StrokeCap.Round)
                }
                DRAWN_JOINTS.forEach { joint ->
                    val p = at(joint) ?: return@forEach
                    val broken = joint in frame.faultJoints
                    drawCircle(colors.skeletonLine, radius = (if (broken) spacing.s8 else spacing.s4).toPx() + spacing.s2.toPx(), center = p)
                    drawCircle(if (broken) colors.fault else colors.jointNormal, radius = (if (broken) spacing.s8 else spacing.s4).toPx(), center = p)
                }
            }
        }
    }
}

private fun Landmark.toOffset(scope: DrawScope) = Offset(x * scope.size.width, y * scope.size.height)
