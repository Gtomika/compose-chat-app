package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.ChatRoom
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
class ChatsViewModel @Inject constructor(
    val snackbarDispatcher: SnackbarDispatcher,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val chatRoomRepository: ChatRoomRepository,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * The logged in user who is using the app.
     */
    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    /**
     * All [ChatRoom]s which the [currentUser] is participating in. All of these
     * have at least one message.
     */
    private val _chats = MutableStateFlow(listOf<ChatRoom>())
    val chats: StateFlow<List<ChatRoom>> = _chats

    /**
     * A user from each [chats], who is not the [currentUser]. These [User]s are used to
     * get title and image for one-to-one conversations.
     */
    private val _otherUsers = MutableStateFlow(listOf<User>())
    val otherUsers: StateFlow<List<User>> = _otherUsers

    init {
        EventBus.getDefault().register(this)
        getCurrentUserAndChats()
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    /**
     * A conversation started with a message, so it must be added to this screen.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChatStartedEvent(event: ChatStartedEvent) {
        Log.d(TAG, "Received chat started event, reloading chats...")
        getCurrentUserAndChats()
    }

    private fun getCurrentUserAndChats() {
        _loading.value = true
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            _loading.value = false
            if(currentUserResult.isSuccessful && currentUserResult.result != null) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get all chat room of this user
                chatRoomRepository.getChatRoomsOfUser(currentUser.value.uid).addOnCompleteListener { chatsResult ->
                    if(chatsResult.isSuccessful && chatsResult.result != null) {
                        val rawData = chatsResult.result!!.toObjects(ChatRoom::class.java)
                        val chats = sortChatRoomsByActivity(rawData)
                        //now get other user UIDs
                        val otherUsers = getOtherUsers(chats)
                        if(otherUsers.isNotEmpty()) {
                            userRepository.getUsersByUid(otherUsers).addOnCompleteListener { otherUserResult ->
                                if(otherUserResult.isSuccessful && otherUserResult.result != null) {
                                    //for each chat we now have a non local user
                                    _otherUsers.value = otherUserResult.result!!.toObjects(User::class.java)
                                } else {
                                    showChatLoadingErrorSnackbar()
                                }
                                _chats.value = chats
                                _loading.value = false
                            }
                        } else {
                            _loading.value = false
                        }
                    } else {
                        _loading.value = false
                        //failed to get chat room
                        showChatLoadingErrorSnackbar()
                    }
                }
            } else {
                _loading.value = false
                //failed to get current user
                showChatLoadingErrorSnackbar()
            }
        }
    }

    /**
     * Collect non [currentUser] UIDs from the chat rooms using [getOtherUserUid].
     */
    private fun getOtherUsers(chatRooms: List<ChatRoom>): List<String> {
        val otherUsers = mutableListOf<String>()
        for(chatRoom in chatRooms) {
            otherUsers.add(getOtherUserUid(chatRoom))
        }
        return otherUsers
    }

    /**
     * Gets a [User] uid from a [ChatRoom], who is not the [currentUser]. In case of
     * groups, where there can be many other users, the returned id will be the ID of the
     * group admin.
     */
    private fun getOtherUserUid(chatRoom: ChatRoom): String {
        return if(chatRoom.group) {
            chatRoom.admin!!
        } else {
            if(chatRoom.chatRoomUsers[0] == currentUser.value.uid) {
                chatRoom.chatRoomUsers[1]
            } else {
                chatRoom.chatRoomUsers[0]
            }
        }
    }

    /**
     * Sorts by the time of the last message. Chat rooms with no message will be ignored.
     * @param rawData The chat rooms as received from firestore.
     * @return The sorted chat rooms, all of them with at least one message.
     */
    private fun sortChatRoomsByActivity(rawData: List<ChatRoom>): List<ChatRoom> {
        val chatsWithMessage = mutableListOf<ChatRoom>()
        for(chatRoom in rawData) {
            if(chatRoom.lastMessageTime != null && chatRoom.lastMessageText != null) {
                chatsWithMessage.add(chatRoom)
            }
        }
        //sort the chat rooms by message time
        return chatsWithMessage.sortedByDescending { it.lastMessageTime }
    }

    private fun showChatLoadingErrorSnackbar() {
        snackbarDispatcher.setSnackbarMessage(context.getString(R.string.home_chats_error))
        snackbarDispatcher.setSnackbarLabel(context.getString(R.string.retry))
        snackbarDispatcher.setSnackbarAction { getCurrentUserAndChats() } //retry action
        snackbarDispatcher.showSnackbar()
    }

    private fun navigateToChatRoom(chatRoomUid: String) {
        Log.d(TAG, "Sending new chat room event, with chat room UID $chatRoomUid...")
        //send event to load chat room
        val event = ChatRoomChangedEvent(chatRoomUid)
        EventBus.getDefault().post(event)
    }

    /**
     * Called when a chat room card is clicked.
     * @param position Index of the card in [chats].
     */
    fun onChatClicked(position: Int) {
        val chatUid = chats.value[position].chatUid
        navigateToChatRoom(chatUid)
    }
}