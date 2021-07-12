package com.gaspar.gasparchat.viewmodel

import android.content.Context
import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.NavigationDispatcher
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.SnackbarDispatcher
import com.gaspar.gasparchat.model.InputField
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model of the login screen.
 * @param navigationDispatcher Object used to send navigation commands.
 * @param snackbarDispatcher Object used to send snackbar show commands.
 * @param firebaseAuth Firebase authentication.
 * @param context The application context.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context //this is not a leak, application context
): ViewModel() {

    /**
     * State of the email.
     */
    private val _email = MutableStateFlow(InputField())
    val email: StateFlow<InputField> = _email

    /**
     * State of the password.
     */
    private val _password = MutableStateFlow(InputField())
    val password: StateFlow<InputField> = _password

    /**
     * True if a recent login failed and the user has not updated anything since then.
     */
    private var loginFailed = false

    /**
     * If an operation is in progress.
     */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun onEmailChanged(newEmailValue: String) {
        _email.value = email.value.copy(input = newEmailValue, isError = false)
        _password.value = password.value.copy(isError = false)
        loginFailed = false
    }

    fun onPasswordChanged(newPasswordValue: String) {
        _password.value = password.value.copy(input = newPasswordValue, isError = false)
        _email.value = email.value.copy(isError = false)
        loginFailed = false
    }

    /**
     * Called when the user clicks the login button.
     */
    fun onLoginButtonClicked() {
        //check for existing errors
        if(loginFailed || email.value.isError || password.value.isError) {
            val message = context.getString(R.string.login_invalid_data)
            showSnackbar(message)
            return
        }
        var errorsFound = false
        //user sees no errors, check his input
        if(email.value.input.isBlank()) {
            _email.value = email.value.copy(isError = true, errorMessage = context.getString(R.string.login_cannot_be_empty))
            errorsFound = true
        }
        if(password.value.input.isBlank()) {
            _password.value = password.value.copy(isError = true, errorMessage = context.getString(R.string.login_cannot_be_empty))
            errorsFound = true
        }
        //don't continue with errors
        if(errorsFound) return
        //no errors, call firebase
        logInWithFirebase(email.value.input, password.value.input)
    }

    /**
     * Logs in to the firebase authentication.
     * @param email Email.
     * @param password Password.
     */
    private fun logInWithFirebase(email: String, password: String) {
        _loading.value = true
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { result ->
                //loading is over
                _loading.value = false
                if(result.isSuccessful) {
                    //redirect to home
                    navigationDispatcher.dispatchNavigationCommand { navController ->
                        navController.popBackStack()
                        navController.navigate(NavDest.HOME)
                    }
                } else {
                    loginFailed = true
                    //handle some errors
                    val labelMessage = context.getString(R.string.login_invalid_data_label)
                    _email.value = this.email.value.copy(isError = true, errorMessage = labelMessage)
                    _password.value = this.password.value.copy(isError = true, errorMessage = labelMessage)
                    val message = context.getString(R.string.login_fail)
                    showSnackbar(message = message)
                }
            }
    }

    /**
     * Called when the user selects that they don't have an account yet.
     */
    fun onRedirectToRegisterClicked() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack()
            navController.navigate(NavDest.REGISTER)
        }
    }

    /**
     * Quick way to show a snackbar.
     * @param message Message to show.
     */
    private fun showSnackbar(message: String) {
        snackbarDispatcher.dispatchSnackbarCommand { snackbarHostState ->
            viewModelScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = null,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
}