package com.gaspar.gasparchat

/**
 * Tag for logging.
 */
const val TAG = "GasparChat"

/**
 * Navigation destination constants.
 */
object NavDest {

    const val LOGIN = "login"

    const val REGISTER = "register"

    const val HOME = "home"

    const val PROFILE = "profile"

    const val SEARCH = "search"

    //home screen sub navigation

    const val HOME_CHATS = "home_chats"

    const val HOME_CONTACTS = "home_contacts"

    const val HOME_BLOCKED = "home_blocked"
}

/**
 * Limits of password length.
 */
object PasswordLimits {

    const val MIN = 6 //at least 6 is required by FIREBASE AUTH

    const val MAX = 30

}

object NameLimits {

    const val MIN = 3

    const val MAX = 30

}

/**
 * Limits of the length of text in the search bar.
 */
object SearchValues {

    const val MIN_LENGTH = NameLimits.MIN

    const val MAX_LENGTH = NameLimits.MAX

    /**
     * The number of milliseconds that passes after the user stopped typing, and the search begins.
     */
    const val SEARCH_DELAY: Long = 1000
}

object FirestoreConstants {

    // --------------------- USER collection. -----------------------------------

    const val USER_COLLECTION = "users"

    const val USER_UID = "uid"

    const val USER_DISPLAY_NAME = "displayName"

    const val USER_CHAT_ROOMS = "chatRooms"

    const val USER_CONTACTS = "contacts"

    const val USER_BLOCKS = "blockedUsers"

    // --------------------- CHAT ROOM collection. -----------------------------------

    const val CHAT_ROOM_COLLECTION = "chatRooms"

    const val CHAT_ROOM_UID = "chatUid"

    const val CHAT_ROOM_NAME = "chatRoomName"

    const val CHAT_ROOM_USERS = "chatRoomUsers"

    const val CHAT_ROOM_MESSAGES = "chatRoomMessages"

    // --------------------- MESSAGE SUB-COLLECTION collection. -----------------------------------

    const val MESSAGE_COLLECTION = "messages"

    const val MESSAGE_UID = "messageUid"

    const val MESSAGE_TEXT = "text"

    const val MESSAGE_TIME = "sendTime"

    const val MESSAGE_SENDER_UID = "senderUid"
}