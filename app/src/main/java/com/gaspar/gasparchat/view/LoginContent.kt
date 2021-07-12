package com.gaspar.gasparchat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun LoginContent(
    viewModel: LoginViewModel
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        content = {
            val loading = viewModel.loading.collectAsState()
            AnimatedVisibility(visible = loading.value) {
                LoadingIndicator()
            }
            AnimatedVisibility(visible = !loading.value) {
                LoginBox(viewModel = viewModel)
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
fun LoginBox(
   viewModel: LoginViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (focusRequester1, focusRequester2) = FocusRequester.createRefs()
        Prompt(promptText = stringResource(id = R.string.login_prompt))
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
            focusRequester = focusRequester2
        )
        Button(
            onClick = viewModel::onLoginButtonClicked,
            content = { Text(stringResource(id = R.string.login)) },
            modifier = Modifier.padding(8.dp)
        )
        RedirectToRegisterButton(onRedirectToRegisterClicked = viewModel::onRedirectToRegisterClicked)
    }
}

@Composable
fun Prompt(
    promptText: String
) {
    Text(
        text = promptText,
        style = MaterialTheme.typography.h5,
        maxLines = 1,
        modifier = Modifier.padding(8.dp)
    )
}

@ExperimentalComposeUiApi
@Composable
fun EmailInput(
    emailInputField: StateFlow<InputField>,
    onEmailChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    focusRequesterNext: FocusRequester? = null
) {
    val email = emailInputField.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = email.value.input,
        onValueChange = { onEmailChanged.invoke(it) },
        label = {
            val text = if(email.value.isError) email.value.errorMessage else stringResource(R.string.login_email)
            Text(text = text)
        },
        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = stringResource(id = R.string.login_email)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = if(focusRequesterNext != null) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() },
            onNext = { focusRequesterNext?.requestFocus() }
        ),
        modifier = Modifier
            .padding(8.dp)
            .focusOrder(focusRequester) {
                focusRequesterNext?.requestFocus()
            },
        isError = email.value.isError,
        maxLines = 1
    )
}

@ExperimentalComposeUiApi
@Composable
fun PasswordInput(
    passwordInputField: StateFlow<InputField>,
    labelText: String,
    onPasswordChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    focusRequesterNext: FocusRequester? = null
) {
    val password = passwordInputField.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = password.value.input,
        onValueChange = { onPasswordChanged(it) },
        label = {
            val text = if(password.value.isError) password.value.errorMessage else labelText
            Text(text = text)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if(focusRequesterNext != null) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.hide() },
            onNext = { focusRequesterNext?.requestFocus() }
        ),
        modifier = Modifier
            .padding(8.dp)
            .focusOrder(focusRequester) {
                focusRequesterNext?.requestFocus()
            },
        visualTransformation = PasswordVisualTransformation(),
        isError = password.value.isError,
        maxLines = 1
    )
}

@Composable
fun RedirectToRegisterButton(onRedirectToRegisterClicked: () -> Unit) {
    TextButton(
        onClick = onRedirectToRegisterClicked,
        content = { Text(text = stringResource(id = R.string.login_redirect_to_register)) },
        modifier = Modifier.padding(8.dp)
    )
}

/**
 * Shows indeterminate loading. Takes up all the space in what container it is used in.
 */
@Composable
fun LoadingIndicator() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}