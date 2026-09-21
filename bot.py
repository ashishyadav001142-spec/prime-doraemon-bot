#!/usr/bin/env python3
"""
PRIME DORAEMON BOT
Production-ready Telegram file-delivery bot with dynamic channel verification,
key validation, and 15-minute auto-deletion of delivered messages.

Compatible with GitHub + Render Free Web Service (embeds aiohttp health check server).
"""

import os
import sys
import logging
import asyncio
import secrets
from datetime import datetime, timezone, timedelta
from typing import List, Optional, Dict, Any

def generate_random_key(prefix: str = "PD-", length: int = 8) -> str:
    alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return prefix + "".join(secrets.choice(alphabet) for _ in range(length))

from aiohttp import web
from dotenv import load_dotenv

from telegram import (
    Update,
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    constants
)
from telegram.constants import ChatMemberStatus, ParseMode
from telegram.ext import (
    Application,
    ApplicationBuilder,
    CommandHandler,
    MessageHandler,
    CallbackQueryHandler,
    ContextTypes,
    filters
)
from telegram.error import TelegramError, BadRequest, Forbidden

from supabase import create_client, Client

# ====================================================================
# 1. CONFIGURATION & LOGGING SETUP
# ====================================================================
load_dotenv()

logging.basicConfig(
    format="%(asctime)s - [%(levelname)s] - %(name)s - %(message)s",
    level=logging.INFO,
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("PrimeDoraemonBot")

# Read Environment Variables
BOT_TOKEN = os.getenv("BOT_TOKEN")
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")
PORT = int(os.getenv("PORT", "10000"))
AUTO_DELETE_SECONDS = int(os.getenv("AUTO_DELETE_SECONDS", "900")) # Default: 15 minutes (900s)

if not BOT_TOKEN:
    logger.critical("FATAL: BOT_TOKEN is missing from environment variables!")
    sys.exit(1)

if not SUPABASE_URL or not SUPABASE_KEY:
    logger.critical("FATAL: SUPABASE_URL or SUPABASE_KEY is missing from environment variables!")
    sys.exit(1)

# Initialize Supabase Client
try:
    supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)
    logger.info("Successfully connected to Supabase.")
except Exception as e:
    logger.critical(f"Failed to initialize Supabase client: {e}")
    sys.exit(1)

# ====================================================================
# 2. SUPABASE HELPER FUNCTIONS
# ====================================================================

def get_bot_settings() -> Dict[str, Any]:
    """Fetch current dynamic branding and messages from Supabase."""
    try:
        response = supabase.table("bot_settings").select("*").eq("id", "default").execute()
        if response.data and len(response.data) > 0:
            return response.data[0]
    except Exception as e:
        logger.error(f"Error fetching bot_settings: {e}")
    return {
        "bot_name": "PRIME DORAEMON BOT",
        "welcome_message": "Please join all required channels to continue.",
        "key_prompt": "Paste Your Key"
    }

def get_active_channels() -> List[Dict[str, Any]]:
    """Fetch all active mandatory channels from Supabase."""
    try:
        response = supabase.table("channels").select("*").eq("active", True).order("created_at").execute()
        return response.data or []
    except Exception as e:
        logger.error(f"Error fetching active channels from Supabase: {e}")
        return []

def get_content_by_key(key: str) -> Optional[Dict[str, Any]]:
    """Fetch content item matching the given key from Supabase."""
    try:
        cleaned_key = key.strip()
        response = supabase.table("content_items").select("*").eq("key", cleaned_key).execute()
        if response.data and len(response.data) > 0:
            return response.data[0]
    except Exception as e:
        logger.error(f"Error querying content by key '{key}': {e}")
    return None

