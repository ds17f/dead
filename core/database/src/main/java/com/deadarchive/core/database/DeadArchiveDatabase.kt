package com.deadarchive.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [
        ConcertEntity::class,
        DownloadEntity::class,
        FavoriteEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DeadArchiveDatabase : RoomDatabase() {
    abstract fun concertDao(): ConcertDao
    abstract fun downloadDao(): DownloadDao
    abstract fun favoriteDao(): FavoriteDao
    
}