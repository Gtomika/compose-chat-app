package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

/**
 * Modifies the [ChatRoom] collection in firestore.
 */
class ChatRoomRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Creates a group chat room in firestore CHAT ROOMS collection, with no messages.
     * @param chatRoomName The name of the chat room.
     * @param userUidList List of user uid-s to be included in this chat room.
     * @return Async [Task] to subscribe to.
     */
    fun createGroupChatRoom(chatRoomName: String, userUidList: List<String>): Task<Void> {
        val chatRoom = ChatRoom( //chat room uid is generated
            chatRoomName = chatRoomName,
            chatRoomUsers = userUidList
            //message sub collection is not created yet, only at first message
        )
        //add to firestore
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoom.chatUid)
            .set(chatRoom)
    }

    /**
     * Creates a private, one-to-one chat room between two users, with no messages. It will get an auto
     * generated name, name is not important for one-to-one chat rooms.
     * @param user1 The first user.
     * @param user2 The second user.
     * @return Async [Task] to subscribe to.
     */
    fun createOneToOneChatRoom(user1: User, user2: User): Task<Void> {
        val name = user1.uid + "-" + user2.uid
        val chatRoom = ChatRoom( //chat room uid is generated
            chatRoomName =  name,
            chatRoomUsers = listOf(user1.uid, user2.uid)
            //message sub collection is not created yet, only at first message
        )
        //add to firestore
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoom.chatUid)
            .set(chatRoom)
    }

    /**
     * Adds a message to the selected chat rooms message sub collection. If the sub collection does not
     * exist, it will be created.
     * @param chatRoomUid Uid of the chat room to which the message will be added to.
     * @param message The message object.
     * @return Async [Task] to subscribe to.
     */
    fun addMessageToChatRoom(chatRoomUid: String, message: Message): Task<Void> {
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .collection(FirestoreConstants.CHAT_ROOM_MESSAGES)
            .document(message.messageUid)
            .set(message)
    }

    /**
     * Removes a message from the selected chat rooms message sub collection.
     * @param chatRoomUid Uid of the chat room to which the message will be removed from.
     * @param messageUid Uid of the message to be removed.
     * @return Async [Task] to subscribe to.
     */
    fun deleteMessageFromChatRoom(chatRoomUid: String, messageUid: String): Task<Void>{
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .collection(FirestoreConstants.CHAT_ROOM_MESSAGES)
            .document(messageUid)
            .delete()
    }
}