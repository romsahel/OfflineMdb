package com.rsahel.offlinemdb

import DatabaseHelper
import android.content.res.Configuration
import android.graphics.fonts.FontStyle.FONT_WEIGHT_BOLD
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsahel.offlinemdb.ui.theme.OfflineMdbTheme
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.ViewModel
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val items = mutableStateListOf<DatabaseItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dbHelper = DatabaseHelper(applicationContext)
        setContent {
            OfflineMdbTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        DrawSearchField(dbHelper)
                        DrawDatabase(items)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DrawSearchField(dbHelper: DatabaseHelper?) {
        var text by remember { mutableStateOf("") }
        OutlinedTextField(
            value = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            onValueChange = { newText ->
                text = newText
                if (dbHelper == null) {
                    return@OutlinedTextField
                }

                items.clear()
                items.addAll(dbHelper.getRowsWithTitle(text))
                Log.d("DrawSearchField", "Found ${items.size} items")
                val set = hashSetOf<String>()
                for (dbItem in items) {
                    if (set.contains(dbItem.id)) {
                        Log.d("DrawSearchField", "ID ${dbItem.id} ALREADY EXISTS")
                    }
                    set.add(dbItem.id)
                }
                Log.d("DrawSearchField", "Found ${items.size} (${set.size} unique) items")
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text(stringResource(id = R.string.search_hint)) }
        )
    }


    @Composable
    fun DrawDatabaseItem(item: DatabaseItem) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 1.dp,

            ) {
            Row(
                modifier = Modifier
                    .padding(all = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrawRatingCircle(item)
                Spacer(modifier = Modifier.width(8.dp))
                DrawDatabaseItemContent(item)
            }
        }
    }

    @Composable
    private fun DrawDatabaseItemContent(
        item: DatabaseItem
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = item.year,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.formattedType()} · ${item.formattedRuntime()} · ${item.numVotes} votes",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Suppress("DEPRECATION")
    @Composable
    @OptIn(ExperimentalTextApi::class)
    private fun DrawRatingCircle(
        item: DatabaseItem
    ) {
        val textMeasurer = rememberTextMeasurer()
        val textToDraw = "${item.rating}"
        val style = LocalTextStyle.current.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            fontWeight = FontWeight(FONT_WEIGHT_BOLD)
        )
        val textLayoutResult = remember(textToDraw, style) {
            textMeasurer.measure(textToDraw, style)
        }

        Canvas(modifier = Modifier.size(54.dp)) {
            drawCircle(Color(245, 197, 24))
            drawText(
                textMeasurer = textMeasurer,
                text = textToDraw,
                style = style,
                topLeft = Offset(
                    x = center.x - textLayoutResult.size.width / 2,
                    y = center.y - textLayoutResult.size.height / 2
                )
            )
        }
    }

    @Composable
    fun DrawDatabase(databaseItems: List<DatabaseItem>) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(all = 8.dp)
        ) {
            items(items = databaseItems, key = { it.id }) { item ->
                DrawDatabaseItem(item)
            }
        }
    }


    @Preview(name = "Light Mode")
    @Preview(
        uiMode = Configuration.UI_MODE_NIGHT_YES,
        showBackground = true,
        name = "Dark Mode"
    )
    @Composable
    fun PreviewDatabaseItem() {
        OfflineMdbTheme {
            Surface {
                DrawDatabaseItem(
                    item = SampleData.databaseSample[0]
                )
            }
        }
    }


    @Preview
    @Composable
    fun PreviewDatabase() {
        OfflineMdbTheme {
            Surface {
                Column {
                    DrawSearchField(null)
                    DrawDatabase(SampleData.databaseSample)
                }
            }
        }
    }


}