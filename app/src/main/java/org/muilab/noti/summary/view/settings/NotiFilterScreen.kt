package org.muilab.noti.summary.view.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.muilab.noti.summary.R

@Composable
fun NotiFilterScreen(context: Context) {

    val notiAttributes = listOf(
        stringResource(R.string.application_name),
        stringResource(R.string.time),
        stringResource(R.string.title),
        stringResource(R.string.content)
    )
    val notiFilterMap = remember {
        mutableStateMapOf(
            context.getString(R.string.application_name) to true,
            context.getString(R.string.time) to true,
            context.getString(R.string.title) to true,
            context.getString(R.string.content) to true
        )
    }
    val notiFilterPrefs = context.getSharedPreferences("noti_filter", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        notiFilterPrefs.all.forEach { (attribute, state) ->
            if (state is Boolean) {
                notiFilterMap[attribute] = state
            }
        }
    }

    LazyColumn {
        items(notiAttributes) { attribute ->
                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                        Text(text = attribute, modifier = Modifier.weight(1f))

                        val checkedState = notiFilterMap.getOrDefault(attribute, false)
                        Switch(
                            checked = checkedState,
                            onCheckedChange = { newState ->
                                notiFilterMap[attribute] = newState
                                with(notiFilterPrefs.edit()) {
                                    putBoolean(attribute, newState)
                                    apply()
                                }
                                val availAttr = mutableListOf<String>()
                                if (notiFilterMap[context.getString(R.string.application_name)] as Boolean)
                                    availAttr.add("appName")
                                if (notiFilterMap[context.getString(R.string.time)] as Boolean)
                                    availAttr.add("time")
                                if (notiFilterMap[context.getString(R.string.title)] as Boolean)
                                    availAttr.add("title")
                                if (notiFilterMap[context.getString(R.string.content)] as Boolean)
                                    availAttr.add("content")
                                availAttr.joinToString(separator = ",")
                            }
                        )
                    }
                }
        }
    }
}