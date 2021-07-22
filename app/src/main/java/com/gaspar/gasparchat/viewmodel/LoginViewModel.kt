package com.gaspar.gasparchat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.NavDest
import com.gaspar.gasparchat.NavigationDispatcher
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.SnackbarDispatcher
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * View model of the login screen.
 * @param navigationDispatcher Object used to send navigation commands.
 * @param snackbarDispatcher Object used to send snackbar show commands.
 * @param firebaseAuth Firebase authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
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

    private val _errorsPresent = MutableStateFlow(false)
    val errorsPresent: StateFlow<Boolean> = _errorsPresent

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
        if(newEmailValue.isBlank()) {
            val message = context.getString(R.string.login_cannot_be_empty)
            _email.value = email.value.copy(input = newEmailValue, isError = true, errorMessage = message)
        } else {
            _email.value = email.value.copy(input = newEmailValue, isError = false)
        }
        if(password.value.input.isNotBlank()) _password.value = password.value.copy(isError = false)
        loginFailed = false
        _errorsPresent.value = errorsPresentInInput()
    }

    fun onPasswordChanged(newPasswordValue: String) {
        if(newPasswordValue.isBlank()) {
            val message = context.getString(R.string.login_cannot_be_empty)
            _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
        } else {
            _password.value = password.value.copy(input = newPasswordValue, isError = false)
        }
        if(email.value.input.isNotBlank()) _email.value = email.value.copy(isError = false)
        loginFailed = false
        _errorsPresent.value = errorsPresentInInput()
    }

    private fun errorsPresentInInput(): Boolean {
        return email.value.isError || password.value.isError
    }

    private fun emptyRequiredInputsPresent(): Boolean {
        return email.value.input.isBlank() || password.value.input.isBlank()
    }

    /**
     * Called when the user clicks the login button.
     */
    fun onLoginButtonClicked() {
        //check for existing errors
        if(loginFailed || emptyRequiredInputsPresent()) {
            val message = context.getString(R.string.login_invalid_data)
            snackbarDispatcher.createOnlyMessageSnackbar(message)
            snackbarDispatcher.showSnackbar()
            return
        }
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
                    //update the newly logged in users message token to this devices message token
                    userRepository.updateUserMessageToken(firebaseAuth.currentUser!!.uid)
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
                    snackbarDispatcher.createOnlyMessageSnackbar(message)
                    snackbarDispatcher.showSnackbar()
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
}