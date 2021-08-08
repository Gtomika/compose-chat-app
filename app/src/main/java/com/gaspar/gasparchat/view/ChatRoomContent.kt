package com.gaspar.gasparchat.view

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.WatchForSnackbar
import com.gaspar.gasparchat.model.Message
import com.gaspar.gasparchat.model.isBlockedBy
import com.gaspar.gasparchat.viewmodel.ChatRoomViewModel
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun ChatRoomContent(viewModel: ChatRoomViewModel) {
    //if this is composed, then chat room view model is displaying stuff
    viewModel.displaying = true
    val scaffoldState = rememberScaffoldState()
    val lazyColumnState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    viewModel.lazyColumnState = lazyColumnState
    viewModel.composableCoroutineScope = coroutineScope
    val loading = viewModel.loading.collectAsState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { ChatRoomTopBar(viewModel) },
        bottomBar = { SendMessageContent(viewModel) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .alpha(if(loading.value) 0.3f else 1.0f))
        {
            ChatRoomBody(viewModel = viewModel, lazyColumnState)
        }
        LoadingIndicator(loadingFlow = viewModel.loading)
    }
    //watch for snackbar
    WatchForSnackbar(snackbarDispatcher = viewModel.snackbarDispatcher, snackbarHostState = scaffoldState.snackbarHostState)
}

@Composable
fun ChatRoomTopBar(viewModel: ChatRoomViewModel) {
    val showMenu = remember { mutableStateOf(false) }
    val showUnblock = viewModel.blockedMembersPresent.collectAsState()
    val users = viewModel.users.collectAsState()
    val localUser = viewModel.localUser.collectAsState()
    val title = viewModel.title.collectAsState()

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = viewModel::onBackClicked) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.profile_back)
                )
            }
        },
        title = { Text(text = title.value) },
        actions = {
            IconButton(onClick = {
                showMenu.value = !showMenu.value
            }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.chat_menu),
                )
            }
            DropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = { showMenu.value = false }
            ) {
                //if this is a group, add one-to-one chat option with all other users
                if(!viewModel.isOneToOneChat()) {
                     for(user in users.value) {
                         if(user.uid != localUser.value.uid) {
                             DropdownMenuItem(onClick = {
                                 showMenu.value = false
                                 viewModel.onDropDownInitiateChatClicked(user.uid)
                             }) {
                                 val chatMessage = stringResource(id = R.string.chat_initiate_with, formatArgs = arrayOf(user.displayName))
                                 Text(text = chatMessage)
                             }
                         }
                     }
                }
                //display block or unblock icon
                DropdownMenuItem(onClick = {
                    showMenu.value = false
                    if(showUnblock.value) viewModel.onDropDownUnblockClicked() else viewModel.onDropDownBlockClicked()
                }) {
                    if(!showUnblock.value) { //display block icon
                        val blockText = stringResource(id = if(viewModel.isOneToOneChat()) R.string.chat_block else R.string.chat_group_block )
                        Text(text = blockText)
                    } else { //display unblock icon
                        val unblockText = stringResource(id = if(viewModel.isOneToOneChat()) R.string.chat_unblock else R.string.chat_group_unblock )
                        Text(text = unblockText)
                    }
                }
            }
        }
    )
}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun ChatRoomBody(
    viewModel: ChatRoomViewModel,
    lazyColumnState: LazyListState
) {
    val blockedUsersPresent = viewModel.blockedMembersPresent.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        //top: block warning, if there is a block
        AnimatedVisibility(
            visible = blockedUsersPresent.value,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier.fillMaxWidth()
        ) {
            ChatRoomBlockedWarning()
        }
        //middle: messages
        MessagesContent(viewModel, lazyColumnState)
    }
}

