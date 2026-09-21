# 🤖 PRIME DORAEMON BOT & PRIME DORAEMON ADMIN

A complete, production-ready Telegram file-delivery ecosystem backed by Supabase with dynamic channel verification, 15-minute message auto-deletion, and a companion Jetpack Compose Android Admin App.

---

## 📑 TABLE OF CONTENTS
1. [Project Overview & Architecture](#1-project-overview--architecture)
2. [Files in This Repository](#2-files-in-this-repository)
3. [Exact List of Environment Variables Required](#3-exact-list-of-environment-variables-required)
4. [Supabase SQL Schema & Database Setup](#4-supabase-sql-schema--database-setup)
5. [Supabase Storage Setup Instructions](#5-supabase-storage-setup-instructions)
6. [Where to Find & Paste Each Supabase Value](#6-where-to-find--paste-each-supabase-value)
7. [Telegram BotFather Setup Instructions](#7-telegram-botfather-setup-instructions)
8. [Adding the Bot as Admin to Required Telegram Channels](#8-adding-the-bot-as-admin-to-required-telegram-channels)
9. [How Channel Verification Works](#9-how-channel-verification-works)
10. [How the 15-Minute Auto-Delete Works](#10-how-the-15-minute-auto-delete-works)
11. [How Admin App Generated Keys Connect to Bot Content](#11-how-admin-app-generated-keys-connect-to-bot-content)
12. [Android Admin App Setup & Build Instructions](#12-android-admin-app-setup--build-instructions)
13. [GitHub Upload Instructions](#13-github-upload-instructions)
14. [Render Free Web Service Deployment Instructions](#14-render-free-web-service-deployment-instructions)
15. [Troubleshooting & FAQs](#15-troubleshooting--faqs)

---

## 1. Project Overview & Architecture

The **PRIME DORAEMON** ecosystem consists of two synchronized components:
1. **Telegram Bot (`bot.py`)**:
   - Single-file async Python bot powered by `python-telegram-bot` v20+.
   - Includes an embedded `aiohttp` web server bound to `$PORT` ensuring 100% compatibility with **Render Free Web Services** without paying for background workers.
   - Dynamic channel verification: When users run `/start`, the bot fetches active channels directly from Supabase, verifies membership, and blocks key redemption until all required channels are joined.
   - Dispatches requested content (Text, Photos, Videos, Documents, Audio, Voice, Animations) and automatically deletes delivered messages after 15 minutes (900 seconds) while preserving database records.
2. **Android Admin App (`PRIME DORAEMON ADMIN`)**:
   - Modern Kotlin + Jetpack Compose application with a dark Cyber/Doraemon palette.
   - Features: Dashboard metrics, Channel Management (+Add, active/inactive switch, delete), Create Key (+Upload media to Supabase Storage, generate `PD-XXXXXXXX` keys, Copy/Share), and Content Library.
   - **Security**: Operates using standard Supabase Anon keys with Row Level Security (RLS). **The sensitive Service Role Key is never exposed in the Android APK.**

---

## 2. Files in This Repository

| File / Folder | Description |
| :--- | :--- |
| [`bot.py`](file:///c:/primebot/bot.py) | Complete production-grade Python Telegram Bot with embedded Render health server |
| [`schema.sql`](file:///c:/primebot/schema.sql) | Complete Supabase database schema, tables, indexes, RLS policies, and RPC functions |
| [`requirements.txt`](file:///c:/primebot/requirements.txt) | Pinned Python dependencies for Render & local execution |
| [`render.yaml`](file:///c:/primebot/render.yaml) | Render Blueprint configuration for 1-click Web Service deployment |
| [`.env.example`](file:///c:/primebot/.env.example) | Template of environment variables for the bot |
| [`.gitignore`](file:///c:/primebot/.gitignore) | Git exclusions for Python virtual environments, secrets, and Android build caches |
| [`admin-app/`](file:///c:/primebot/admin-app/) | Complete native Android Admin App source code (Jetpack Compose & Material 3) |

---

## 3. Exact List of Environment Variables Required

These variables must be set in your Render dashboard (or in `.env` for local testing):

| Variable Name | Required | Default | Description | Example |
| :--- | :---: | :---: | :--- | :--- |
| `BOT_TOKEN` | **Yes** | - | Telegram Bot token created via `@BotFather` | `7182938475:AAF_9182...` |
| `SUPABASE_URL` | **Yes** | - | Your Supabase Project HTTPS API URL | `https://xyzproject.supabase.co` |
| `SUPABASE_KEY` | **Yes** | - | Supabase **Service Role (secret)** API key | `eyJhbGciOiJIUzI1...` |
| `PORT` | Optional | `10000` | Port for the embedded HTTP health check server | `10000` |
| `AUTO_DELETE_SECONDS`| Optional | `900` | Delay before deleting delivered messages (15 mins) | `900` |

---

## 4. Supabase SQL Schema & Database Setup

1. Log into your [Supabase Dashboard](https://supabase.com/dashboard).
2. Open your project (or create a new free project).
3. In the left navigation sidebar, click on **SQL Editor**.
4. Click **New query**.
5. Copy the complete contents of [`schema.sql`](file:///c:/primebot/schema.sql) and paste it into the editor.
6. Click **Run** (or press `Ctrl + Enter`).
7. You should see `Success. No rows returned`.

### What was created:
- **`channels`**: Dynamic mandatory channels with status toggles.
- **`content_items`**: Keys, media references, captions, and expiry dates.
- **`bot_settings`**: Configurable bot name, welcome prompt, and key instructions.
- **`delivery_logs`**: Tracks sent message IDs for 15-minute auto-deletion.
- **Indexes & RLS Policies**: Secures data while permitting rapid lookups.
- **Storage Bucket**: Configures the `content-files` public bucket.

---

## 5. Supabase Storage Setup Instructions

The SQL script automatically inserts the storage bucket record. To verify and complete bucket settings:
1. In the Supabase left sidebar, click **Storage**.
2. Under **Buckets**, confirm that **`content-files`** exists.
3. If it is not marked as **Public**, click the three dots (`...`) next to `content-files` -> **Edit bucket** -> toggle **Public bucket** to **ON** -> click **Save**.
4. Allowed MIME types: Images, Videos, Audio, and Documents (up to 50 MB per file).

---

## 6. Where to Find & Paste Each Supabase Value

### In Supabase Dashboard:
1. Go to **Project Settings** (gear icon at bottom left).
2. Click **API** under Configuration.
3. You will see:
   - **Project URL**: e.g., `https://abcdefghijklm.supabase.co`
   - **Project API keys**:
     - `anon` `public`: e.g., `eyJhbGciOi...`
     - `service_role` `secret`: e.g., `eyJhbGciOi...`

### Where to Paste Each Value:

#### For the Telegram Bot (`bot.py` / Render):
- Set `SUPABASE_URL` = `Project URL`
- Set `SUPABASE_KEY` = `service_role` secret key (gives the bot permission to deliver content, query channels, and log auto-deletions).

#### For the Android Admin App (`PRIME DORAEMON ADMIN`):
1. Install and open the Android Admin App.
2. Tap the **Settings** tab.
3. Paste `Project URL` into **Supabase URL**.
4. Paste `anon` public key into **Supabase Anon (Public) Key**.
5. Tap **Save Keys**, then tap **Test Connection**.
6. *Note*: Never put the `service_role` key inside the Android app!

---

## 7. Telegram BotFather Setup Instructions

1. Open the Telegram app and search for `@BotFather`.
2. Send `/start`, then send `/newbot`.
3. Enter your bot display name:
   ```text
   PRIME DORAEMON BOT
   ```
4. Enter a unique username ending in `bot` (e.g. `PrimeDoraemonDeliveryBot`).
5. `@BotFather` will reply with your API token:
   ```text
   Use this token to access the HTTP API:
   7182938475:AAF_9182ABcDefGhiJkLmNoPqrStUvWxYz
   ```
6. Copy this token. This is your `BOT_TOKEN`.
7. (Optional) Set description and bot picture in BotFather using `/setdescription` and `/setuserpic`.

---

## 8. Adding the Bot as Admin to Required Telegram Channels

To verify whether a user has joined a channel, Telegram requires the bot to be an **Administrator** in that channel with the `Can invite users via link` or `Manage chat` permission.

### Steps:
1. Open your Telegram Channel.
2. Tap the Channel title to open Channel Info -> tap the **Edit (Pencil)** icon.
3. Tap **Administrators** -> tap **Add Admin**.
4. Search for your bot username (e.g. `@PrimeDoraemonDeliveryBot`) and select it.
5. Grant standard administrative rights (at minimum: *Invite Users via Link* / *Post Messages*).
6. Tap **Done** / **Save**.
7. Get your Channel ID:
   - For public channels: `@channelusername`
   - For private channels: Forward a message from the channel to `@userinfobot` or `@JsonDumpBot` to get the numerical ID starting with `-100...` (e.g. `-1001987654321`).
8. Add this Channel in your **PRIME DORAEMON ADMIN** mobile app under the **Channels** screen!

---

## 9. How Channel Verification Works

1. When a user opens the bot and sends `/start`, `bot.py` queries the Supabase `channels` table for active channels (`active = true`).
2. If active channels exist, the bot inspects the user's membership in each channel using the Telegram Bot API:
   ```python
   member = await bot.get_chat_member(chat_id=channel_id, user_id=user_id)
   ```
3. Permitted statuses are `MEMBER`, `ADMINISTRATOR`, and `OWNER`.
4. If the user has not joined all channels:
   - The bot dynamically filters and displays **only the channels the user is missing**.
   - Each missing channel has a direct `📢 Join Channel` button + a `🔄 Re-Verify` button.
5. When the user taps `VERIFY`:
   - The bot re-checks the remaining channels.
   - If joined: Advances to `🔑 Paste Your Key`.
6. Dynamic synchronization: If you add or deactivate a channel from the Admin App, the bot reflects the change immediately on the next `/start` or verify request without restarting.

---

## 10. How the 15-Minute Auto-Delete Works

1. Once a valid key is entered, the bot transmits the corresponding content (photo, video, text, document, etc.) and records:
   - Status confirmation message ID
   - Media message ID
2. The message IDs are logged in Supabase under `delivery_logs`.
3. An asynchronous non-blocking task is scheduled in Python:
   ```python
   asyncio.create_task(auto_delete_task(bot, chat_id, message_ids, delay_seconds=900))
   ```
4. After 900 seconds (15 minutes), the task calls `bot.delete_message(chat_id, msg_id)` for each sent message.
5. **Safety Guarantee**:
   - If the user has already deleted the message or blocked the bot, the exception (`BadRequest` / `MessageCantBeDeleted`) is caught and logged gracefully.
   - **The original content record in `content_items` is NEVER deleted** from Supabase. Only the user's temporary delivered chat messages are removed.

---

## 11. How Admin App Generated Keys Connect to Bot Content

1. In the Android Admin App, navigate to **Create Key**.
2. Choose **Text** or pick a **Media** file (Image, Video, Document, Audio, File).
3. If media is picked, the app streams the bytes directly into the Supabase Storage bucket `content-files` via HTTPS REST and receives a permanent public URL.
4. The app generates a cryptographically random, collision-resistant key in the format:
   ```text
   PD-7KX92M4Q
   ```
5. The key, content type, storage URL or text, and optional expiration date are inserted into Supabase `content_items`.
6. When a user pastes `PD-7KX92M4Q` into the Telegram bot:
   - The bot executes a fast indexed query: `supabase.table("content_items").select("*").eq("key", key).execute()`.
   - If found, active, and not expired: The bot downloads or forwards the media and dispatches it directly into the user's chat!

---

## 12. Android Admin App Setup & Build Instructions

The Android Admin App is located in the [`admin-app/`](file:///c:/primebot/admin-app/) directory.

### Requirements:
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android Device or Emulator running Android 8.0 (API 26) or newer

### Steps to Build & Install:
1. Open **Android Studio**.
2. Click **Open** -> browse and select `c:\primebot\admin-app`.
3. Allow Gradle to sync dependencies.
4. Connect your Android device via USB (with Developer Options & USB Debugging enabled) or start an Android Virtual Device (AVD).
5. Click **Run** (`Shift + F10`) to build and launch **PRIME DORAEMON ADMIN**.
6. To generate a release APK:
   - Go to **Build** -> **Generate Signed Bundle / APK** -> choose **APK** -> select your keystore -> build release.

---

## 13. GitHub Upload Instructions

Run these commands in your terminal to initialize and push your project to a private GitHub repository:

```bash
cd c:\primebot

# 1. Initialize git
git init

# 2. Add files
git add .

# 3. Commit
git commit -m "feat: complete PRIME DORAEMON BOT and Android Admin ecosystem"

# 4. Rename default branch to main
git branch -M main

# 5. Link to your GitHub repository (replace with your repo URL)
git remote add origin https://github.com/yourusername/prime-doraemon-bot.git

# 6. Push to GitHub
git push -u origin main
```

---

## 14. Render Free Web Service Deployment Instructions

Render's Free Web Service provides 24/7 online hosting without extra costs.

### Steps:
1. Log into [Render.com](https://render.com).
2. Click **New +** -> **Web Service**.
3. Connect your GitHub repository (`prime-doraemon-bot`).
4. Enter the following configuration:
   - **Name**: `prime-doraemon-bot`
   - **Region**: Oregon (US West) or closest to you
   - **Branch**: `main`
   - **Runtime**: `Python 3`
   - **Build Command**:
     ```bash
     pip install -r requirements.txt
     ```
   - **Start Command**:
     ```bash
     python bot.py
     ```
   - **Instance Type**: `Free`
5. Scroll down to **Environment Variables** and add:
   - `BOT_TOKEN`: `your_telegram_bot_token`
   - `SUPABASE_URL`: `https://your-project.supabase.co`
   - `SUPABASE_KEY`: `your_supabase_service_role_secret_key`
   - `AUTO_DELETE_SECONDS`: `900`
6. Click **Create Web Service**.
7. Render will build the dependencies, launch `bot.py`, and bind to `$PORT`. The embedded health check server will return `200 OK`, keeping your bot active.

---

## 15. Troubleshooting & FAQs

### Q: Why does the bot say "User not found" or fail channel verification?
> **Solution**: Ensure your bot has been added as an **Administrator** in each required channel. Telegram does not allow bots to query member lists unless the bot is an admin.

### Q: Does the 15-minute auto-delete delete the file from Supabase?
> **No**. Auto-delete only deletes the messages inside the user's private Telegram chat. The original content and key remain securely stored in Supabase until explicitly deleted from the Admin App.

### Q: Can I change channel requirements without touching `bot.py`?
> **Yes**. The bot queries the database dynamically on every `/start` and `VERIFY` click. Adding, disabling, or modifying channels in the Android Admin App takes effect immediately.

### Q: Why does Render free tier work with `bot.py`?
> Standard telegram bots don't open HTTP ports, causing Render web services to fail health checks after 15 minutes. `bot.py` includes an embedded `aiohttp` web server running concurrently on `$PORT` that responds `200 OK` on `/` and `/health`, keeping Render happy while long-polling updates in the background.
