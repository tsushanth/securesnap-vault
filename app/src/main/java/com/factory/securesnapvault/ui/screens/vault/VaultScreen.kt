package com.factory.securesnapvault.ui.screens.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.factory.securesnapvault.R
import com.factory.securesnapvault.data.model.MediaItem
import com.factory.securesnapvault.data.model.MediaType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FREE_ITEM_LIMIT = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    isPremium: Boolean,
    onMediaClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onPaywallClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val mediaCount by viewModel.mediaCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(uris)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text(stringResource(R.string.vault_selected_count, uiState.selectedItems.size))
                    } else {
                        Column {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                stringResource(R.string.vault_items_count, mediaCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.vault_clear_selection))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteDialog = true
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.vault_delete_selected),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Box {
                            IconButton(onClick = {
                                if (isPremium) {
                                    showFilterMenu = true
                                } else {
                                    onPaywallClick()
                                }
                            }) {
                                Box {
                                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.vault_filter))
                                    if (!isPremium) {
                                        ProBadge(modifier = Modifier.align(Alignment.TopEnd))
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_filter_all)) },
                                    onClick = {
                                        viewModel.setFilter(MediaFilter.ALL)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.FilterList, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_filter_photos)) },
                                    onClick = {
                                        viewModel.setFilter(MediaFilter.IMAGES)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.vault_filter_videos)) },
                                    onClick = {
                                        viewModel.setFilter(MediaFilter.VIDEOS)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.VideoLibrary, null) }
                                )
                            }
                        }
                        if (!isPremium) {
                            IconButton(onClick = onPaywallClick) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = stringResource(R.string.vault_upgrade_pro),
                                    tint = Color(0xFFFFD700)
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (!isPremium && mediaCount >= FREE_ITEM_LIMIT) {
                            onPaywallClick()
                        } else if (isPremium) {
                            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                        } else {
                            mediaPickerLauncher.launch(arrayOf("image/*"))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.vault_import_media),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (mediaItems.isEmpty() && !uiState.isImporting) {
                EmptyVaultContent()
            } else {
                MediaGrid(
                    mediaItems = mediaItems,
                    selectedItems = uiState.selectedItems,
                    isSelectionMode = uiState.isSelectionMode,
                    onItemClick = { item ->
                        if (uiState.isSelectionMode) {
                            viewModel.toggleSelection(item.id)
                        } else {
                            onMediaClick(item.id)
                        }
                    },
                    onItemLongClick = { item ->
                        if (isPremium) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleSelection(item.id)
                        } else {
                            onPaywallClick()
                        }
                    }
                )
            }

            val importingDescription = stringResource(R.string.cd_importing)
            AnimatedVisibility(
                visible = uiState.isImporting,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics { contentDescription = importingDescription }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(32.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.vault_importing))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.vault_delete_title)) },
            text = {
                Text(stringResource(R.string.vault_delete_message, uiState.selectedItems.size))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteSelected()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(R.string.vault_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.vault_cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyVaultContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.vault_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.vault_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGrid(
    mediaItems: List<MediaItem>,
    selectedItems: Set<Long>,
    isSelectionMode: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(mediaItems, key = { it.id }) { item ->
            val isSelected = item.id in selectedItems
            val itemDescription = if (isSelectionMode) {
                if (isSelected) "${item.originalFileName}, ${stringResource(R.string.cd_selected)}"
                else "${item.originalFileName}, ${stringResource(R.string.cd_unselected)}"
            } else {
                item.originalFileName
            }

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (isSelected) Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(4.dp)
                        ) else Modifier
                    )
                    .semantics { contentDescription = itemDescription }
                    .combinedClickable(
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onClickLabel = if (isSelectionMode) {
                            if (isSelected) stringResource(R.string.vault_clear_selection) else stringResource(R.string.cd_selected)
                        } else null,
                        onLongClickLabel = stringResource(R.string.vault_clear_selection)
                    )
            ) {
                val imagePath = item.thumbnailPath ?: item.vaultPath
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (item.mediaType == MediaType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = stringResource(R.string.cd_video),
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                    if (item.duration > 0) {
                        Text(
                            text = formatDuration(item.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isSelectionMode) {
                    val selectedDesc = stringResource(R.string.cd_selected)
                    val unselectedDesc = stringResource(R.string.cd_unselected)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = selectedDesc,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.White, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(2.dp, Color.White, CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .semantics { contentDescription = unselectedDesc }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Text(
        text = "PRO",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 7.sp,
        modifier = modifier
            .background(
                Color(0xFFFFD700),
                RoundedCornerShape(3.dp)
            )
            .padding(horizontal = 3.dp, vertical = 1.dp)
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
