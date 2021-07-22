package com.gaspar.gasparchat.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.viewmodel.GroupDialogViewModel
import com.gaspar.gasparchat.viewmodel.StringMethod
import com.gaspar.gasparchat.viewmodel.VoidMethod
import kotlinx.coroutines.flow.StateFlow

@ExperimentalComposeUiApi
@Composable
fun GroupDialogContent(
    onDialogDismissed: VoidMethod,
    onGroupCreated: (String, List<String>) -> Unit,
    viewModel: GroupDialogViewModel = hiltViewModel()
) {
    val validGroupState = viewModel.validGroupState.collectAsState()

    AlertDialog(
        onDismissRequest = onDialogDismissed,
        text = {
            GroupDialogBody(viewModel = viewModel)
        },
        dismissButton = {
            TextButton(onClick = onDialogDismissed) {
                Text(stringResource(id = R.string.authenticate_dialog_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onGroupCreated.invoke(viewModel.groupName.value.input, viewModel.selectedUsers.value) },
                enabled = validGroupState.value
            ) {
                Text(stringResource(id = R.string.group_create))
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@ExperimentalComposeUiApi
@Composable
fun GroupDialogBody(viewModel: GroupDialogViewModel) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.group_create_title),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        GroupNameField(
            nameInput = viewModel.groupName,
            onNameChanged = viewModel::onGroupNameChanged
        )
        Text(
            text = stringResource(id = R.string.group_add_friends),
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        val friendList = viewModel.friendList.collectAsState()
        if(friendList.value.isNotEmpty()) {
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
            ) {
                itemsIndexed(friendList.value) { position, friend ->
                    val selected = viewModel.isFriendSelected(friendUid = friend.uid)
                    FriendSelectorCard(
                        position = position,
                        displayName = friend.displayName,
                        selected = selected,
                        onFriendSelected = viewModel::onFriendSelected,
                        onFriendUnselected = viewModel::onFriendUnselected
                    )
                }
            }
        } else {
            Text(
                text = stringResource(id = R.string.home_no_friends),
                style = MaterialTheme.typography.subtitle2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun GroupNameField(
    nameInput: StateFlow<InputField>,
    onNameChanged: StringMethod
) {
    val groupName = nameInput.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = groupName.value.input,
        onValueChange = { onNameChanged(it) },
        isError = groupName.value.isError,
        label = {
            val text = if(groupName.value.isError) groupName.value.errorMessage else stringResource(R.string.group_create_name)
            Text(text = text)
        },
        maxLines = 1,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() },
        )
    )
}

@Composable
fun FriendSelectorCard(
    position: Int,
    displayName: String,
    selected: Boolean,
    onFriendSelected: (Int) -> Unit,
    onFriendUnselected: (Int) -> Unit
) {
    val selectedState = remember(position) { mutableStateOf(selected) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Checkbox(
                    checked = selectedState.value,
                    onCheckedChange = { checked ->
                        selectedState.value = checked
                        if(checked) {
                            onFriendSelected.invoke(position)
                        } else {
                            onFriendUnselected.invoke(position)
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
                //TODO: when implemented, this can be replaced with profile picture
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.search_profile_image_description, formatArgs = arrayOf(displayName)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}