package org.muilab.noti.summary.view.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.muilab.noti.summary.R
import org.muilab.noti.summary.view.component.NoPaddingAlertDialog
import org.muilab.noti.summary.viewModel.APIKeyViewModel

@Composable
fun APIKeyScreen(apiKeyViewModel: APIKeyViewModel) {
    MaterialTheme {
        APIKeyList(apiKeyViewModel)
        AddKeyButton(apiKeyViewModel)
    }
}

@Composable
fun APICreationLink() {

    val uriHandler = LocalUriHandler.current

    val annotatedLinkString: AnnotatedString = buildAnnotatedString {
        val annotStr = stringResource(R.string.create_api_key)
        val startIndex = 0
        val endIndex = annotStr.length
        append(annotStr)
        addStyle(
            style = SpanStyle(
                color = Color(0xff64B5F6),
                textDecoration = TextDecoration.Underline
            ), start = startIndex, end = endIndex
        )
        addStringAnnotation(
            tag = "URL",
            annotation = "https://platform.openai.com/account/api-keys",
            start = startIndex,
            end = endIndex
        )
    }
    ClickableText(
        annotatedLinkString,
        modifier = Modifier.padding(15.dp, 10.dp),
        onClick = {
            annotatedLinkString
                .getStringAnnotations("URL", it, it)
                .firstOrNull()?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }
        }
    )
}

@Composable
fun APIKeyList(apiKeyViewModel: APIKeyViewModel) {
    val selectedOption = apiKeyViewModel.apiKey.observeAsState()
    val allAPIKey = apiKeyViewModel.allAPIKey.observeAsState(listOf(""))

    Column {
        APICreationLink()
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            itemsIndexed(allAPIKey.value) { _, item ->
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
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(5.dp),
                            text = "sk-**********" + item.takeLast(4),
                            color =
                            if (item == selectedOption.value) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )

                        if (allAPIKey.value.size > 1) {
                            IconButton(
                                modifier = Modifier
                                    .size(42.dp)
                                    .padding(3.dp),
                                onClick = { apiKeyViewModel.deleteAPI(item) }
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "delete api")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddKeyButton(apiKeyViewModel: APIKeyViewModel) {

    val showDialog = remember { mutableStateOf(false) }
    val inputKey = remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = {
                showDialog.value = true
            },
        ) {
            Icon(Icons.Filled.Add, "add new key")
        }
    }

    val confirmAction = {
        if (inputKey.value != "" && inputKey.value.startsWith("sk-")) {
            apiKeyViewModel.addAPI(inputKey.value)
            inputKey.value = ""
            showDialog.value = false
        }
    }

    val dismissAction = {
        inputKey.value = ""
    }

    if (showDialog.value) {
        val titleContent: @Composable () -> Unit = {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, bottom = 20.dp)
                    .height(70.dp),
                painter = painterResource(id = R.drawable.key),
                contentDescription = "key_icon",
            )
        }
        APIKeyEditor(showDialog, inputKey, titleContent, confirmAction, dismissAction)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeyEditor(
    showDialog: MutableState<Boolean>,
    defaultPromptInTextBox: MutableState<String>,
    title: @Composable () -> Unit,
    confirmAction: () -> Unit,
    dismissAction: () -> Unit = {},
) {
    NoPaddingAlertDialog(
        title = title,
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .fillMaxWidth(),
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
