package com.mystockmanager.app.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.mystockmanager.app.R
import com.mystockmanager.app.core.CryptoManager
import com.mystockmanager.app.core.SessionManager
import com.mystockmanager.app.data.local.dao.ShopDao
import com.mystockmanager.app.data.local.dao.StorageDao
import com.mystockmanager.app.data.local.dao.UserDao
import com.mystockmanager.app.data.local.entities.ShopEntity
import com.mystockmanager.app.data.local.entities.StorageEntity
import com.mystockmanager.app.data.local.entities.UserEntity
import com.mystockmanager.app.data.remote.AccountApi
import com.mystockmanager.app.data.remote.AccountApiException
import com.mystockmanager.app.data.remote.AccountAuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val storageDao: StorageDao,
    private val shopDao: ShopDao,
    private val cryptoManager: CryptoManager,
    private val sessionManager: SessionManager,
    private val accountApi: AccountApi
) {
    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    /**
     * Inscription — le compte est créé côté serveur (api.noshi.be) d'abord, c'est
     * lui qui fait foi. Une copie locale est conservée pour l'usage hors-ligne.
     */
    suspend fun register(email: String, password: String, firstName: String, lastName: String): String {
        val normalized = email.trim().lowercase()
        if (userDao.getUserByEmail(normalized) != null) throw Exception("EMAIL_EXISTS")

        val salt = cryptoManager.generateSalt()
        val passwordHash = cryptoManager.hashPassword(password, salt)

        val recoveryCode = cryptoManager.generateRecoveryCode()
        val recoverySalt = cryptoManager.generateSalt()
        val recoveryHash = cryptoManager.hashPassword(recoveryCode.replace("-", ""), recoverySalt)

        val syncGuid = UUID.randomUUID().toString()

        val remote = try {
            accountApi.register(normalized, password, nom = lastName, prenom = firstName, guid = syncGuid, deviceName = deviceName())
        } catch (e: AccountApiException) {
            if (e.httpStatus == 409) throw Exception("EMAIL_EXISTS")
            throw e
        }

        val user = UserEntity(
            email = normalized,
            firstName = firstName,
            lastName = lastName,
            passwordHash = passwordHash,
            salt = salt,
            recoveryHash = recoveryHash,
            recoverySalt = recoverySalt,
            syncChannelGuid = remote.guid ?: syncGuid,
            createdAt = Instant.now().toString(),
            compteId = remote.compteId,
            deviceToken = remote.token,
            emailVerifie = remote.emailVerifie
        )

        val userId = userDao.insertUser(user)

        // Comptes par défaut (uniquement pour une inscription réelle, pas pour un
        // appareil qui rejoint un compte existant via login — voir login()).
        createDefaultStorages(userId.toString())
        createDefaultShops(userId.toString())

        sessionManager.setSession(userId, normalized, user.syncChannelGuid, firstName, lastName)
        sessionManager.setDeviceToken(remote.token)

        return recoveryCode
    }

    private suspend fun createDefaultStorages(userId: String) {
        val now = Instant.now().toString()
        val defaults = listOf(
            StorageEntity(UUID.randomUUID().toString(), userId, null, context.getString(R.string.seed_storage_fridge), "🧊", "cold", true, now),
            StorageEntity(UUID.randomUUID().toString(), userId, null, context.getString(R.string.seed_storage_freezer), "❄️", "frozen", false, now),
            StorageEntity(UUID.randomUUID().toString(), userId, null, context.getString(R.string.seed_storage_cupboard), "🚪", "dry", false, now)
        )
        defaults.forEach { storageDao.insertStorage(it) }
    }

    private suspend fun createDefaultShops(userId: String) {
        val defaults = listOf("Carrefour", "Colruyt", "Lidl")
        defaults.forEach { name ->
            shopDao.insertShop(ShopEntity(UUID.randomUUID().toString(), userId, name, Instant.now().toString(), Instant.now().toString()))
        }
    }

    /**
     * Connexion — vérifiée côté serveur. Si aucun compte local ne correspond à cet
     * email, c'est un nouvel appareil qui rejoint un compte existant : on adopte le
     * foyer (guid) renvoyé par le serveur plutôt que d'en créer un nouveau.
     */
    suspend fun login(email: String, password: String): Boolean {
        val normalized = email.trim().lowercase()
        val localUser = userDao.getUserByEmail(normalized)

        val remote = try {
            accountApi.login(normalized, password, guid = localUser?.syncChannelGuid, deviceName = deviceName())
        } catch (e: AccountApiException) {
            if (e.httpStatus == 401) return false
            throw e
        }

        persistRemoteSession(normalized, password, remote)
        return true
    }

    /**
     * Réinitialise le mot de passe via le code de récupération à 20 caractères
     * affiché à l'inscription (§4.5 du cahier des charges — pas d'email, le code
     * EST le facteur de récupération). Un succès reconnecte directement l'appareil,
     * exactement comme /account/login.
     */
    suspend fun recoverPassword(email: String, recoveryCode: String, newPassword: String): Boolean {
        val normalized = email.trim().lowercase()
        val localUser = userDao.getUserByEmail(normalized)
        val cleanedCode = recoveryCode.replace("-", "").trim().uppercase()

        val remote = try {
            accountApi.recoverPassword(normalized, cleanedCode, newPassword, guid = localUser?.syncChannelGuid, deviceName = deviceName())
        } catch (e: AccountApiException) {
            if (e.httpStatus == 401) return false
            throw e
        }

        persistRemoteSession(normalized, newPassword, remote)
        return true
    }

    /**
     * Met à jour (ou crée) la copie locale de l'utilisateur à partir d'une réponse
     * serveur (login/recover), et recalcule le hash local avec le mot de passe
     * courant — nécessaire après une récupération, sans effet sinon (même hash).
     */
    private suspend fun persistRemoteSession(normalizedEmail: String, password: String, remote: AccountAuthResponse) {
        val localUser = userDao.getUserByEmail(normalizedEmail)

        val user = if (localUser != null) {
            localUser.copy(
                passwordHash = cryptoManager.hashPassword(password, localUser.salt),
                compteId = remote.compteId,
                deviceToken = remote.token,
                emailVerifie = remote.emailVerifie,
                syncChannelGuid = remote.guid ?: localUser.syncChannelGuid
            )
        } else {
            // Le serveur ne renvoie ni prénom ni nom : l'utilisateur les complètera
            // dans Préférences > Profil. Le stock/liste arrivera via la sync (guid).
            val newSalt = cryptoManager.generateSalt()
            UserEntity(
                email = normalizedEmail,
                passwordHash = cryptoManager.hashPassword(password, newSalt),
                salt = newSalt,
                recoveryHash = "",
                recoverySalt = "",
                syncChannelGuid = remote.guid ?: UUID.randomUUID().toString(),
                createdAt = Instant.now().toString(),
                compteId = remote.compteId,
                deviceToken = remote.token,
                emailVerifie = remote.emailVerifie
            )
        }

        val userId = if (localUser != null) {
            userDao.updateUser(user)
            user.id
        } else {
            userDao.insertUser(user)
        }

        sessionManager.setSession(userId, user.email, user.syncChannelGuid, user.firstName, user.lastName)
        sessionManager.setDeviceToken(remote.token)
    }

    /**
     * Migre vers api.noshi.be un compte créé localement avant l'existence du
     * service de comptes (hash/sel déjà calculés, transmis tels quels — pas besoin
     * du mot de passe en clair). Appelée au démarrage pour les sessions existantes
     * qui n'ont pas encore de device_token. Non bloquante : une erreur réseau ou un
     * 409 (déjà migré depuis un autre appareil) sont simplement journalisés, la
     * migration sera retentée au prochain démarrage ou via une connexion explicite.
     */
    suspend fun migrateIfNeeded() {
        val userId = sessionManager.getUserId()
        if (userId == -1L) return
        val user = userDao.getUserById(userId) ?: return
        if (!user.deviceToken.isNullOrBlank()) return

        try {
            val remote = accountApi.migrate(
                email = user.email,
                nom = user.lastName,
                prenom = user.firstName,
                passwordHash = user.passwordHash,
                passwordSalt = user.salt,
                recoveryCodeHash = user.recoveryHash.ifBlank { null },
                recoveryCodeSalt = user.recoverySalt.ifBlank { null },
                guid = user.syncChannelGuid,
                deviceName = deviceName()
            )
            userDao.updateUser(
                user.copy(
                    compteId = remote.compteId,
                    deviceToken = remote.token,
                    emailVerifie = remote.emailVerifie
                )
            )
            sessionManager.setDeviceToken(remote.token)
        } catch (e: AccountApiException) {
            Log.w("AuthRepository", "Migration vers api.noshi.be différée (${e.httpStatus}) : ${e.message}")
        } catch (e: Exception) {
            Log.w("AuthRepository", "Migration vers api.noshi.be impossible (réseau ?)", e)
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    suspend fun updateProfile(firstName: String, lastName: String) {
        val userId = sessionManager.getUserId()
        val user = userDao.getUserById(userId)
        if (user != null) {
            val updatedUser = user.copy(firstName = firstName, lastName = lastName)
            userDao.updateUser(updatedUser)
            sessionManager.setSession(
                userId = updatedUser.id,
                email = updatedUser.email,
                syncChannelGuid = updatedUser.syncChannelGuid,
                firstName = updatedUser.firstName,
                lastName = updatedUser.lastName
            )
        }
    }
}