def log_delivery(telegram_user_id: int, content_id: Optional[str], key: str, message_ids: List[int], delete_after: datetime) -> Optional[str]:
    """Log delivered messages in delivery_logs table."""
    try:
        data = {
            "telegram_user_id": telegram_user_id,
            "content_id": content_id,
            "key": key,
            "delivered_message_ids": message_ids,
            "delete_after": delete_after.isoformat(),
            "deleted": False
        }
        res = supabase.table("delivery_logs").insert(data).execute()
        if res.data and len(res.data) > 0:
            return res.data[0].get("id")
    except Exception as e:
        logger.error(f"Failed to insert delivery log: {e}")
    return None

def mark_delivery_deleted(log_id: Optional[str] = None):
    """Mark delivered messages as deleted in database."""
    if not log_id:
        return
    try:
        supabase.table("delivery_logs").update({"deleted": True}).eq("id", log_id).execute()
    except Exception as e:
        logger.debug(f"Failed to mark delivery log {log_id} deleted: {e}")

# ====================================================================
# 3. CHANNEL MEMBERSHIP VERIFICATION
# ====================================================================

async def check_user_membership(bot, user_id: int, channel: Dict[str, Any]) -> bool:
    """
    Check if a user is a member of the given channel via Telegram Bot API.
    Handles channel IDs formatted as integer strings (-100...) or @usernames.
    """
    channel_id_raw = channel.get("channel_id", "").strip()
    if not channel_id_raw:
        return True

    # Parse numerical channel ID if applicable
    chat_identifier: Any = channel_id_raw
    if channel_id_raw.startswith("-") or channel_id_raw.isdigit():
        try:
            chat_identifier = int(channel_id_raw)
        except ValueError:
            pass

    try:
        member = await bot.get_chat_member(chat_id=chat_identifier, user_id=user_id)
        # Permitted statuses: MEMBER, ADMINISTRATOR, OWNER
        valid_statuses = [
            ChatMemberStatus.MEMBER,
            ChatMemberStatus.ADMINISTRATOR,
            ChatMemberStatus.OWNER
        ]
        return member.status in valid_statuses
    except BadRequest as e:
        error_msg = str(e).lower()
        if "user not found" in error_msg or "chat not found" in error_msg:
            logger.warning(f"User {user_id} not found in channel {chat_identifier}: {e}")
            return False
        elif "bot is not a member" in error_msg or "chat admin" in error_msg:
            logger.error(
                f"BOT PERMISSION ERROR: The bot is NOT an administrator in channel '{chat_identifier}'. "
                "Please add the bot as Admin in the channel with permission to see members!"
            )
            # Fail closed or open: to prevent blocking all users if admin misconfigured,
            # return False so admin notices the prompt.
            return False
        logger.warning(f"BadRequest checking membership for {user_id} in {chat_identifier}: {e}")
        return False
    except Forbidden as e:
        logger.error(f"BOT ACCESS FORBIDDEN in channel {chat_identifier}: {e}")
        return False
    except Exception as e:
        logger.error(f"Unexpected error checking membership in channel {chat_identifier}: {e}")
        return False

