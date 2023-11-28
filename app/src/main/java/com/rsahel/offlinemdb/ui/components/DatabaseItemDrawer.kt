package com.rsahel.offlinemdb.ui.components

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.fonts.FontStyle
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rsahel.offlinemdb.DatabaseItem
import com.rsahel.offlinemdb.SampleData
import com.rsahel.offlinemdb.ui.theme.OfflineMdbTheme

@Composable
fun DrawDatabaseItem(item: DatabaseItem, context: Context?) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 1.dp,
        modifier = Modifier.clickable(onClick = { OnDatabaseItemClicked(item, context!!) })
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

private fun OnDatabaseItemClicked(item: DatabaseItem, context: Context): Unit {
    val url = "https://www.imdb.com/title/${item.id}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
    return ContextCompat.startActivity(context, intent, null)
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
        fontWeight = FontWeight(FontStyle.FONT_WEIGHT_BOLD)
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
private fun DrawDatabaseItemContent(
    item: DatabaseItem
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Text(
                text = item.year,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${item.formattedType()} · ${item.formattedRuntime()} · ${item.formattedNumVotes()} votes",
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
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
                item = SampleData.databaseSample[0], null
            )
        }
    }
}