package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.*
import javax.inject.Inject

/**
 * Modifies the [ChatRoom] collection in firestore.
 */
class ChatRoomRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Finds a [ChatRoom] from firestore. Can also be used to check if the given chat room exists.
     * @param chatRoomUid The uid of the chat room.
     * @return Async [Task].
     */
    fun getChatRoom(chatRoomUid: String): Task<DocumentSnapshot> {
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .get()
    }

    /**
     * Finds all chat room that a user is in, including groups and one-to-one conversations.
     * @param userUid The user whose chat rooms will be listed.
     * @return Async [Task].
     */
    fun getChatRoomsOfUser(userUid: String): Task<QuerySnapshot> {
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .whereArrayContains(FirestoreConstants.CHAT_ROOM_USERS, userUid)
            .get()
    }

    /**
     * Finds all chat room GROUPS of a user.
     * @param userUid The user whose chat rooms will be listed.
     * @return Async [Task].
     */
    fun getGroupsOfUser(userUid: String): Task<QuerySnapshot> {
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .whereArrayContains(FirestoreConstants.CHAT_ROOM_USERS, userUid)
            .whereEqualTo(FirestoreConstants.CHAT_ROOM_GROUP, true)
            .get()
    }

    /**
     * Finds all [Message] objects that are in a specific [ChatRoom]. Not ordered!
     * @param chatRoomUid The uid of the chat room.
     * @return Async [Task].
     */
    fun getMessagesOfChatRoom(chatRoomUid: String): Task<QuerySnapshot> {
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .collection(FirestoreConstants.CHAT_ROOM_MESSAGES)
            //.orderBy(FirestoreConstants.MESSAGE_TIME) --> NOT working, will return empty list
            .get()
    }

    /**
     * Creates a group chat room in firestore CHAT ROOMS collection, with no messages.
     * @param chatRoomName The name of the chat room.
     * @param userUidList List of user uid-s to be included in this chat room, including the admin.
     * @param adminUid UID of the admin [User].
     * @return Async [Task] to subscribe to.
     */
    fun createGroupChatRoom(chatRoomName: String, userUidList: List<String>, adminUid: String): Task<Void> {
        val chatRoom = ChatRoom(
            chatUid = UUID.randomUUID().toString(),
            chatRoomName = chatRoomName,
            chatRoomUsers = userUidList,
            group = true,
            admin = adminUid
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
     * @param userUid1 The first user UID.
     * @param userUid2 The second user UID.
     * @return Async [Task] to subscribe to.
     */
    fun createOneToOneChatRoom(userUid1: String, userUid2: String): Task<Void> {
        val chatRoom = ChatRoom(
            chatUid = generateChatUid(userUid1, userUid2),
            chatRoomName =  "OneOnOnChat", //name is not important here
            chatRoomUsers = listOf(userUid1, userUid2),
            group = false
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
    fun addMessageToChatRoom(chatRoomUid: String, message: Message): Task<MutableList<Task<*>>>? {
        //insert the message in the sub collection
        val messageAdderTask = firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .collection(FirestoreConstants.CHAT_ROOM_MESSAGES)
            .document(message.messageUid)
            .set(message)

        val newTimeText = hashMapOf<String, Any>(
            FirestoreConstants.CHAT_ROOM_LAST_MESSAGE_TIME to message.messageTime,
            FirestoreConstants.CHAT_ROOM_LAST_MESSAGE_TEXT to message.messageText
        )

        //update the last message time and text
        val chatRoomUpdaterTask = firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .update(newTimeText)

        return Tasks.whenAllSuccess(messageAdderTask, chatRoomUpdaterTask)
    }

    /**
     * "Deletes" a message from the selected chat rooms message sub collection. The message will only get
     * the deleted flag set to true, it won't actually be removed.
     * @param chatRoomUid Uid of the chat room.
     * @param messageUid Uid of the message to be deleted.
     * @return Async [Task] to subscribe to.
     */
    fun deleteMessageFromChatRoom(chatRoomUid: String, messageUid: String): Task<Void>{
        return firestore
            .collection(FirestoreConstants.CHAT_ROOM_COLLECTION)
            .document(chatRoomUid)
            .collection(FirestoreConstants.CHAT_ROOM_MESSAGES)
            .document(messageUid)
            .update(FirestoreConstants.MESSAGE_DELETED, true)
    }

    /**
     * Generates a chat room UID from the UIDs of the two [User]s. This is not that long of a string,
     * so no shortening is applied. The order of the 2 users do not matter.
     * @param userUid1 The UID of the first user.
     * @param userUid2 The UID of the second user.
     * @return A UID for the chat room between these 2 users.
     */
    fun generateChatUid(userUid1: String, userUid2: String): String {
        return if (userUid1 < userUid2) {
            userUid1 + userUid2
        } else {
            userUid2 + userUid1
        }
    }
}