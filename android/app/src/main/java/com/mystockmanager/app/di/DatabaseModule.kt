package com.mystockmanager.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    /**
     * Ajout des colonnes liées au compte distant (api.noshi.be) — additive,
     * préserve les données existantes. Indispensable&nbsp;: la destruction
     * automatique (fallbackToDestructiveMigration) viderait la table users
     * AVANT que AuthRepository.migrateIfNeeded() ait pu lire le hash local pour
     * rattacher le compte au serveur, rendant les comptes existants irrécupérables.
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN compteId TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN deviceToken TEXT")
            db.execSQL("ALTER TABLE users ADD COLUMN emailVerifie INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** Nouvelle table pour l'historique des dates de péremption effacées (voir ExpiryHistoryEntity) — additive. */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS expiry_history (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    itemId TEXT NOT NULL,
                    itemName TEXT NOT NULL,
                    previousExpiryDate TEXT NOT NULL,
                    clearedAt TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mystockmanager_native.db"
        ).addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
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

    @Provides
    fun provideUnitDao(db: AppDatabase): UnitDao = db.unitDao()

    @Provides
    fun provideDomicileDao(db: AppDatabase): DomicileDao = db.domicileDao()

    @Provides
    fun provideExpiryHistoryDao(db: AppDatabase): ExpiryHistoryDao = db.expiryHistoryDao()
}
