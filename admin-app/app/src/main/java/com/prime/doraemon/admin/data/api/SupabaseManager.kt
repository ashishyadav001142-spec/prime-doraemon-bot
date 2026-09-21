package com.prime.doraemon.admin.data.api

import com.prime.doraemon.admin.PrimeAdminApp
import com.prime.doraemon.admin.data.model.BotSettings
import com.prime.doraemon.admin.data.model.ChannelItem
import com.prime.doraemon.admin.data.model.ContentItem
import com.prime.doraemon.admin.data.model.DashboardStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object SupabaseManager {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun getBaseUrl(): String = PrimeAdminApp.instance.getSupabaseUrl()
    private fun getSupabaseKey(): String = PrimeAdminApp.instance.getSupabaseKey()

    private fun buildRequest(path: String): Request.Builder {
        val url = "${getBaseUrl()}$path"
        val key = getSupabaseKey()
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "PrimeAdminApp/1.0")
            .header("apikey", key)

        // Only add Authorization: Bearer if key is a legacy JWT (starts with eyJ)
        // Secret keys (sb_secret_...) must only be passed in apikey header
        if (key.startsWith("eyJ")) {
            builder.header("Authorization", "Bearer $key")
        }
        return builder
    }

    // ====================================================================
    // DASHBOARD METRICS
    // ====================================================================
    suspend fun fetchDashboardStats(): Result<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            // First attempt to call the RPC function get_admin_dashboard_stats
            val rpcRequest = buildRequest("/rest/v1/rpc/get_admin_dashboard_stats")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(rpcRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val stats = json.decodeFromString<DashboardStats>(body)
                    return@withContext Result.success(stats)
                }
            }

            // Fallback: Query tables individually if RPC is not deployed yet
            val channelsRes = fetchChannels().getOrDefault(emptyList())
            val contentRes = fetchContentItems().getOrDefault(emptyList())

            val stats = DashboardStats(
                totalKeys = contentRes.size.toLong(),
                activeKeys = contentRes.count { it.active }.toLong(),
                totalChannels = channelsRes.size.toLong(),
                activeChannels = channelsRes.count { it.active }.toLong(),
                totalDeliveries = 0
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====================================================================
    // CHANNELS CRUD
    // ====================================================================
    suspend fun fetchChannels(): Result<List<ChannelItem>> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/channels?select=*&order=created_at.desc")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: "[]"
                val items = json.decodeFromString<List<ChannelItem>>(body)
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChannel(channel: ChannelItem): Result<ChannelItem> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildJsonObject {
                put("name", channel.name)
                put("channel_id", channel.channelId)
                put("invite_link", channel.inviteLink)
                put("active", channel.active)
            }.toString()

            val request = buildRequest("/rest/v1/channels")
                .addHeader("Prefer", "return=representation")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: "[]"
                val list = json.decodeFromString<List<ChannelItem>>(body)
                if (list.isNotEmpty()) Result.success(list[0])
                else Result.failure(IOException("Empty response creating channel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChannelActive(channelId: String, active: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildJsonObject { put("active", active) }.toString()
            val request = buildRequest("/rest/v1/channels?id=eq.$channelId")
                .patch(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed to update status: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChannel(channelId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/channels?id=eq.$channelId")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed to delete channel: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====================================================================
    // CONTENT ITEMS CRUD
    // ====================================================================
    suspend fun fetchContentItems(): Result<List<ContentItem>> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/content_items?select=*&order=created_at.desc")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: "[]"
                val items = json.decodeFromString<List<ContentItem>>(body)
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createContentItem(item: ContentItem): Result<ContentItem> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildJsonObject {
                if (item.id.isNotBlank()) put("id", item.id)
                put("key", item.key)
                put("content_type", item.contentType)
                item.textContent?.let { put("text_content", it) }
                item.telegramFileId?.let { put("telegram_file_id", it) }
                item.storagePath?.let { put("storage_path", it) }
                item.caption?.let { put("caption", it) }
                put("active", item.active)
                item.expiresAt?.let { put("expires_at", it) }
            }.toString()

            val request = buildRequest("/rest/v1/content_items")
                .addHeader("Prefer", "return=representation")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $body"))
                }
                val list = json.decodeFromString<List<ContentItem>>(if (body.startsWith("[")) body else "[$body]")
                if (list.isNotEmpty()) Result.success(list[0])
                else Result.failure(IOException("Empty response creating content item"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContentItemActive(id: String, active: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildJsonObject { put("active", active) }.toString()
            val request = buildRequest("/rest/v1/content_items?id=eq.$id")
                .patch(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed to toggle content item: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateContentItemExpiry(id: String, expiresAt: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildJsonObject {
                if (expiresAt != null) put("expires_at", expiresAt)
                else put("expires_at", null as String?)
            }.toString()
            val request = buildRequest("/rest/v1/content_items?id=eq.$id")
                .patch(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed to update expiry: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContentItem(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/content_items?id=eq.$id")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed to delete content item: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====================================================================
    // STORAGE UPLOAD (Supabase Storage: /storage/v1/object/content-files)
    // ====================================================================
    suspend fun uploadFileToStorage(fileName: String, mimeType: String, fileBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bucketName = "content-files"
            val sanitizedName = "${System.currentTimeMillis()}_${fileName.replace(" ", "_")}"
            val path = "/storage/v1/object/$bucketName/$sanitizedName"

            val mediaType = mimeType.toMediaType()
            val requestBody = fileBytes.toRequestBody(mediaType)

            val request = buildRequest(path)
                .addHeader("Content-Type", mimeType)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    return@withContext Result.failure(IOException("Storage upload failed (HTTP ${response.code}): $err"))
                }
                // Return public URL to the uploaded object
                val publicUrl = "${getBaseUrl()}/storage/v1/object/public/$bucketName/$sanitizedName"
                Result.success(publicUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====================================================================
    // BOT SETTINGS
    // ====================================================================
    suspend fun fetchBotSettings(): Result<BotSettings> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/rest/v1/bot_settings?id=eq.default&select=*")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: "[]"
                val list = json.decodeFromString<List<BotSettings>>(body)
                if (list.isNotEmpty()) Result.success(list[0])
                else Result.success(BotSettings())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBotSettings(settings: BotSettings): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = json.encodeToString(settings)
            val request = buildRequest("/rest/v1/bot_settings?id=eq.default")
                .patch(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed to update bot settings: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
