package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.firebase.firestore.PropertyName
import java.util.*

/**
 * A chat room is a set of users, usually 2, who can send each other messages. Each room has an id, name and
 * a list of [User] ids (the users in the chat), and a sub collection, which stores [Message]s that were
 * sent in this chat room.
 * <p>
 * Chat rooms can be private (one-to-one) or groups with more then 2 members. This is determined by the
 * [group] flag.
 */
data class ChatRoom(

    /**
     * Unique identifier of the chat room. This is the document key in firestore collection. For one
     * to one chat rooms, the UID is created from the 2 users, for groups it is random.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_UID)
    var chatUid: String = "",

    /**
     * Name of the chat room. In case of one-to-one chat rooms, this is created from the uid of the 2 users,
     * but each user will see the the other user's name instead. For chat rooms with more then 2 users (groups),
     * this is an actual name that all members will see.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_NAME)
    var chatRoomName: String = "",

    /**
     * List of [User] uid-s who are in this chat room.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_USERS)
    var chatRoomUsers: List<String> = listOf(),

    /**
     * This flag stores if the chat room is a group, or a one-to-one conversation.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_GROUP)
    var group: Boolean = false,

    /**
     * UID of the [User] that is the admin of the group, or null in case of one-to-one conversations,
     * as those have no admin. Only use this value if [group] is true.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_ADMIN)
    var admin: String? = null,

    /**
     * The [Date] of the last message, or null if no message was ever sent. This is user to order groups
     * based on activity.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_LAST_MESSAGE_TIME)
    var lastMessageTime: Date? = null,

    /**
     * The text of the last message, or null if there was no message sent. This is used to show a preview
     * of the chat rooms content.
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_LAST_MESSAGE_TEXT)
    var lastMessageText: String? = null

    //MESSAGES sub collection is created when the first message is sent.
)