package com.prime.doraemon.admin.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.doraemon.admin.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = CardBorder,
    backgroundColor: Color = DarkSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = DoraemonBlue,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    active: Boolean,
    isExpired: Boolean = false
) {
    val (label, bg, fg) = when {
        isExpired -> Triple("EXPIRED", AccentCoral.copy(alpha = 0.18f), AccentCoral)
        active -> Triple("ACTIVE", SuccessGreen.copy(alpha = 0.18f), SuccessGreen)
        else -> Triple("INACTIVE", Color.Gray.copy(alpha = 0.2f), Color.LightGray)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TypeBadge(
    type: String
) {
    val iconColor = when (type.lowercase()) {
        "photo", "image" -> DoraemonBlue
        "video" -> AccentCoral
        "document", "file" -> AccentAmber
        "audio", "voice" -> NeonCyan
        else -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = iconColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = type.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = iconColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun KeyDisplayCard(
    keyString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUploadKey = keyString.startsWith("PD-UP-") || keyString.startsWith("UP-")

    GlassCard(
        modifier = modifier,
        borderColor = if (isUploadKey) AccentAmber.copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.5f),
        backgroundColor = DarkSurfaceElevated
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isUploadKey) Icons.Default.Share else Icons.Default.ContentCopy,
                contentDescription = null,
                tint = if (isUploadKey) AccentAmber else NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isUploadKey) "UPLOAD KEY GENERATED (SEND TO BOT)" else "DELIVERY KEY GENERATED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUploadKey) AccentAmber else NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = keyString,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                ),
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (isUploadKey) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "📋 Next Steps to Link Content:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Text("1. Copy this Upload Key below.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("2. Open @PrimeDoraemonBot in Telegram.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("3. Send this Upload Key to the bot.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("4. Forward your File, Video, Photo, or Message to the bot.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("5. Bot will return your FINAL DELIVERY KEY!", style = MaterialTheme.typography.bodySmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Prime Bot Key", keyString))
                    Toast.makeText(context, "Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (isUploadKey) AccentAmber else DoraemonBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isUploadKey) "COPY UPLOAD KEY" else "COPY KEY", fontWeight = FontWeight.Bold)
            }

            if (isUploadKey) {
                OutlinedButton(
                    onClick = {
                        val botIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/PrimeDoraemonBot"))
                        context.startActivity(botIntent)
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OPEN BOT", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Your PRIME DORAEMON BOT Key: $keyString")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Access Key"))
                    },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, NeonCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHARE KEY", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
