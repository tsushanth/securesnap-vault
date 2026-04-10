package com.factory.securesnapvault.ui.screens.vault

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factory.securesnapvault.data.model.MediaItem
import com.factory.securesnapvault.data.model.MediaType
import com.factory.securesnapvault.data.repository.MediaRepository
import com.factory.securesnapvault.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MediaFilter {
    ALL, IMAGES, VIDEOS
}

data class VaultUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val selectedItems: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val filter: MediaFilter = MediaFilter.ALL
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private val allMedia = repository.getAllMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaItems: StateFlow<List<MediaItem>> = combine(
        allMedia,
        _uiState
    ) { items, state ->
        when (state.filter) {
            MediaFilter.ALL -> items
            MediaFilter.IMAGES -> items.filter { it.mediaType == MediaType.IMAGE }
            MediaFilter.VIDEOS -> items.filter { it.mediaType == MediaType.VIDEO }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mediaCount: StateFlow<Int> = repository.getMediaCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun importMedia(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            val deleteOriginal = preferencesManager.deleteOriginalAfterImport.first()
            var successCount = 0
            var failCount = 0
            for (uri in uris) {
                try {
                    repository.importMedia(uri)
                    successCount++
                    if (deleteOriginal) {
                        tryDeleteOriginal(uri)
                    }
                } catch (e: Exception) {
                    failCount++
                }
            }
            val error = when {
                failCount > 0 && successCount > 0 ->
                    "Imported $successCount item(s), $failCount failed"
                failCount > 0 ->
                    "Failed to import $failCount item(s)"
                else -> null
            }
            _uiState.value = _uiState.value.copy(isImporting = false, error = error)
        }
    }

    private fun tryDeleteOriginal(uri: Uri) {
        try {
            val context = getApplication<Application>()
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {
            // Some providers don't allow deletion; silently ignore
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedItems
            try {
                selected.forEach { id ->
                    repository.deleteMediaById(id)
                }
                _uiState.value = _uiState.value.copy(
                    selectedItems = emptySet(),
                    isSelectionMode = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete: ${e.message}"
                )
            }
        }
    }

    fun deleteItem(mediaItem: MediaItem) {
        viewModelScope.launch {
            try {
                repository.deleteMedia(mediaItem)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete: ${e.message}"
                )
            }
        }
    }

    fun toggleSelection(id: Long) {
        val current = _uiState.value
        val newSelection = if (id in current.selectedItems) {
            current.selectedItems - id
        } else {
            current.selectedItems + id
        }
        _uiState.value = current.copy(
            selectedItems = newSelection,
            isSelectionMode = newSelection.isNotEmpty()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItems = emptySet(),
            isSelectionMode = false
        )
    }

    fun setFilter(filter: MediaFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
