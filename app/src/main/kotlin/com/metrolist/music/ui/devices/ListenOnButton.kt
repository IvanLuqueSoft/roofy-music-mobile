/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.device.DeviceSessionManager
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.utils.rememberPreference

@Composable
fun ListenOnButton(
    navController: NavController,
    modifier: Modifier = Modifier,
    tintColor: Color,
    playerBottomSheetState: BottomSheetState? = null,
) {
    val menuState = LocalMenuState.current
    val sessionUi by DeviceSessionManager.uiState.collectAsStateWithLifecycle()
    val isPaired = sessionUi.isPaired
    val isControllingComputer = sessionUi.playbackTarget == "computer"

    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tintColor.copy(alpha = if (isControllingComputer) 0.32f else 0.18f))
                .clickable {
                    menuState.show {
                        ListenOnSheet(
                            onDismiss = menuState::dismiss,
                            navController = navController,
                            playerBottomSheetState = playerBottomSheetState,
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (isControllingComputer) R.drawable.cast_connected else R.drawable.cast),
            contentDescription = stringResource(R.string.listen_on_title),
            tint =
                if (isControllingComputer) {
                    tintColor
                } else if (isPaired) {
                    tintColor.copy(alpha = 0.85f)
                } else {
                    tintColor.copy(alpha = 0.65f)
                },
            modifier = Modifier.size(22.dp),
        )
    }
}
