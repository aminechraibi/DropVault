package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.InboxItem
import com.example.data.database.ItemType
import com.example.ui.InboxViewModel
import com.example.ui.components.InboxCard
import com.example.ui.viewers.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onOpenWebAccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val items by viewModel.items.collectAsState()

    var activeDialogItem by remember { mutableStateOf<InboxItem?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddUrlDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveUri(context.contentResolver, it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Multi-select Action Bar or Search Bar
        if (uiState.isMultiSelectMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.selectedItemIds.size} Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = { viewModel.deleteSelectedItems() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }
        } else {
            // Search Bar & Grid/List Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search everything...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { viewModel.toggleGridView() }) {
                    Icon(
                        imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View"
                    )
                }
            }
        }

        // Filter Chips
        val filters = listOf(
            null to "All",
            ItemType.TEXT to "Text",
            ItemType.URL to "Links",
            ItemType.IMAGE to "Images",
            ItemType.AUDIO to "Audio",
            ItemType.VIDEO to "Video",
            ItemType.PDF to "PDFs",
            ItemType.FILE to "Files"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(filters) { (type, label) ->
                FilterChip(
                    selected = uiState.selectedTypeFilter == type,
                    onClick = { viewModel.setTypeFilter(type) },
                    label = { Text(label) }
                )
            }
        }

        // Main Items Grid / List
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) "No matching items found" else "Inbox is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val columns = if (uiState.isGridView) GridCells.Fixed(2) else GridCells.Fixed(1)
            LazyVerticalGrid(
                columns = columns,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items, key = { it.id }) { item ->
                    InboxCard(
                        item = item,
                        isSelected = uiState.selectedItemIds.contains(item.id),
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        onClick = {
                            if (uiState.isMultiSelectMode) {
                                viewModel.toggleItemSelection(item.id)
                            } else {
                                activeDialogItem = item
                            }
                        },
                        onLongClick = {
                            viewModel.toggleItemSelection(item.id)
                        },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(item)
                        }
                    )
                }
            }
        }

        // FAB Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallFloatingActionButton(
                onClick = { showAddUrlDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.Link, contentDescription = "Add Link")
            }

            Spacer(modifier = Modifier.width(8.dp))

            SmallFloatingActionButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Import File")
            }

            Spacer(modifier = Modifier.width(8.dp))

            ExtendedFloatingActionButton(
                onClick = { showAddNoteDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Note") }
            )
        }
    }

    // Active Item Viewer Dialog
    activeDialogItem?.let { item ->
        when (item.type) {
            ItemType.TEXT, ItemType.URL -> TextViewerDialog(
                item = item,
                onDismiss = { activeDialogItem = null },
                onSaveEdit = { title, text ->
                    viewModel.updateItem(item.copy(title = title, text = text))
                },
                onDelete = {
                    viewModel.deleteItem(item)
                    activeDialogItem = null
                },
                onToggleFavorite = { viewModel.toggleFavorite(item) }
            )
            ItemType.IMAGE -> ImageViewerDialog(
                item = item,
                onDismiss = { activeDialogItem = null },
                onDelete = {
                    viewModel.deleteItem(item)
                    activeDialogItem = null
                }
            )
            ItemType.AUDIO -> AudioPlayerDialog(
                item = item,
                onDismiss = { activeDialogItem = null },
                onDelete = {
                    viewModel.deleteItem(item)
                    activeDialogItem = null
                }
            )
            ItemType.VIDEO -> VideoPlayerDialog(
                item = item,
                onDismiss = { activeDialogItem = null },
                onDelete = {
                    viewModel.deleteItem(item)
                    activeDialogItem = null
                }
            )
            else -> FileViewerDialog(
                item = item,
                onDismiss = { activeDialogItem = null },
                onDelete = {
                    viewModel.deleteItem(item)
                    activeDialogItem = null
                }
            )
        }
    }

    // Add Note Dialog
    if (showAddNoteDialog) {
        var noteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("New Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Type note text here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            viewModel.saveText(noteText)
                            showAddNoteDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add URL Dialog
    if (showAddUrlDialog) {
        var urlText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddUrlDialog = false },
            title = { Text("Add Link") },
            text = {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlText.isNotBlank()) {
                            viewModel.saveText(urlText)
                            showAddUrlDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
