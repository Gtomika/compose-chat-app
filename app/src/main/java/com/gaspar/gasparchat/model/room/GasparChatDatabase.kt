package com.gaspar.gasparchat.model.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * SQLite database of the app, managed by Room.
 */
@Database(version = 1, entities = [CachedProfilePicture::class])
abstract class GasparChatDatabase(): RoomDatabase() {

    /**
     * Get data access object of [CachedProfilePicture] table.
     */
    abstract fun getCachedProfilePictureDao(): CachedProfilePictureDao

}

/**
 * Name of the database.
 */
const val DATABASE_NAME = "gaspar_chat_database"