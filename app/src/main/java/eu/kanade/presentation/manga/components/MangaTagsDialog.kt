package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.domain.manga.model.Tag

enum class TagSelectState { NONE, SOME, ALL }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageTagsDialog(
    allTags: ImmutableList<Tag>,
    mangaTags: ImmutableList<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    onCreateTag: (String) -> Unit,
    onDeleteTag: (Long) -> Unit,
    // For bulk mode
    selectedAll: ImmutableList<Long> = persistentListOf(),
    selectedSome: ImmutableList<Long> = persistentListOf(),
    isBulk: Boolean = false,
    onBulkConfirm: ((addIds: List<Long>, removeIds: List<Long>) -> Unit)? = null,
) {
    val isBulkMode = isBulk && onBulkConfirm != null

    // For single manga mode
    val selectedTagIds = remember(mangaTags) {
        mutableStateOf(mangaTags.map { it.id }.toMutableSet())
    }

    // For bulk mode — track per-tag state
    val tagStates = remember(selectedAll, selectedSome) {
        mutableStateMapOf<Long, TagSelectState>().apply {
            allTags.forEach { tag ->
                this[tag.id] = when {
                    tag.id in selectedAll -> TagSelectState.ALL
                    tag.id in selectedSome -> TagSelectState.SOME
                    else -> TagSelectState.NONE
                }
            }
        }
    }

    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (allTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allTags.forEach { tag ->
                            if (isBulkMode) {
                                val state = tagStates[tag.id] ?: TagSelectState.NONE
                                FilterChip(
                                    selected = state != TagSelectState.NONE,
                                    onClick = {
                                        tagStates[tag.id] = when (state) {
                                            TagSelectState.NONE -> TagSelectState.ALL
                                            TagSelectState.SOME -> TagSelectState.ALL
                                            TagSelectState.ALL -> TagSelectState.NONE
                                        }
                                    },
                                    label = { Text(tag.name) },
                                    leadingIcon = if (state == TagSelectState.SOME) {
                                        {
                                            Icon(
                                                Icons.Filled.Remove,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    } else null,
                                    colors = if (state == TagSelectState.SOME) {
                                        FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        )
                                    } else FilterChipDefaults.filterChipColors(),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onDeleteTag(tag.id) },
                                            modifier = Modifier.size(18.dp),
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Delete tag")
                                        }
                                    },
                                )
                            } else {
                                FilterChip(
                                    selected = tag.id in selectedTagIds.value,
                                    onClick = {
                                        val newSet = selectedTagIds.value.toMutableSet()
                                        if (tag.id in newSet) newSet.remove(tag.id) else newSet.add(tag.id)
                                        selectedTagIds.value = newSet
                                    },
                                    label = { Text(tag.name) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onDeleteTag(tag.id) },
                                            modifier = Modifier.size(18.dp),
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Delete tag")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        placeholder = { Text("New tag...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            if (newTagText.isNotBlank()) {
                                IconButton(onClick = { newTagText = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                            }
                        },
                    )
                    IconButton(
                        onClick = {
                            if (newTagText.isNotBlank()) {
                                onCreateTag(newTagText.trim())
                                newTagText = ""
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add tag")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isBulkMode) {
                        val addIds = tagStates.filter { it.value == TagSelectState.ALL }.keys.toList()
                        val removeIds = tagStates.filter { it.value == TagSelectState.NONE }
                            .keys
                            .filter { it in selectedAll || it in selectedSome }
                            .toList()
                        onBulkConfirm!!(addIds, removeIds)
                    } else {
                        onConfirm(selectedTagIds.value.toList())
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
