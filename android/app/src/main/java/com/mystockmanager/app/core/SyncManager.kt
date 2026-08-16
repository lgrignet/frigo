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
import kotlinx.coroutines.channels.Channel
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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }
    
    // File d'attente pour les messages à envoyer
    private val outgoingChannel = Channel<SyncMessage>(Channel.BUFFERED)

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
                        Log.i("Sync", "--- CONNECTÉ EN KOTLIN NATIF ---")
                        
                        // 1. Lancer l'envoi des messages en parallèle dans la même session
                        val sendJob = launch {
                            for (message in outgoingChannel) {
                                try {
                                    val text = json.encodeToString(message)
                                    send(Frame.Text(text))
                                    Log.d("Sync", "Message envoyé: ${message.type}")
                                } catch (e: Exception) {
                                    Log.e("Sync", "Erreur lors de l'envoi: ${e.message}")
                                }
                            }
                        }

                        // 2. Lancer un heartbeat (Ping) toutes les 30 secondes
                        val heartbeatJob = launch {
                            while (isActive) {
                                delay(30000)
                                try {
                                    val ping = SyncMessage("system", "ping", "{}", deviceId)
                                    send(Frame.Text(json.encodeToString(ping)))
                                    Log.d("Sync", "Heartbeat envoyé")
                                } catch (e: Exception) {
                                    Log.e("Sync", "Erreur heartbeat: ${e.message}")
                                }
                            }
                        }

                        // 3. Envoyer le message de bienvenue
                        val hello = SyncMessage("system", "hello", "{\"device\":\"android-native\"}", deviceId)
                        outgoingChannel.send(hello)
                        
                        // 4. Boucle de réception
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleIncomingMessage(frame.readText())
                            }
                        }
                        
                        heartbeatJob.cancel()
                        sendJob.cancel()
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Erreur WebSocket: ${e.message}")
                    delay(5000)
                } finally {
                    Log.w("Sync", "Déconnecté du serveur, nouvelle tentative dans 5s...")
                }
            }
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            val message = json.decodeFromString<SyncMessage>(text)
            if (message.origin == deviceId) return
            if (message.action == "ping") return // Ignorer les heartbeats des autres

            Log.i("Sync", "Donnée reçue : ${message.type} (${message.action})")
            
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
        enqueueMessage(SyncMessage("item", action, json.encodeToString(entity), deviceId))
    }

    fun syncStorage(entity: StorageEntity, action: String = "put") {
        enqueueMessage(SyncMessage("storage", action, json.encodeToString(entity), deviceId))
    }

    fun syncShop(entity: ShopEntity, action: String = "put") {
        enqueueMessage(SyncMessage("shop", action, json.encodeToString(entity), deviceId))
    }

    fun syncShopping(entity: ShoppingEntity, action: String = "put") {
        enqueueMessage(SyncMessage("shopping", action, json.encodeToString(entity), deviceId))
    }

    fun syncPrefs(entity: PreferenceEntity) {
        enqueueMessage(SyncMessage("preferences", "put", json.encodeToString(entity), deviceId))
    }

    private fun enqueueMessage(message: SyncMessage) {
        scope.launch {
            outgoingChannel.send(message)
        }
    }

    fun stopSync() {
        scope.cancel()
        client.close()
    }
}
