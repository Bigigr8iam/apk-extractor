package domilopment.apkextractor.ui.settings

import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import domilopment.apkextractor.R
import domilopment.apkextractor.ui.components.ItemListDragAndDropState
import domilopment.apkextractor.ui.components.move
import domilopment.apkextractor.ui.components.rememberDragDropListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun APKNamePreference(
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
    APKNamePreference(
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
fun APKNamePreference(
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
    val context = LocalContext.current
    val entriesMap = remember { entries.zip(entryValues) }

    val value = rememberSaveable(saver = listSaver(save = { stateList ->
        if (stateList.isNotEmpty()) {
            val first = stateList.first()
            check(canBeSaved(first)) {
                "${first::class} cannot be saved. By default only types which can be stored in the Bundle class can be saved."
            }
        }
        stateList.toList()
    }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(*sortedValues(state.value))
    }

    val dragMap = rememberSaveable(saver = listSaver(save = { stateList ->
        if (stateList.isNotEmpty()) {
            val first = stateList.first()
            check(canBeSaved(first)) {
                "${first::class} cannot be saved. By default only types which can be stored in the Bundle class can be saved."
            }
        }
        stateList.toList()
    }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(*entriesMap.sortedBy {
            val index = value.indexOf(it.second)
            if (index > -1) index else Int.MAX_VALUE
        }.toTypedArray())
    }

    var dialog by rememberSaveable {
        mutableStateOf(false)
    }

    val isValid by remember {
        derivedStateOf {
            value.any { it == "name" || it == "package" }
        }
    }

    LaunchedEffect(isValid) {
        if (!isValid) Toast.makeText(
            context, context.getString(R.string.app_save_name_toast), Toast.LENGTH_LONG
        ).show()
    }

    Preference(icon = icon,
        enabled = enabled,
        iconDesc = iconDesc,
        name = name,
        summary = summary,
        onClick = {
            value.apply {
                clear()
                addAll(sortedValues(state.value))
            }
            dragMap.apply {
                if (isNotEmpty()) clear()
                addAll(entriesMap.sortedBy {
                    val index = value.indexOf(it.second)
                    if (index > -1) index else Int.MAX_VALUE
                })
            }
            dialog = true
        })

    if (dialog) AlertDialog(onDismissRequest = { dialog = false }, confirmButton = {
        TextButton(onClick = {
            val selectedItemsInOrder = dragMap.map { it.second }.filter { it in value }
            onClick(value.map { "${selectedItemsInOrder.indexOf(it)}:$it" }.toSet())
            dialog = false
        }, enabled = isValid) {
            Text(text = stringResource(id = R.string.app_name_dialog_ok))
        }
    }, dismissButton = {
        TextButton(onClick = { dialog = false }) {
            Text(text = stringResource(id = R.string.app_name_dialog_cancel))
        }
    }, title = { Text(text = name) }, text = {
        val scope = rememberCoroutineScope()
        val overscrollJob = remember { mutableStateOf<Job?>(null) }
        val itemListDragAndDropState = rememberDragDropListState(onMove = { from, to ->
            val items = dragMap.map { it.second }
            val fromItem = items[from]
            val toItem = items[to]
            dragMap.move(from, to)
            if (fromItem !in value && toItem in value) value.add(fromItem)
            else if (fromItem in value && toItem !in value) value.remove(fromItem)
        })

        LazyColumn(
            modifier = Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDrag = { change, offset ->
                    change.consume()
                    itemListDragAndDropState.onDrag(offset)
                    handleOverscrollJob(overscrollJob, scope, itemListDragAndDropState)
                },
                    onDragStart = { offset -> itemListDragAndDropState.onDragStart(offset) },
                    onDragEnd = { itemListDragAndDropState.onDragInterrupted() },
                    onDragCancel = { itemListDragAndDropState.onDragInterrupted() })
            }, state = itemListDragAndDropState.getLazyListState()
        ) {
            itemsIndexed(items = dragMap, key = { _, item -> item.second }) { index, item ->
                val displacementOffset =
                    if (index == itemListDragAndDropState.getCurrentIndexOfDraggedListItem()) {
                        itemListDragAndDropState.elementDisplacement.takeIf { it != 0f }
                    } else {
                        null
                    }
                ListItem(headlineContent = { Text(text = item.first) },
                    modifier = Modifier
                        .clickable {
                            if (value.contains(item.second)) value.remove(item.second) else value.add(
                                item.second
                            )
                            dragMap.sortBy { it.second !in value }
                        }
                        .graphicsLayer { translationY = displacementOffset ?: 0f },
                    leadingContent = {
                        Checkbox(
                            checked = value.contains(item.second), onCheckedChange = null
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.DragIndicator, contentDescription = null
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (displacementOffset != null) {
                            ListItemDefaults.containerColor.copy(alpha = 0.9f)
                        } else {
                            ListItemDefaults.containerColor
                        }
                    )
                )
            }
        }
    })
}

private fun handleOverscrollJob(
    overscrollJob: MutableState<Job?>,
    scope: CoroutineScope,
    itemListDragAndDropState: ItemListDragAndDropState
) {
    if (overscrollJob.value?.isActive == true) return
    val overscrollOffset = itemListDragAndDropState.checkForOverScroll()
    if (overscrollOffset != 0f) {
        overscrollJob.value = scope.launch {
            itemListDragAndDropState.getLazyListState().scrollBy(overscrollOffset)
        }
    } else {
        overscrollJob.value?.cancel()
    }
}

private fun sortedValues(value: Set<String>): Array<String> {
    return try {
        value.toSortedSet(compareBy { it[0].digitToInt() }).map { it.removeRange(0, 2) }
    } catch (e: Exception) {
        value
    }.toTypedArray()
}
