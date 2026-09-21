package com.prime.doraemon.admin.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelItem(
    val id: String = "",
    val name: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("invite_link") val inviteLink: String,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ContentItem(
    val id: String = "",
    val key: String,
    @SerialName("content_type") val contentType: String, // text, photo, video, document, audio, voice, animation
    @SerialName("text_content") val textContent: String? = null,
    @SerialName("telegram_file_id") val telegramFileId: String? = null,
    @SerialName("storage_path") val storagePath: String? = null,
    val caption: String? = null,
    val active: Boolean = true,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BotSettings(
    val id: String = "default",
    @SerialName("bot_name") val botName: String = "PRIME DORAEMON BOT",
    @SerialName("welcome_message") val welcomeMessage: String = "Please join all required channels to continue.",
    @SerialName("key_prompt") val keyPrompt: String = "Paste Your Key",
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class DashboardStats(
    @SerialName("total_keys") val totalKeys: Long = 0,
    @SerialName("active_keys") val activeKeys: Long = 0,
    @SerialName("total_channels") val totalChannels: Long = 0,
    @SerialName("active_channels") val activeChannels: Long = 0,
    @SerialName("total_deliveries") val totalDeliveries: Long = 0
)
