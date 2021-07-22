package com.gaspar.gasparchat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.WatchForSnackbar
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
            Box(modifier = Modifier.fillMaxSize()) {
                LoadingIndicator(viewModel.loading)
                LoginBox(viewModel = viewModel)
            }
        }
    )
    //watch for snackbar
    WatchForSnackbar(snackbarDispatcher = viewModel.snackbarDispatcher, snackbarHostState = scaffoldState.snackbarHostState)
}

@ExperimentalComposeUiApi
@Composable
fun LoginBox(
   viewModel: LoginViewModel
) {
    val loading = viewModel.loading.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
            .alpha(if(loading.value) 0.5f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
        val errorsPresent = viewModel.errorsPresent.collectAsState()
        Button(
            onClick = viewModel::onLoginButtonClicked,
            content = { Text(stringResource(id = R.string.login)) },
            modifier = Modifier.padding(8.dp),
            enabled = !errorsPresent.value
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusOrder(focusRequester) {
                focusRequesterNext?.requestFocus()
            }
            .fillMaxWidth(),
        isError = email.value.isError,
        maxLines = 1
    )
}

/**
 * Creates a password field. Validates password.
 * @param passwordInputField State flow of the [InputField] that belongs to this password.
 * @param labelText Displayed label text, if there is no error.
 * @param onPasswordChanged Called with the new value if the user types.
 * @param focusRequester [FocusRequester] of this field.
 * @param focusRequesterNext [FocusRequester] of the next field, or null if there is none.
 */
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .focusOrder(focusRequester) {
                focusRequesterNext?.requestFocus()
            }
            .fillMaxWidth(),
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
 * Shows indeterminate loading. Takes up all the space in what container it is used in, so it should be placed
 * above the content, with a [Box]. Has animated visibility.
 */
@ExperimentalAnimationApi
@Composable
fun LoadingIndicator(
    loadingFlow: StateFlow<Boolean>
) {
    val loading = loadingFlow.collectAsState()
    AnimatedVisibility(
        visible = loading.value,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }
}