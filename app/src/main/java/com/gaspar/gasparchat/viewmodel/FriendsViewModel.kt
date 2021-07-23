package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.ChatRoomRepository
import com.gaspar.gasparchat.model.User
import com.gaspar.gasparchat.model.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
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
    private val _friends = MutableStateFlow(listOf<User>())
    val friends: StateFlow<List<User>> = _friends

    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    init {
        EventBus.getDefault().register(this)
        getCurrentUserAndFriends()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFriendListUpdatedEvent(event: FriendsChangedEvent) {
        //refresh contents
        getCurrentUserAndFriends()
    }

    private fun getCurrentUserAndFriends() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            if(currentUserResult.isSuccessful) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get contact users
                if(currentUser.value.friends.isNotEmpty()) {
                    userRepository.getUsersByUid(currentUser.value.friends).addOnCompleteListener { contactsQueryResult ->
                        _loading.value = false
                        if(contactsQueryResult.isSuccessful && contactsQueryResult.result != null) {
                            val rawData = contactsQueryResult.result!!.toObjects(User::class.java)
                            _friends.value = rawData.sortedBy { it.displayName }
                        } else {
                            //failed to get contacts
                            showFriendsErrorSnackbar()
                        }
                    }
                } else {
                    //no contacts
                    _loading.value = false
                    _friends.value = listOf()
                }
            } else {
                _loading.value = false
                //failed to get current user
                showFriendsErrorSnackbar()
            }
        }
    }

    fun onFriendClicked(position: Int) {
        _loading.value = true
        val failMessage = context.getString(R.string.search_failed_to_start_chat)
        Log.d(TAG, "Contact ${friends.value[position].displayName} was clicked!")
        //can generate the chatRoomUid from the user Uid-s
        val chatRoomUid = chatRoomRepository.generateChatUid(currentUser.value.uid, friends.value[position].uid)
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
                    chatRoomRepository.createOneToOneChatRoom(userUid1 = currentUser.value.uid, userUid2 = friends.value[position].uid)
                        .addOnCompleteListener { chatRoomCreateResult ->
                            if(chatRoomCreateResult.isSuccessful) {
                                //chat room now exists, can open chat
                                _loading.value = false
                                navigateToChatRoom(chatRoomUid)
                            } else {
                                //failed to create chat room
                                _loading.value = false
                                snackbarDispatcher.createOnlyMessageSnackbar(failMessage)
                                snackbarDispatcher.showSnackbar()
                            }
                        }
                }
            } else {
                //failed to query chat room
                _loading.value = false
                snackbarDispatcher.createOnlyMessageSnackbar(failMessage)
                snackbarDispatcher.showSnackbar()
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

    private fun showFriendsErrorSnackbar() {
        snackbarDispatcher.setSnackbarMessage(context.getString(R.string.home_friends_error))
        snackbarDispatcher.setSnackbarLabel(context.getString(R.string.retry))
        snackbarDispatcher.setSnackbarAction { getCurrentUserAndFriends() } //retry action
        snackbarDispatcher.showSnackbar()
    }
}