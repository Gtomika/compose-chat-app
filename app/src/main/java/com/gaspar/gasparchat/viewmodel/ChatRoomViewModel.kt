package com.gaspar.gasparchat.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.model.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
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
    val snackbarDispatcher: SnackbarDispatcher,
    private val navigationDispatcher: NavigationDispatcher,
    private val pictureRepository: PictureRepository,
    private val firebaseAuth: FirebaseAuth
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
     * Image displayed in the top app bar. For private chats, this is the other users image,
     * for groups it is the group image. If this is null, then nothing is displayed.
     */
    private val _chatRoomImage = MutableStateFlow<Bitmap?>(null)
    val chatRoomImage: StateFlow<Bitmap?> = _chatRoomImage

    /**
     * Messages of the current chat room. This is more then just the message UIDs in the [ChatRoom]
     * objects, these are the [Message] objects.
     */
    private val _messages = MutableStateFlow(listOf<Message>())
    val messages: StateFlow<List<Message>> = _messages

    /**
     * [User]s in this chat room including the local user. The local user is also stored in [localUser].
     */
    private val _users = MutableStateFlow(listOf<User>())
    val users: StateFlow<List<User>> = _users

    /**
     * The [DisplayUser]s in the chat room.
     */
    private val _displayUsers = MutableStateFlow(listOf<DisplayUser>())
    val displayUsers: StateFlow<List<DisplayUser>> = _displayUsers

    /**
     * The [User] in the chat room who is currently logged in, in other words the one using the app.
     */
    private val _localUser = MutableStateFlow<User>(User())
    val localUser: StateFlow<User> = _localUser

    /**
     * The [DisplayUser] object of the [localUser]. Can be used to fetch local users image and name fast.
     */
    private val _localDisplayUser = MutableStateFlow<DisplayUser?>(null)
    val localDisplayUser: StateFlow<DisplayUser?> = _localDisplayUser

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

    /**
     * Can be used to control the message lazy column.
     */
    var lazyColumnState: LazyListState? = null

    /**
     * Some operations require composable scope.
     */
    var composableCoroutineScope: CoroutineScope? = null

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
        //set loading process amount: number of separate async tasks that must finish
        loadingProcessAmount = 2
        //indicate loading (it will disappear once loading process amount resets to 0)
        _loading.value = true
        //load all data about the chat room (async)
        getChatRoomAndMembers(event.chatRoomId)
        //navigate to chat room destination
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.CHAT_ROOM)
        }
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
                if(user.uid != localUser.value.uid && isBlockedBy(localUser.value, user.uid)) {
                    //found somebody who is not the local user and is blocked by the local user.
                    _blockedMembersPresent.value = true
                    break
                }
            }
            //get chat room top bar title
            _title.value = getChatRoomTitle()
            //scroll to the bottom (NEEDS composable scope)
            if(lazyColumnState != null) {
                composableCoroutineScope?.launch {
                    lazyColumnState?.animateScrollToItem(messages.value.size)
                }
            }
            //hide loading
            _loading.value = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageReceived(event: MessageReceivedEvent) {
        Log.d(TAG, "Message received event arrived for chat room ${event.chatRoomUid}")
        //only reload when chat room is VISIBLE and this chat room received message event.
        if(displaying && event.chatRoomUid == chatRoom.value.chatUid) {
            Log.d(TAG, "Displayed Chat Room received new message, reloading...")
            reloadMessages(event.chatRoomUid)
        } else {
            val id = MessageType.getIdForMessageType(type = event.messageType, chatRoomUid = event.chatRoomUid)
            //a new message arrived, but view model is not showing, or it is for another chat room: PUSH notification
            buildNotification(
                context = context,
                title = event.title,
                text = event.text,
                notificationId = id,
                chatRoomUid = event.chatRoomUid
            )
        }
    }

    /**
     * Gets the local user from the list of obtained [DisplayUser] objects.
     */
    private fun findLocalDisplayUser() {
        val localUserUid = firebaseAuth.currentUser!!.uid
        _localDisplayUser.value = findDisplayUserForUid(localUserUid)
    }

    /**
     * Gets the local user from the list of obtained [User] objects.
     */
    private fun findLocalUser() {
        val localUserUid = firebaseAuth.currentUser!!.uid
        _localUser.value = findUserForUid(localUserUid)
    }

    /**
     * From the [chatRoom] and [displayUsers] this function assigns [chatRoomImage].
     */
    private fun findChatRoomImage() {
        if(chatRoom.value.group) {
            //TODO: group images are not implemented yet
            _chatRoomImage.value = null
        } else {
            //get other users uid
            val otherUserUid = if(localUser.value.uid == chatRoom.value.chatRoomUsers[0]) {
                chatRoom.value.chatRoomUsers[1]
            } else {
                chatRoom.value.chatRoomUsers[0]
            }
            val otherDisplayUser = findDisplayUserForUid(otherUserUid)
            _chatRoomImage.value = otherDisplayUser.profilePicture
        }
    }

    /**
     * Queries the [ChatRoom] (including [Message]es) and its member [User]s from firestore (including [localUser]).
     * After this, the images of the users will be obtained.
     */
    private fun getChatRoomAndMembers(chatRoomUid: String) {
        Log.d(TAG, "Getting Chat Room Object, User objects and Message objects, chat room Uid: $chatRoomUid")
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
                snackbarDispatcher.createOnlyMessageSnackbar(failMessage)
                snackbarDispatcher.showSnackbar()
            }
        }
        //continue with getting the users AND the messages (these can happen at the same time)
        chatRoomTask.continueWith { previousTask ->
            if(previousTask.isSuccessful) {
                Log.d(TAG, "Continue with getting list of User objects...")
                //only continue if the chat room query was successful
                //get the USERS who are in this chat room
                val usersTask = userRepository.getUsersByUid(chatRoom.value.chatRoomUsers)
                usersTask.addOnCompleteListener { usersResult ->
                    if(usersResult.isSuccessful && usersResult.result != null) {
                        Log.d(TAG, "Received User objects participating in this chat room, converting to display users...")
                        //save users, and start async conversion to display users
                        _users.value = usersResult.result!!.toObjects(User::class.java)
                        findLocalUser()
                        createDisplayUsers(
                            pictureRepository = pictureRepository,
                            users = users.value,
                            onCompletion = { displayUsers: List<DisplayUser> ->
                                _displayUsers.value = displayUsers
                                findLocalDisplayUser()
                                findChatRoomImage()
                                Log.d(TAG, "Received DisplayUser objects.")
                                EventBus.getDefault().post(LoadingFinishedEvent)
                            }
                        )
                    } else {
                        //failed, cannot continue
                        EventBus.getDefault().post(LoadingFinishedEvent)
                        snackbarDispatcher.createOnlyMessageSnackbar(failMessage)
                        snackbarDispatcher.showSnackbar()
                    }
                }
            }
        }
        chatRoomTask.continueWith { previousTask ->
            if(previousTask.isSuccessful) {
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
                        snackbarDispatcher.createOnlyMessageSnackbar(failMessage)
                        snackbarDispatcher.showSnackbar()
                    }
                    EventBus.getDefault().post(LoadingFinishedEvent)
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
                //scroll to the bottom (NEEDS composable scope)
                composableCoroutineScope?.launch {
                    lazyColumnState?.animateScrollToItem(messages.value.size)
                }
            } else {
                val message = context.getString(R.string.chat_load_fail)
                snackbarDispatcher.createOnlyMessageSnackbar(message)
                snackbarDispatcher.showSnackbar()
            }
        }
    }

    /**
     * @return True only if there are exactly 2 participants.
     */
    fun isOneToOneChat(): Boolean {
        return !chatRoom.value.group
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
        return if(isOneToOneChat() && users.value.size == 2) {
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
                    //recompose messages to hide the blocked user(s) messages
                    _messages.value = messages.value.toList()
                } else {
                    val message = context.getString(R.string.chat_operation_failed)
                    snackbarDispatcher.createOnlyMessageSnackbar(message)
                    snackbarDispatcher.showSnackbar()
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
                    //recompose messages to show the unblocked user(s) messages
                    _messages.value = messages.value.toList()
                } else {
                    val message = context.getString(R.string.chat_operation_failed)
                    snackbarDispatcher.createOnlyMessageSnackbar(message)
                    snackbarDispatcher.showSnackbar()
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

    /**
     * Called when the message at [position] has been deleted. This won't actually remove the message, just
     * hide it's text and display a "deleted" message.
     */
    fun onMessageDeleted(position: Int) {
        chatRoomRepository.deleteMessageFromChatRoom(
            chatRoomUid = chatRoom.value.chatUid,
            messageUid = messages.value[position].messageUid
        ).addOnCompleteListener { deleteResult ->
            if(deleteResult.isSuccessful) {
                //update messages
                val updatedMessages = mutableListOf<Message>()
                updatedMessages.addAll(messages.value)
                updatedMessages[position] = updatedMessages[position].copy(deleted = true)
                _messages.value = updatedMessages.toList()
            } else {
                val message = context.getString(R.string.chat_message_delete_failed)
                snackbarDispatcher.createOnlyMessageSnackbar(message)
                snackbarDispatcher.showSnackbar()
            }
        }
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
        //check if this is the first message
        val wasFirstMessage = messages.value.isEmpty()
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
            ?.addOnCompleteListener { messageResult ->
                if(messageResult.isSuccessful) {
                    //if the first message was sent, refresh chats
                    if(wasFirstMessage) {
                        EventBus.getDefault().post(ChatStartedEvent)
                    }
                } else {
                    val error = context.getString(R.string.chat_message_send_fail)
                    snackbarDispatcher.createOnlyMessageSnackbar(error)
                    snackbarDispatcher.showSnackbar()
                }
            }
        //scroll to the bottom (NEEDS composable scope)
        composableCoroutineScope?.launch {
            lazyColumnState?.animateScrollToItem(messages.value.size)
        }
    }

    fun findDisplayUserForUid(userUid: String): DisplayUser {
        for(user in displayUsers.value) {
            if(user.uid == userUid) {
                return user
            }
        }
        return displayUsers.value[0] //should not get here
    }

    private fun findUserForUid(userUid: String): User {
        for(user in users.value) {
            if(user.uid == userUid) {
                return user
            }
        }
        return users.value[0] //should not get here
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
 * @param messageType Determines what triggered the message (notification). One of [MessageType] constants.
 * @param title From the FCM message, a notification title.
 * @param text From the FCM message, a notification text.
 */
@Keep
class MessageReceivedEvent(val chatRoomUid: String, val messageType: String, val title: String, val text: String)