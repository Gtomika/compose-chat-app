package com.gaspar.gasparchat.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.PasswordLimits
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.model.InputField
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

typealias VoidMethod = () -> Unit
typealias StringMethod = (String) -> Unit

@HiltViewModel
class AuthenticateDialogViewModel @Inject constructor(
    @ApplicationContext private val context: Context
): ViewModel() {

    /**
     * Displayed password inside the authenticate dialog
     */
    private val _password = MutableStateFlow(InputField())
    val password: StateFlow<InputField> = _password

    /**
     * Stores if the typed in password is valid.
     */
    private val _passwordValid = MutableStateFlow(false)
    val passwordValid: StateFlow<Boolean> = _passwordValid

    /**
     * Called when the dialog is dismissed. Use [bindCallbacks] to set.
     */
    var onDialogDismissed: VoidMethod = {}
        private set

    /**
     * Called when the dialog is accepted. Use [bindCallbacks] to set. It's parameter will be the typed in
     * password, which is validated.
     */
    var onDialogConfirmed: StringMethod = {}
        private set

    /**
     * Called when typed password value changes.
     */
    fun onPasswordChanged(newPasswordValue: String) {
        when {
            newPasswordValue.isBlank() -> {
                val message = context.getString(R.string.login_cannot_be_empty)
                _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
                _passwordValid.value = false
            }
            newPasswordValue.contains(' ') -> {
                val message = context.getString(R.string.register_whitespace_illegal)
                _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
                _passwordValid.value = false
            }
            newPasswordValue.length !in PasswordLimits.MIN..PasswordLimits.MAX -> {
                //they are matching here
                val message = context.getString(R.string.register_password_incorrect_length, PasswordLimits.MIN, PasswordLimits.MAX)
                _password.value = password.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
                _passwordValid.value = false
            }
            else -> {
                _password.value = password.value.copy(input = newPasswordValue, isError = false)
                _passwordValid.value = true
            }
        }
    }

    /**
     * Sets the callbacks to be invoked when the dialog is hidden.
     */
    fun bindCallbacks(onDialogDismissed: VoidMethod, onDialogConfirmed: StringMethod) {
        this.onDialogDismissed = onDialogDismissed
        this.onDialogConfirmed = onDialogConfirmed
    }

    fun clearPassword() {
        _password.value = password.value.copy(input = "")
        _passwordValid.value = true
    }
}