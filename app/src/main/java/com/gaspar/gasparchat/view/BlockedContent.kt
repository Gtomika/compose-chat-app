package com.gaspar.gasparchat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.viewmodel.BlockedViewModel
import kotlin.reflect.KFunction1

@ExperimentalAnimationApi
@Composable
fun BlockedContent(viewModel: BlockedViewModel, ) {
    val loading = viewModel.loading.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        LoadingIndicator(loadingFlow = viewModel.loading)
        AnimatedVisibility(
            visible = !loading.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BlockedBody(viewModel = viewModel)
        }
    }
}

@Composable
fun BlockedBody(viewModel: BlockedViewModel) {
    //depending on the amount of contacts
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val contacts = viewModel.blockedUsers.collectAsState()
        if(contacts.value.isNotEmpty()) {
            //there are contacts
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(contacts.value) { position, blockedUser ->
                   BlockedCard(
                       position = position,
                       blockedUser = blockedUser,
                       onUnblockClicked = viewModel::onUnblockClicked
                   )
                }
            }
        } else {
            //no contacts
            Text(
                text = stringResource(id = R.string.home_no_blocked),
                modifier = Modifier
                    .padding(top = 50.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BlockedCard(
    position: Int,
    blockedUser: User,
    onUnblockClicked: KFunction1<Int, Unit>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                //TODO: when implemented, this can be replaced with profile picture
                Icon(
                    painter = painterResource(id = R.drawable.icon_block),
                    contentDescription = stringResource(id = R.string.search_profile_image_description, formatArgs = arrayOf(blockedUser.displayName)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = blockedUser.displayName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            IconButton(
                onClick = { onUnblockClicked.invoke(position) },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_undo),
                    contentDescription = stringResource(id = R.string.undo)
                )
            }
        }
    }
}