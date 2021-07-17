package com.gaspar.gasparchat.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.*
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
class ChatRoomViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRoomRepository: ChatRoomRepository,
    private val userRepository: UserRepository,
    private val snackbarDispatcher: SnackbarDispatcher
): ViewModel() {

    /**
     * Stores is a background task in ongoing.
     */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /**
     * The currently displayed [ChatRoom].
     */
    private val _chatRoom = MutableStateFlow(ChatRoom())
    val chatRoom: StateFlow<ChatRoom> = _chatRoom

    /**
     * Messages of the current chat room. This is more then just the message UIDs in the [ChatRoom]
     * objects, these are the [Message] objects.
     */
    private val _messages = MutableStateFlow(listOf<Message>())
    val messages: StateFlow<List<Message>> = _messages

    /**
     * Users in this chat room. The local user is stored in [localUser]. These are more then just
     * UID-s, these are the [User] objects.
     */
    private val _users = MutableStateFlow(listOf<User>())
    val users: StateFlow<List<User>> = _users

    /**
     * The [User] in the chat room who is currently logged in, in other words the one using the app.
     */
    private val _localUser = MutableStateFlow(User())
    val localUser: StateFlow<User> = _localUser

    /**
     * The amount of tasks that must be completed before the chat can be displayed.
     */
    private var loadingProcessAmount: Int = 0

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Called when a new chat room must be loaded.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChatRoomChanged(event: ChatRoomChangedEvent) {
        Log.d(TAG, "Chat room change event arrived, new chat room UID is ${event.chatRoomId}")
        //set loading process amount
        loadingProcessAmount = 2
        //indicate loading (it will disappear once loading process amount resets to 0)
        _loading.value = true
        //load current user
        loadLocalUserAndAddChatRoom(event.chatRoomId)
        //load all data about the chat room
        getChatRoomAndMembers(event.chatRoomId)
    }

    @Subscribe
    fun onLoadingProcessFinished(event: LoadingFinishedEvent) {
        loadingProcessAmount--
        Log.d(TAG, "A loading process finished, $loadingProcessAmount remains.")
        if(loadingProcessAmount == 0) {
            Log.d(TAG, "All loading processes finished, showing content...")
            _loading.value = false
        }
    }

    /**
     * Fetches the local user from firestore and adds this chat room to their chat room list,
     * if not already present.
     */
    private fun loadLocalUserAndAddChatRoom(chatRoomUid: String) {
        userRepository.getCurrentUser().addOnCompleteListener { currentUserResult ->
            if(currentUserResult.isSuccessful && currentUserResult.result != null) {
                _localUser.value = currentUserResult.result!!.toObjects(User::class.java)[0]
                //continue with other task
            } else {
                //this task cannot go on
                EventBus.getDefault().post(LoadingFinishedEvent)
                val failMessage = context.getString(R.string.chat_load_fail)
                showSnackbar(failMessage)
            }
        }.continueWith { previousTask ->
            //add chat room if needed
            if(previousTask.isSuccessful) {
                Log.d(TAG, "Obtained local user ${localUser.value.displayName}, checking their chat room UID...")
                userRepository.addUserChatRoom(localUser.value, chatRoomUid)?.addOnCompleteListener { addGroupResult ->
                    if(!addGroupResult.isSuccessful) {
                        val failMessage = context.getString(R.string.chat_load_fail)
                        showSnackbar(failMessage)
                    }
                    //local user is ready and up to date
                    EventBus.getDefault().post(LoadingFinishedEvent)
                }
            }
        }
    }

    /**
     * Queries the [ChatRoom] (including [Message]es) and its member [User]s from firestore (including [localUser]).
     * While this is ongoing, the [loading] state will be set to true.
     */
    private fun getChatRoomAndMembers(chatRoomUid: String) {
        val failMessage = context.getString(R.string.chat_load_fail)
        //get chat room
        val chatRoomTask = chatRoomRepository.getChatRoom(chatRoomUid)
        chatRoomTask.addOnCompleteListener { chatRoomResult ->
            if(chatRoomResult.isSuccessful && chatRoomResult.result != null) {
                //save chat room, to get UID-s for messages and users
                 _chatRoom.value = chatRoomResult.result!!.toObject(ChatRoom::class.java)!!
            } else {
                EventBus.getDefault().post(LoadingFinishedEvent)
                //failed to get chat room, can't continue
                showSnackbar(failMessage)
            }
        }
        //continue with getting the users
        chatRoomTask.continueWith { previousTask ->
            if(previousTask.isSuccessful) {
                //only continue if the chat room query was successful
                //get the USERS who are in this chat room
                val usersTask = userRepository.getUsersByUid(chatRoom.value.chatRoomUsers)
                usersTask.addOnCompleteListener { usersResult ->
                    if(usersResult.isSuccessful && usersResult.result != null) {
                        //save users to state
                        _users.value = usersResult.result!!.toObjects(User::class.java)
                    } else {
                        //failed, cannot continue
                        EventBus.getDefault().post(LoadingFinishedEvent)
                        showSnackbar(failMessage)
                    }
                }
                //continue with getting the messages
                usersTask.continueWith { _previousTask ->
                    if(_previousTask.isSuccessful) {
                        //continue only if the user query succeeded
                        val messagesTask = chatRoomRepository.getMessagesOfChatRoom(chatRoomUid)
                        messagesTask.addOnCompleteListener { messagesResult ->
                            EventBus.getDefault().post(LoadingFinishedEvent)
                            if(messagesResult.isSuccessful && messagesResult.result != null) {
                                //messages, users and chat room is downloaded
                                _messages.value = messagesResult.result!!.toObjects(Message::class.java)
                            } else {
                                //failed to get message objects
                                showSnackbar(failMessage)
                            }
                        }
                    }
                }
            }
        }
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

/**
 * Sent when a loading process finishes.
 */
@Keep
object LoadingFinishedEvent