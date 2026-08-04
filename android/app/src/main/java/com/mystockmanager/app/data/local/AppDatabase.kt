package com.mystockmanager.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mystockmanager.app.data.local.dao.*
import com.mystockmanager.app.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        ItemEntity::class,
        StorageEntity::class,
        ShopEntity::class,
        ShoppingEntity::class,
        PreferenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun userDao(): UserDao
    abstract fun storageDao(): StorageDao
    abstract fun shopDao(): ShopDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun preferenceDao(): PreferenceDao
}
