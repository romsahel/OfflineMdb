package com.rsahel.offlinemdb.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsahel.offlinemdb.DatabaseHelper
import com.rsahel.offlinemdb.R
import com.rsahel.offlinemdb.ui.theme.OfflineMdbTheme

class WelcomeMenu {

    private val formattedLastRefresh = mutableStateOf("")
    private val formattedCount = mutableStateOf("")

    fun update(context: Context? = null, dbHelper: DatabaseHelper? = null) {
        var lastRefresh: String? = null
        var itemCount = 0
        if (context != null && dbHelper != null) {
            lastRefresh = dbHelper.getLastUpdate(context)
            itemCount = dbHelper.getItemCount(context)
        }
        if (lastRefresh != null) {
            formattedLastRefresh.value = String.format(context!!.getString(R.string.database_lastRefresh), lastRefresh)
        } else {
            formattedLastRefresh.value = ""
        }
        if (itemCount > 0) {
            formattedCount.value = String.format(context!!.getString(R.string.database_itemCount), itemCount)
        } else {
            formattedCount.value = ""
        }
    }

    @Composable
    fun draw(context: Context, onRefreshClick: () -> Unit) {
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
                    context.getString(R.string.app_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    context.getString(R.string.app_subtitle),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRefreshClick) {
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



    @Preview
    @Composable
    fun PreviewOnlyDatabase() {

        OfflineMdbTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                WelcomeMenu().draw(LocalContext.current) {
                }
            }
        }
    }
}