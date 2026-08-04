package com.mystockmanager.app.core

import android.util.Log
import com.mystockmanager.app.data.local.AppDatabase
import com.mystockmanager.app.data.local.entities.*
import com.mystockmanager.app.data.remote.SyncMessage
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }
    
    private val deviceId = UUID.randomUUID().toString()
    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    init {
        Log.i("Sync", "SyncManager initialisé avec deviceId: $deviceId")
    }

    fun startSync() {
        val guid = sessionManager.getSyncGuid()
        if (guid == null) {
            Log.e("Sync", "Impossible de démarrer la synchro : GUID manquant")
            return
        }
        
        Log.i("Sync", "Démarrage du service de synchro pour le canal: $guid")
        
        scope.launch {
            while (isActive) {
                try {
                    Log.i("Sync", "Tentative de connexion à wss://sync.noshi.be/$guid...")
                    client.webSocket("wss://sync.noshi.be/$guid?device=android_native") {
                        session = this
                        Log.i("Sync", "--- CONNECTÉ EN KOTLIN NATIF ---")
                        
                        // Envoi d'un message JSON très explicite
                        val testMsg = "{\"type\":\"system\", \"action\":\"hello\", \"data\":\"Android Native Device\", \"origin\":\"$deviceId\"}"
                        send(Frame.Text(testMsg))
                        
                        // Écouter les messages entrants
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                Log.i("Sync", "Message reçu du serveur: $text")
                                handleIncomingMessage(text)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Erreur WebSocket: ${e.message}")
                    e.printStackTrace()
                    delay(5000)
                } finally {
                    session = null
                    Log.w("Sync", "Déconnecté du serveur")
                }
            }
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            val message = json.decodeFromString<SyncMessage>(text)
            if (message.origin == deviceId) return // Ne pas traiter ses propres messages

            Log.i("Sync", "Donnée reçue de l'extérieur: ${message.type} (${message.action})")
            
            withContext(Dispatchers.IO) {
                when (message.type) {
                    "item" -> {
                        val entity = json.decodeFromString<ItemEntity>(message.data)
                        if (message.action == "put") db.itemDao().insertItem(entity)
                        else if (message.action == "delete") db.itemDao().deleteItem(entity)
                    }
                    "storage" -> {
                        val entity = json.decodeFromString<StorageEntity>(message.data)
                        if (message.action == "put") db.storageDao().insertStorage(entity)
                        else if (message.action == "delete") db.storageDao().deleteStorage(entity)
                    }
                    "shop" -> {
                        val entity = json.decodeFromString<ShopEntity>(message.data)
                        if (message.action == "put") db.shopDao().insertShop(entity)
                        else if (message.action == "delete") db.shopDao().deleteShop(entity)
                    }
                    "shopping" -> {
                        val entity = json.decodeFromString<ShoppingEntity>(message.data)
                        if (message.action == "put") db.shoppingDao().insertShoppingItem(entity)
                        else if (message.action == "delete") db.shoppingDao().deleteShoppingItem(entity)
                    }
                    "preferences" -> {
                        val entity = json.decodeFromString<PreferenceEntity>(message.data)
                        db.preferenceDao().insertPreferences(entity)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Erreur traitement message: ${e.message}")
        }
    }

    fun syncItem(entity: ItemEntity, action: String = "put") {
        sendSyncMessage(SyncMessage("item", action, json.encodeToString(entity), deviceId))
    }

    fun syncStorage(entity: StorageEntity, action: String = "put") {
        sendSyncMessage(SyncMessage("storage", action, json.encodeToString(entity), deviceId))
    }

    fun syncShop(entity: ShopEntity, action: String = "put") {
        sendSyncMessage(SyncMessage("shop", action, json.encodeToString(entity), deviceId))
    }

    fun syncShopping(entity: ShoppingEntity, action: String = "put") {
        sendSyncMessage(SyncMessage("shopping", action, json.encodeToString(entity), deviceId))
    }

    fun syncPrefs(entity: PreferenceEntity) {
        sendSyncMessage(SyncMessage("preferences", "put", json.encodeToString(entity), deviceId))
    }

    private fun sendSyncMessage(message: SyncMessage) {
        val sessionRef = session
        if (sessionRef != null && sessionRef.isActive) {
            scope.launch {
                try {
                    val text = json.encodeToString(message)
                    sessionRef.send(Frame.Text(text))
                } catch (e: Exception) {
                    Log.e("Sync", "Erreur envoi message: ${e.message}")
                }
            }
        }
    }

    fun stopSync() {
        scope.cancel()
        client.close()
    }
}
