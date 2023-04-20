package org.muilab.noti.summary.view.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.muilab.noti.summary.R
import org.muilab.noti.summary.util.insertUserAction
import org.muilab.noti.summary.view.component.NoPaddingAlertDialog
import org.muilab.noti.summary.viewModel.APIKeyViewModel

@Composable
fun APIKeyScreen(context: Context, apiKeyViewModel: APIKeyViewModel) {

    MaterialTheme {
        APIKeyList(apiKeyViewModel)
        AddKeyButton(context, apiKeyViewModel)
    }

}


@Composable
fun APIKeyList(apiKeyViewModel: APIKeyViewModel) {
    val selectedOption = apiKeyViewModel.apiKey.observeAsState()
    val allAPIKey = apiKeyViewModel.allAPIKey.observeAsState(listOf(""))
    val defaultAPIKey = stringResource(R.string.system_key)

    LazyColumn(modifier = Modifier.fillMaxHeight()) {
        itemsIndexed(listOf(defaultAPIKey) + allAPIKey.value) { index, item ->
            if (index == 0) {
                Text(
                    stringResource(R.string.default_key),
                    modifier = Modifier.padding(start = 15.dp, top = 10.dp, bottom = 10.dp)
                )
            } else if (index == 1) {
                Text(
                    stringResource(R.string.user_key),
                    modifier = Modifier.padding(start = 15.dp, top = 10.dp, bottom = 10.dp)
                )
            }
            Card(
                modifier = Modifier
                    .padding(start = 15.dp, end = 15.dp, top = 2.dp, bottom = 2.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        apiKeyViewModel.chooseAPI(item)
                    },
                colors = CardDefaults.cardColors(
                    containerColor =
                    if (item == selectedOption.value) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.inverseOnSurface
                    }
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp).fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = if (item != defaultAPIKey) {
                            "sk-**********" + item.takeLast(4)
                        } else {
                            defaultAPIKey
                        },
                        color =
                        if (item == selectedOption.value) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )

                    if (item != defaultAPIKey) {
                        IconButton(onClick = { apiKeyViewModel.deleteAPI(item) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "delete api")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AddKeyButton(context: Context, apiKeyViewModel: APIKeyViewModel) {

    val showDialog = remember { mutableStateOf(false) }
    val inputKey = remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = {
                showDialog.value = true
                insertUserAction("keyDialog", "launch", context)
            },
        ) {
            Icon(Icons.Filled.Add, "add new key")
        }
    }

    val confirmAction = {
        if (inputKey.value != "" && inputKey.value.startsWith("sk-")) {
            apiKeyViewModel.addAPI(inputKey.value)
            insertUserAction("keyDialog", "confirm", context)
            inputKey.value = ""
            showDialog.value = false
        }
    }

    val dismissAction = {
        inputKey.value = ""
        insertUserAction("keyDialog", "dismiss", context)
    }

    if (showDialog.value) {
        APIKeyEditor(showDialog, inputKey, confirmAction, dismissAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeyEditor(
    showDialog: MutableState<Boolean>,
    defaultPromptInTextBox: MutableState<String>,
    confirmAction: () -> Unit,
    dismissAction: () -> Unit = {},
) {
    NoPaddingAlertDialog(
        title = {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 20.dp)
                    .height(70.dp),
                painter = painterResource(id = R.drawable.key),
                contentDescription = "key_icon",
            )
        },
        text = {
            OutlinedTextField(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp).fillMaxWidth(),
                singleLine = true,
                value = defaultPromptInTextBox.value,
                onValueChange = { defaultPromptInTextBox.value = it },
                label = { Text(stringResource(R.string.api_key)) },
                textStyle = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.padding(all = 3.dp),
                onClick = {
                    confirmAction()
                }
            )
            {
                Text(
                    text = stringResource(R.string.ok),
                    modifier = Modifier.padding(start = 30.dp, end = 30.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.padding(all = 3.dp),
                onClick = {
                    dismissAction()
                    showDialog.value = false
                }
            )
            {
                Text(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.padding(start = 30.dp, end = 30.dp)
                )
            }
        }
    )
}
