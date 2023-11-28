package com.rsahel.offlinemdb.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsahel.offlinemdb.R
import com.rsahel.offlinemdb.ui.theme.OfflineMdbTheme

// Essentially a wrapper around a lambda function to give it a name and icon
// akin to Android menu XML entries.
// As an item on the action bar, the action will be displayed with an IconButton
// with the given icon, if not null. Otherwise, the string from the name resource is used.
// In overflow menu, item will always be displayed as text.
data class ActionItem(
    @StringRes val nameRes: Int,
    val icon: ImageVector? = null,
    val overflowMode: OverflowMode = OverflowMode.IF_NECESSARY,
    val doAction: () -> Unit,
) {
    // allow 'calling' the action like a function
    operator fun invoke() = doAction()
}

// Whether action items are allowed to overflow into a dropdown menu - or NOT SHOWN to hide
enum class OverflowMode {
    NEVER_OVERFLOW, IF_NECESSARY, ALWAYS_OVERFLOW, NOT_SHOWN
}

// Note: should be used in a RowScope
@Composable
fun ActionMenu(
    items: List<ActionItem>,
    numIcons: Int = 3, // includes overflow menu icon; may be overridden by NEVER_OVERFLOW
    menuVisible: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    if (items.isEmpty()) {
        return
    }
    // decide how many action items to show as icons
    val (appbarActions, overflowActions) = remember(items, numIcons) {
        separateIntoIconAndOverflow(items, numIcons)
    }

    for (item in appbarActions) {
        key(item.hashCode()) {
            val name = stringResource(item.nameRes)
            if (item.icon != null) {
                IconButton(onClick = item.doAction) {
                    Icon(item.icon, name)
                }
            } else {
                TextButton(onClick = item.doAction) {
                    Text(
                        text = name,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (overflowActions.isNotEmpty()) {
        Box {
            IconButton(onClick = { menuVisible.value = true }) {
                Icon(Icons.Default.MoreVert, "More actions")
            }
            DropdownMenu(
                expanded = menuVisible.value,
                onDismissRequest = { menuVisible.value = false },
            ) {
                for (item in overflowActions) {
                    key(item.hashCode()) {
                        DropdownMenuItem(text = { Text(stringResource(item.nameRes)) }, onClick = {
                            menuVisible.value = false
                            item.doAction()
                        })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuExamplePreview() {
    val menuVisible: MutableState<Boolean> = remember { mutableStateOf(false) }
    val items = listOf(
        ActionItem(
            R.string.refresh_action, Icons.Default.Refresh, OverflowMode.ALWAYS_OVERFLOW
        ) {},
        ActionItem(
            R.string.app_name, Icons.Default.Refresh, OverflowMode.ALWAYS_OVERFLOW
        ) {},
        ActionItem(
            R.string.refresh_action, Icons.Default.Refresh, OverflowMode.ALWAYS_OVERFLOW
        ) {},
        ActionItem(
            R.string.app_name, Icons.Default.Refresh, OverflowMode.ALWAYS_OVERFLOW
        ) {},
    )
    OfflineMdbTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "This is a title",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { menuVisible.value = true }) {
                Icon(Icons.Default.MoreVert, "More actions")
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                DropdownMenu(
                    expanded = menuVisible.value,
                    onDismissRequest = { menuVisible.value = false },
                ) {
                    for (item in items) {
                        key(item.hashCode()) {
                            DropdownMenuItem(text = { Text(stringResource(item.nameRes)) },
                                onClick = {
                                    menuVisible.value = false
                                    item.doAction()
                                })
                        }
                    }
                }
            }
        }
    }
}

private fun separateIntoIconAndOverflow(
    items: List<ActionItem>, numIcons: Int
): Pair<List<ActionItem>, List<ActionItem>> {
    var (iconCount, overflowCount, preferIconCount) = Triple(0, 0, 0)
    for (item in items) {
        when (item.overflowMode) {
            OverflowMode.NEVER_OVERFLOW -> iconCount++
            OverflowMode.IF_NECESSARY -> preferIconCount++
            OverflowMode.ALWAYS_OVERFLOW -> overflowCount++
            OverflowMode.NOT_SHOWN -> {}
        }
    }

    val needsOverflow = iconCount + preferIconCount > numIcons || overflowCount > 0
    val actionIconSpace = numIcons - (if (needsOverflow) 1 else 0)

    val iconActions = ArrayList<ActionItem>()
    val overflowActions = ArrayList<ActionItem>()

    var iconsAvailableBeforeOverflow = actionIconSpace - iconCount
    for (item in items) {
        when (item.overflowMode) {
            OverflowMode.NEVER_OVERFLOW -> {
                iconActions.add(item)
            }

            OverflowMode.ALWAYS_OVERFLOW -> {
                overflowActions.add(item)
            }

            OverflowMode.IF_NECESSARY -> {
                if (iconsAvailableBeforeOverflow > 0) {
                    iconActions.add(item)
                    iconsAvailableBeforeOverflow--
                } else {
                    overflowActions.add(item)
                }
            }

            OverflowMode.NOT_SHOWN -> {
                // skip
            }
        }
    }
    return Pair(iconActions, overflowActions)
}


@Composable
@Preview
fun MyApp() {
    MaterialTheme {
        Column {
            // Your main content
            // Text with Icon Button and Dropdown
            TextWithDropdownMenu()
        }
    }
}

@Composable
fun TextWithDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    val items = listOf("Item 1", "Item 2", "Item 3")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clickable {
                // Toggle the dropdown menu visibility
                expanded = !expanded
            }
    ) {
        Text("Click me! Click me! Click me! Click me!", modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            // Icon button to trigger the dropdown
            IconButton(
                onClick = {
                    expanded = !expanded
                }
            ) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
            }

            // Dropdown menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        onClick = {
                            // Handle the click on each dropdown item
                            selectedIndex = index
                            expanded = false
                        },
                        text = { Text(text = item) }
                    )
                }
            }

        }
    }
}

@Stable
class DropdownMenuState {
    var expanded by mutableStateOf(false)
        private set

    fun expand() {
        expanded = true
    }

    fun collapse() {
        expanded = false
    }
}