package com.rsahel.offlinemdb.ui.components

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rsahel.offlinemdb.DatabaseHelper
import com.rsahel.offlinemdb.DatabaseItem
import com.rsahel.offlinemdb.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawSearchBar(
    dbHelper: DatabaseHelper?,
    items: SnapshotStateList<DatabaseItem>,
    modifier: Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        modifier = modifier,
        onValueChange = { newText ->
            text = newText
        },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        placeholder = { Text(stringResource(id = R.string.search_hint)) }
    )

    LaunchedEffect(key1 = text) {
        if (dbHelper == null) return@LaunchedEffect
        delay(200)

        if (text.isEmpty()) {
            if (!items.isEmpty()) {
                coroutineScope.launch {
                    items.clear()
                }
            }
            return@LaunchedEffect
        }

        val searchResult = dbHelper.getItemsWithTitle(text)
        Log.d("DrawSearchField", "Found ${searchResult.size} items")
        coroutineScope.launch {
            items.clear()
            items.addAll(searchResult)
            Log.d("DrawSearchField", "Setting items ${items.size} items")
        }
    }
}