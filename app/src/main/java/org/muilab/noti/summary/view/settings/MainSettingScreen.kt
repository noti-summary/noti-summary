package org.muilab.noti.summary.view.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.muilab.noti.summary.R


@Composable
fun MainSettingScreen(context: Context, navController: NavHostController) {

    val uriHandler = LocalUriHandler.current
    val isGroupTop = {item: SettingScreenItem ->
        item in listOf(SettingScreenItem.SettingPrompt, SettingScreenItem.SettingAPI)}
    val isGroupBottom = {item: SettingScreenItem ->
        item in listOf(SettingScreenItem.SettingNotiFilter, SettingScreenItem.About)
    }

    MaterialTheme {
        Column {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(
                    items = SettingScreenItem.values()
                        .slice(1 until SettingScreenItem.values().size)
                ) { item ->
                    Card(
                        modifier = Modifier
                            .padding(
                                start = 10.dp,
                                end = 10.dp,
                                top = if (isGroupTop(item)) 6.dp else 1.dp,
                                bottom = if (isGroupBottom(item)) 6.dp else 1.dp
                            )
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(
                                RoundedCornerShape(
                                    topStart = if(isGroupTop(item)) 12.dp else 0.dp,
                                    topEnd = if (isGroupTop(item)) 12.dp else 0.dp,
                                    bottomStart = if (isGroupBottom(item)) 12.dp else 0.dp,
                                    bottomEnd = if (isGroupBottom(item)) 12.dp else 0.dp
                                )
                            )
                            .clickable {
                                when (item.name) {
                                    SettingScreenItem.Feedback.name -> {
                                        uriHandler.openUri(context.getString(R.string.feedback_URL))
                                    }
                                    SettingScreenItem.About.name -> {
                                        uriHandler.openUri(context.getString(R.string.github_URL))
                                    }
                                    else -> { navController.navigate(item.name) }
                                }
                            },
                        shape = RoundedCornerShape(
                            topStart = if(isGroupTop(item)) 12.dp else 0.dp,
                            topEnd = if (isGroupTop(item)) 12.dp else 0.dp,
                            bottomStart = if (isGroupBottom(item)) 12.dp else 0.dp,
                            bottomEnd = if (isGroupBottom(item)) 12.dp else 0.dp
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(25.dp),
                                painter = painterResource(id = item.iconId),
                                contentDescription = stringResource(item.titleId),
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(0.93f),
                                text = stringResource(item.titleId),
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowRight,
                                stringResource(item.titleId)
                            )
                        }
                    }
                }
            }
        }
    }
}