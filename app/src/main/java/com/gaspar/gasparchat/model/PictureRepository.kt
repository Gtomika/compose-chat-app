package com.gaspar.gasparchat.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.gaspar.gasparchat.PictureConstants
import com.gaspar.gasparchat.TAG
import com.gaspar.gasparchat.model.room.CachedProfilePicture
import com.gaspar.gasparchat.model.room.GasparChatDatabase
import com.gaspar.gasparchat.model.room.cacheProfilePicture
import com.gaspar.gasparchat.postToUiThread
import com.gaspar.gasparchat.viewmodel.VoidMethod
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Contains method that can upload and download images using firebase storage.
 */
class PictureRepository @Inject constructor(
    private val firebaseStorage: FirebaseStorage,
    private val database: GasparChatDatabase
) {

    /**
     * Uploads a new profile picture for the selected user. On success, it will also cache this image
     * in the local database, in the [CachedProfilePicture] table
     * @param userUid The user ID.
     * @param picture The picture.
     * @param onSuccess Callback to be invoked on successful update.
     * @param onError Callback to be invoked on failed update.
     */
    fun updateProfilePicture(
        userUid: String,
        picture: Bitmap,
        onSuccess: VoidMethod,
        onError: VoidMethod
    ) {
        //get bytes
        val data = bitmapToByteArray(picture)
        //get reference: profile picture folder / user uid . jpg (this will set creating time millis)
        val pictureRef = firebaseStorage.reference.child("${PictureConstants.PROFILE_PICTURES_FOLDER}/$userUid.jpg")
        //upload bytes and return task
        pictureRef.putBytes(data).addOnSuccessListener {
            //cache it in local database
            CoroutineScope(Dispatchers.Default).launch {
                cacheProfilePicture(userUid, data, database)
            }
            //invoke callback
            postToUiThread { onSuccess() }
        }.addOnFailureListener { exception ->
            onError.invoke()
            Log.d(TAG, "Failed to update profile picture.")
            exception.printStackTrace()
        }
    }

    /**
     * Retrieves a users profile picture from either firebase storage, or the [CachedProfilePicture]s
     * table. The [Bitmap] of the picture will be returned in the callback ([onPictureObtained]), or null if this user has no profile picture or
     * due to some error the picture could not be retrieved. If the profile picture was downloaded from storage,
     * it will be cached.
     * @param userUid The user whose profile picture will be returned.
     * @param onPictureObtained Called when the image was successfully obtained.
     * @param onError Called when an error happens.
     */
     fun getAndCacheProfilePicture(
        userUid: String,
        onPictureObtained: (Bitmap?) -> Unit,
        onError: VoidMethod
    ) {
        /*
        Compare the "validate timestamp" in the cached images, and the "create timestamp" in firebase storage.
        - First check if the user has an uploaded profile picture in firebase storage (by downloading metadata).
        - If not, "return" null bitmap.
        - If there is a profile picture, read "create timestamp" from metadata.
        - Read the users cached profile picture from local database. If there is none, download the one
        from firebase storage and return that (ALSO CACHE IT).
        - If there is a cached picture, compare its "validate timestamp" with the "create timestamp" from firestore.
        - If the "validate timestamp" is more recent, return cached image. If the "create timestamp" is more recent,
        download the new image from firestore and return that (ALSO CACHE IT)
         */
        val pictureRef = firebaseStorage.reference.child("${PictureConstants.PROFILE_PICTURES_FOLDER}/$userUid.jpg")
        pictureRef.metadata.addOnSuccessListener { metadata ->
            val createTimestamp = metadata.creationTimeMillis
            //is there a cached picture for this user?
            CoroutineScope(Dispatchers.Default).launch {
                val cachedPicture = database.getCachedProfilePictureDao().getCachedProfilePicture(userUid)
                if(cachedPicture != null) {
                    //there is both a cached picture and a picture in firebase storage
                    if(cachedPicture.validatedTimestamp >= createTimestamp) {
                        //the cached picture is valid, use that
                        Log.d(TAG, "Using cached picture for user $userUid")
                        val picture = byteArrayToBitmap(cachedPicture.imageData)
                        postToUiThread { onPictureObtained(picture) }
                    } else {
                        Log.d(TAG, "Outdated cached picture for user $userUid, re-downloading.")
                        //the cached picture is invalid, download newer one and cache that
                        downloadAndCacheProfilePicture(userUid, pictureRef, onPictureObtained, onError)
                    }
                } else {
                    Log.d(TAG, "User $userUid has no cached picture. Downloading it.")
                    //there is no cached picture, must download the one from firebase storage
                    downloadAndCacheProfilePicture(userUid, pictureRef, onPictureObtained, onError)
                }
            }
        }.addOnFailureListener {
            Log.d(TAG, "Failed to download metadata, returning no bitmap (reason can be error or not existing image).")
            onPictureObtained(null)
            //onError is not called, this is most likely due to the image not existing, which is expected behaviour
        }
    }

    /**
     * See [getAndCacheProfilePicture], but this one always downloads from firebase storage. After download, it
     * caches the image. Should be called from the background.
     */
    private fun downloadAndCacheProfilePicture(
        userUid: String,
        pictureRef: StorageReference,
        onPictureObtained: (Bitmap?) -> Unit,
        onError: VoidMethod
    ) {
        val maxSize: Long = 1024 * 1024
        pictureRef.getBytes(maxSize).addOnSuccessListener { data ->
            //data downloaded, convert it to bitmap
            val picture = byteArrayToBitmap(data)
            //cache it to local database
            CoroutineScope(Dispatchers.Default).launch {
                cacheProfilePicture(userUid, data, database)
            }
            //post this picture to the callback
            onPictureObtained(picture)
        }.addOnFailureListener { exception ->
            Log.d(TAG, "Failed to download picture from firebase storage.")
            exception.printStackTrace()
            onError()
        }
    }

    /**
     * Removes the users profile picture from both the firebase storage, and the cached images.
     * @param userUid The uid of the user whose picture will be deleted.
     * @param onPictureDeleted If the delete was successful.
     * @param onError If the delete failed.
     */
    fun deleteProfilePicture(
        userUid: String,
        onPictureDeleted: VoidMethod,
        onError: VoidMethod
    ) {
        val pictureRef = firebaseStorage.reference.child("${PictureConstants.PROFILE_PICTURES_FOLDER}/$userUid.jpg")
        pictureRef.delete().addOnSuccessListener {
            Log.d(TAG, "Profile picture of user $userUid was deleted from storage.")
            //remove from cache as well if needed
            CoroutineScope(Dispatchers.Default).launch {
                val cachedPicture = database.getCachedProfilePictureDao().getCachedProfilePicture(userUid)
                if(cachedPicture != null) {
                    database.getCachedProfilePictureDao().deleteCachedProfilePicture(cachedPicture)
                }
            }
            onPictureDeleted()
        }.addOnFailureListener { exception ->
            Log.d(TAG, "Failed to delete profile picture of user $userUid from storage: ${exception.javaClass.simpleName}")
            onError()
        }
    }

    /**
     * Converts a [Bitmap] to a [ByteArray] that can be uploaded to firebase storage. The JPEG
     * file format is used. The [PictureConstants.PICTURE_COMPRESS_RATIO] determines the compress amount.
     * @param bitmap The bitmap.
     * @return The byte array of the bitmap.
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, PictureConstants.PICTURE_COMPRESS_RATIO, baos)
            return baos.toByteArray()
        }
    }

    /**
     * Converts a [ByteArray] back to [Bitmap]. Using the JPEG image format.
     * @param data The byte array.
     * @return The decoded bitmap, or null if the operation failed.
     */
    private fun byteArrayToBitmap(data: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }

}