@Composable
fun ColumnScope.ChatRoomBlockedWarning() {
    Surface(
        color = MaterialTheme.colors.error,
        border = BorderStroke(1.dp, Color.Black),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .weight(0.1f)
    ) {
        Text(
            text = stringResource(id = R.string.chat_room_blocked_warning),
            style = MaterialTheme.typography.subtitle1 + TextStyle(color = MaterialTheme.colors.onError),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

}

@SuppressLint("UnrememberedMutableState")
@ExperimentalAnimationApi
@Composable
fun MessagesContent(
    viewModel: ChatRoomViewModel,
    lazyColumnState: LazyListState
) {
    val messages = viewModel.messages.collectAsState()

    if(messages.value.isNotEmpty()) {
        //there are messages
        LazyColumn(
            state = lazyColumnState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            itemsIndexed(messages.value) { position, message ->
                val localMessage = message.senderUid == viewModel.localUser.value.uid
                if(localMessage) {
                    LocalMessageContent(
                        position = position,
                        messageText = message.messageText,
                        senderDisplayName = viewModel.localUser.value.displayName,
                        sendTime = message.messageTime,
                        deleted = message.deleted,
                        onMessageDeleted = viewModel::onMessageDeleted
                    )
                } else {
                    val senderDisplayName = remember(message) { mutableStateOf(viewModel.findDisplayNameForUid(message.senderUid)) }
                    val blocked = mutableStateOf(isBlockedBy(viewModel.localUser.value, message.senderUid))
                    MessageContent(
                        position = position,
                        messageText = message.messageText,
                        senderDisplayName = senderDisplayName.value,
                        sendTime = message.messageTime,
                        deleted = message.deleted,
                        blocked = blocked.value
                    )
                }
            }
        }
    } else {
        //no messages
        Text(
            text = stringResource(id = R.string.chat_no_messages),
            modifier = Modifier
                .padding(top = 50.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )
    }
}

@ExperimentalAnimationApi
@Preview
@Composable
fun PreviewMessageContent() {
    val message = Message(messageText = "This is a text message")
    MessageContent(
        position = 0,
        messageText = message.messageText,
        senderDisplayName = "Teszt Géza",
        sendTime = message.messageTime,
        deleted = false,
        blocked = false
    )
}



@ExperimentalAnimationApi
@Composable
fun MessageContent(
    position: Int,
    messageText: String,
    senderDisplayName: String,
    sendTime: Date,
    deleted: Boolean,
    blocked: Boolean
) {
    val formatter = remember { SimpleDateFormat("yyyy.MM.dd '-' HH:mm a", Locale.ENGLISH) }
    val formattedSendTime = remember(sendTime) { formatter.format(sendTime) }
    val showActions = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.Start),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start,
        ) {
            //TODO can be replaced with profile picture
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(id = R.string.chat_profile_picture),
                modifier = Modifier.padding(top = 16.dp, end = 8.dp)
            )
            Column(verticalArrangement = Arrangement.Top) {
                //display name of sender
                Text(
                    text = senderDisplayName,
                    style = MaterialTheme.typography.overline,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 2.dp)
                )
                //message surface
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.Start)
                        .clickable { showActions.value = true },
                    shape = RoundedCornerShape(size = 8.dp),
                    elevation = 5.dp,
                    color = Color.Gray,
                ) {
                    Text(
                        text = getDisplayText(messageText = messageText, deleted = deleted, blocked = blocked),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                //actions such as delete message
                AnimatedVisibility(
                    visible = showActions.value,
                    modifier = Modifier.align(Alignment.Start),
                    enter = slideInVertically(),
                    exit = slideOutVertically()
                ) {
                    Row(modifier = Modifier.wrapContentWidth()) {
                        //TODO: more actions can be added here
                        IconButton(onClick = { showActions.value = false }) {
                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                        }
                    }
                }
                //message send time
                Text(
                    text = formattedSendTime,
                    style = MaterialTheme.typography.overline,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@Preview
@Composable
fun PreviewLocalMessageContent() {
    val message = Message(messageText = "This is a local user message.")
    LocalMessageContent(
        position = 0,
        messageText = message.messageText,
        senderDisplayName = "Gáspár Tamás",
        sendTime = message.messageTime,
        deleted = false,
        onMessageDeleted = {}
    )
}

@ExperimentalAnimationApi
@Composable
fun LocalMessageContent(
    position: Int,
    messageText: String,
    senderDisplayName: String,
    sendTime: Date,
    deleted: Boolean,
    onMessageDeleted: (Int) -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy.MM.dd '-' HH:mm a", Locale.ENGLISH) }
    val formattedSendTime = remember(sendTime) { formatter.format(sendTime) }
    val showActions = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.End),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.End,
        ) {
            Column(verticalArrangement = Arrangement.Top) {
                //display name of sender
                Text(
                    text = senderDisplayName,
                    style = MaterialTheme.typography.overline,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 2.dp)
                )
                //message surface
                Surface(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.End)
                        .clickable { showActions.value = true },
                    shape = RoundedCornerShape(size = 8.dp),
                    elevation = 5.dp,
                    color = MaterialTheme.colors.primary,
                ) {
                    Text(
                        text = getDisplayText(messageText = messageText, deleted = deleted, blocked = false),
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                //actions such as delete message
                AnimatedVisibility(
                    visible = showActions.value,
                    modifier = Modifier.align(Alignment.End),
                    enter = slideInVertically(),
                    exit = slideOutVertically()
                ) {
                    Row(modifier = Modifier.wrapContentWidth()) {
                        IconButton(onClick = { onMessageDeleted.invoke(position) }, enabled = !deleted) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.chat_message_deleted))
                        }
                        IconButton(onClick = { showActions.value = false }) {
                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                        }
                    }
                }
                //message send time
                Text(
                    text = formattedSendTime,
                    style = MaterialTheme.typography.overline,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 2.dp)
                )
            }
            //TODO can be replaced with profile picture
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(id = R.string.chat_profile_picture),
                modifier = Modifier.padding(top = 16.dp, start = 8.dp)
            )
        }
    }
}

@Composable
fun getDisplayText(
    messageText: String,
    deleted: Boolean,
    blocked: Boolean
): String {
    return if(blocked && deleted) {
        stringResource(id = R.string.chat_message_deleted)
    } else if(blocked) {
        stringResource(id = R.string.chat_message_blocked)
    } else if(deleted) {
        stringResource(id = R.string.chat_message_deleted)
    } else {
        messageText
    }
}

@ExperimentalComposeUiApi
@Composable
fun SendMessageContent(viewModel: ChatRoomViewModel) {
    val message = viewModel.typedMessage.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = message.value.input,
            onValueChange = { viewModel.onTypedMessageChanged(it) },
            label = {
                val text = if(message.value.isError) message.value.errorMessage else stringResource(R.string.chat_send_message)
                Text(text = text)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() },
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .weight(0.9f),
            isError = message.value.isError,
            maxLines = 3
        )
        IconButton(
            onClick = { viewModel.onMessageSent() },
            enabled = message.value.input.isNotBlank() && !message.value.isError,
            modifier = Modifier
                .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                .weight(0.1f)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = stringResource(id = R.string.chat_send_message)
            )
        }
    }
}


