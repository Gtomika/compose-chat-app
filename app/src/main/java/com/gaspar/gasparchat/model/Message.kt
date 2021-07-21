package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.firebase.firestore.PropertyName
import java.util.*

/**
 * Represents a message inside a [ChatRoom]. A message has an id, text, send time, and the id of the
 * [User] who sent it.
 */
data class Message(

    /**
     * Unique identifier of the message. Auto generated.
     */
    @PropertyName(FirestoreConstants.MESSAGE_UID)
    var messageUid: String = createUid(),

    /**
     * Text contents of the message.
     */
    @PropertyName(FirestoreConstants.MESSAGE_TEXT)
    var messageText: String = "",

    /**
     * Time when the message was sent. Auto generated.
     */
    @PropertyName(FirestoreConstants.MESSAGE_TIME)
    var messageTime: Date = Date(),

    /**
     * Uid of the [User] who sent the message
     */
    @PropertyName(FirestoreConstants.MESSAGE_SENDER_UID)
    var senderUid: String = "",

    /**
     * [User] display name of the sender.
     */
    @PropertyName(FirestoreConstants.MESSAGE_SENDER_NAME)
    var senderName: String = "",

    /**
     * Flag that stores if the message was deleted. Deleted messages remain in the chat room,
     * but don't show their text anymore.
     */
    @PropertyName(FirestoreConstants.MESSAGE_DELETED)
    var deleted: Boolean = false
)