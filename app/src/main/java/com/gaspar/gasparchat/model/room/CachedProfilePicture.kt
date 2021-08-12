package com.gaspar.gasparchat.model.room

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.BLOB
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gaspar.gasparchat.PictureConstants
import com.gaspar.gasparchat.TAG

/**
 * Represents a profile image used by the application that is cached in Room database. Used to
 * avoid downloading the same image multiple times.
 */
@Entity(tableName = PictureConstants.CACHED_PROFILE_PICTURE_TABLE)
data class CachedProfilePicture(

    /**
     * The image's ID, same as the users UID.
     */
    @PrimaryKey
    @ColumnInfo(name = PictureConstants.CACHED_PROFILE_PICTURE_ID)
    val userUid: String,

    /**
     * Timestamp which stores the last time this image was validated (using the creation timestamp
     * of the actual image from firebase storage). If the firebase storage timestamp is more recent, that means
     * the image in firebase storage is newer, and the cached picture is outdated. If the validation timestamp
     * is more recent, that means the cached image is valid, and can be used to avoid a re-download.
     */
    @ColumnInfo(name = PictureConstants.CACHED_PICTURE_VALIDATE_TIMESTAMP)
    val validatedTimestamp: Long,

    /**
     * The bytes of the cached image, compressed as JPEG format.
     */
    @ColumnInfo(name = PictureConstants.CACHED_PICTURE_IMAGE_DATA, typeAffinity = BLOB)
    val imageData: ByteArray
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedProfilePicture

        if (userUid != other.userUid) return false
        if (validatedTimestamp != other.validatedTimestamp) return false
        if (!imageData.contentEquals(other.imageData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userUid.hashCode()
        result = 31 * result + validatedTimestamp.hashCode()
        result = 31 * result + imageData.contentHashCode()
        return result
    }
}

/**
 * Caches a profile picture, taking [PictureConstants.MAX_CACHED_PICTURES] into account. Should be
 * called from a coroutine.
 * @param userUid The user whose picture is cached.
 * @param pictureData The picture encoded into [ByteArray].
 * @param database Database object.
 */
@WorkerThread
fun cacheProfilePicture(userUid: String, pictureData: ByteArray, database: GasparChatDatabase) {
    //make new object
    val cachedImage = CachedProfilePicture(
        userUid = userUid,
        validatedTimestamp = System.currentTimeMillis(),
        imageData = pictureData
    )
    //check limit
    val cachedCount = database.getCachedProfilePictureDao().getCachedProfilePictureCount()
    if(cachedCount >= PictureConstants.MAX_CACHED_PICTURES) {
        //reached cache limit, use replacement strategy: where validate timestamp is the oldest
        Log.d(TAG, "Inserting new cached profile picture, REPLACING one other.")
        //get least recently used
        val toBeReplaced = database.getCachedProfilePictureDao().getLeastRecentlyValidatedPicture()
        //delete it
        database.getCachedProfilePictureDao().deleteCachedProfilePicture(toBeReplaced)
        //now add new one
        database.getCachedProfilePictureDao().insertCachedProfilePicture(cachedImage)
    } else {
        Log.d(TAG, "Inserting new cached profile picture, no replacement needed.")
        //there is still more spots for cached images, just insert.
        database.getCachedProfilePictureDao().insertCachedProfilePicture(cachedImage)
    }
}