package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.firebase.firestore.PropertyName

/**
 * A chat room is a set of users, usually 2, who can send each other messages. Each room has an id, name and
 * a list of [User] ids (the users in the chat), and a sub collection, which stores [Message]s that were
 * sent in this chat room.
 */
data class ChatRoom(

    /**
     * Unique identifier of the chat room. This is the document key in firestore collection
     */
    @PropertyName(FirestoreConstants.CHAT_ROOM_UID)
    var chatUid: String = createUid(),

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
    var chatRoomUsers: List<String> = listOf()

    //MESSAGES sub collection is created when the first message is sent.
)