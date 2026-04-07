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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import tachiyomi.domain.manga.model.Tag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageTagsDialog(
    allTags: ImmutableList<Tag>,
    mangaTags: ImmutableList<Tag>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    onCreateTag: (String) -> Unit,
    onDeleteTag: (Long) -> Unit,
) {
    val selectedTagIds = remember(mangaTags.size) { mutableStateOf(mangaTags.map { it.id }.toMutableSet()) }
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
            TextButton(onClick = { onConfirm(selectedTagIds.value.toList()) }) {
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
