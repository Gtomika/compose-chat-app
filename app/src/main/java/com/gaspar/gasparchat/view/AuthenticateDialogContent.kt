package com.gaspar.gasparchat.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.viewmodel.AuthenticateDialogViewModel
import com.gaspar.gasparchat.viewmodel.StringMethod
import com.gaspar.gasparchat.viewmodel.VoidMethod

/**
 * Reusable dialog that displays a password field, asking the user the authenticate themselves.
 * @param onDialogDismissed Called when the dialog is dismissed.
 * @param onDialogConfirmed Called when the OK is pressed, the current password value is passed.
 */
@ExperimentalComposeUiApi
@Composable
fun AuthenticateDialogContent(
   onDialogDismissed: VoidMethod,
   onDialogConfirmed: StringMethod
) {
    val viewModel = hiltViewModel<AuthenticateDialogViewModel>()
    val password = viewModel.password.collectAsState()
    val passwordValid = viewModel.passwordValid.collectAsState()
    val (focusRequester) = FocusRequester.createRefs()

    AlertDialog(
       onDismissRequest = {
          viewModel.clearPassword()
          onDialogDismissed.invoke()
       },
       title = {
          Text(
             text = stringResource(id = R.string.authenticate_dialog_title),
             style = MaterialTheme.typography.h6,
             modifier = Modifier.padding(bottom = 16.dp)
          )
       },
       text = {
          Column(modifier = Modifier.fillMaxWidth()) {
             Spacer(modifier = Modifier.height(16.dp))
             PasswordInput(
                passwordInputField = viewModel.password,
                labelText = stringResource(R.string.authneticate_dialog_current_password),
                onPasswordChanged = viewModel::onPasswordChanged,
                focusRequester = focusRequester,
                focusRequesterNext = null
             )
          }

       },
       dismissButton = {
          TextButton(onClick = {
             viewModel.clearPassword()
             onDialogDismissed.invoke()
          }) {
             Text(stringResource(id = R.string.authenticate_dialog_cancel))
          }
       },
       confirmButton = {
          Button(
             onClick = {
                viewModel.clearPassword()
                onDialogConfirmed(password.value.input)
             },
             enabled = passwordValid.value
          ) {
             Text(text = stringResource(id = R.string.authenticate_dialog_confirm))
          }
       },
       modifier = Modifier.padding(16.dp)
    )
}