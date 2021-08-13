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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.DisplayUser
import com.gaspar.gasparchat.viewmodel.FriendsViewModel

@ExperimentalAnimationApi
@Composable
fun FriendsContent(viewModel: FriendsViewModel, ) {
    val loading = viewModel.loading.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        LoadingIndicator(loadingFlow = viewModel.loading)
        AnimatedVisibility(
            visible = !loading.value,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FriendsBody(viewModel = viewModel)
        }
    }
}

@Composable
fun FriendsBody(viewModel: FriendsViewModel) {
    //depending on the amount of contacts
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val contacts = viewModel.friends.collectAsState()
        if(contacts.value.isNotEmpty()) {
            //there are contacts
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(contacts.value) { position, friend ->
                    FriendCard(
                        position = position,
                        friend = friend,
                        onContactClicked = viewModel::onFriendClicked
                    )
                }
            }
        } else {
            //no contacts
            Text(
                text = stringResource(id = R.string.home_no_friends),
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
fun FriendCard(
    position: Int,
    friend: DisplayUser,
    onContactClicked: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onContactClicked.invoke(position) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                if(friend.profilePicture != null) {
                    ProfilePicture(
                        picture = friend.profilePicture,
                        displayName = friend.displayName
                    )
                } else {
                    //use default picture
                    DefaultProfilePicture(displayName = friend.displayName)
                }
                Text(
                    text = friend.displayName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
