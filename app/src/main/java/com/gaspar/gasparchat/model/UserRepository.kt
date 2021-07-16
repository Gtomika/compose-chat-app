package com.gaspar.gasparchat.model

import android.util.Log
import androidx.annotation.Nullable
import com.gaspar.gasparchat.FirestoreConstants
import com.gaspar.gasparchat.TAG
import com.gaspar.gasparchat.viewmodel.VoidMethod
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import java.util.*
import javax.inject.Inject

/**
 * Singleton class that manages [User] operations. Note that some user operations are performed by
 * [com.google.firebase.auth.FirebaseAuth] and [com.google.firebase.auth.FirebaseUser]. This class only manages
 * the user document in Firestore.
 */
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
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
     * Called when a user registered to the application. Creates their data in the users collection
     * @param firebaseUser The [FirebaseUser] created after registration. will be converted into [User].
     * @return Async [Task], can be used to add callbacks.
     */
    fun addUser(firebaseUser: FirebaseUser): Task<Void> {
        val user = firebaseUserToUser(firebaseUser)
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
     * Updates [User] display name in the database.
     * @param firebaseUser Firebase user object. It should already have the updated display name.
     * @return Async [Task], can be used to add callbacks.
     */
    fun updateUserDisplayName(firebaseUser: FirebaseUser): Task<Void> {
        val user = firebaseUserToUser(firebaseUser)
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .set(user, SetOptions.merge())
    }

    /**
     * Adds a contact to a user. Nothing will happen if the contact is already added to this user.
     * @param user The user who will gain a new contact.
     * @param contactUserUid The user who will be added as a contact.
     * @return Async [Task], can be used to add callbacks, or null if this contact was added.
     */
    @Nullable
    fun addUserContact(user: User, contactUserUid: String): Task<Void>? {
        if(user.contacts.contains(contactUserUid)) {
            Log.d(TAG, "User already had this contact, doing nothing and returning null...")
            return null
        }
        user.contacts = user.contacts + contactUserUid
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .set(user)
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
        user.blockedUsers = user.blockedUsers + blockUserId
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .set(user)
    }

    /**
     * Removes a [User] from another users block list.
     * @param user Whose block list will be shortened.
     * @param blockUserId Who will be removed from the block list.
     * @return Async [Task] or null if the user was not even on the blocklist.
     */
    fun removeUserBlock(user: User, blockUserId: String): Task<Void>? {
        if(!user.blockedUsers.contains(blockUserId)) {
            Log.d(TAG, "User did not have the selected user blocked, doing nothing...")
            return null
        }
        user.blockedUsers = user.blockedUsers - blockUserId
        return firestore
            .collection(FirestoreConstants.USER_COLLECTION)
            .document(user.uid)
            .set(user)
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
fun isContactOf(user: User, otherUser: User): Boolean {
    for (contactUid in user.contacts) {
        if(contactUid == otherUser.uid) {
            return true
        }
    }
    return false
}

/**
 * Utility method to check if a user is blocked by another one.
 * @param user The block list of this user will be checked.
 * @param otherUser This user will be checked if they are blocked.
 * @return True only if [otherUser] is blocked by [user].
 */
fun isBlockedBy(user: User, otherUser: User): Boolean {
    for(blockedUid in user.blockedUsers) {
        if(blockedUid == otherUser.uid) {
            return true
        }
    }
    return false
}