package com.rsahel.offlinemdb

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.rsahel.offlinemdb.ui.components.ActionItem
import com.rsahel.offlinemdb.ui.components.ActionMenu
import com.rsahel.offlinemdb.ui.components.DrawDatabaseItem
import com.rsahel.offlinemdb.ui.components.DrawSearchBar
import com.rsahel.offlinemdb.ui.components.LoadingDialog
import com.rsahel.offlinemdb.ui.components.OverflowMode
import com.rsahel.offlinemdb.ui.theme.OfflineMdbTheme

class MainActivity : ComponentActivity() {

    private val formattedLastRefresh = mutableStateOf("")
    private val formattedCount = mutableStateOf("")
    private val items = mutableStateListOf<DatabaseItem>()
    private val loadingDialog = LoadingDialog()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndAskForPermissions(this)

        RefreshWorker.observe(applicationContext, this) { workInfo ->
            val progress = workInfo.progress.getInt(RefreshWorker.Progress, 0)
            val message = if (progress == 0) "Fetching database..." else "${progress}%"
            loadingDialog.updateContent(
                workInfo.state == WorkInfo.State.RUNNING,
                message
            )
        }

        val dbHelper = DatabaseHelper(applicationContext) {
            updateFormattedLastRefresh(applicationContext, it)
        }
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
                        DrawSearchBar(dbHelper, items, Modifier.weight(1f))
                        DrawActionMenu()
                    }
                    DrawDatabase(dbItems, context)
                }

                loadingDialog.Draw()
            }
        }
    }

    @Composable
    fun DrawDatabase(
        dbItems: List<DatabaseItem>,
        context: Context? = null
    ) {

        Box {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(all = 8.dp)
            ) {
                items(items = dbItems, key = { it.id }) { item ->
                    DrawDatabaseItem(item, context)
                }
            }
            if (dbItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                )
                {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Offline Movie Database",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "provided by imdb.com",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { Refresh() }) {
                            Text(stringResource(R.string.refresh_action))
                        }
                        Text(
                            formattedLastRefresh.value,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                        )
                        Text(
                            formattedCount.value,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DrawActionMenu() {
        val items = listOf(
            ActionItem(
                R.string.refresh_action,
                Icons.Default.Refresh,
                OverflowMode.ALWAYS_OVERFLOW
            ) {
                Refresh()
            },
        )
        ActionMenu(items)
    }

    private fun Refresh() {
        items.clear()
        RefreshWorker.buildAndEnqueue(applicationContext)
    }

    fun updateFormattedLastRefresh(context: Context? = null, dbHelper: DatabaseHelper? = null) {
        var lastRefresh: String? = null
        var itemCount = 0
        if (context != null && dbHelper != null) {
            lastRefresh = dbHelper.getLastUpdate(context)
            itemCount = dbHelper.getItemCount(context)
        }
        if (lastRefresh != null) {
            formattedLastRefresh.value = "Last refresh: ${lastRefresh}"
        } else {
            formattedLastRefresh.value = ""
        }
        if (itemCount > 0) {
            formattedCount.value = "${itemCount} items in database"
        } else {
            formattedCount.value = ""
        }
    }

    @Preview
    @Composable
    fun PreviewOnlyDatabase() {
        DrawDatabase(dbItems = listOf())
    }

//    @Preview
//    @Composable
//    fun PreviewDatabase() {
//        DrawMdb(dbItems = SampleData.databaseSample)
//    }
}