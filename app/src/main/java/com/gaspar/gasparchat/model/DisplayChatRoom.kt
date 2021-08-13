package com.gaspar.gasparchat.model

import android.graphics.Bitmap
import android.util.Log
import com.gaspar.gasparchat.TAG
import java.util.*

/**
 * Similar to [ChatRoom] but stores only the necessary information to display and operate a "chat room card". This
 * object is only LOCAL and stores how the chat room will be displayed on the current device. The same chat room
 * may be displayed differently on different devices.
 */
data class DisplayChatRoom(

    /**
     * The chat rooms UID, used if the user selects the card.
     */
    var chatUid: String = "",

    /**
     * This flag stores if the chat room is a group, or a one-to-one conversation.
     */
    var group: Boolean = false,

    /**
     * Name of the chat room, important for groups.
     */
    var chatRoomName: String = "",

    /**
     * The amount of users in the chat room.
     */
    var memberCount: Int = 0,

    /**
     * Display name of a [User] associated with this chat room. In case of private chats, this will be the non-local
     * other user, in case of groups it is the admin.
     */
    var displayUserName: String = "",

    /**
     * The [Date] of the last message, or null if no message was ever sent. This is user to order groups
     * based on activity.
     */
    var lastMessageTime: Date? = null,

    /**
    * The text of the last message, or null if there was no message sent. This is used to show a preview
    * of the chat rooms content.
    */
    var lastMessageText: String? = null,

    /**
     * The image of the chat room. In case of private chats, this is the picture of the other user. For
     * groups, this is the group picture. It can be null, in which case the default pictures will be used.
     */
    var chatRoomPicture: Bitmap? = null
)

/**
 * Creates a list of [DisplayChatRoom]s from the queried [ChatRoom] and [User] objects.
 * This process is async, because the images must be loaded.
 * @param chatRooms The [ChatRoom] objects.
 * @param users The [User] objects. Assumed to be the same size as [chatRooms]
 * @param onCompletion Callback that will receive the completed [DisplayChatRoom] list.
 * @param pictureRepository Performs the image queries.
 */
fun mergeChatRoomsAndUsers(
    chatRooms: List<ChatRoom>,
    users: List<User>,
    onCompletion: (List<DisplayChatRoom>) -> Unit,
    pictureRepository: PictureRepository
) {
    //display chat rooms are added to this list
    val displayChatRooms = mutableListOf<DisplayChatRoom>()
    if(chatRooms.size != users.size) {
        Log.d(TAG, "Error: different amount of chat rooms (${chatRooms.size}) and users (${users.size}).")
        onCompletion(displayChatRooms)
        return
    }
    for(i in chatRooms.indices) {
        if(chatRooms[i].group) {
            //this is a group
            val displayChatRoom = DisplayChatRoom(
                chatUid = chatRooms[i].chatUid,
                chatRoomName = chatRooms[i].chatRoomName,
                group = chatRooms[i].group,
                displayUserName = users[i].displayName,
                memberCount = chatRooms[i].chatRoomUsers.size,
                lastMessageText = chatRooms[i].lastMessageText,
                lastMessageTime = chatRooms[i].lastMessageTime,
                chatRoomPicture = null //this is not implemented yet
            )
            displayChatRooms.add(displayChatRoom)
            //TODO: implement group image, must get it here in an async way
            if(displayChatRooms.size >= chatRooms.size) {
                //This was the last chat room, call on completed callback
                onCompletion(displayChatRooms)
            }
        } else {
            val displayChatRoom = DisplayChatRoom(
                chatUid = chatRooms[i].chatUid,
                chatRoomName = chatRooms[i].chatRoomName,
                group = chatRooms[i].group,
                displayUserName = users[i].displayName,
                memberCount = chatRooms[i].chatRoomUsers.size,
                lastMessageText = chatRooms[i].lastMessageText,
                lastMessageTime = chatRooms[i].lastMessageTime,
                //picture will be set after the async task completes
            )
            //this is a private chat
            pictureRepository.getAndCacheProfilePicture(
                userUid = users[i].uid,
                onPictureObtained = { picture ->
                    displayChatRoom.chatRoomPicture = picture
                    displayChatRooms.add(displayChatRoom)
                    if(displayChatRooms.size >= chatRooms.size) {
                        //This was the last chat room, call on completed callback
                        onCompletion(displayChatRooms)
                    }
                },
                onError = {
                    //failed to get image for this chat, use default
                    displayChatRoom.chatRoomPicture = null
                    displayChatRooms.add(displayChatRoom)
                    if(displayChatRooms.size >= chatRooms.size) {
                        //This was the last chat room, call on completed callback
                        onCompletion(displayChatRooms)
                    }
                }
            )
        }
    }
}