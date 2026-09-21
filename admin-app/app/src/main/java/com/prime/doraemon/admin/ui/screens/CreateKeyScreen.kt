package com.prime.doraemon.admin.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.doraemon.admin.data.api.SupabaseManager
import com.prime.doraemon.admin.data.model.ContentItem
import com.prime.doraemon.admin.ui.components.*
import com.prime.doraemon.admin.ui.theme.*
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateKeyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf("Text") }
    var textMessage by remember { mutableStateOf("") }
    var captionText by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var telegramFileId by remember { mutableStateOf("") }
    var selectedExpiryOption by remember { mutableStateOf("Never") } // Never, 1 Hour, 24 Hours, 7 Days, 30 Days

    var isUploading by remember { mutableStateOf(false) }
    var generatedKey by remember { mutableStateOf<String?>(null) }

    // Media Types
    val mediaTypes = listOf("Text", "Photo", "Video", "Document", "Audio", "File")
    val expiryOptions = listOf("Never", "1 Hour", "24 Hours", "7 Days", "30 Days")

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        if (uri != null) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        selectedFileName = it.getString(nameIndex)
                    }
                }
            }
            if (selectedFileName.isEmpty()) {
                selectedFileName = "selected_file"
            }
        }
    }

    fun generateUniqueKey(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude ambiguous chars like I, O, 1, 0
        val random = SecureRandom()
        val randomString = (1..8)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
        return "PD-$randomString"
    }

    fun calculateExpiryTimestamp(option: String): String? {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        when (option) {
            "1 Hour" -> now.add(Calendar.HOUR_OF_DAY, 1)
            "24 Hours" -> now.add(Calendar.DAY_OF_YEAR, 1)
            "7 Days" -> now.add(Calendar.DAY_OF_YEAR, 7)
            "30 Days" -> now.add(Calendar.DAY_OF_YEAR, 30)
            else -> return null
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(now.time)
    }

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "UPLOAD CONTENT & DISPATCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create Access Key",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Display Generated Key if available
            if (generatedKey != null) {
                item {
                    KeyDisplayCard(keyString = generatedKey!!)
                }
            }

            // Content Type Selector
            item {
                GlassCard {
                    Text(
                        text = "SELECT CONTENT TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mediaTypes.take(3).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    generatedKey = null
                                },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DoraemonBlue,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mediaTypes.drop(3).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    generatedKey = null
                                },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DoraemonBlue,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Content Input Area based on selected type
            item {
                GlassCard {
                    if (selectedType == "Text") {
                        Text(
                            text = "TEXT CONTENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textMessage,
                            onValueChange = { textMessage = it },
                            placeholder = { Text("Enter secret text, instructions, links, or messages to deliver...") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DoraemonBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    } else {
                        // Media Picker & Optional Telegram File ID
                        Text(
                            text = "$selectedType Media Source".uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // File picker button
                        OutlinedButton(
                            onClick = {
                                val mime = when (selectedType) {
                                    "Photo" -> "image/*"
                                    "Video" -> "video/*"
                                    "Audio" -> "audio/*"
                                    "Document" -> "application/pdf"
                                    else -> "*/*"
                                }
                                filePickerLauncher.launch(mime)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DoraemonBlue)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = DoraemonBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedFileName.isNotEmpty()) "Selected: $selectedFileName"
                                else "Choose $selectedType from Device",
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "OR USE TELEGRAM FILE_ID (OPTIONAL):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = telegramFileId,
                            onValueChange = { telegramFileId = it },
                            placeholder = { Text("e.g. BAECAQADAgAD... (if already hosted on Telegram)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DoraemonBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "CAPTION / ACCOMPANYING TEXT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            placeholder = { Text("Optional caption for this media...") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DoraemonBlue,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            // Expiry Picker
            item {
                GlassCard {
                    Text(
                        text = "EXPIRATION PERIOD",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        expiryOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedExpiryOption == opt,
                                onClick = { selectedExpiryOption = opt },
                                label = { Text(opt, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Upload & Generate Key Button
            item {
                Button(
                    onClick = {
                        // Validation
                        if (selectedType == "Text" && textMessage.isBlank()) {
                            Toast.makeText(context, "Please enter some text content", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedType != "Text" && selectedFileUri == null && telegramFileId.isBlank()) {
                            Toast.makeText(context, "Please select a file or enter a Telegram file_id", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        scope.launch {
                            isUploading = true
                            var storageUrl: String? = null

                            // 1. Upload file if picked
                            if (selectedFileUri != null) {
                                try {
                                    val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
                                    val bytes = inputStream?.readBytes()
                                    inputStream?.close()

                                    if (bytes != null) {
                                        val mime = context.contentResolver.getType(selectedFileUri!!) ?: "application/octet-stream"
                                        val uploadRes = SupabaseManager.uploadFileToStorage(
                                            fileName = selectedFileName.ifEmpty { "file" },
                                            mimeType = mime,
                                            fileBytes = bytes
                                        )
                                        if (uploadRes.isSuccess) {
                                            storageUrl = uploadRes.getOrNull()
                                        } else {
                                            Toast.makeText(context, "Storage upload failed: ${uploadRes.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                            isUploading = false
                                            return@launch
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error reading file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    isUploading = false
                                    return@launch
                                }
                            }

                            // 2. Generate Key & Save Record
                            val newKey = generateUniqueKey()
                            val expiryDate = calculateExpiryTimestamp(selectedExpiryOption)
                            val normalizedType = when (selectedType) {
                                "Photo" -> "photo"
                                "Video" -> "video"
                                "Document" -> "document"
                                "Audio" -> "audio"
                                "File" -> "document"
                                else -> "text"
                            }

                            val itemToSave = ContentItem(
                                key = newKey,
                                contentType = normalizedType,
                                textContent = if (selectedType == "Text") textMessage.trim() else null,
                                telegramFileId = telegramFileId.trim().ifEmpty { null },
                                storagePath = storageUrl,
                                caption = captionText.trim().ifEmpty { null },
                                active = true,
                                expiresAt = expiryDate
                            )

                            val saveRes = SupabaseManager.createContentItem(itemToSave)
                            isUploading = false

                            if (saveRes.isSuccess) {
                                generatedKey = newKey
                                Toast.makeText(context, "Key Generated: $newKey", Toast.LENGTH_LONG).show()
                                // Reset inputs
                                textMessage = ""
                                captionText = ""
                                selectedFileUri = null
                                selectedFileName = ""
                                telegramFileId = ""
                            } else {
                                Toast.makeText(context, "Failed to save: ${saveRes.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DoraemonBlue),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("UPLOADING & STORING...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UPLOAD & GENERATE KEY", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
