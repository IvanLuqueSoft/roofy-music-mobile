package com.metrolist.music.ui.screens.equalizer

import android.annotation.SuppressLint
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.metrolist.music.ui.component.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.eq.data.SavedEQProfile
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroTokens
import timber.log.Timber

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun EqScreen(
    navController: NavController,
    viewModel: EQViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showError by remember { mutableStateOf<String?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                var fileName = "custom_eq.txt"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex >= 0) {
                            val name = cursor.getString(displayNameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                }
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importCustomProfile(
                        fileName = fileName,
                        inputStream = inputStream,
                        onSuccess = {
                            Timber.d("Custom EQ profile imported successfully: $fileName")
                        },
                        onError = { error ->
                            Timber.d("Error: Unable to import Custom EQ profile: $fileName")
                            showError = context.getString(R.string.import_error_title) + ": " + error.message
                        })
                } else {
                    showError = context.getString(R.string.error_file_read)
                }
            } catch (e: Exception) {
                showError = context.getString(R.string.error_file_open, e.message)
            }
        }
    }

    val activeProfile = state.profiles.find { it.id == state.activeProfileId }

    EqScreenContent(
        profiles = state.profiles,
        activeProfileId = state.activeProfileId,
        activeProfile = activeProfile,
        onProfileSelected = { viewModel.selectProfile(it) },
        onNavigateBack = { navController.navigateUp() },
        onWizardClicked = { navController.navigate("eq_wizard") },
        onImportClicked = {
            filePickerLauncher.launch("text/plain")
        },
        onDeleteProfile = { viewModel.deleteProfile(it) }
    )

    if (showError != null) {
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text(stringResource(R.string.import_error_title)) },
            text = { Text(showError ?: "") },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(stringResource(R.string.error_title)) },
            text = { Text(stringResource(R.string.error_eq_apply_failed, state.error ?: "")) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqScreenContent(
    profiles: List<SavedEQProfile>,
    activeProfileId: String?,
    activeProfile: SavedEQProfile?,
    onProfileSelected: (String?) -> Unit,
    onNavigateBack: () -> Unit,
    onWizardClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.equalizer_header)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = LocalPlayerAwareWindowInsets.current
                .asPaddingValues().calculateBottomPadding())
        ) {
            item {
                EqFrequencyResponseGraph(
                    bands = activeProfile?.bands ?: emptyList(),
                    preamp = activeProfile?.preamp ?: 0.0
                )
            }

            item {
                NoEqualizationItem(
                    isSelected = activeProfileId == null,
                    onSelected = { onProfileSelected(null) }
                )
            }

            val customProfiles = profiles.filter { it.isCustom }

            if (customProfiles.isNotEmpty()) {
                items(customProfiles) { profile ->
                    EQProfileItem(
                        profile = profile,
                        isSelected = activeProfileId == profile.id,
                        onSelected = { onProfileSelected(profile.id) },
                        onDelete = { onDeleteProfile(profile.id) }
                    )
                }
            }

            if (customProfiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.equalizer),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = RetroTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_profiles),
                                style = MaterialTheme.typography.titleMedium,
                                color = RetroTokens.TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    RetroButton(
                        onClick = onWizardClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("[ WIZARD ]", style = MaterialTheme.typography.labelSmall, color = RetroTokens.Text)
                    }
                    RetroButton(
                        onClick = onImportClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("[ IMPORT ]", style = MaterialTheme.typography.labelSmall, color = RetroTokens.Text)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EqFrequencyResponseGraph(
    bands: List<com.metrolist.music.eq.data.ParametricEQBand>,
    preamp: Double
) {
    if (bands.isEmpty()) return

    RetroPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 16.dp.toPx()
            val graphWidth = width - padding * 2
            val graphHeight = height - padding * 2

            // Grid lines
            val gridColor = RetroTokens.BorderDark
            val lineColor = RetroTokens.Border

            // Horizontal grid lines
            for (i in 0..4) {
                val y = padding + (graphHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(width - padding, y),
                    strokeWidth = 1f
                )
            }

            // Vertical grid lines
            for (i in 0..bands.size) {
                val x = padding + (graphWidth / bands.size) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, padding),
                    end = Offset(x, height - padding),
                    strokeWidth = 1f
                )
            }

            // Frequency response line
            if (bands.isNotEmpty()) {
                val points = bands.mapIndexed { index, band ->
                    val x = padding + (graphWidth / (bands.size - 1).coerceAtLeast(1)) * index
                    val y = height / 2 - (band.gain.toFloat() * (graphHeight / 30f))
                    Offset(x, y.coerceIn(padding, height - padding))
                }

                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

@Composable
private fun NoEqualizationItem(
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .background(RetroTokens.Panel)
            .border(1.dp, if (isSelected) RetroTokens.Border else RetroTokens.BorderDark, RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (isSelected) "[*]" else "[ ]",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) RetroTokens.TextHot else RetroTokens.TextDim,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = stringResource(R.string.eq_disabled),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) RetroTokens.TextHot else RetroTokens.Text,
        )
    }
}

@Composable
private fun EQProfileItem(
    profile: SavedEQProfile,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelected)
            .background(RetroTokens.Panel)
            .border(1.dp, if (isSelected) RetroTokens.Border else RetroTokens.BorderDark, RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (isSelected) "[*]" else "[ ]",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) RetroTokens.TextHot else RetroTokens.TextDim,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.deviceModel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = RetroTokens.Text,
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.band_count,
                    count = profile.bands.size,
                    profile.bands.size
                ),
                style = MaterialTheme.typography.bodySmall,
                color = RetroTokens.TextMuted,
            )
        }
        RetroButton(
            onClick = { showDeleteDialog = true },
        ) {
            Text("[x]", style = MaterialTheme.typography.labelSmall, color = RetroTokens.TextMuted)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_profile_desc)) },
            text = {
                Text(
                    stringResource(R.string.delete_profile_confirmation, profile.name)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
