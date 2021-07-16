package com.gaspar.gasparchat.viewmodel

import android.app.Application
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.ContactsChangedEvent
import com.gaspar.gasparchat.GasparChatApplication
import com.gaspar.gasparchat.R
import com.gaspar.gasparchat.SnackbarDispatcher
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    val snackbarDispatcher: SnackbarDispatcher,
    private val userRepository: UserRepository,
    application: GasparChatApplication
): AndroidViewModel(application) {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * The current users contact list.
     */
    private val _contacts = MutableStateFlow(listOf<User>())
    val contacts: StateFlow<List<User>> = _contacts

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    private val context: Application = getApplication()

    init {
        EventBus.getDefault().register(this)
        getCurrentUserAndContacts()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onContactListUpdatedEvent(event: ContactsChangedEvent) {
        //refresh contents
        getCurrentUserAndContacts()
    }

    fun getCurrentUserAndContacts() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            if(currentUserResult.isSuccessful) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get contact users
                if(currentUser.value.contacts.isNotEmpty()) {
                    userRepository.getUsersByUid(currentUser.value.contacts).addOnCompleteListener { contactsQueryResult ->
                        _loading.value = false
                        if(contactsQueryResult.isSuccessful && contactsQueryResult.result != null) {
                            _contacts.value = contactsQueryResult.result!!.toObjects(User::class.java)
                        } else {
                            //failed to get contacts
                            showContactsErrorSnackbar()
                        }
                    }
                } else {
                    //no contacts
                    _loading.value = false
                    _contacts.value = listOf()
                }
            } else {
                _loading.value = false
                //failed to get current user
                showContactsErrorSnackbar()
            }
        }
    }

    fun onContactClicked(position: Int) {

    }

    private fun showContactsErrorSnackbar() {
        val message = context.getString(R.string.home_contacts_error)
        val actionLabel = context.getString(R.string.retry)
        val onActionClicked = { getCurrentUserAndContacts() } //retry action
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
            onActionClicked = onActionClicked,
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