package com.mystockmanager.app.di

import android.content.Context
import androidx.room.Room
import com.mystockmanager.app.data.local.AppDatabase
import com.mystockmanager.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mystockmanager_native.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()

    @Provides
    fun provideStorageDao(db: AppDatabase): StorageDao = db.storageDao()

    @Provides
    fun provideShopDao(db: AppDatabase): ShopDao = db.shopDao()

    @Provides
    fun provideShoppingDao(db: AppDatabase): ShoppingDao = db.shoppingDao()

    @Provides
    fun providePreferenceDao(db: AppDatabase): PreferenceDao = db.preferenceDao()
}
