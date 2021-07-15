package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

/**
 * Represents a message inside a [ChatRoom]. A message has an id, text, send time, and the id of the
 * [User] who sent it.
 */
data class Message(

    /**
     * Unique identifier of the message.
     */
    @PropertyName(FirestoreConstants.MESSAGE_UID)
    val messageUid: String = createUid(),

    /**
     * Text contents of the message.
     */
    @PropertyName(FirestoreConstants.MESSAGE_TEXT)
    val messageText: String = "",

    /**
     * Time when the message was sent. This is populated by firestore.
     */
    @PropertyName(FirestoreConstants.MESSAGE_TIME)
    @ServerTimestamp
    val messageTime: Date? = null,

    /**
     * Uid of the [User] who sent the message
     */
    @PropertyName(FirestoreConstants.MESSAGE_SENDER_UID)
    val senderUid: String = ""
)