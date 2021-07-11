package com.gaspar.gasparchat.view

import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import com.gaspar.gasparchat.viewmodel.HomeViewModel

/**
 * Contents of the home screen.
 * @param lifecycleOwner Lifecycle owner used to listen to events.
 * @param viewModel Home view model.
 */
@Composable
fun HomeContent(
    lifecycleOwner: LifecycleOwner,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val scaffoldState = rememberScaffoldState()
    val firebaseUser = viewModel.firebaseAuth.currentUser
    if(firebaseUser == null) {
        //there is nobody logged in, redirect to home
        viewModel.redirectToLogin()
        return
    }
    Scaffold(
        scaffoldState = scaffoldState,
        content = {
            Text(firebaseUser.displayName!!)
        }
    )
    //watch for snackbar
    viewModel.snackbarDispatcher.snackbarEmitter.observe(lifecycleOwner) { snackbarCommand ->
        snackbarCommand.invoke(scaffoldState.snackbarHostState)
    }
}