package com.gaspar.gasparchat.viewmodel

import android.app.Application
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.GasparChatApplication
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.SnackbarDispatcher
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    val snackbarDispatcher: SnackbarDispatcher,
    private val userRepository: UserRepository,
    application: GasparChatApplication
): AndroidViewModel(application) {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    private val context: Application = getApplication()

    init {
        getCurrentUserAndChats()
    }

    private fun getCurrentUserAndChats() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            _loading.value = false
            if(currentUserResult.isSuccessful) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //TODO: get chats
            } else {
                //failed to get current user
                showChatLoadingErrorSnackbar()
            }
        }
    }

    private fun showChatLoadingErrorSnackbar() {
        val message = context.getString(R.string.home_chats_error)
        val actionLabel = context.getString(R.string.retry)
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
            onActionClicked = { getCurrentUserAndChats() }
        )
    }

    /**
     * Quick way to show a snackbar.
     * @param message Message to show.
     */
    private fun showSnackbar(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        actionLabel: String? = null,
        onActionClicked: VoidMethod = {}
    ) {
        snackbarDispatcher.dispatchSnackbarCommand { snackbarHostState ->
            viewModelScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration
                )
                when(result) {
                    SnackbarResult.ActionPerformed -> onActionClicked.invoke()
                    SnackbarResult.Dismissed -> { }
                }
            }
        }
    }

}