package com.mystockmanager.app.data.repository

import com.mystockmanager.app.core.CryptoManager
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.dao.ShopDao
import com.mystockmanager.app.data.local.dao.StorageDao
import com.mystockmanager.app.data.local.dao.UserDao
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.local.entities.UserEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val storageDao: StorageDao,
    private val shopDao: ShopDao,
    private val cryptoManager: CryptoManager,
    private val sessionManager: SessionManager
) {
    suspend fun register(email: String, password: String): String {
        val normalized = email.trim().lowercase()
        if (userDao.getUserByEmail(normalized) != null) throw Exception("EMAIL_EXISTS")

        val salt = cryptoManager.generateSalt()
        val passwordHash = cryptoManager.hashPassword(password, salt)

        val recoveryCode = cryptoManager.generateRecoveryCode()
        val recoverySalt = cryptoManager.generateSalt()
        val recoveryHash = cryptoManager.hashPassword(recoveryCode.replace("-", ""), recoverySalt)

        val syncGuid = UUID.randomUUID().toString()

        val user = UserEntity(
            email = normalized,
            passwordHash = passwordHash,
            salt = salt,
            recoveryHash = recoveryHash,
            recoverySalt = recoverySalt,
            syncChannelGuid = syncGuid,
            createdAt = Instant.now().toString()
        )

        val userId = userDao.insertUser(user)
        
        // Create default storages
        createDefaultStorages(userId.toString())
        createDefaultShops(userId.toString())

        sessionManager.setSession(userId, normalized, syncGuid)

        return recoveryCode
    }

    private suspend fun createDefaultStorages(userId: String) {
        val defaults = listOf(
            StorageEntity(UUID.randomUUID().toString(), userId, "Frigo", "🧊", "cold", true, Instant.now().toString()),
            StorageEntity(UUID.randomUUID().toString(), userId, "Congélateur", "❄️", "frozen", false, Instant.now().toString()),
            StorageEntity(UUID.randomUUID().toString(), userId, "Armoire", "🚪", "dry", false, Instant.now().toString())
        )
        defaults.forEach { storageDao.insertStorage(it) }
    }

    private suspend fun createDefaultShops(userId: String) {
        val defaults = listOf("Carrefour", "Colruyt", "Lidl")
        defaults.forEach { name ->
            shopDao.insertShop(ShopEntity(UUID.randomUUID().toString(), userId, name, Instant.now().toString(), Instant.now().toString()))
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        val normalized = email.trim().lowercase()
        val user = userDao.getUserByEmail(normalized) ?: return false

        val hash = cryptoManager.hashPassword(password, user.salt)
        if (hash == user.passwordHash) {
            sessionManager.setSession(user.id, user.email, user.syncChannelGuid)
            return true
        }
        return false
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
}
