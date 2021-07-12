package com.gaspar.gasparchat.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.viewmodel.RegisterViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
@Composable
fun RegisterContent(
    viewModel: RegisterViewModel
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        content = {
            val loading = viewModel.loading.collectAsState()
            if(loading.value) {
                LoadingIndicator()
            } else {
                RegisterBox(viewModel = viewModel)
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

@ExperimentalComposeUiApi
@Composable
fun RegisterBox(
    viewModel: RegisterViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (focusRequester1, focusRequester2, focusRequester3, focusRequester4) = FocusRequester.createRefs()

        Prompt(promptText = stringResource(id = R.string.register_prompt))
        EmailInput(
            emailInputField = viewModel.email,
            onEmailChanged = viewModel::onEmailChanged,
            focusRequester = focusRequester1,
            focusRequesterNext = focusRequester2
        )
        PasswordInput(
            passwordInputField = viewModel.password,
            labelText = stringResource(id = R.string.login_password),
            onPasswordChanged = viewModel::onPasswordChanged,
            focusRequester = focusRequester2,
            focusRequesterNext = focusRequester3
        )
        PasswordInput(
            passwordInputField = viewModel.passwordAgain,
            labelText = stringResource(id = R.string.register_password_again),
            onPasswordChanged = viewModel::onPasswordAgainChanged,
            focusRequester = focusRequester3,
            focusRequesterNext = focusRequester4
        )
        NameInput(
            nameInputField = viewModel.name,
            onNameChanged = viewModel::onNameChanged,
            focusRequester = focusRequester4
        )
        Button(
            onClick = viewModel::onRegisterButtonClicked,
            content = { Text(stringResource(id = R.string.register)) },
            modifier = Modifier.padding(8.dp)
        )
        RedirectToLoginButton(onRedirectToLoginClicked = viewModel::onRedirectToLoginClicked)
    }
}

@ExperimentalComposeUiApi
@Composable
fun NameInput(
    nameInputField: StateFlow<InputField>,
    onNameChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    focusRequesterNext: FocusRequester? = null
) {
    val name = nameInputField.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = name.value.input,
        onValueChange = { onNameChanged(it) },
        label = {
            val text = if(name.value.isError) name.value.errorMessage else stringResource(R.string.register_username)
            Text(text = text)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(id = R.string.register_username)
            ) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = if(focusRequesterNext != null) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() },
            onNext = { focusRequesterNext?.requestFocus() }
        ),
        modifier = Modifier.padding(8.dp)
            .focusOrder(focusRequester) { focusRequesterNext?.requestFocus() },
        maxLines = 1,
        isError = name.value.isError
    )
}

@Composable
fun RedirectToLoginButton(onRedirectToLoginClicked: () -> Unit) {
    TextButton(
        onClick = onRedirectToLoginClicked,
        content = { Text(text = stringResource(id = R.string.register_redirect_to_login)) },
        modifier = Modifier.padding(8.dp)
    )
}