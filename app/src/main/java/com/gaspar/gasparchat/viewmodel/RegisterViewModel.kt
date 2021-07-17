package com.gaspar.gasparchat.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.UserRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model of the register screen.
 * @param navigationDispatcher Object used to send navigation commands.
 * @param snackbarDispatcher Object used to send snackbar show commands.
 * @param firebaseAuth Firebase authentication.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _email = MutableStateFlow(InputField())
    val email: StateFlow<InputField> = _email

    private val _password = MutableStateFlow(InputField())
    val password: StateFlow<InputField> = _password

    private val _passwordAgain = MutableStateFlow(InputField())
    val passwordAgain: StateFlow<InputField> = _passwordAgain

    private val _name = MutableStateFlow(InputField())
    val name: StateFlow<InputField> = _name

    private val _errorsPresent = MutableStateFlow(false)
    val errorsPresent: StateFlow<Boolean> = _errorsPresent

    /**
     * If an operation is in progress.
     */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * Stores if the last register attempt failed. This is cleared in any modification. Used to prevent
     * sending request with data that already failed.
     */
    private var registerFailed = false

    fun onEmailChanged(newEmailValue: String) {
        if(newEmailValue.isBlank()) {
            val message = context.getString(R.string.login_cannot_be_empty)
            _email.value = email.value.copy(input = newEmailValue, isError = true, errorMessage = message)
        } else {
            _email.value = _email.value.copy(input = newEmailValue, isError = false)
        }
        registerFailed = false //clear fail
        _errorsPresent.value = errorsInInputPresent()
    }

    fun onPasswordChanged(newPasswordValue: String) {
        when {
            newPasswordValue.isBlank() -> {
                val message = context.getString(R.string.login_cannot_be_empty)
                _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
            }
            newPasswordValue.contains(' ') -> {
                val message = context.getString(R.string.register_whitespace_illegal)
                _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
            }
            newPasswordValue.length !in PasswordLimits.MIN..PasswordLimits.MAX -> {
                //they are matching here
                val message = context.getString(R.string.register_password_incorrect_length, PasswordLimits.MIN, PasswordLimits.MAX)
                _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
            }
            else -> {
                _password.value = _password.value.copy(input = newPasswordValue, isError = false)
            }
        }
        registerFailed = false //clear fail
        _errorsPresent.value = errorsInInputPresent()
    }

    fun onPasswordAgainChanged(newPasswordAgainValue: String) {
        when {
            newPasswordAgainValue.isBlank() -> {
                val message = context.getString(R.string.login_cannot_be_empty)
                _passwordAgain.value = passwordAgain.value.copy(input = newPasswordAgainValue, isError = true, errorMessage = message)
            }
            newPasswordAgainValue.contains(' ') -> {
                val message = context.getString(R.string.register_whitespace_illegal)
                _passwordAgain.value = passwordAgain.value.copy(input = newPasswordAgainValue, isError = true, errorMessage = message)
            }
            newPasswordAgainValue.length !in PasswordLimits.MIN..PasswordLimits.MAX -> {
                //they are matching here
                val message = context.getString(R.string.register_password_incorrect_length, PasswordLimits.MIN, PasswordLimits.MAX)
                _passwordAgain.value = passwordAgain.value.copy(input = newPasswordAgainValue, isError = true, errorMessage = message)
            }
            else -> {
                _passwordAgain.value = _passwordAgain.value.copy(input = newPasswordAgainValue, isError = false)
            }
        }
        registerFailed = false //clear fail
        _errorsPresent.value = errorsInInputPresent()
    }

    fun onNameChanged(newNameValue: String) {
        if(newNameValue.isNotBlank() && newNameValue.length !in NameLimits.MIN..NameLimits.MAX) {
            val message = context.getString(R.string.register_name_incorrect_length, NameLimits.MIN, NameLimits.MAX)
            _name.value = name.value.copy(input = newNameValue, isError = true, errorMessage = message)
        } else {
            _name.value = _name.value.copy(input = newNameValue, isError = false)
        }
        registerFailed = false //clear fail
        _errorsPresent.value = errorsInInputPresent()
    }

    fun onRegisterButtonClicked() {
        //don't have to worry about errors: button is disabled in that case
        //check if any of the fields is empty but does not have error: this is the initial state
        if(emptyRequiredInputsPresent()) {
            val message = context.getString(R.string.login_invalid_data)
            showSnackbar(message = message)
            return
        }
        //password conditions: match
        if(password.value.input != passwordAgain.value.input) {
            _password.value = password.value.copy(isError = true, errorMessage = context.getString(R.string.register_password_not_matching))
            _passwordAgain.value = passwordAgain.value.copy(isError = true, errorMessage = context.getString(R.string.register_password_not_matching))
            return
        }
        //pass to firebase
        firebaseSignUp(email = email.value.input, password = password.value.input)
    }

    private fun errorsInInputPresent(): Boolean {
        return registerFailed || email.value.isError || password.value.isError ||
                passwordAgain.value.isError || name.value.isError
    }

    private fun emptyRequiredInputsPresent(): Boolean {
        return email.value.input.isBlank() || password.value.input.isBlank()
                || passwordAgain.value.input.isBlank()
    }

    /**
     * Adds an user to firebase (register).
     * @param email Email of the user. Not validated here, that is managed by firebase.
     * @param password Password, already validated.
     */
    private fun firebaseSignUp(email: String, password: String) {
        _loading.value = true
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { result ->
                //process result
                if(result.isSuccessful) { //success
                    val firebaseUser = firebaseAuth.currentUser
                    val displayName = name.value.input.ifBlank { getEmailFirstPart(email) }
                    //set the display name after registration
                    setDisplayNameOfRegisteredUser(firebaseUser!!, displayName).addOnCompleteListener { displayNameResult ->
                           if(displayNameResult.isSuccessful) {
                               //firebase has registered user. now add to database as well
                               userRepository.addUser(firebaseUser = firebaseUser).addOnCompleteListener { dbUserResult ->
                                   if(dbUserResult.isSuccessful) {
                                       //indicate that loading ended
                                       _loading.value = false
                                       //now we can redirect to home screen
                                       navigationDispatcher.dispatchNavigationCommand { navController ->
                                           navController.popBackStack()
                                           navController.navigate(NavDest.HOME)
                                       }
                                   }
                               }
                           }
                        }
                } else { //fail
                    //indicate that loading ended
                    _loading.value = false
                    //indicate that register has failed
                    registerFailed = true
                    Log.w(TAG, "Register fail", result.exception)
                    //handle some errors
                    when((result.exception as FirebaseAuthException).errorCode) {
                        "ERROR_INVALID_EMAIL" -> {
                            val message = context.getString(R.string.register_email_invalid)
                            _email.value = this.email.value.copy(isError = true, errorMessage = message)
                            showSnackbar(message = message)
                        }
                        "ERROR_EMAIL_ALREADY_IN_USE" -> {
                            val message = context.getString(R.string.register_email_in_use)
                            _email.value = this.email.value.copy(isError = true, errorMessage = message)
                            showSnackbar(message = message)
                        }
                        else -> { //other error
                            val message = context.getString(R.string.register_unknown_error)
                            showSnackbar(message = message)
                        }
                    }
                }
            }
    }

    fun onRedirectToLoginClicked() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack()
            navController.navigate(NavDest.LOGIN)
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

    /**
     * Starts an async task that changes the user's display name. This is part of the registration process.
     * @param registeredUser The newly registered user.
     * @param name The name.
     */
    private fun setDisplayNameOfRegisteredUser(registeredUser: FirebaseUser, name: String): Task<Void> {
        return registeredUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name).build()
        )
    }

    /**
     * Cuts the part form an email before @.
     */
    private fun getEmailFirstPart(email: String): String {
        return email.split("@")[0]
    }
}