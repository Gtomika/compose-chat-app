package com.gaspar.gasparchat.viewmodel

import android.app.Application
import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.InputField
import com.gaspar.gasparchat.model.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Controls the state of the profile page.
 * @param navigationDispatcher Object that can send navigation commands.
 * @param snackbarDispatcher Object that can send snackbar commands.
 * @param firebaseAuth Firebase authentication object.
 * @param context Application context.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    application: GasparChatApplication
): AndroidViewModel(application) {

    /**
     * Stores if there is any loading in the profile screen.
     */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * Stores if the logout dialog should be displayed.
     */
    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog

    /**
     * Stores if the re-authenticate dialog should be displayed (for password update).
     */
    private val _showUpdateAuthenticateDialog = MutableStateFlow(false)
    val showUpdateAuthenticateDialog: StateFlow<Boolean> = _showUpdateAuthenticateDialog

    /**
     * Stores if the re-authenticate dialog should be displayed (for account delete).
     */
    private val _showDeleteAuthenticateDialog = MutableStateFlow(false)
    val showDeleteAuthenticateDialog: StateFlow<Boolean> = _showDeleteAuthenticateDialog

    /**
     * Display name of the user.
     */
    private val _displayName = MutableStateFlow(if(firebaseAuth.currentUser != null) firebaseAuth.currentUser!!.displayName!! else "")
    val displayName: StateFlow<String> = _displayName

    /**
     * The content of the update display name text field. This is not necessarily the actual display name ([displayName]).
     */
    private val _typedDisplayName = MutableStateFlow(InputField(
        input = if(firebaseAuth.currentUser != null) firebaseAuth.currentUser!!.displayName!! else ""
    ))
    val typedDisplayName: StateFlow<InputField> = _typedDisplayName

    /**
     * Stores if the information about display names should be shown.
     */
    private val _showDisplayNameInfo = MutableStateFlow(false)
    val showDisplayNameInfo: StateFlow<Boolean> = _showDisplayNameInfo

    /**
     * Value of the new password input field.
     */
    private val _newPassword = MutableStateFlow(InputField())
    val newPassword: StateFlow<InputField> = _newPassword

    /**
     * Value of the new password AGAIN input field.
     */
    private val _newPasswordAgain = MutableStateFlow(InputField())
    val newPasswordAgain: StateFlow<InputField> = _newPasswordAgain

    private val context: Application = getApplication()

    fun redirectToLogin() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack()
            navController.navigate(NavDest.LOGIN)
        }
    }

    fun goBack() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack(NavDest.HOME, false)
        }
    }

    fun displayLogoutDialog() {
        _showLogoutDialog.value = true
    }

    fun hideLogoutDialog() {
        _showLogoutDialog.value = false
    }

    /**
     * Called when the user confirms logging out. Redirects to login screen.
     */
    fun onLogoutConfirmed() {
        hideLogoutDialog()
        firebaseAuth.signOut()
        redirectToLogin()
    }

    /**
     * This is called when the typed value changes. Not actually updating in firebase. Already detects invalid names.
     */
    fun onTypedDisplayNameChanged(newTypedDisplayName: String) {
        if(newTypedDisplayName.isNotBlank() && newTypedDisplayName.length !in NameLimits.MIN..NameLimits.MAX) {
            val message = context.getString(R.string.register_name_incorrect_length, NameLimits.MIN, NameLimits.MAX)
            _typedDisplayName.value = typedDisplayName.value.copy(input = newTypedDisplayName, isError = true, errorMessage = message)
        } else {
            _typedDisplayName.value = typedDisplayName.value.copy(input = newTypedDisplayName, isError = false)
        }
    }

    /**
     * This is called when the user presses the button to start the name change.
     */
    fun onUpdateDisplayName() {
        if(typedDisplayName.value.isError) {
            return
        }
        val user = firebaseAuth.currentUser!!
        //show loading
        _loading.value = true
        //new name
        val newDisplayName = typedDisplayName.value.input.ifBlank { getEmailFirstPart(user.email!!) }
        //build request
        val request =  UserProfileChangeRequest.Builder()
            .setDisplayName(newDisplayName)
            .build()
        //call firebase
        user.updateProfile(request).addOnCompleteListener { result ->
            //check result
            if(result.isSuccessful) {
                //updated in firebase auth, now update in database as well
                userRepository.updateUserDisplayName(firebaseUser = firebaseAuth.currentUser!!).addOnCompleteListener { updateResult ->
                    if(updateResult.isSuccessful) {
                        //loading is over
                        _loading.value = false
                        val message = context.getString(R.string.profile_display_name_update_success)
                        showSnackbar(message = message)
                        //update actual string as well, that is watched by other composables
                        _displayName.value = newDisplayName
                    } else {
                        //loading is over
                        _loading.value = false
                        val message = context.getString(R.string.profile_display_name_update_fail)
                        showSnackbar(message = message)
                    }
                }

            } else {
                //loading is over
                _loading.value = false
                val message = context.getString(R.string.profile_display_name_update_fail)
                showSnackbar(message = message)
            }
        }
    }

    fun onShowDisplayNameInfoCheckedChanged(isChecked: Boolean) {
        _showDisplayNameInfo.value = isChecked
    }

    fun onNewPasswordChanged(newPasswordValue: String) {
        when {
            newPasswordValue.isBlank() -> {
                val message = context.getString(R.string.login_cannot_be_empty)
                _newPassword.value = newPassword.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
            }
            newPasswordValue.contains(' ') -> {
                val message = context.getString(R.string.register_whitespace_illegal)
                _newPassword.value = newPassword.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
            }
            newPasswordValue.length !in PasswordLimits.MIN..PasswordLimits.MAX -> {
                //they are matching here
                val message = context.getString(R.string.register_password_incorrect_length, PasswordLimits.MIN, PasswordLimits.MAX)
                _newPassword.value = newPassword.value.copy(input = newPasswordValue, isError = true, errorMessage = message)
            }
            else -> {
                _newPassword.value = newPassword.value.copy(input = newPasswordValue, isError = false)
            }
        }
    }

    fun onNewPasswordAgainChanged(newPasswordAgainValue: String) {
        when {
            newPasswordAgainValue.isBlank() -> {
                val message = context.getString(R.string.login_cannot_be_empty)
                _newPasswordAgain.value = newPasswordAgain.value.copy(input = newPasswordAgainValue, isError = true, errorMessage = message)
            }
            newPasswordAgainValue.contains(' ') -> {
                val message = context.getString(R.string.register_whitespace_illegal)
                _newPasswordAgain.value = newPasswordAgain.value.copy(input = newPasswordAgainValue, isError = true, errorMessage = message)
            }
            newPasswordAgainValue.length !in PasswordLimits.MIN..PasswordLimits.MAX -> {
                //they are matching here
                val message = context.getString(R.string.register_password_incorrect_length, PasswordLimits.MIN, PasswordLimits.MAX)
                _newPasswordAgain.value = newPasswordAgain.value.copy(input = newPasswordAgainValue, isError = true, errorMessage = message)
            }
            else -> {
                _newPasswordAgain.value = newPasswordAgain.value.copy(input = newPasswordAgainValue, isError = false)
            }
        }
    }

    /**
     * Makes a call to firebase to first re-authenticate the user, then if that succeeds, changes the
     * password.
     * @param oldPassword The old password of the user, used to re-authenticate.
     */
    fun onUpdatePassword(oldPassword: String) {
        //hide dialog and show loading
        hideUpdateAuthenticateDialog()
        _loading.value = true
        //update
        val user = firebaseAuth.currentUser!!
        val credit = EmailAuthProvider.getCredential(user.email!!, oldPassword)
        user.reauthenticate(credit).addOnCompleteListener { result ->
            if(result.isSuccessful) {
                //the user entered correct password, ready to update
                //here it is assumed that the entered passwords are valid and matching
                user.updatePassword(newPassword.value.input).addOnCompleteListener { updateResult ->
                    _loading.value = false
                    if(updateResult.isSuccessful) {
                        _newPassword.value = newPassword.value.copy(input = "")
                        _newPasswordAgain.value = newPasswordAgain.value.copy(input = "")
                        val message = context.getString(R.string.profile_update_password_success)
                        showSnackbar(message)
                    } else {
                        val message = context.getString(R.string.profile_update_password_fail)
                        showSnackbar(message)
                    }
                }
            } else {
                _loading.value = false
                //something went wrong: probably incorrect password
                val message = context.getString(R.string.profile_update_password_old_incorrect)
                showSnackbar(message)
            }
        }
    }

    /**
     * Called when the user clicks on the update button after typing in their new password twice
     * ([newPassword], [newPasswordAgain]). The passwords are assumed to be valid, but they might not
     * match.
     */
    fun checkNewPasswords() {
        if(newPassword.value.input == newPasswordAgain.value.input) {
            //they match
           displayUpdateAuthenticateDialog() //start re authentication
        } else {
            val message = context.getString(R.string.register_password_not_matching)
            showSnackbar(message = message)
        }
    }

    /**
     * Called when the user types in their password to confirm they want to delete account. Makes a call
     * to firebase and deletes the account
     * @param password The password the user typed in.
     */
    fun onAccountDeleted(password: String) {
        //hide dialog and show loading
        hideDeleteAuthenticateDialog()
        _loading.value = true
        //update
        val firebaseUser = firebaseAuth.currentUser!!
        val user = userRepository.firebaseUserToUser(firebaseUser) //will be needed later
        val credit = EmailAuthProvider.getCredential(firebaseUser.email!!, password)
        firebaseUser.reauthenticate(credit).addOnCompleteListener { result ->
            if(result.isSuccessful) {
                //the user entered correct password, ready to delete
                firebaseUser.delete().addOnCompleteListener { deleteResult ->
                    if(deleteResult.isSuccessful) {
                        //the user is deleted from firebase auth, also remove all data from firestore
                        userRepository.deleteUser(user) {}
                        //redirect to login
                        redirectToLogin()
                    } else {
                        //failed to delete
                        val message = context.getString(R.string.profile_delete_account_fail)
                        showSnackbar(message)
                    }
                }
            } else {
                _loading.value = false
                //something went wrong: probably incorrect password
                val message = context.getString(R.string.profile_update_password_old_incorrect)
                showSnackbar(message)
            }
        }
    }

    private fun displayUpdateAuthenticateDialog() {
        _showUpdateAuthenticateDialog.value = true
    }

    fun hideUpdateAuthenticateDialog() {
        _showUpdateAuthenticateDialog.value = false
    }

    fun displayDeleteAuthenticateDialog() {
        _showDeleteAuthenticateDialog.value = true
    }

    fun hideDeleteAuthenticateDialog() {
        _showDeleteAuthenticateDialog.value = false
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
     * Cuts the part form an email before @.
     */
    private fun getEmailFirstPart(email: String): String {
        return email.split("@")[0]
    }
}