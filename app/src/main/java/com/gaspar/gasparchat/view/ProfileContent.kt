package com.gaspar.gasparchat.view

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun ProfileContent() {
    val viewModel = hiltViewModel<ProfileViewModel>()
    val firebaseUser = viewModel.firebaseAuth.currentUser
    if(firebaseUser == null) {
        //there is nobody logged in, redirect to home
        viewModel.redirectToLogin()
        return
    }

    val displayName = viewModel.displayName.collectAsState()
    val loading = viewModel.loading.collectAsState()

    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            ProfileTopBar(
                displayName = displayName.value,
                onBackClicked = viewModel::goBack,
                onLogoutClicked = viewModel::displayLogoutDialog,
                onLogoutDismissed = viewModel::hideLogoutDialog,
                onLogoutConfirmed = viewModel::onLogoutConfirmed,
                viewModel.showLogoutDialog
            )
        }
    ) {
        AnimatedVisibility(visible = loading.value) {
            LoadingIndicator()
        }
        AnimatedVisibility(visible = !loading.value) {
            ProfileBody(viewModel = viewModel)
        }
    }

    //watch for snackbar
    LaunchedEffect(key1 = viewModel, block = {
        launch {
            viewModel.snackbarDispatcher.snackbarEmitter.collect { snackbarCommand ->
                snackbarCommand?.invoke(scaffoldState.snackbarHostState)
            }
        }
    })
}

@ExperimentalAnimationApi
@Composable
fun ProfileTopBar(
    displayName: String,
    onBackClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onLogoutDismissed: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    showLogoutFlow: StateFlow<Boolean>
) {
    val showLogoutDialog = showLogoutFlow.collectAsState()
    val title = stringResource(id = R.string.profile_of, formatArgs = arrayOf(displayName))
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, 
                    contentDescription = stringResource(id = R.string.profile_back)
                )
            }
        },
        actions = {
            IconButton(onClick = onLogoutClicked) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = stringResource(id = R.string.profile_log_out)
                )
            }
        }
    )
    //alert dialog
    AnimatedVisibility(visible = showLogoutDialog.value) {
        LogoutDialog(
            onLogoutDismissed = onLogoutDismissed,
            onLogoutConfirmed = onLogoutConfirmed
        )
    }
}

@Composable
fun LogoutDialog(
    onLogoutDismissed: () -> Unit,
    onLogoutConfirmed: () -> Unit,
) {
    AlertDialog(
        title = { Text(stringResource(id = R.string.profile_log_out)) },
        text = { Text(stringResource(id = R.string.profile_log_out_confirm)) },
        onDismissRequest = onLogoutDismissed,
        dismissButton = {
            TextButton(onClick = onLogoutDismissed) {
                Text(text = stringResource(id = R.string.no))
            }
        },
        confirmButton = {
            TextButton(onClick = onLogoutConfirmed) {
                Text(text = stringResource(id = R.string.yes))
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun ProfileBody(
    viewModel: ProfileViewModel
) {
    val scrollState = rememberScrollState()
    Column( //not many composables here, no need for a lazy column
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
    ) {
        DisplayNameChanger(
            displayNameInputField = viewModel.typedDisplayName,
            showInfoFlow = viewModel.showDisplayNameInfo,
            onUpdateTypedDisplayName = viewModel::onTypedDisplayNameChanged,
            onUpdateDisplayName = viewModel::onUpdateDisplayName,
            onShowInfoCheckedChanged = viewModel::onShowDisplayNameInfoCheckedChanged
        )
    }
}

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun DisplayNameChanger(
    displayNameInputField: StateFlow<InputField>,
    showInfoFlow: StateFlow<Boolean>,
    onUpdateTypedDisplayName: (String) -> Unit,
    onUpdateDisplayName: () -> Unit,
    onShowInfoCheckedChanged: (Boolean) -> Unit
) {
    val displayName = displayNameInputField.collectAsState()
    val showInfo = showInfoFlow.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(id = R.string.profile_update_display_name),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = displayName.value.input,
                onValueChange = { onUpdateTypedDisplayName(it) },
                isError = displayName.value.isError,
                label = {
                    val text = if(displayName.value.isError) displayName.value.errorMessage else stringResource(R.string.profile_display_name)
                    Text(text = text)
                },
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() },
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(id = R.string.profile_display_name)
                    )
                }
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                IconToggleButton(
                    checked = showInfo.value,
                    onCheckedChange = onShowInfoCheckedChanged,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(id = R.string.info)
                    )
                }
                Button(
                    onClick = onUpdateDisplayName,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 16.dp),
                    enabled = !displayName.value.isError
                ) {
                    Text(text = stringResource(id = R.string.profile_save))
                }
            }
            //info, only shows when needed
            AnimatedVisibility(
                visible = showInfo.value,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Text(
                    text = stringResource(id = R.string.profile_display_name_info),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}