/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.link

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.google.zxing.integration.android.IntentIntegrator
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.pairing.PhoneLinkSetup
import com.metrolist.music.pairing.RoofyPairingLinks
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroSurface
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkComputerScreen(
    navController: NavController,
    autoScan: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val dataStore = context.dataStore
    val coroutineScope = rememberCoroutineScope()
    val serverUrl by rememberPreference(PersonalLibraryServerUrlKey, "")
    val linked = serverUrl.isNotBlank()

    val scanLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val scanResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
            val contents = scanResult?.contents?.trim().orEmpty()
            if (contents.isBlank()) return@rememberLauncherForActivityResult
            val pairingUri = contents.toUri()
            val isDevicePairLink = RoofyPairingLinks.isDevicePairLink(pairingUri)
            coroutineScope.launch(Dispatchers.IO) {
                val ok =
                    PhoneLinkSetup.applyPairingUri(
                        context = context,
                        dataStore = dataStore,
                        uri = pairingUri,
                    )
                if (ok) {
                    withContext(Dispatchers.Main) {
                        if (isDevicePairLink) {
                            navController.navigate("link_computer/success") {
                                launchSingleTop = true
                            }
                        } else {
                            Toast.makeText(context, R.string.phone_link_paired, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.phone_link_pairing_invalid, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    fun openScanner() {
        val host = activity ?: return
        val intent =
            IntentIntegrator(host)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setPrompt(context.getString(R.string.phone_link_scan_prompt))
                .setBeepEnabled(false)
                .setOrientationLocked(false)
                .createScanIntent()
        scanLauncher.launch(intent)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openScanner()
            } else {
                Toast.makeText(context, R.string.phone_link_camera_denied, Toast.LENGTH_LONG).show()
            }
        }

    fun startScan() {
        val hasCamera =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (hasCamera) {
            openScanner()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var autoScanConsumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(autoScan) {
        if (autoScan && !autoScanConsumed) {
            autoScanConsumed = true
            startScan()
        }
    }

    Column(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        RetroSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.phone_link_screen_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RetroButton(
                    onClick = ::startScan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.phone_link_scan_qr))
                }

                if (linked) {
                    Text(
                        text = stringResource(R.string.phone_link_connected_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.phone_link_title)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
