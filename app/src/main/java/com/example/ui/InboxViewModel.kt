package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.domain.InboxRepository
import com.example.data.domain.SaveResult
import com.example.data.preferences.SettingsRepository
import com.example.data.storage.FileStorageManager
import com.example.device.DeviceInfoProvider
import com.example.device.StorageInfoProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InboxUiState(
    val searchQuery: String = "",
    val selectedTypeFilter: String? = null,
    val selectedFolderId: Long? = null,
    val isArchiveTab: Boolean = false,
    val isFavoritesTab: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isGridView: Boolean = false
)

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    val repository: InboxRepository
    val settingsRepository: SettingsRepository
    val deviceInfoProvider: DeviceInfoProvider
    val storageInfoProvider: StorageInfoProvider

    init {
        val db = AppDatabase.getDatabase(application)
        val fileManager = FileStorageManager(application)
        settingsRepository = SettingsRepository(application)
        repository = InboxRepository(application, db.inboxDao(), fileManager, settingsRepository)
        deviceInfoProvider = DeviceInfoProvider(application)
        storageInfoProvider = StorageInfoProvider(application)
    }

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    val folders: StateFlow<List<InboxFolder>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<InboxTag>> = repository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storageUsage: StateFlow<List<TypeStorageUsage>> = repository.getStorageUsageByType()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInboxSize: StateFlow<Long> = repository.getTotalInboxSize()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val items: StateFlow<List<InboxItem>> = _uiState.flatMapLatest { state ->
        when {
            state.isFavoritesTab -> repository.getFavoriteItems()
            state.isArchiveTab -> repository.getArchivedItems()
            state.selectedFolderId != null -> repository.getItemsByFolder(state.selectedFolderId)
            state.searchQuery.isNotBlank() || state.selectedTypeFilter != null ->
                repository.searchItems(state.searchQuery, state.selectedTypeFilter, state.isArchiveTab)
            else -> repository.getAllItems()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setTypeFilter(type: String?) {
        _uiState.update { it.copy(selectedTypeFilter = if (it.selectedTypeFilter == type) null else type) }
    }

    fun setFolderFilter(folderId: Long?) {
        _uiState.update { it.copy(selectedFolderId = folderId, isFavoritesTab = false, isArchiveTab = false) }
    }

    fun setFavoritesTab(enabled: Boolean) {
        _uiState.update { it.copy(isFavoritesTab = enabled, isArchiveTab = false, selectedFolderId = null) }
    }

    fun setArchiveTab(enabled: Boolean) {
        _uiState.update { it.copy(isArchiveTab = enabled, isFavoritesTab = false, selectedFolderId = null) }
    }

    fun toggleGridView() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleItemSelection(id: Long) {
        _uiState.update { state ->
            val newSelected = state.selectedItemIds.toMutableSet()
            if (newSelected.contains(id)) newSelected.remove(id) else newSelected.add(id)
            state.copy(selectedItemIds = newSelected, isMultiSelectMode = newSelected.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItemIds = emptySet(), isMultiSelectMode = false) }
    }

    fun saveText(text: String) {
        viewModelScope.launch {
            repository.saveTextOrUrl(text, sourceApp = "Manual Entry")
        }
    }

    fun saveUri(contentResolver: ContentResolver, uri: Uri, onResult: (SaveResult) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.saveUri(contentResolver, uri, sourceApp = "Import")
            onResult(res)
        }
    }

    fun updateItem(item: InboxItem) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun setDuplicateBehavior(behavior: String) {
        viewModelScope.launch { settingsRepository.setDuplicateBehavior(behavior) }
    }

    fun setQuickSaveEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setQuickSaveEnabled(enabled) }
    }

    fun setServerPort(port: Int) {
        viewModelScope.launch { settingsRepository.setServerPort(port) }
    }

    fun deleteItem(item: InboxItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun deleteSelectedItems() {
        val selectedIds = _uiState.value.selectedItemIds
        viewModelScope.launch {
            val currentItems = items.value.filter { selectedIds.contains(it.id) }
            repository.deleteItems(currentItems)
            clearSelection()
        }
    }

    fun toggleFavorite(item: InboxItem) {
        viewModelScope.launch { repository.toggleFavorite(item) }
    }

    fun toggleArchive(item: InboxItem) {
        viewModelScope.launch { repository.toggleArchive(item) }
    }

    fun moveSelectedToFolder(folderId: Long?) {
        val selectedIds = _uiState.value.selectedItemIds.toList()
        viewModelScope.launch {
            repository.moveItemsToFolder(selectedIds, folderId)
            clearSelection()
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createFolder(name) }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch { repository.deleteFolder(folderId) }
    }

    fun clearCache() {
        viewModelScope.launch { repository.clearCache() }
    }
}