async def get_missing_channels(bot, user_id: int, channels: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Returns the list of mandatory channels that the user has NOT joined yet."""
    missing = []
    for ch in channels:
        is_member = await check_user_membership(bot, user_id, ch)
        if not is_member:
            missing.append(ch)
    return missing

def build_channel_keyboard(channels: List[Dict[str, Any]], is_reverify: bool = False) -> InlineKeyboardMarkup:
    """Constructs Telegram inline buttons for channel links and verification."""
    keyboard = []
    for ch in channels:
        name = ch.get("name", "Telegram Channel")
        link = ch.get("invite_link", "").strip()
        if link:
            keyboard.append([InlineKeyboardButton(text=f"📢 JOIN CHANNEL • {name}", url=link)])

    verify_label = "🔄 RE-VERIFY" if is_reverify else "✅ VERIFY"
    keyboard.append([InlineKeyboardButton(text=verify_label, callback_data="verify_channels")])
    return InlineKeyboardMarkup(keyboard)

# ====================================================================
# 4. AUTO-DELETE WORKER
# ====================================================================

async def auto_delete_task(bot, chat_id: int, message_ids: List[int], delay_seconds: int, log_id: Optional[str] = None):
    """
    Asynchronously deletes delivered messages after the designated delay (e.g. 15 minutes).
    Handles exceptions gracefully without crashing or interrupting other operations.
    The database record in content_items is NEVER deleted.
    """
    logger.info(f"Auto-delete timer set: Messages {message_ids} in chat {chat_id} will be deleted in {delay_seconds} seconds.")
    try:
        await asyncio.sleep(delay_seconds)
        for msg_id in message_ids:
            try:
                await bot.delete_message(chat_id=chat_id, message_id=msg_id)
                logger.info(f"Successfully auto-deleted message {msg_id} in chat {chat_id}.")
            except BadRequest as e:
                # E.g., message already deleted by user or older than 48 hours
                logger.debug(f"Could not auto-delete message {msg_id}: {e}")
            except Exception as e:
                logger.warning(f"Error during auto-delete of message {msg_id}: {e}")
        mark_delivery_deleted(log_id=log_id)
    except asyncio.CancelledError:
        logger.info(f"Auto-delete task for chat {chat_id} cancelled.")
    except Exception as e:
        logger.error(f"Unexpected error in auto_delete_task: {e}")

async def periodic_deletion_sweeper(bot):
    """
    Background worker that runs every 60 seconds to ensure any messages that were not
    deleted due to server restarts or sleep cycles are safely deleted from Telegram.
    """
    logger.info("Background auto-deletion sweeper initialized.")
    while True:
        try:
            await asyncio.sleep(60)
            now_iso = datetime.now(timezone.utc).isoformat()
            res = supabase.table("delivery_logs").select("*").lte("delete_after", now_iso).eq("deleted", False).limit(25).execute()
            if res.data:
                for entry in res.data:
                    c_id = entry.get("telegram_user_id")
                    m_ids = entry.get("delivered_message_ids") or []
                    for m_id in m_ids:
                        try:
                            await bot.delete_message(chat_id=c_id, message_id=m_id)
                            logger.info(f"Sweeper deleted message {m_id} in chat {c_id}.")
                        except Exception:
                            pass
                    mark_delivery_deleted(log_id=entry.get("id"))
        except asyncio.CancelledError:
            break
        except Exception as e:
            logger.debug(f"Sweeper tick notice: {e}")

# ====================================================================
# 5. BOT HANDLERS & WORKFLOW
# ====================================================================

async def start_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handles the /start command."""
    if not update.effective_user or not update.effective_chat:
        return

    user = update.effective_user
    chat_id = update.effective_chat.id
    settings = get_bot_settings()
    bot_name = settings.get("bot_name", "PRIME DORAEMON BOT")
    welcome_msg = settings.get("welcome_message", "Please join all required channels to continue.")

    channels = get_active_channels()

    if not channels:
        # No mandatory channels configured; allow direct key input
        msg = (
            f"✨ **Welcome to {bot_name}!**\n\n"
            f"🔑 **{settings.get('key_prompt', 'Paste Your Key')}**\n"
            "Please send your key below to access your content."
        )
        await context.bot.send_message(
            chat_id=chat_id,
            text=msg,
            parse_mode=ParseMode.MARKDOWN
        )
        return

    # Check if user has already joined all channels
    missing = await get_missing_channels(context.bot, user.id, channels)

    if not missing:
        # Already joined all channels
        msg = (
            f"🤖 **{bot_name}**\n\n"
            "✅ **Verification Successful!**\n\n"
            f"🔑 **{settings.get('key_prompt', 'Paste Your Key')}**\n"
            "Please send your unique key below to receive your content."
        )
        await context.bot.send_message(
            chat_id=chat_id,
            text=msg,
            parse_mode=ParseMode.MARKDOWN
        )
        return

    # User still needs to join channels
    keyboard = build_channel_keyboard(missing, is_reverify=False)
    text = (
        f"🤖 **{bot_name}**\n\n"
        f"{welcome_msg}\n\n"
        "👇 Click the buttons below to join, then press **VERIFY**:"
    )
    await context.bot.send_message(
        chat_id=chat_id,
        text=text,
        reply_markup=keyboard,
        parse_mode=ParseMode.MARKDOWN
    )

async def verify_callback_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handles the VERIFY button callback query."""
    query = update.callback_query
    if not query or not update.effective_user:
        return

    await query.answer()
    user_id = update.effective_user.id
    settings = get_bot_settings()
    bot_name = settings.get("bot_name", "PRIME DORAEMON BOT")

    channels = get_active_channels()
    if not channels:
        await query.edit_message_text(
            f"✅ **Verification Successful!**\n\n"
            f"🔑 **{settings.get('key_prompt', 'Paste Your Key')}**\n"
            "Please send your key below to receive your content.",
            parse_mode=ParseMode.MARKDOWN
        )
        return

    missing = await get_missing_channels(context.bot, user_id, channels)

    if not missing:
        # All channels joined
        await query.edit_message_text(
            f"🤖 **{bot_name}**\n\n"
            "✅ **Verification Successful!**\n\n"
            f"🔑 **{settings.get('key_prompt', 'Paste Your Key')}**\n"
            "Send your key below to receive your content.",
            parse_mode=ParseMode.MARKDOWN
        )
    else:
        # Some channels still missing
        keyboard = build_channel_keyboard(missing, is_reverify=True)
        text = (
            f"⚠️ **PRIME DORAEMON BOT**\n\n"
            "❌ You have not joined all required channels yet.\n"
            "Please join the remaining channel(s) below and click **Re-Verify**:"
        )
        try:
            await query.edit_message_text(
                text=text,
                reply_markup=keyboard,
                parse_mode=ParseMode.MARKDOWN
            )
        except BadRequest:
            # If message content hasn't changed, Telegram returns BadRequest
            pass

async def text_key_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Handles user incoming messages (key redemption)."""
    if not update.effective_message or not update.effective_user or not update.effective_chat:
        return

    user_text = update.effective_message.text.strip()
    user_id = update.effective_user.id
    chat_id = update.effective_chat.id
    user_msg_id = update.effective_message.message_id

    # 1. Search for the key in Supabase
    content_item = get_content_by_key(user_text)

    if not content_item:
        await context.bot.send_message(
            chat_id=chat_id,
            text="❌ Invalid key. Please check your key and try again."
        )
        return

    # Check if this is an Admin Upload Key (for forwarding media)
    is_upload_key = (
        user_text.startswith("PD-UP-") or
        user_text.startswith("UP-") or
        content_item.get("telegram_file_id") == "PENDING_UPLOAD" or
        content_item.get("content_type") == "pending"
    )

    if is_upload_key:
        context.user_data["pending_upload_id"] = content_item["id"]
        context.user_data["pending_upload_key"] = user_text
        context.user_data["pending_caption"] = content_item.get("caption") or ""

        await context.bot.send_message(
            chat_id=chat_id,
            text=(
                "📥 **Upload Key Verified!**\n\n"
                "Ab aap jo bhi **Photo, Video, Document/File, Audio, Voice, Animation ya Forwarded Message** save karna chahte hain, use **is chat me send ya forward karein**.\n\n"
                "👉 File aate hi main use link karke aapko **Final Delivery Key** bana kar de dunga!\n\n"
                "*(Cancel karne ke liye /cancel likhein)*"
            ),
            parse_mode=ParseMode.MARKDOWN
        )
        return

    # 2. For Delivery Keys, verify that the user is a member of all mandatory channels
    channels = get_active_channels()
    missing = await get_missing_channels(context.bot, user_id, channels)

    if missing:
        keyboard = build_channel_keyboard(missing, is_reverify=True)
        await context.bot.send_message(
            chat_id=chat_id,
            text=(
                "⚠️ **Access Blocked**\n\n"
                "You must join all required channels before redeeming any key.\n"
                "Please join the channels below and verify:"
            ),
            reply_markup=keyboard,
            parse_mode=ParseMode.MARKDOWN
        )
        return

    # 3. Validate status
    if not content_item.get("active", True):
        await context.bot.send_message(
            chat_id=chat_id,
            text="⚠️ **This key has been deactivated.** Please contact the administrator.",
            parse_mode=ParseMode.MARKDOWN
        )
        return

    # 4. Check expiration
    expires_at_str = content_item.get("expires_at")
    if expires_at_str:
        try:
            # Handle ISO string parsing
            expires_at = datetime.fromisoformat(expires_at_str.replace("Z", "+00:00"))
            if datetime.now(timezone.utc) > expires_at:
                await context.bot.send_message(
                    chat_id=chat_id,
                    text="⏳ This key has expired."
                )
                return
        except Exception as e:
            logger.error(f"Error parsing expires_at timestamp '{expires_at_str}': {e}")

    # 5. Key is Valid -> Deliver Content!
    status_msg = await context.bot.send_message(
        chat_id=chat_id,
        text="✅ **Key verified**\n\nDelivering your content now...\n*(Delivered messages will automatically disappear after ~15 minutes)*",
        parse_mode=ParseMode.MARKDOWN
    )

    delivered_msg_ids = [status_msg.message_id, user_msg_id]
    content_type = content_item.get("content_type", "text")
    caption = content_item.get("caption") or ""
    file_id = content_item.get("telegram_file_id")
    storage_path = content_item.get("storage_path")
    text_content = content_item.get("text_content")

    # Determine media source: Telegram file_id has priority, fallback to Storage URL
    media_source = file_id if (file_id and file_id.strip()) else storage_path

    try:
        delivered_media_msg = None

        if content_type == "text":
            delivered_media_msg = await context.bot.send_message(
                chat_id=chat_id,
                text=text_content or caption or "No text content provided."
            )

        elif content_type == "photo":
            if not media_source:
                raise ValueError("Missing photo file_id or storage_path.")
            delivered_media_msg = await context.bot.send_photo(
                chat_id=chat_id,
                photo=media_source,
                caption=caption
            )

        elif content_type == "video":
            if not media_source:
                raise ValueError("Missing video file_id or storage_path.")
            delivered_media_msg = await context.bot.send_video(
                chat_id=chat_id,
                video=media_source,
                caption=caption
            )

        elif content_type == "document":
            if not media_source:
                raise ValueError("Missing document file_id or storage_path.")
            delivered_media_msg = await context.bot.send_document(
                chat_id=chat_id,
                document=media_source,
                caption=caption
            )

        elif content_type == "audio":
            if not media_source:
                raise ValueError("Missing audio file_id or storage_path.")
            delivered_media_msg = await context.bot.send_audio(
                chat_id=chat_id,
                audio=media_source,
                caption=caption
            )

        elif content_type == "voice":
            if not media_source:
                raise ValueError("Missing voice file_id or storage_path.")
            delivered_media_msg = await context.bot.send_voice(
                chat_id=chat_id,
                voice=media_source,
                caption=caption
            )

        elif content_type == "animation":
            if not media_source:
                raise ValueError("Missing animation file_id or storage_path.")
            delivered_media_msg = await context.bot.send_animation(
                chat_id=chat_id,
                animation=media_source,
                caption=caption
            )

        elif content_type in ["forward", "message"]:
            if file_id and ":" in file_id:
                parts = file_id.split(":", 1)
                f_chat: Any = parts[0]
                if f_chat.startswith("-") or f_chat.isdigit():
                    f_chat = int(f_chat)
                f_msg_id = int(parts[1])
                delivered_media_msg = await context.bot.copy_message(
                    chat_id=chat_id,
                    from_chat_id=f_chat,
                    message_id=f_msg_id,
                    caption=caption or None
                )
            else:
                raise ValueError("Forward content requires 'from_chat_id:message_id' formatted in telegram_file_id.")

        else:
            # Fallback
            delivered_media_msg = await context.bot.send_message(
                chat_id=chat_id,
                text=text_content or caption or "Delivered content."
            )

        if delivered_media_msg:
            delivered_msg_ids.append(delivered_media_msg.message_id)

    except Exception as e:
        logger.error(f"Failed to deliver content for key '{user_text}': {e}")
        await context.bot.send_message(
            chat_id=chat_id,
            text=f"⚠️ **Delivery Error:** Could not transmit media. ({str(e)})",
            parse_mode=ParseMode.MARKDOWN
        )
        return

    # 6. Schedule 15-Minute Auto-Delete
    delete_target_time = datetime.now(timezone.utc) + timedelta(seconds=AUTO_DELETE_SECONDS)
    # Log delivery in database
    log_id = log_delivery(
        telegram_user_id=user_id,
        content_id=content_item.get("id"),
        key=user_text,
        message_ids=delivered_msg_ids,
        delete_after=delete_target_time
    )

    # Spawn background auto-delete timer
    asyncio.create_task(
        auto_delete_task(
            bot=context.bot,
            chat_id=chat_id,
            message_ids=delivered_msg_ids,
            delay_seconds=AUTO_DELETE_SECONDS,
            log_id=log_id
        )
    )

async def cancel_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """Cancels any pending upload session."""
    if "pending_upload_id" in context.user_data:
        context.user_data.pop("pending_upload_id", None)
        context.user_data.pop("pending_upload_key", None)
        context.user_data.pop("pending_caption", None)
        await update.effective_message.reply_text("❌ Upload session cancel ho gaya hai.")
    else:
        await update.effective_message.reply_text("Koi active upload session nahi hai.")

async def incoming_message_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    """
    Unified router for all user messages:
    - If user is in upload mode -> saves the forwarded or sent media and outputs the Final Delivery Key.
    - If user sent text -> handles key redemption (or upload key).
    - Otherwise -> prompts the user to enter a key.
    """
    if not update.effective_message or not update.effective_user or not update.effective_chat:
        return

    message = update.effective_message
    chat_id = update.effective_chat.id

    # -------------------------------------------------------------
    # CASE 1: Admin is in Upload Mode (awaiting forward/file)
    # -------------------------------------------------------------
    if "pending_upload_id" in context.user_data:
        pending_id = context.user_data.pop("pending_upload_id")
        pending_key = context.user_data.pop("pending_upload_key", None)
        saved_caption = context.user_data.pop("pending_caption", "")

        detected_type = None
        file_id = None
        text_content = None

        if message.photo:
            detected_type = "photo"
            file_id = message.photo[-1].file_id
        elif message.video:
            detected_type = "video"
            file_id = message.video.file_id
        elif message.document:
            detected_type = "document"
            file_id = message.document.file_id
        elif message.audio:
            detected_type = "audio"
            file_id = message.audio.file_id
        elif message.voice:
            detected_type = "voice"
            file_id = message.voice.file_id
        elif message.animation:
            detected_type = "animation"
            file_id = message.animation.file_id
        elif message.text:
            detected_type = "text"
            text_content = message.text
        else:
            await context.bot.send_message(
                chat_id=chat_id,
                text="⚠️ Format recognize nahi hua. Kripya koi Photo, Video, Document ya Forward message bhejein."
            )
            # Restore session so admin can retry
            context.user_data["pending_upload_id"] = pending_id
            context.user_data["pending_upload_key"] = pending_key
            context.user_data["pending_caption"] = saved_caption
            return

        final_caption = message.caption or saved_caption or ""
        final_key = generate_random_key(prefix="PD-", length=8)

        try:
            update_data = {
                "key": final_key,
                "content_type": detected_type,
                "telegram_file_id": file_id,
                "text_content": text_content,
                "caption": final_caption if final_caption else None,
                "active": True
            }
            supabase.table("content_items").update(update_data).eq("id", pending_id).execute()

            await context.bot.send_message(
                chat_id=chat_id,
                text=(
                    "🎉 **Content Successfully Linked & Stored!**\n\n"
                    f"📁 **Type:** `{detected_type.upper()}`\n"
                    f"🔑 **Final Delivery Key:** `{final_key}`\n\n"
                    "━━━━━━━━━━━━━━━━━━━━\n"
                    "👉 Ab ye key aap kisi bhi user ko share kar sakte hain.\n"
                    "👉 Jab bhi koi user ye key bot me paste karega, usko ye file deliver hogi aur **theek 15 minute baad us user ki chat se automatically delete ho jayegi**!\n"
                    "👉 Database me ye content hamesha safe rahega."
                ),
                parse_mode=ParseMode.MARKDOWN
            )
        except Exception as e:
            logger.error(f"Error updating content item: {e}")
            await context.bot.send_message(
                chat_id=chat_id,
                text=f"❌ Error saving content: {str(e)}"
            )
        return

    # -------------------------------------------------------------
    # CASE 2: Message contains text -> process key
    # -------------------------------------------------------------
    if message.text:
        await text_key_handler(update, context)
        return

    # -------------------------------------------------------------
    # CASE 3: Random media without an upload session
    # -------------------------------------------------------------
    await context.bot.send_message(
        chat_id=chat_id,
        text="⚠️ **Invalid Action:** Pehle apna Key enter karein ya `/start` dabayein.",
        parse_mode=ParseMode.MARKDOWN
    )

# ====================================================================
# 6. RENDER DUMMY WEB SERVER (HEALTH CHECK FOR FREE WEB SERVICES)
# ====================================================================

async def handle_health(request: web.Request) -> web.Response:
    """HTTP endpoint returning 200 OK for Render health checks."""
    return web.Response(
        text="PRIME DORAEMON BOT is running smoothly.",
        content_type="text/plain"
    )

async def start_web_server(port: int):
    """Runs a lightweight aiohttp server in the background for Render port binding."""
    app = web.Application()
    app.router.add_get("/", handle_health)
    app.router.add_get("/health", handle_health)

    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, "0.0.0.0", port)
    await site.start()
    logger.info(f"Health check web server listening on port {port}.")

