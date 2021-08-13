package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.*
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
    private val pictureRepository: PictureRepository
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * The logged in user who is using the app.
     */
    private val _currentUser = MutableStateFlow(User())
    val currentUser: StateFlow<User> = _currentUser

    /**
     * All [ChatRoom]s which the [currentUser] is participating in, as [DisplayChatRoom] objects.
     * All of these have at least one message.
     */
    private val _chats = MutableStateFlow(listOf<DisplayChatRoom>())
    val chats: StateFlow<List<DisplayChatRoom>> = _chats

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
            if(currentUserResult.isSuccessful && currentUserResult.result != null) {
                _currentUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //get all chat room of this user
                chatRoomRepository.getChatRoomsOfUser(currentUser.value.uid).addOnCompleteListener { chatsResult ->
                    if(chatsResult.isSuccessful && chatsResult.result != null) {
                        var chats = chatsResult.result!!.toObjects(ChatRoom::class.java)
                        //only keep the ones with messages
                        chats = getChatRoomsWithMessages(chats)
                        //now get other user UIDs
                        val otherUsersIds = getOtherUsers(chats)
                        //other users may have repetition, must query individually
                        var queryCounter = 0
                        var queryFailCounter = 0
                        val otherUsers = Array(otherUsersIds.size) { User() }
                        for(otherUserUid in otherUsersIds) {
                            userRepository.getUserById(otherUserUid).addOnCompleteListener { userResult ->
                                queryCounter++
                                if(userResult.isSuccessful && userResult.result != null) {
                                    //add this new user to the SAME index it originally was
                                    otherUsers[otherUsersIds.indexOf(otherUserUid)] = userResult.result!!.toObject(User::class.java)!!
                                    if(queryCounter == otherUsersIds.size) {
                                        //all users have been queried, but maybe some failed
                                        if(queryFailCounter > 0) {
                                            _loading.value = false
                                            showChatLoadingErrorSnackbar()
                                        } else {
                                            //no fails, all users and chats are ready, create DisplayChatRoom objects (async)
                                            createDisplayChatRooms(chats, otherUsers.toList())
                                        }
                                    }
                                } else {
                                    queryFailCounter++
                                }
                            }
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
     * Merges the [ChatRoom] and the other [User] objects, while getting the images. When finished
     * the result is sorted and shown.
     */
    private fun createDisplayChatRooms(chats: List<ChatRoom>, otherUsers: List<User>) {
        val onCompletion = { displayChatRooms: List<DisplayChatRoom> ->
            //sort by "activity"
            _chats.value = displayChatRooms.sortedByDescending { it.lastMessageTime }
            _loading.value = false
        }
        mergeChatRoomsAndUsers(
            chatRooms = chats,
            users = otherUsers,
            onCompletion = onCompletion,
            pictureRepository = pictureRepository
        )
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
     * Selects only the chat rooms which have at least 1 message.
     * @param rawData The chat rooms as received from firestore.
     * @return The chat rooms all of them with at least one message.
     */
    private fun getChatRoomsWithMessages(rawData: List<ChatRoom>): List<ChatRoom> {
        val chatsWithMessage = mutableListOf<ChatRoom>()
        for(chatRoom in rawData) {
            if(chatRoom.lastMessageTime != null && chatRoom.lastMessageText != null) {
                chatsWithMessage.add(chatRoom)
            }
        }
        return chatsWithMessage
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