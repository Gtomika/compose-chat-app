package com.gaspar.gasparchat.view

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.TAG
import com.gaspar.gasparchat.model.Message
import com.gaspar.gasparchat.viewmodel.ChatRoomViewModel
import kotlinx.coroutines.launch
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


    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { ChatRoomTopBar(viewModel) },
        bottomBar = { SendMessageContent(viewModel, lazyColumnState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ChatRoomBody(viewModel = viewModel, lazyColumnState)
        }
    }
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
                                 Icon(
                                     painter = painterResource(id = R.drawable.icon_chat),
                                     contentDescription = chatMessage
                                 )
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
                        Icon(
                            painter = painterResource(id = R.drawable.icon_block),
                            contentDescription = blockText
                        )
                    } else { //display unblock icon
                        val unblockText = stringResource(id = if(viewModel.isOneToOneChat()) R.string.chat_unblock else R.string.chat_group_unblock )
                        Text(text = unblockText)
                        Icon(
                            painter = painterResource(id = R.drawable.icon_undo),
                            contentDescription = unblockText
                        )
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
                        sendTime = message.messageTime
                    )
                } else {
                    val senderDisplayName = remember(message) { viewModel.findDisplayNameForUid(message.senderUid) }
                    MessageContent(
                        position = position,
                        messageText = message.messageText,
                        senderDisplayName = senderDisplayName,
                        sendTime = message.messageTime
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

@Preview
@Composable
fun PreviewMessageContent() {
    val message = Message(messageText = "This is a text message")
    MessageContent(
        position = 0,
        messageText = message.messageText,
        senderDisplayName = "Teszt Géza",
        sendTime = message.messageTime
    )
}



@Composable
fun MessageContent(
    position: Int,
    messageText: String,
    senderDisplayName: String,
    sendTime: Date
) {
    val formatter = remember { SimpleDateFormat("yyyy.MM.dd '-' HH:mm a", Locale.ENGLISH) }
    val formattedSendTime = remember(sendTime) { formatter.format(sendTime) }

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
                        .align(Alignment.Start),
                    shape = RoundedCornerShape(size = 8.dp),
                    elevation = 5.dp,
                    color = Color.Gray,
                ) {
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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

@Preview
@Composable
fun PreviewLocalMessageContent() {
    val message = Message(messageText = "This is a local user message.")
    LocalMessageContent(
        position = 0,
        messageText = message.messageText,
        senderDisplayName = "Gáspár Tamás",
        sendTime = message.messageTime
    )
}

@Composable
fun LocalMessageContent(
    position: Int,
    messageText: String,
    senderDisplayName: String,
    sendTime: Date
) {
    val formatter = remember { SimpleDateFormat("yyyy.MM.dd '-' HH:mm a", Locale.ENGLISH) }
    val formattedSendTime = remember(sendTime) { formatter.format(sendTime) }

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
                        .align(Alignment.End),
                    shape = RoundedCornerShape(size = 8.dp),
                    elevation = 5.dp,
                    color = MaterialTheme.colors.primary,
                ) {
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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

@ExperimentalComposeUiApi
@Composable
fun SendMessageContent(
    viewModel: ChatRoomViewModel,
    lazyColumnState: LazyListState
) {
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


