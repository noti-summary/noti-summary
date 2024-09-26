package org.muilab.noti.summary.view.home

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.muilab.noti.summary.R
import org.muilab.noti.summary.ui.theme.DarkColorScheme
import org.muilab.noti.summary.viewModel.SummaryViewModel
import java.lang.Float.max

@Composable
fun NotiDrawer(context: Context, sumViewModel: SummaryViewModel) {
    val notifications by sumViewModel.notifications.observeAsState()
    var drawerHeight by remember { mutableStateOf(Float.POSITIVE_INFINITY) }

    val state = rememberLazyListState()

    val padToPx = with(LocalDensity.current) {16.dp.toPx() / drawerHeight}
    val brush = Brush.verticalGradient(
        0.0f to MaterialTheme.colorScheme.surfaceVariant,
        padToPx to Color.Transparent,
        1 - padToPx  to Color.Transparent,
        1.0f to MaterialTheme.colorScheme.surfaceVariant
    )

    remember {
        mutableStateMapOf(
            context.getString(R.string.application_name) to true,
            context.getString(R.string.time) to true,
            context.getString(R.string.title) to true,
            context.getString(R.string.content) to true
        )
    }
    val notiFilterPrefs = context.getSharedPreferences("noti_filter", Context.MODE_PRIVATE)
    var showAppName by remember { mutableStateOf(true) }
    var showTime by remember { mutableStateOf(true) }
    var showTitle by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        sumViewModel.resetNotiDrawer()
        showAppName = notiFilterPrefs.getBoolean(context.getString(R.string.application_name), true)
        showTime = notiFilterPrefs.getBoolean(context.getString(R.string.time), true)
        showTitle = notiFilterPrefs.getBoolean(context.getString(R.string.title), true)
        showContent = notiFilterPrefs.getBoolean(context.getString(R.string.content), true)
    }

    Box {

        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .onSizeChanged { drawerHeight = max(it.height.toFloat(), 1F) },
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            notifications?.forEach {
                item {
                    Card(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.background(DarkColorScheme.secondary)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                                Text(
                                    text = it.appName,
                                    style = TextStyle(color = if (showAppName) {
                                        DarkColorScheme.onSecondary
                                    } else {
                                        Color.Gray
                                    })
                                )
                                Text(
                                    text = " / ",
                                    style = TextStyle(color = if (showAppName && showTime) {
                                        DarkColorScheme.onSecondary
                                    } else {
                                        Color.Gray
                                    })
                                )
                                Text(
                                    text = it.time,
                                    style = TextStyle(color = if (showTime) {
                                        DarkColorScheme.onSecondary
                                    } else {
                                        Color.Gray
                                    })
                                )
                                Spacer(modifier = Modifier.weight(1F))
                            }
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .background(Color.Transparent),
                                text = it.title,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = if (showTitle) {
                                        DarkColorScheme.onSecondary
                                    } else {
                                        Color.Gray
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .background(Color.Transparent),
                                text = it.content,
                                style = TextStyle(color = if (showContent) {
                                    DarkColorScheme.onSecondary
                                } else {
                                    Color.Gray
                                }),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().background(brush)) { }
    }
}