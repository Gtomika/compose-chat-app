package com.gaspar.gasparchat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.gaspar.gasparchat.viewmodel.ChatsViewModel

@Composable
@ExperimentalAnimationApi
fun ChatsContent(
    viewModel: ChatsViewModel
) {
    val loading = viewModel.loading.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        LoadingIndicator(loadingFlow = viewModel.loading)
        AnimatedVisibility(visible = !loading.value) {
            ChatsBody(viewModel = viewModel)
        }
    }
}

@Composable
fun ChatsBody(viewModel: ChatsViewModel) {
    Text(text = "Chats of ${viewModel.currentUser.value.displayName}")
}