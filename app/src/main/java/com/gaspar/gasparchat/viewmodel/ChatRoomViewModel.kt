package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
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
    private val snackbarDispatcher: SnackbarDispatcher,
    private val navigationDispatcher: NavigationDispatcher
): ViewModel() {

    /**
     * Stores if the chat room view model is currently displaying.
     */
    var displaying = false

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
     * Users in this chat room including the local user. The local user is also stored in [localUser]. These are more then just
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
     * Stores if ANY OTHER chat room members are blocked for [localUser].
     */
    private val _blockedMembersPresent = MutableStateFlow(false)
    val blockedMembersPresent: StateFlow<Boolean> = _blockedMembersPresent

    /**
     * Title of the Top app bar.
     */
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    /**
     * Stores what is typed into the mesasge sender field.
     */
    private val _typedMessage = MutableStateFlow(InputField())
    val typedMessage: StateFlow<InputField> = _typedMessage

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadingProcessFinished(event: LoadingFinishedEvent) {
        loadingProcessAmount--
        Log.d(TAG, "A loading process finished, $loadingProcessAmount remains.")
        if(loadingProcessAmount == 0) {
            //if we are here both local user and other users are loaded
            Log.d(TAG, "All loading processes finished, showing content...")
            //determine if block is present
            for(user in users.value) {
                if(user.uid != localUser.value.uid && isBlockedBy(localUser.value, user)) {
                    //found somebody who is not the local user and is blocked by the local user.
                    _blockedMembersPresent.value = true
                    break
                }
            }
            //get chat room top bar title
            _title.value = getChatRoomTitle()
            //hide loading
            _loading.value = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageReceived(event: MessageReceivedEvent) {
        //only reload when chat room is VISIBLE and this chat room received message event.
        if(displaying && event.chatRoomUid == chatRoom.value.chatUid) {
            Log.d(TAG, "Displayed Chat Room received new message, reloading...")
            reloadMessages(event.chatRoomUid)
        } else {
            //a new message arrived, but view model is not showing, or it is for another chat room: PUSH notification
            buildNotification(
                context = context,
                title = event.title,
                text = event.text,
                chatRoomUid = event.chatRoomUid
            )
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
                if(!isUserInChatRoom(localUser.value, chatRoomUid)) {
                    //user is not in this chat room yet
                    userRepository.addUserChatRoom(localUser.value, chatRoomUid)?.addOnCompleteListener { addGroupResult ->
                        //local user is ready and up to date
                        EventBus.getDefault().post(LoadingFinishedEvent)
                        if(!addGroupResult.isSuccessful) {
                            val failMessage = context.getString(R.string.chat_load_fail)
                            showSnackbar(failMessage)
                        }
                    }
                } else {
                    //user is already in the chat room, nothing more to do
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
        Log.d(TAG, "Getting Chat Room Object, User objects and Message objects...")
        val failMessage = context.getString(R.string.chat_load_fail)
        //get chat room
        val chatRoomTask = chatRoomRepository.getChatRoom(chatRoomUid)
        chatRoomTask.addOnCompleteListener { chatRoomResult ->
            if(chatRoomResult.isSuccessful && chatRoomResult.result != null) {
                Log.d(TAG, "Received Chat Room Object!")
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
                Log.d(TAG, "Continue with getting list of User objects...")
                //only continue if the chat room query was successful
                //get the USERS who are in this chat room
                val usersTask = userRepository.getUsersByUid(chatRoom.value.chatRoomUsers)
                usersTask.addOnCompleteListener { usersResult ->
                    if(usersResult.isSuccessful && usersResult.result != null) {
                        Log.d(TAG, "Received User objects participating in this chat room!")
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
                        Log.d(TAG, "Continue with getting list of Message objects...")
                        //continue only if the user query succeeded
                        val messagesTask = chatRoomRepository.getMessagesOfChatRoom(chatRoomUid)
                        messagesTask.addOnCompleteListener { messagesResult ->
                            if(messagesResult.isSuccessful && messagesResult.result != null) {
                                //messages, users and chat room is downloaded
                                val queriedMessages = messagesResult.result!!.toObjects(Message::class.java)
                                queriedMessages.sortBy { it.messageTime }
                                _messages.value = queriedMessages
                                Log.d(TAG, "Received Message objects that belong to this chat room, ${messages.value.size} in total.")
                                //all good, this loading chain is done
                            } else {
                                //failed to get message objects
                                showSnackbar(failMessage)
                            }
                            EventBus.getDefault().post(LoadingFinishedEvent)
                        }
                    }
                }
            }
        }
    }

    private fun reloadMessages(chatRoomUid: String) {
        chatRoomRepository.getMessagesOfChatRoom(chatRoomUid).addOnCompleteListener { reloadResult ->
            if(reloadResult.isSuccessful && reloadResult.result != null) {
                Log.d(TAG, "Reloaded messages in current displayed chat room!")
                val queriedMessages = reloadResult.result!!.toObjects(Message::class.java)
                queriedMessages.sortBy { it.messageTime }
                _messages.value = queriedMessages
            } else {
                val message = context.getString(R.string.chat_load_fail)
                showSnackbar(message)
            }
        }
    }

    /**
     * @return True only if there are exactly 2 participants.
     */
    fun isOneToOneChat(): Boolean {
        return users.value.size == 2
    }

    fun onBackClicked() {
        //mark no longer displaying
        displaying = false
        //navigate back
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigateUp()
        }
    }

    private fun getChatRoomTitle(): String {
        return if(isOneToOneChat()) {
            //one-to-one conversation
            if(localUser.value.displayName == users.value[0].displayName) users.value[1].displayName else users.value[0].displayName
        } else {
            //this is a group
            chatRoom.value.chatRoomName
        }
    }

    /**
     * @return A list of user UIDs of all chat room members, except [localUser]
     */
    private fun getNonLocalUserUidList(): List<String> {
        val nonLocalUserIds = mutableListOf<String>()
        for(user in users.value) {
            if(user.uid != localUser.value.uid) {
                nonLocalUserIds.add(user.uid)
            }
        }
        return nonLocalUserIds.toList()
    }

    /**
     * Called when the user clicks the block option from the drop down menu.
     */
    fun onDropDownBlockClicked() {
        //show loading
        _loading.value = true
        //select non local users
        val nonLocalUserIds = getNonLocalUserUidList()
        Log.d(TAG, "Blocking all other members in this chat room...")
        userRepository.addUserBlocks(localUser.value, nonLocalUserIds)
            ?.addOnCompleteListener { blockResult ->
                _loading.value = false
                if (blockResult.isSuccessful) {
                    val prevBlockList = mutableListOf<String>() //copy of current blocklist, useful for undo
                    prevBlockList.addAll(localUser.value.blockedUsers)
                    //update local user: expand block list
                    val newBlockList = mutableListOf<String>()
                    newBlockList.addAll(prevBlockList)
                    for(newlyBlockedUid in nonLocalUserIds) {
                        if(!newBlockList.contains(newlyBlockedUid)) {
                            newBlockList.add(newlyBlockedUid)
                        }
                    }
                    _localUser.value = localUser.value.copy(blockedUsers = newBlockList.toList())
                    //update block status
                    _blockedMembersPresent.value = true
                    //show snackbar
                    val undoAction = {
                        _loading.value = true
                        //undo: unblock all those who were just blocked
                        userRepository.removeUserBlocks(localUser.value, nonLocalUserIds)
                            ?.addOnCompleteListener { undoResult ->
                                _loading.value = false
                                if(undoResult.isSuccessful) {
                                    //update block status
                                    _blockedMembersPresent.value = false
                                    //update local user: shrink block list
                                    _localUser.value = localUser.value.copy(blockedUsers = prevBlockList.toList())
                                } else {
                                    val undoMessage = context.getString(R.string.chat_operation_failed)
                                    showSnackbar(undoMessage)
                                }
                            }
                    }
                    showSnackbar(
                        message = context.getString(R.string.chat_block_success),
                        actionLabel = context.getString(R.string.undo),
                        onActionClicked = { undoAction.invoke() }
                    )
                } else {
                    val message = context.getString(R.string.chat_operation_failed)
                    showSnackbar(message)
                }
            }
    }

    /**
     * Called when the user clicks the Unblock option from the drop down menu.
     */
    fun onDropDownUnblockClicked() {
        //show loading
        _loading.value = true
        //select non local users
        val nonLocalUserIds = getNonLocalUserUidList()
        Log.d(TAG, "Unblocking all other members in this chat room...")
        userRepository.removeUserBlocks(localUser.value, nonLocalUserIds)
            ?.addOnCompleteListener { unblockResult ->
                _loading.value = false
                if(unblockResult.isSuccessful) {
                    //save previous block list, useful for undo
                    val prevBlockList = mutableListOf<String>()
                    prevBlockList.addAll(localUser.value.blockedUsers)
                    //update local user: shrink block list
                    val newBlockList = mutableListOf<String>()
                    newBlockList.addAll(prevBlockList)
                    for(newlyUnblockedUid in nonLocalUserIds) {
                        if(newBlockList.contains(newlyUnblockedUid)) {
                            newBlockList.remove(newlyUnblockedUid)
                        }
                    }
                    _localUser.value = localUser.value.copy(blockedUsers = newBlockList.toList())
                    //update block status
                    _blockedMembersPresent.value = false
                    //show snackbar with undo option
                    val undoAction = {
                        _loading.value = true
                        //undo: block all those who were just unblocked
                        userRepository.addUserBlocks(localUser.value, nonLocalUserIds)
                            ?.addOnCompleteListener { undoResult ->
                                _loading.value = false
                                if(undoResult.isSuccessful) {
                                    //update block status
                                    _blockedMembersPresent.value = true
                                    //update local user: expand block list
                                    _localUser.value = localUser.value.copy(blockedUsers = prevBlockList.toList())
                                } else {
                                    val undoMessage = context.getString(R.string.chat_operation_failed)
                                    showSnackbar(undoMessage)
                                }
                            }
                    }
                    showSnackbar(
                        message = context.getString(R.string.chat_block_success),
                        actionLabel = context.getString(R.string.undo),
                        onActionClicked = { undoAction.invoke() }
                    )
                } else {
                    val message = context.getString(R.string.chat_operation_failed)
                    showSnackbar(message)
                }
            }
    }

    /**
     * Called when the user selects another user in a group chat from the drop down menu to initiate
     * a private conversation with.
     * @param userUid UID of the user who was selected.
     */
    fun onDropDownInitiateChatClicked(userUid: String) {
        //get the chat room uid
        val chatRomUid = chatRoomRepository.generateChatUid(userUid1 = localUser.value.uid, userUid2 = userUid)
        //send event to load new chat room
        EventBus.getDefault().post(ChatRoomChangedEvent(chatRomUid))
    }

    fun onTypedMessageChanged(newMessage: String) {
        when {
            newMessage.length > MessageValues.MAX_LENGTH -> {
                val message = context.getString(R.string.chat_message_too_long, MessageValues.MAX_LENGTH, newMessage.length)
                _typedMessage.value = typedMessage.value.copy(input = newMessage, isError = true, errorMessage = message)
            }
            else -> {
                _typedMessage.value = typedMessage.value.copy(input = newMessage, isError = false)
            }
        }
    }

    /**
     * Called when the user clicks send message. The typed message is assumed to be valid.
     */
    fun onMessageSent() {
        //create message object, UID and timestamp are auto generated
        val message = Message(
            messageText = typedMessage.value.input,
            senderUid = localUser.value.uid,
            senderName = localUser.value.displayName
        )
        //update messages instantly, assuming success
        _messages.value = messages.value + message
        //clear typed message
        _typedMessage.value = typedMessage.value.copy(input = "", isError = false)
        //start async task that sends message to firestore
        chatRoomRepository.addMessageToChatRoom(chatRoomUid = chatRoom.value.chatUid, message = message)
            .addOnCompleteListener { messageResult ->
                if(!messageResult.isSuccessful) {
                    val error = context.getString(R.string.chat_message_send_fail)
                    showSnackbar(error)
                }
            }
    }

    fun findDisplayNameForUid(userUid: String): String {
        for(user in users.value) {
            if(user.uid == userUid) {
                return user.displayName
            }
        }
        return "Unknown"
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

/**
 * Sent when this chat room gets a new message.
 * @param chatRoomUid UID of the chat room which received message.
 * @param title From the FCM message, a notification title.
 * @param text From the FCM message, a notification text.
 */
@Keep
class MessageReceivedEvent(val chatRoomUid: String, val title: String, val text: String)