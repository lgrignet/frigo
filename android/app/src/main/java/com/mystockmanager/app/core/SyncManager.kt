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
    
    private val outgoingChannel = Channel<SyncMessage>(Channel.BUFFERED)

    init {
        Log.i("Sync", "SyncManager initialisé avec deviceId: $deviceId")
    }

    fun startSync() {
        val guid = sessionManager.getSyncGuid()
        if (guid == null) return
        
        scope.launch {
            while (isActive) {
                try {
                    // Le device_token (§4.4/§5.4 du cahier des charges) permet au relais de
                    // vérifier l'appareil avant d'accepter la connexion sur ce salon.
                    val token = sessionManager.getDeviceToken().orEmpty()
                    client.webSocket("wss://sync.noshi.be/$guid?device=android_native&token=$token") {
                        Log.i("Sync", "--- CONNECTÉ EN KOTLIN NATIF ---")

                        val sendJob = launch {
                            for (message in outgoingChannel) {
                                try {
                                    send(Frame.Text(json.encodeToString(message)))
                                } catch (e: Exception) {}
                            }
                        }

                        val heartbeatJob = launch {
                            while (isActive) {
                                delay(30000)
                                try {
                                    send(Frame.Text(json.encodeToString(SyncMessage("system", "ping", "{}", deviceId))))
                                } catch (e: Exception) {}
                            }
                        }

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleIncomingMessage(frame.readText())
                            }
                        }

                        heartbeatJob.cancel()
                        sendJob.cancel()
                    }
                } catch (e: Exception) {
                    Log.w("Sync", "Connexion perdue : ${e.message}")
                }
                // Délai systématique avant de retenter, que la connexion précédente se
                // soit terminée normalement (ex. token refusé par le relais) ou en erreur —
                // évite une boucle de reconnexion immédiate en cas de rejet d'authentification.
                delay(5000)
            }
        }
    }

    private suspend fun handleIncomingMessage(text: String) {
        try {
            val message = json.decodeFromString<SyncMessage>(text)
            if (message.origin == deviceId) return
            
            if (message.type == "system") {
                if (message.action == "sync_catalog") {
                    handleSyncCatalog(message.data)
                }
                return
            }

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
                    "domicile" -> {
                        val entity = json.decodeFromString<DomicileEntity>(message.data)
                        if (message.action == "put") db.domicileDao().insertDomicile(entity)
                        else if (message.action == "delete") db.domicileDao().deleteDomicile(entity)
                    }
                    "unit" -> {
                        val entity = json.decodeFromString<UnitEntity>(message.data)
                        if (message.action == "put") db.unitDao().insertUnit(entity)
                        else if (message.action == "delete") db.unitDao().deleteUnit(entity)
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

    private suspend fun handleSyncCatalog(catalogData: String) {
        val userId = sessionManager.getUserId().toString()
        val pairs = catalogData.split(",").filter { it.contains(":") }
        val validIdsByType = pairs.map { it.split(":") }.groupBy({ it[0] }, { it[1] })

        Log.i("Sync", "Nettoyage via catalogue reçu : ${pairs.size} objets valides")

        withContext(Dispatchers.IO) {
            // Items
            val validItems = validIdsByType["item"] ?: emptyList()
            db.itemDao().getAllItems(userId).first().forEach { local ->
                if (!validItems.contains(local.id)) db.itemDao().deleteItem(local)
            }
            // Storages
            val validStorages = validIdsByType["storage"] ?: emptyList()
            db.storageDao().getAllStorages(userId).first().forEach { local ->
                if (!validStorages.contains(local.id)) db.storageDao().deleteStorage(local)
            }
            // Shops
            val validShops = validIdsByType["shop"] ?: emptyList()
            db.shopDao().getAllShops(userId).first().forEach { local ->
                if (!validShops.contains(local.id)) db.shopDao().deleteShop(local)
            }
            // Domiciles
            val validDomiciles = validIdsByType["domicile"] ?: emptyList()
            db.domicileDao().getAllDomiciles().first().forEach { local ->
                if (!validDomiciles.contains(local.id)) db.domicileDao().deleteDomicile(local)
            }
            // Units
            val validUnits = validIdsByType["unit"] ?: emptyList()
            db.unitDao().getAllUnits(userId).first().forEach { local ->
                if (!validUnits.contains(local.id)) db.unitDao().deleteUnit(local)
            }
            // Shopping
            val validShopping = validIdsByType["shopping"] ?: emptyList()
            db.shoppingDao().getShoppingList(userId).first().forEach { local ->
                if (!validShopping.contains(local.id)) db.shoppingDao().deleteShoppingItem(local)
            }
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

    fun syncDomicile(entity: DomicileEntity, action: String = "put") {
        enqueueMessage(SyncMessage("domicile", action, json.encodeToString(entity), deviceId))
    }

    fun syncUnit(entity: UnitEntity, action: String = "put") {
        enqueueMessage(SyncMessage("unit", action, json.encodeToString(entity), deviceId))
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
