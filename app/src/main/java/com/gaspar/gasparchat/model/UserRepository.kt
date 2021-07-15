package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.gaspar.gasparchat.viewmodel.VoidMethod
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
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
    private val firestore: FirebaseFirestore
) {

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