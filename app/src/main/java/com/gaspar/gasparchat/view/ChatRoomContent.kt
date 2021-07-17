package com.gaspar.gasparchat.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaspar.gasparchat.viewmodel.ChatRoomViewModel

@Composable
fun ChatRoomContent(
    chatRoomUid: String,
    viewModel: ChatRoomViewModel
) {
    Text(text = "This is a Chat Room with UID $chatRoomUid", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}