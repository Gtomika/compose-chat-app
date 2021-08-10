package com.gaspar.gasparchat.model

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
    var lastMessageText: String? = null

)

/**
 * Creates a list of [DisplayChatRoom]s from the queried [ChatRoom] and [User] objects. The display names
 * of the users will be the [DisplayChatRoom]'s display user names.
 */
fun mergeChatRoomsAndUsers(chatRooms: List<ChatRoom>, users: List<User>): List<DisplayChatRoom> {
    val displayChatRooms = mutableListOf<DisplayChatRoom>()
    if(chatRooms.size != users.size) {
        Log.d(TAG, "Error: different amount of chat rooms (${chatRooms.size}) and users (${users.size}).")
        return displayChatRooms
    }
    for(i in 0..chatRooms.size-1) {
        val displayChatRoom = DisplayChatRoom(
            chatUid = chatRooms[i].chatUid,
            chatRoomName = chatRooms[i].chatRoomName,
            group = chatRooms[i].group,
            displayUserName = users[i].displayName,
            memberCount = chatRooms[i].chatRoomUsers.size,
            lastMessageText = chatRooms[i].lastMessageText,
            lastMessageTime = chatRooms[i].lastMessageTime
        )
        displayChatRooms.add(displayChatRoom)
    }
    return displayChatRooms
}