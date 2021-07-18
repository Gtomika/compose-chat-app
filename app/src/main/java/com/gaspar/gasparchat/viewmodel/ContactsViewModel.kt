package com.gaspar.gasparchat.viewmodel

import android.app.Application
import android.content.Context
import android.util.EventLog
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.ChatRoomRepository
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val navigationDispatcher: NavigationDispatcher,
    private val userRepository: UserRepository,
    private val chatRoomRepository: ChatRoomRepository,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * The current users contact list.
     */
    private val _contacts = MutableStateFlow(listOf<User>())
    val contacts: StateFlow<List<User>> = _contacts

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

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

    private fun getCurrentUserAndContacts() {
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
        _loading.value = true
        val failMessage = context.getString(R.string.search_failed_to_start_chat)
        Log.d(TAG, "Contact ${contacts.value[position].displayName} was clicked!")
        //can generate the chatRoomUid from the user Uid-s
        val chatRoomUid = chatRoomRepository.generateChatUid(currentUser.value.uid, contacts.value[position].uid)
        //get chatroom see if it exists or not
        chatRoomRepository.getChatRoom(chatRoomUid).addOnCompleteListener { chatRoomResult ->
            if(chatRoomResult.isSuccessful && chatRoomResult.result != null) {
                //query for chat room successful
                if(chatRoomResult.result!!.exists()) {
                    //this chat room already exists
                    _loading.value = false
                    navigateToChatRoom(chatRoomUid)
                } else {
                    //the chat room between these 2 users doesn't exist yet: CREATE it
                    chatRoomRepository.createOneToOneChatRoom(userUid1 = currentUser.value.uid, userUid2 = contacts.value[position].uid)
                        .addOnCompleteListener { chatRoomCreateResult ->
                            if(chatRoomCreateResult.isSuccessful) {
                                //chat room now exists, can open chat
                                _loading.value = false
                                navigateToChatRoom(chatRoomUid)
                            } else {
                                //failed to create chat room
                                _loading.value = false
                                showSnackbar(failMessage)
                            }
                        }
                }
            } else {
                //failed to query chat room
                _loading.value = false
                showSnackbar(failMessage)
            }
        }
    }

    private fun navigateToChatRoom(chatRoomUid: String) {
        Log.d(TAG, "Sending new chat room event, with chat room UID $chatRoomUid...")
        //send event to load chat room
        val event = ChatRoomChangedEvent(chatRoomUid)
        EventBus.getDefault().post(event)
        //navigate there
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.CHAT_ROOM)
        }
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