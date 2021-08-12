package com.gaspar.gasparchat.model.room

import androidx.room.*
import com.gaspar.gasparchat.PictureConstants

/**
 * Defines operation in the [CachedProfilePicture] table.
 */
@Dao
interface CachedProfilePictureDao {

    /**
     * Insert (cache) a profile picture. If this user already has a cached picture, that will be replaced.
     * Don't use this directly, use [cacheProfilePicture], which takes replacement strategy into account as well.
     * @param cachedPicture The new profile picture.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCachedProfilePicture(cachedPicture: CachedProfilePicture)

    /**
     * Gets the cached profile picture of a user.
     * @param userUid THe user whose picture will be queried.
     * @return The cached picture or null if the user has no cached profile picture.
     */
    @Query("SELECT * FROM cached_profile_pictures WHERE user_id == :userUid LIMIT 1")
    fun getCachedProfilePicture(userUid: String): CachedProfilePicture?

    /**
     * Counts the amount of cached profile pictures to be compared again [PictureConstants.MAX_CACHED_PICTURES].
     */
    @Query("SELECT COUNT(*) FROM cached_profile_pictures")
    fun getCachedProfilePictureCount(): Int

    /**
     * Returns the [CachedProfilePicture] which was least recently validated. This will be replaced if
     * there are too many. This method assumes the [CachedProfilePicture] table isn't empty.
     */
    @Query("SELECT * FROM cached_profile_pictures ORDER BY validate_timestamp ASC LIMIT 1")
    fun getLeastRecentlyValidatedPicture() : CachedProfilePicture

    /**
     * Removes a cache profile picture from the database.
     */
    @Delete
    fun deleteCachedProfilePicture(cachedPicture: CachedProfilePicture)
}