package com.gaspar.gasparchat.model

import android.content.Context
import android.util.Log
import androidx.annotation.Nullable
import com.gaspar.gasparchat.*
import com.gaspar.gasparchat.viewmodel.VoidMethod
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

/**
 * Singleton class that manages [User] operations. Note that some user operations are performed by
 * [com.google.firebase.auth.FirebaseAuth] and [com.google.firebase.auth.FirebaseUser]. This class only manages
 * the user document in Firestore.
 */
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {

    /**
     * Finds the [User] object of the currently logged in user. Assumes there is a logged in user.
     * @return Async [Task], can be used to add callbacks.
     */
    fun getCurrentUser(): Task<QuerySnapshot> {
        val uid = firebaseAuth.currentUser!!.uid
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .whereEqualTo(FirestoreConstants.USER_UID, uid)
            .get()
    }

    /**
     * Called when a user registered to the application. Creates their data in the users collection. The
     * FCM message token of this device will be bound to this users account.
     * @param firebaseUser The [FirebaseUser] created after registration. will be converted into [User].
     * @return Async [Task], can be used to add callbacks.
     */
    fun addUser(firebaseUser: FirebaseUser): Task<Void> {
        val user = firebaseUserToUser(firebaseUser)

        //get message token
        val prefs = context.getSharedPreferences(GASPAR_CHAT_PREFERENCES, Context.MODE_PRIVATE)
        user.messageToken = prefs.getString(MESSAGE_TOKEN_PREFERENCE, TOKEN_NOT_FOUND) ?: TOKEN_NOT_FOUND
        Log.d(TAG, "Creating user, message token retrieved from preferences: ${user.messageToken}")

        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .set(user)
        //the sub-collection "chatRooms" is not created yet, as this user has no chats yet.
    }

    /**
     * Removes user data from firestore, including messages. Due to the implementation this does not return a task
     * to subscribe to (because there are multiple tasks). Rather pass callback as parameter.
     * @param user The user whose data is going to be deleted.
     * @param onDeleted Callback to be invoked when the user is deleted.
     */
    fun deleteUser(user: User, onDeleted: VoidMethod) {
        //delete the user document
        firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .delete().addOnCompleteListener { result ->
                if(result.isSuccessful) {
                    onDeleted.invoke()
                }
            }
        //TODO: deleted other user related data, such as messages and admin-ed groups
    }

    /**
     * Updates the [User]s FCM message token stored in firestore, with the current device's message
     * token. This is used on 2 occasions:
     *  - When a user logs in: This will make sure he gets FCM messages on the device he logged in.
     *  - When [GasparChatMessageService.onNewToken] is fired and a user is logged in. It will update their
     *      token in the database.
     * @param userUid UID if the user who will have his message token updated.
     * @return Async [Task].
     */
    fun updateUserMessageToken(userUid: String): Task<Void> {
        //get device message token
        val prefs = context.getSharedPreferences(GASPAR_CHAT_PREFERENCES, Context.MODE_PRIVATE)
        val deviceMessageToken = prefs.getString(MESSAGE_TOKEN_PREFERENCE, TOKEN_NOT_FOUND) ?: TOKEN_NOT_FOUND
        Log.d(TAG, "Updating message token, retrieved from preferences: $deviceMessageToken")
        //update
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(userUid)
            .update(FirestoreConstants.USER_MESSAGE_TOKEN, deviceMessageToken)
    }

    /**
     * Updates [User] display name in the database.
     * @param userUid UID of the user whose name will be updated.
     * @param displayName The new display name.
     * @return Async [Task], can be used to add callbacks.
     */
    fun updateUserDisplayName(userUid: String, displayName: String): Task<Void> {
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(userUid)
            .update(FirestoreConstants.USER_DISPLAY_NAME, displayName)
    }

    /**
     * Adds a contact to a user. Nothing will happen if the contact is already added to this user.
     * @param user The user who will gain a new contact.
     * @param contactUserUid The user who will be added as a contact.
     * @return Async [Task], can be used to add callbacks, or null if this contact was added.
     */
    @Nullable
    fun addUserFriend(user: User, contactUserUid: String): Task<Void>? {
        if(user.friends.contains(contactUserUid)) {
            Log.d(TAG, "User already had this contact, doing nothing and returning null...")
            return null
        }
        val updatedContacts = user.friends + contactUserUid
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .update(FirestoreConstants.USER_FRIENDS, updatedContacts)
    }

    /**
     * Adds a user as blocked to another users blocklist. Does nothing if [blockUserId] is already blocked.
     * @param user The user whose block list will be expanded.
     * @param blockUserId The uid of the user who will be blocked by [user].
     * @return Async [Task] or null if [blockUserId] was already blocked by [user].
     */
    fun addUserBlock(user: User, blockUserId: String): Task<Void>? {
        if(user.blockedUsers.contains(blockUserId)) {
            Log.d(TAG, "User already had the selected user blocked, doing nothing and returning null...")
            return null
        }
        val newBlocks = user.blockedUsers + blockUserId
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .update(FirestoreConstants.USER_BLOCKS, newBlocks)
    }

    /**
     * Adds a list of other users to a users block list. Only adds those who are not already blocked.
     * @param user The user whose block list will be expanded.
     * @param blockUserIds The list of UIDs of the users who will be blocked by [user].
     * @return Async [Task] or null if all of [blockUserIds] was already blocked by [user] and there is nothing to be done.
     */
    fun addUserBlocks(user: User, blockUserIds: List<String>): Task<Void>? {
        val notBlockedUsers = mutableListOf<String>()
        for(blockUserId in blockUserIds) {
           if(!user.blockedUsers.contains(blockUserId)) {
               //this user is not blocked, not they will be
               notBlockedUsers.add(blockUserId)
           }
        }
        return if(notBlockedUsers.isNotEmpty()) {
            //there are users who must be blocked
            val newBlocks = user.blockedUsers + notBlockedUsers.toList()
            return firestore
                .collection(FirestoreConstants.USER_COLLECTION)
                .document(user.uid)
                .update(FirestoreConstants.USER_BLOCKS, newBlocks)
        } else {
            Log.d(TAG, "All of the given users were already blocked, doing nothing...")
            null //no users present who are not blocked yet
        }
    }

    /**
     * Removes a [User] from another users block list.
     * @param user Whose block list will be shortened.
     * @param unblockUserId Who will be removed from the block list.
     * @return Async [Task] or null if the user was not even on the blocklist.
     */
    fun removeUserBlock(user: User, unblockUserId: String): Task<Void>? {
        if(!user.blockedUsers.contains(unblockUserId)) {
            Log.d(TAG, "User did not have the selected user blocked, doing nothing...")
            return null
        }
        val newBlocks = user.blockedUsers - unblockUserId
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .update(FirestoreConstants.USER_BLOCKS, newBlocks)
    }

    /**
     * Removes a list of [User]s from another users block list.
     * @param user Whose block list will be shortened.
     * @param unblockUserIds List of who will be removed from the block list.
     * @return Async [Task] or null if none of [unblockUserIds] was even blocked.
     */
    fun removeUserBlocks(user: User, unblockUserIds: List<String>): Task<Void>? {
        val blockedUsers = mutableListOf<String>()
        for(unblockUserId in unblockUserIds) {
            if(user.blockedUsers.contains(unblockUserId)) {
                //this user is actually blocked, can unblock them
                blockedUsers.add(unblockUserId)
            }
        }
        //now for those actually blocked, unblock them
        return if(blockedUsers.isNotEmpty()) {
            val newBlocks = user.blockedUsers - blockedUsers.toList()
            firestore
                .collection(FirestoreConstants.USER_COLLECTION)
                .document(user.uid)
                .update(FirestoreConstants.USER_BLOCKS, newBlocks)
        } else {
            //nobody needs to be unblocked
            Log.d(TAG, "None of the users were even blocked, doing nothing...")
            null
        }
    }

    /**
     * Queries the user documents.
     * @return [Task] that the caller can add callbacks to.
     */
    fun getUsers(): Task<QuerySnapshot> {
        //for large collections, this is bad, not scalable. but this app doesn't have large user base
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .get()
    }

    /**
     * Finds [User] objects from firestore.
     * @param userUidList The uid-s of the users.
     * @return Async [Task].
     */
    fun getUsersByUid(userUidList: List<String>): Task<QuerySnapshot> {
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .whereIn(FirestoreConstants.USER_UID, userUidList)
            .get()
    }

    /**
     * Converts [FirebaseUser] object to [User] object.
     * @param firebaseUser The firebase user.
     * @return The [User] object.
     */
    fun firebaseUserToUser(firebaseUser: FirebaseUser): User {
        return User(uid = firebaseUser.uid, displayName = firebaseUser.displayName!!)
    }
}

/**
 * Creates a random unique identifier string.
 * @return The uid.
 */
fun createUid(): String {
    return UUID.randomUUID().toString().replace("-","")
}

/**
 * Utility method to check if a user is a contact of another one.
 * @param user The contacts of this user will be checked.
 * @param otherUser This user will be checked if they are a contact.
 * @return True only if [otherUser] is a contact of [user].
 */
fun isFriendOf(user: User, otherUser: User): Boolean {
    for (contactUid in user.friends) {
        if(contactUid == otherUser.uid) {
            return true
        }
    }
    return false
}

/**
 * Utility method to check if a user is blocked by another one.
 * @param user The block list of this user will be checked.
 * @param otherUserUid This user will be checked if they are blocked.
 * @return True only if [otherUserUid] is blocked by [user].
 */
fun isBlockedBy(user: User, otherUserUid: String): Boolean {
    for(blockedUid in user.blockedUsers) {
        if(blockedUid == otherUserUid) {
            return true
        }
    }
    return false
}