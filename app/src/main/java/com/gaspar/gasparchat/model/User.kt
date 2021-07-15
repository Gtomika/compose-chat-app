package com.gaspar.gasparchat.model

import com.gaspar.gasparchat.FirestoreConstants
import com.google.firebase.firestore.PropertyName

/**
 * Represents public information about a registered user, hence it does not contain email and other
 * stuff that is present in a firebase user object. Each user can be part of [ChatRoom]s, the uid-s of
 * these chat rooms are stored here as well.
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
     * [ChatRoom] uid-s that this user is part of.
     */
    @PropertyName(FirestoreConstants.USER_CHAT_ROOMS)
    var chatRooms: List<String> = listOf()

    //TODO: maybe image URL, other stuff
)