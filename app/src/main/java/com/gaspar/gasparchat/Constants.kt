package com.gaspar.gasparchat

import androidx.annotation.Keep

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

    const val CHAT_ROOM = "chatRoom"

    const val CHAT_ROOM_UID = "chatRoomUid"

    //home screen sub navigation

    const val HOME_CHATS = "home_chats"

    const val HOME_FRIENDS = "home_contacts"

    const val HOME_BLOCKED = "home_blocked"

    const val HOME_GROUPS = "home_groups"
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

object GroupLimits {

    const val MAX_MEMBERS = 8

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

object MessageValues {

    const val MAX_LENGTH = 600

}

object FirestoreConstants {

    // --------------------- USER collection. -----------------------------------

    const val USER_COLLECTION = "users"

    const val USER_UID = "uid"

    const val USER_DISPLAY_NAME = "displayName"

    const val USER_FRIENDS = "friends"

    const val USER_BLOCKS = "blockedUsers"

    const val USER_MESSAGE_TOKEN = "messageToken"

    // --------------------- CHAT ROOM collection. -----------------------------------

    const val CHAT_ROOM_COLLECTION = "chatRooms"

    const val CHAT_ROOM_UID = "chatUid"

    const val CHAT_ROOM_NAME = "chatRoomName"

    const val CHAT_ROOM_USERS = "chatRoomUsers"

    const val CHAT_ROOM_MESSAGES = "chatRoomMessages"

    const val CHAT_ROOM_GROUP = "group"

    const val CHAT_ROOM_ADMIN = "admin"

    const val CHAT_ROOM_LAST_MESSAGE_TIME = "lastMessageTime"

    const val CHAT_ROOM_LAST_MESSAGE_TEXT = "lastMessageText"

    // --------------------- MESSAGE SUB-COLLECTION collection. -----------------------------------

    const val MESSAGE_COLLECTION = "messages"

    const val MESSAGE_UID = "messageUid"

    const val MESSAGE_TEXT = "messageText"

    const val MESSAGE_TIME = "sendTime"

    const val MESSAGE_SENDER_UID = "senderUid"

    const val MESSAGE_SENDER_NAME = "senderName"

    const val MESSAGE_DELETED = "deleted"
}

object PictureConstants {

    const val PROFILE_PICTURE_SIZE = 75

    const val PROFILE_PICTURES_FOLDER = "profilePictures"

    /**
     * A percentage, which defines the compress amount, and thus the quality of the compressed
     * picture. The higher this is, the better the quality will be.
     */
    const val PICTURE_COMPRESS_RATIO = 100

    const val CACHED_PROFILE_PICTURE_TABLE = "cached_profile_pictures"

    const val CACHED_PROFILE_PICTURE_ID = "user_id"

    const val CACHED_PICTURE_VALIDATE_TIMESTAMP = "validate_timestamp"

    const val CACHED_PICTURE_IMAGE_DATA = "image_data"

    /**
     * The maximum mount of images (of a category, for example profile pictures) that will be cached.
     * Replacement strategy is used is a new image is attempted to be cached over this limit.
     */
    const val MAX_CACHED_PICTURES = 10
}

/**
 * Message type that is fired when a component signals that the block list has updated.
 */
@Keep
object BlocklistChangedEvent

/**
 * Message type that is fired when a component signals that the contacts list has updated.
 */
@Keep
object FriendsChangedEvent

/**
 * Message type that is sent when a component changes the current users groups.
 */
@Keep
object GroupsChangedEvent

/**
 * Send when the first message is sent in a chat room.
 */
@Keep
object ChatStartedEvent

/**
 * Message type that is fired when a component signals that a new chat room should be loaded.
 */
@Keep
class ChatRoomChangedEvent(val chatRoomId: String)