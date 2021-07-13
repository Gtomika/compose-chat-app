package com.gaspar.gasparchat.view

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Contents of the home screen.
 */
@ExperimentalAnimationApi
@Composable
fun HomeContent() {
    val viewModel = hiltViewModel<HomeViewModel>()
    val firebaseUser = viewModel.firebaseAuth.currentUser
    if(firebaseUser == null) {
        //there is nobody logged in, redirect to home
        viewModel.redirectToLogin()
        return
    }
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            HomeTopBar(
                onProfileClicked = viewModel::redirectToProfile,
                onSearchClicked = viewModel::redirectToSearch
            )
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                LoadingIndicator(loadingFlow = viewModel.loading)
                Text(firebaseUser.displayName!!) //TODO: when replaced with real content, set alpha according to loading
            }
        }
    )
    //watch for snackbar
    LaunchedEffect(key1 = viewModel, block = {
        launch {
            viewModel.snackbarDispatcher.snackbarEmitter.collect { snackbarCommand ->
                snackbarCommand?.invoke(scaffoldState.snackbarHostState)
            }
        }
    })
}

@Composable
fun HomeTopBar(
    onProfileClicked: () -> Unit,
    onSearchClicked: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(id = R.string.home_title)) },
        navigationIcon = {
            IconButton(onClick = onProfileClicked) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(id = R.string.profile_title)
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search_title)
                )
            }
        }
    )
}