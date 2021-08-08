package com.gaspar.gasparchat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.viewmodel.ChatsViewModel

@Composable
@ExperimentalAnimationApi
fun ChatsContent(
    viewModel: ChatsViewModel
) {
    val loading = viewModel.loading.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        LoadingIndicator(loadingFlow = viewModel.loading)
        AnimatedVisibility(
            visible = !loading.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ChatsBody(viewModel = viewModel)
        }
    }
}

@Composable
fun ChatsBody(viewModel: ChatsViewModel) {

    val chats = viewModel.chats.collectAsState()
    val otherUsers = viewModel.otherUsers.collectAsState()

    if(chats.value.isNotEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(chats.value) { position, chat ->
                if(chat.group) {
                    GroupChatCard(
                        position = position,
                        groupName = chat.chatRoomName,
                        lastMessageText = chat.lastMessageText!!,
                        onChatClicked = viewModel::onChatClicked
                    )
                } else {
                    //its possible that other users may not have loaded here
                    val otherUserName = try {
                        otherUsers.value[position].displayName
                    } catch (e: Exception) {
                        "..."
                    }
                    OneToOneChatCard(
                        position = position,
                        otherUserName = otherUserName,
                        lastMessageText = chat.lastMessageText!!,
                        onChatClicked = viewModel::onChatClicked
                    )
                }
            }
        }
    } else {
        Text(
            text = stringResource(id = R.string.home_no_chats),
            modifier = Modifier
                .padding(top = 50.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GroupChatCard(
    position: Int,
    groupName: String,
    lastMessageText: String,
    onChatClicked: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onChatClicked.invoke(position) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                //TODO: when implemented, this can be replaced with other user picture
                Icon(
                    painter = painterResource(id = R.drawable.icon_group),
                    contentDescription = groupName,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.home_last_message),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 16.dp)
                )
                //displays last message
                Text(
                    text = lastMessageText,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun OneToOneChatCard(
    position: Int,
    otherUserName: String,
    lastMessageText: String,
    onChatClicked: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onChatClicked.invoke(position) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                //TODO: when implemented, this can be replaced with other user picture
                Icon(
                    painter = painterResource(id = R.drawable.icon_chat),
                    contentDescription = otherUserName,
                    modifier = Modifier.padding(start = 16.dp)
                )
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.home_last_message),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
                //displays last message
                Text(
                    text = lastMessageText,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

        }
    }
}