package domilopment.apkextractor.ui.settings

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import domilopment.apkextractor.R

@Composable
fun MultiSelectListPreference(
    icon: ImageVector? = null,
    enabled: Boolean = true,
    @StringRes iconDesc: Int? = null,
    @StringRes name: Int,
    @StringRes summary: Int? = null,
    @ArrayRes entries: Int,
    @ArrayRes entryValues: Int,
    state: State<Set<String>>,
    onClick: (Set<String>) -> Unit
) {
    MultiSelectListPreference(
        icon = icon,
        enabled = enabled,
        iconDesc = iconDesc?.let { stringResource(id = it) },
        name = stringResource(id = name),
        summary = summary?.let { stringResource(id = it) },
        entries = stringArrayResource(id = entries),
        entryValues = stringArrayResource(
            id = entryValues
        ),
        state = state,
        onClick = onClick
    )
}

@Composable
fun MultiSelectListPreference(
    icon: ImageVector? = null,
    iconDesc: String? = null,
    name: String,
    summary: String? = null,
    enabled: Boolean = true,
    entries: Array<String>,
    entryValues: Array<String>,
    state: State<Set<String>>,
    onClick: (Set<String>) -> Unit
) {
    val entriesMap = remember(entries, entryValues) { entries.zip(entryValues) }

    val value = rememberSaveable(saver = listSaver(save = { stateList ->
        if (stateList.isNotEmpty()) {
            val first = stateList.first()
            check(canBeSaved(first)) {
                "${first::class} cannot be saved. By default only types which can be stored in the Bundle class can be saved."
            }
        }
        stateList.toList()
    }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(*state.value.toTypedArray())
    }

    var dialog by rememberSaveable {
        mutableStateOf(false)
    }

    Preference(icon = icon,
        enabled = enabled,
        iconDesc = iconDesc,
        name = name,
        summary = summary,
        onClick = {
            value.apply {
                clear()
                addAll(state.value)
            }
            dialog = true
        })

    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, confirmButton = {
        TextButton(onClick = {
            onClick(value.toSet())
            dialog = false
        }) {
            Text(text = stringResource(id = R.string.app_name_dialog_ok))
        }
    }, dismissButton = {
        TextButton(onClick = { dialog = false }) {
            Text(text = stringResource(id = R.string.app_name_dialog_cancel))
        }
    }, title = { Text(text = name) }, text = {
        LazyColumn {
            items(items = entriesMap, key = { it.second }) {
                ListItem(headlineContent = { Text(text = it.first) },
                    modifier = Modifier.clickable {
                        if (value.contains(it.second)) value.remove(it.second) else value.add(it.second)
                    },
                    leadingContent = {
                        Checkbox(
                            checked = value.contains(it.second), onCheckedChange = null
                        )
                    })
            }
        }
    })
}

