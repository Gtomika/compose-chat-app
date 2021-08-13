package com.gaspar.gasparchat.model

import android.graphics.Bitmap

/**
 * Contains all the information needed to display a [User] object in a "user card". Some of its
 * information is from the [User] object, other must be obtained separately, for example the profile
 * picture.
 */
data class DisplayUser(

    /**
     * Users UID. This is not displayed, but is useful when somebody interacts with the user card
     * and events need to be fired.
     */
    val uid: String,

    /**
     * The user appears with this name.
     */
    val displayName: String,

    /**
     * The profile picture of the user. This can be null, which indicates that the user has the
     * default profile picture.
     */
    val profilePicture: Bitmap?
)

/**
 * This function can convert [User] objects into [DisplayUser] by obtaining the missing information
 * such as profile pictures. This process happens in an async way.
 * @param pictureRepository Used to query for images.
 * @param users The list of [User]s who are to be converted to [DisplayUser]s
 * @param onCompletion Callback to be invoked when the operation completes. The list of converted [DisplayUser]s
 * will be passed as the parameter (will have the same size as [users]).
 */
fun createDisplayUsers(
    pictureRepository: PictureRepository,
    users: List<User>,
    onCompletion: (List<DisplayUser>) -> Unit
) {
    //this list is going to get filled up
    val displayUsers = mutableListOf<DisplayUser>()
    //save profile pictures in a map, so if there is a repeating user, the picture can be reused
    val userPicturePairs = hashMapOf<String, Bitmap?>()
    //launch async picture getter task for each user
    for(user in users) {
        //check the hash map for this users image, possibly avoids another query if this user is repeating
        if(userPicturePairs.containsKey(user.uid)) {
            val displayUser = DisplayUser(
                uid = user.uid,
                displayName = user.displayName,
                profilePicture = userPicturePairs[user.uid] //may be null, that means default picture
            )
            displayUsers.add(displayUser)
            //was this the last user? if yes invoke the completion callback
            if(displayUsers.size >= users.size) {
                onCompletion(displayUsers)
            }
            continue
        }
        //the image of the user was not found in the hash map: start query
        pictureRepository.getAndCacheProfilePicture(
            userUid = user.uid,
            onPictureObtained = { picture: Bitmap? ->
                //build display user object
                val displayUser = DisplayUser(
                    uid = user.uid,
                    displayName = user.displayName,
                    profilePicture = picture //this may be null, that means default picture
                )
                //add this user to the list
                displayUsers.add(displayUser)
                //save the image to the hash map
                userPicturePairs[user.uid] = picture
                //was this the last user? if yes invoke the completion callback
                if(displayUsers.size >= users.size) {
                    onCompletion(displayUsers)
                }
            },
            onError = {
                //this users picture could not be obtained. it will be set to null, and will behave
                //as if they have the default profile picture
                val displayUser = DisplayUser(
                    uid = user.uid,
                    displayName = user.displayName,
                    profilePicture = null
                )
                displayUsers.add(displayUser)
                //add null to the hash map to indicate this user has default picture
                userPicturePairs[user.uid] = null
                //was this the last user? if yes invoke the completion callback
                if(displayUsers.size >= users.size) {
                    onCompletion(displayUsers)
                }
            }
        )
    }
}