# ====================================================================
# 7. MAIN ENTRY POINT
# ====================================================================

async def main():
    logger.info("Initializing PRIME DORAEMON BOT...")

    # 1. Start HTTP Health Server for Render
    await start_web_server(PORT)

    # 2. Build Telegram Application
    app = (
        ApplicationBuilder()
        .token(BOT_TOKEN)
        .concurrent_updates(True)
        .build()
    )

    # Register Handlers
    app.add_handler(CommandHandler("start", start_command))
    app.add_handler(CommandHandler("cancel", cancel_command))
    app.add_handler(CallbackQueryHandler(verify_callback_handler, pattern="^verify_channels$"))
    app.add_handler(MessageHandler(~filters.COMMAND, incoming_message_handler))
    app.add_error_handler(error_handler)

    logger.info("Bot application configured. Starting long polling...")

    # Run Bot Polling
    async with app:
        await app.start()
        await app.updater.start_polling(drop_pending_updates=True)
        logger.info("PRIME DORAEMON BOT is now LIVE and listening for Telegram updates.")

        # Spawn background periodic auto-deletion sweeper
        sweeper_task = asyncio.create_task(periodic_deletion_sweeper(app.bot))

        # Keep running until process is terminated
        stop_signal = asyncio.Event()
        try:
            await stop_signal.wait()
        except (KeyboardInterrupt, SystemExit):
            logger.info("Shutdown signal received.")
        finally:
            logger.info("Stopping Telegram bot...")
            sweeper_task.cancel()
            await app.updater.stop()
            await app.stop()
            logger.info("Bot shutdown complete.")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except (KeyboardInterrupt, SystemExit):
        logger.info("Exited cleanly.")
