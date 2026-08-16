package by.tigre.audiobook.core.presentation.audiobook_catalog.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import by.tigre.audiobook.core.entity.catalog.LibrarySpace
import by.tigre.audiobook.core.presentation.catalog.resources.Res
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_create
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_create_upsell
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_create_upsell_hint
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_kids_suggestion
import by.tigre.audiobook.core.presentation.catalog.resources.library_space_new_name_hint
import by.tigre.audiobook.core.presentation.catalog.resources.library_spaces_sheet_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySpaceSwitcherSheet(
    spaces: List<LibrarySpace>,
    activeSpaceId: LibrarySpace.Id?,
    canManageSpaces: Boolean,
    onDismiss: () -> Unit,
    onSpaceSelected: (LibrarySpace.Id) -> Unit,
    onCreateSpaceClicked: () -> Unit,
    onConfirmCreateSpace: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.library_spaces_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            spaces.forEach { space ->
                val selected = space.id == activeSpaceId
                Text(
                    text = space.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpaceSelected(space.id) }
                        .padding(vertical = 12.dp),
                )
            }
            if (creating && canManageSpaces) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text(stringResource(Res.string.library_space_new_name_hint)) },
                    placeholder = { Text(stringResource(Res.string.library_space_kids_suggestion)) },
                    singleLine = true,
                )
                val kidsSuggestion = stringResource(Res.string.library_space_kids_suggestion)
                TextButton(
                    onClick = {
                        onConfirmCreateSpace(name.ifBlank { kidsSuggestion })
                        creating = false
                    },
                ) {
                    Text(stringResource(Res.string.library_space_create))
                }
            } else {
                TextButton(
                    onClick = {
                        if (canManageSpaces) {
                            creating = true
                            name = ""
                        } else {
                            onCreateSpaceClicked()
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (canManageSpaces) {
                                Res.string.library_space_create
                            } else {
                                Res.string.library_space_create_upsell
                            },
                        ),
                    )
                }
                if (!canManageSpaces) {
                    Text(
                        text = stringResource(Res.string.library_space_create_upsell_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
