package com.rsahel.offlinemdb

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.rsahel.offlinemdb.ui.theme.OfflineMdbTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val items = mutableStateListOf<DatabaseItem>()
    private val showDialog = mutableStateOf(false)
    private val dialogMessage = mutableStateOf("Fetching database...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = DatabaseHelper(applicationContext)
        setContent {
            DrawMdb(dbItems = items, dbHelper, applicationContext)
        }
    }

    @Composable
    fun DrawMdb(
        dbItems: List<DatabaseItem>,
        dbHelper: DatabaseHelper? = null,
        context: Context? = null
    ) {
        OfflineMdbTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column {
                    Row(
                        modifier = Modifier.padding(all = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawSearchField(dbHelper, Modifier.weight(1f))
                        DrawActionMenu(dbHelper)
                    }
                    DrawDatabase(dbItems, context)
                }

                if (showDialog.value) {
                    DrawDialog()
                }
            }
        }
    }

    @Composable
    @Preview
    fun DrawDialog() {
        Dialog(
            onDismissRequest = { },
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(144.dp)
                    .background(White, shape = RoundedCornerShape(8.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        dialogMessage.value,
                        color = MaterialTheme.colorScheme.surface,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DrawSearchField(dbHelper: DatabaseHelper?, modifier: Modifier) {
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

            val searchResult = dbHelper.getRowsWithTitle(text)
            Log.d("DrawSearchField", "Found ${searchResult.size} items")
            coroutineScope.launch {
                items.clear()
                items.addAll(searchResult)
                Log.d("DrawSearchField", "Setting items ${items.size} items")
            }
        }
    }


    @Composable
    fun DrawDatabase(databaseItems: List<DatabaseItem>, context: Context?) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(all = 8.dp)
        ) {
            items(items = databaseItems, key = { it.id }) { item ->
                DrawDatabaseItem(item, context)
            }
        }
    }

    @Composable
    fun DrawActionMenu(dbHelper: DatabaseHelper?) {
        val items = listOf(
            ActionItem(
                R.string.refresh_action,
                Icons.Default.Refresh,
                OverflowMode.ALWAYS_OVERFLOW
            ) {
                showDialog.value = true

                val workManager = WorkManager.getInstance(applicationContext)
                val workRequest: WorkRequest = OneTimeWorkRequestBuilder<RefreshWorker>()
                    .addTag(RefreshWorker.Tag)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                workManager
                    .getWorkInfoByIdLiveData(workRequest.id)
                    .observe(this) { workInfo: WorkInfo? ->
                        if (workInfo != null) {
                            val progress = workInfo.progress.getInt(RefreshWorker.Progress, 0)
                            dialogMessage.value = "$progress%"
                            showDialog.value = workInfo.state == WorkInfo.State.RUNNING
                        }
                    }
                workManager.enqueue(workRequest)
            },
        )
        ActionMenu(items)
    }


    @Preview
    @Composable
    fun PreviewDatabase() {
        DrawMdb(dbItems = SampleData.databaseSample)
    }
}