package com.calisvision.ui.guide

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.calisvision.R
import com.calisvision.domain.rules.Exercise
import com.calisvision.ui.components.CalisTopBar
import com.calisvision.ui.theme.CalisTheme

private val TIPS = listOf(
    R.string.guide_tip_side_title to R.string.guide_tip_side_body,
    R.string.guide_tip_height_title to R.string.guide_tip_height_body,
    R.string.guide_tip_distance_title to R.string.guide_tip_distance_body,
    R.string.guide_tip_hold_title to R.string.guide_tip_hold_body,
)

/** The only entry point to the gallery picker, so the guide is always seen first (AC-11). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShootingGuideScreen(
    exercise: Exercise,
    pickVideo: ActivityResultContract<PickVisualMediaRequest, Uri?>,
    onVideoPicked: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(pickVideo) { uri -> if (uri != null) onVideoPicked(uri) }
    val guide = exercise.shootingGuide
    Scaffold(
        topBar = { CalisTopBar(stringResource(R.string.guide_title), onBack = onBack) },
        containerColor = CalisTheme.colors.backgroundNormalNormal,
        bottomBar = {
            Button(
                onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                shape = RoundedCornerShape(CalisTheme.radius.component),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(CalisTheme.spacing.s20),
            ) {
                Text(stringResource(R.string.guide_pick_video), style = CalisTheme.typography.body1Bold)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CalisTheme.spacing.s20),
            verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s16),
        ) {
            Text(stringResource(R.string.guide_headline), style = CalisTheme.typography.title3Bold, color = CalisTheme.colors.labelStrong)
            Row(horizontalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8)) {
                Text(stringResource(R.string.guide_view_label), style = CalisTheme.typography.label1Medium, color = CalisTheme.colors.labelAlternative)
                Text(guide.view, style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.primaryNormal)
            }
            Text(guide.instruction, style = CalisTheme.typography.body1ReadingRegular, color = CalisTheme.colors.labelNeutral)
            TIPS.forEach { (title, body) -> TipCard(title, body) }
            Text(stringResource(R.string.guide_faults_covered), style = CalisTheme.typography.label1Bold, color = CalisTheme.colors.labelNeutral)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8),
                verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s8),
            ) {
                guide.faultsCovered.forEach { name ->
                    Surface(shape = RoundedCornerShape(CalisTheme.radius.large), color = CalisTheme.colors.fillAlternative) {
                        Text(
                            name,
                            style = CalisTheme.typography.label2Medium,
                            color = CalisTheme.colors.labelNeutral,
                            modifier = Modifier.padding(horizontal = CalisTheme.spacing.s12, vertical = CalisTheme.spacing.s4),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TipCard(@StringRes title: Int, @StringRes body: Int) {
    Surface(
        shape = RoundedCornerShape(CalisTheme.radius.component),
        color = CalisTheme.colors.backgroundElevatedNormal,
        border = BorderStroke(CalisTheme.spacing.divider, CalisTheme.colors.lineNormalNormal),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(CalisTheme.spacing.s16), verticalArrangement = Arrangement.spacedBy(CalisTheme.spacing.s4)) {
            Text(stringResource(title), style = CalisTheme.typography.headline2Bold, color = CalisTheme.colors.labelNormal)
            Text(stringResource(body), style = CalisTheme.typography.label1Regular, color = CalisTheme.colors.labelAlternative)
        }
    }
}
