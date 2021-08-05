package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.firebase.firestore.PropertyName

/**
 * Represents public information about a registered user, hence it does not contain email and other
 * stuff that is present in a firebase user object.
 */
data class User(

    /**
     * Unique identifier that comes from firebase authentication.
     */
    @PropertyName(FirestoreConstants.USER_UID)
    var uid: String = "",

    /**
     * Display name.
     */
    @PropertyName(FirestoreConstants.USER_DISPLAY_NAME)
    var displayName: String = "",

    /**
     * Friends ([User] uid-s) of this user.
     */
    @PropertyName(FirestoreConstants.USER_FRIENDS)
    var friends: List<String> = listOf(),

    /**
     * [User] uid-s of those users who have been blocked by this user.
     */
    @PropertyName(FirestoreConstants.USER_BLOCKS)
    var blockedUsers: List<String> = listOf(),

    /**
     * FCM message token of this user. Used to target the user (their device) with FCM messages.
     */
    @PropertyName(FirestoreConstants.USER_MESSAGE_TOKEN)
    var messageToken: String = ""
)