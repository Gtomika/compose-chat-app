package com.gaspar.gasparchat.viewmodel

import android.content.Context
import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.InputField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog

    private val _displayName = MutableStateFlow(if(firebaseAuth.currentUser != null) firebaseAuth.currentUser!!.displayName!! else "")
    val displayName: StateFlow<String> = _displayName

    private val _typedDisplayName = MutableStateFlow(InputField(
        input = if(firebaseAuth.currentUser != null) firebaseAuth.currentUser!!.displayName!! else ""
    ))
    val typedDisplayName: StateFlow<InputField> = _typedDisplayName

    private val _showDisplayNameInfo = MutableStateFlow(false)
    val showDisplayNameInfo: StateFlow<Boolean> = _showDisplayNameInfo

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
        //show loading
        _loading.value = true
        //call firebase
        firebaseAuth.currentUser!!.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(typedDisplayName.value.input)
                .build()
        ).addOnCompleteListener { result ->
            //loading is over
            _loading.value = false
            //check result
            if(result.isSuccessful) {
                val message = context.getString(R.string.profile_display_name_update_success)
                showSnackbar(message = message)
                //update actual string as well, that is watched by other composables
                _displayName.value = typedDisplayName.value.input
            } else {
                val message = context.getString(R.string.profile_display_name_update_fail)
                showSnackbar(message = message)
            }
        }
    }

    fun onShowDisplayNameInfoCheckedChanged(isChecked: Boolean) {
        _showDisplayNameInfo.value = isChecked
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