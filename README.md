# StreamlyTV

A free, open-source IPTV player for Android / Amazon Fire Stick.

## Features
- 📺 M3U playlist support (URL or file)
- 🔍 Search channels
- 📂 Categories / Groups
- ⭐ Favourites
- 📅 EPG / TV Guide (XMLTV)
- 🌐 Language filter (English + Telugu by default)
- 4️⃣ 4K channel badge detection
- 🔊 5.1 audio badge detection
- ⚡ Hardware video decoder (ExoPlayer)
- 🎵 Dolby AC3/EAC3 audio passthrough for soundbars

---

## How to Build & Install (No coding needed)

### Step 1: Upload to GitHub
1. Go to github.com → click **"New repository"**
2. Name it `StreamlyTV`
3. Set to **Public**
4. Click **"Create repository"**
5. Click **"uploading an existing file"** link
6. Drag and drop ALL the files from this folder
7. Click **"Commit changes"**

### Step 2: Auto-Build
1. Click the **"Actions"** tab at the top of your repository
2. You should see **"Build StreamlyTV APK"** workflow
3. Click **"Run workflow"** → **"Run workflow"** (green button)
4. Wait ~5 minutes for it to finish (green checkmark = success)

### Step 3: Download APK
1. Click on the completed workflow run
2. Scroll down to **"Artifacts"**
3. Click **"StreamlyTV-debug"** to download the APK zip
4. Unzip to get `app-debug.apk`

### Step 4: Install on Fire Stick
1. Upload `app-debug.apk` to a cloud service (Google Drive, Dropbox)
2. On Fire Stick, open **Downloader** app
3. Enter the direct download link to your APK
4. Install it

---

## First Run
1. Open StreamlyTV
2. Tap the **+** button (bottom right)
3. Enter your M3U playlist name and URL from your provider
4. Tap **Load** — channels will appear in seconds
5. Tap any channel to watch!

## Settings
- **Hardware Decoder**: Keep ON for best 4K performance
- **Audio Passthrough**: Keep ON for Dolby 5.1 to soundbar
- **Buffer**: 10 seconds recommended, increase if buffering
- **EPG URL**: Enter XMLTV URL from your provider for TV guide
- **Language Filter**: English + Telugu selected by default
