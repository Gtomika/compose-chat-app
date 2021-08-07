package com.gaspar.gasparchat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gaspar.gasparchat.model.UserRepository
import com.gaspar.gasparchat.viewmodel.MessageReceivedEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


/**
 * The key for the messaging token in the shared preferences.
 */
const val MESSAGE_TOKEN_PREFERENCE = "message_token_pref"

/**
 * This will be the value returned for the [MESSAGE_TOKEN_PREFERENCE] shared preference value, if
 * for some reason the preferences don't contain [MESSAGE_TOKEN_PREFERENCE].
 */
const val TOKEN_NOT_FOUND = "token_not_found"

class GasparChatMessageService: FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     *
     * The token is saved to the shared preferences with key [MESSAGE_TOKEN_PREFERENCE].
     */
    override fun onNewToken(token: String) {
        val firebaseAuth = FirebaseAuth.getInstance()
        Log.d(TAG, "FCM Refreshed token: $token")
        //save it to shared preferences
        val prefs = getSharedPreferences(GASPAR_CHAT_PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit().putString(MESSAGE_TOKEN_PREFERENCE, token).apply()
        //if there is a logged in user, update his message token in firestore
        if(firebaseAuth.currentUser != null) {
            Log.d(TAG, "User is logged in, updating their message token in firestore...")
            userRepository.updateUserMessageToken(firebaseAuth.currentUser!!.uid).addOnCompleteListener { tokenUpdateResult ->
                if(tokenUpdateResult.isSuccessful) {
                    Log.d(TAG, "FCM message token updated in firestore for user ${firebaseAuth.currentUser!!.displayName}.")
                } else {
                    Log.d(TAG, "Token update failed for user ${firebaseAuth.currentUser!!.displayName}. User won't get notifications on this device!")
                }
            }
        }
    }

    /**
     * Called when message is received.
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "FCM message arrived from: ${remoteMessage.from}")
        // Check if message contains a notification payload.
        if(
            remoteMessage.notification != null &&
            remoteMessage.notification!!.title != null &&
            remoteMessage.notification!!.body != null &&
            remoteMessage.data.containsKey("chatRoomUid") &&
            remoteMessage.data.containsKey("messageType")
        ) {
            //message contains all necessary field
            Log.d(TAG, "This FCM message is a notification message with chat room UID. Notification Body: ${remoteMessage.notification!!.body}")
            Log.d(TAG, "This FCM message has data: ${remoteMessage.data}")
            //publish an event, received by the chat room view model
            val event = MessageReceivedEvent(
                chatRoomUid = remoteMessage.data["chatRoomUid"]!!,
                messageType = remoteMessage.data["messageType"]!!,
                title = remoteMessage.notification!!.title!!,
                text = remoteMessage.notification!!.body!!,
            )
            //the view model will decide what to do with it
            EventBus.getDefault().post(event)
            //in case of new group create, refresh group list
            if(remoteMessage.data["messageType"] == MessageType.NEW_GROUP) {
                EventBus.getDefault().post(GroupsChangedEvent)
            }
        }
    }
}

/**
 * The notification intent will contain the chat room id with this key.
 */
const val INTENT_CHAT_ROOM_ID = "intent_chat_room_id"

/**
 * Builds a notification for a chat room. If this chat room already displays a notification, the old
 * one will be replaced. This notification directs the user to the said chat room.
 * @param context Application context.
 * @param title Notification title.
 * @param text Notification text.
 * @param notificationId Notification ID.
 */
@SuppressLint("UnspecifiedImmutableFlag")
fun buildNotification(context: Context, title: String, text: String, notificationId: Int, chatRoomUid: String) {
    Log.d(TAG, "Displaying a notification with title $title and body $text!")

    //this intent will be delivered to the activity's onNewIntent
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(INTENT_CHAT_ROOM_ID, chatRoomUid)
    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    Log.d(TAG, "This notification will redirect to chat room $chatRoomUid")

    val color = ContextCompat.getColor(context, R.color.purple_500)

    val builder = NotificationCompat.Builder(context, context.getString(R.string.gaspar_chat_notification_channel_id))
        .setSmallIcon(R.drawable.gaspar_chat_notification_icon)
        .setColorized(true)
        .setColor(color)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setDefaults(Notification.DEFAULT_SOUND)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)

    with(NotificationManagerCompat.from(context)) {
        //notification ID generated from chat room UID
        notify(notificationId, builder.build())
    }
}

/**
 * Incoming messages contain these types, the app can differentiate using these.
 */
object MessageType {

    const val NEW_MESSAGE = "new_message"

    const val NEW_GROUP = "new_group"

    /**
     * Converts incoming message types and chat room UIDs into notification IDs.
     */
    fun getIdForMessageType(type: String, chatRoomUid: String): Int {
        return when (type) {
            NEW_MESSAGE -> {
                chatRoomUid.hashCode()
            }
            NEW_GROUP -> {
                chatRoomUid.hashCode() + 1 //this notification will be separate from new message notification
            }
            else -> {
                chatRoomUid.hashCode()
            }
        }
    }
}