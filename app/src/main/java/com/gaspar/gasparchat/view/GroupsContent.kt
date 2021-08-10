package com.gaspar.gasparchat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.DisplayChatRoom
import com.gaspar.gasparchat.viewmodel.GroupsViewModel
import com.gaspar.gasparchat.viewmodel.VoidMethod

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun GroupsContent(
    viewModel: GroupsViewModel,
    homePaddingValues: PaddingValues
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            GroupDialogFloatingActionButton(onClick = viewModel::showGroupDialog)
        }
    ) {
        val loading = viewModel.loading.collectAsState()
        Box(modifier = Modifier.padding(homePaddingValues)) {
           LoadingIndicator(loadingFlow = viewModel.loading)
           AnimatedVisibility(
               visible = !loading.value,
               enter = fadeIn(),
               exit = fadeOut()
           ) {
               GroupsBody(viewModel = viewModel)
           }
        }
    }
    //the group create dialog
    val displayGroupDialog = viewModel.displayGroupDialog.collectAsState()
    AnimatedVisibility(visible = displayGroupDialog.value) {
        GroupDialogContent(
            onDialogDismissed = viewModel::hideGroupDialog,
            onGroupCreated = viewModel::onGroupCreated
        )
    }
}

@Composable
fun GroupDialogFloatingActionButton(onClick: VoidMethod) {
    FloatingActionButton(onClick = onClick) {
        Icon(
            painter = painterResource(id = R.drawable.icon_group_create),
            contentDescription = stringResource(id = R.string.group_create_title)
        )
    }
}

@Composable
fun GroupsBody(viewModel: GroupsViewModel) {
    val groups = viewModel.groups.collectAsState()
    val loading = viewModel.loading.collectAsState()

    if(groups.value.isNotEmpty() && !loading.value) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(groups.value) { position, group ->
                GroupCard(
                    position = position,
                    group = group,
                    onGroupClicked = viewModel::onGroupClicked,
                )
            }
        }
    } else if(!loading.value) {
        //no groups yet
        Text(
            text = stringResource(id = R.string.home_no_groups),
            modifier = Modifier
                .padding(top = 50.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GroupCard(
    position: Int,
    group: DisplayChatRoom,
    onGroupClicked: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onGroupClicked.invoke(position) }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            //first row: name and image
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                //TODO: when implemented, this can be replaced with group picture
                Icon(
                    painter = painterResource(id = R.drawable.icon_group),
                    contentDescription = stringResource(id = R.string.search_profile_image_description, formatArgs = arrayOf(group.chatRoomName)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    text = group.chatRoomName,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            //second row: name of admin
            Text(
                text = stringResource(id = R.string.home_groups_admin_name, formatArgs = arrayOf(group.displayUserName)),
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            //third row: members count
            Text(
                text = stringResource(id = R.string.home_groups_member_count, formatArgs = arrayOf(group.memberCount)),
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}

