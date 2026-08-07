# 📥 DropVault - Local-First Cross-Device Inbox & Wi-Fi Share

**DropVault** is a local-first, offline-capable Android application and drop zone designed for instant content capturing, media organization, text collection, and seamless Wi-Fi local network web sharing between Android devices, PCs, Macs, and tablets.

---

## ✨ Features

- **🚀 System Share Integration**: Share text, links, pictures, audio recordings, videos, PDFs, and documents directly from any Android app into DropVault.
- **⚡ Floating Process Text Action**: Highlight text anywhere on your phone and tap **Process Text** to instantly drop it into your local inbox without switching apps.
- **🌐 Local Wi-Fi Web Server**: Start an embedded, lightweight Ktor HTTP server with PIN protection. Open the displayed IP address on any desktop, laptop, or tablet browser on the same Wi-Fi network to browse, search, copy, view, and download inbox items instantly.
- **📁 Custom Folders & Collections**: Organize saved items into custom color-coded folders or filter by content type (Text, Links, Images, Audio, Video, PDFs, Files).
- **🔒 SHA-256 Duplicate Prevention**: Automatic file hashing prevents saving exact duplicate physical files and saves storage space.
- **📊 Storage Breakdown & Analytics**: Built-in phone storage gauge and itemized breakdown of local storage consumption by media type with one-tap temporary cache clearing.
- **🎵 Built-in Media Viewers & Players**: Fullscreen image viewer, ExoPlayer audio player with variable playback speed controls, ExoPlayer video player, and external document launcher.
- **⭐ Favorites & Archive**: Star important notes or archive items to clean up your active inbox view.

---

## 📱 Screenshots & Interface

- **Inbox Grid & List Views**: Toggle between compact vertical list and multi-column visual grid view.
- **Web Access Center**: Real-time IP address display, configurable port, one-tap server toggle, and secure PIN generator.
- **Folders Manager**: Quick navigation between All Items, Favorites, Archive, and user-created folders.

---

## 🌐 Local Web Access Guide

1. Connect your Android phone and computer/laptop to the same Wi-Fi network.
2. Open **DropVault** and tap on the **Web Access** tab.
3. Tap **Start Web Server**.
4. Note the displayed IP address (e.g. `http://192.168.1.100:8080`) and the 6-digit access PIN.
5. Open any web browser on your PC or tablet, navigate to the URL, enter the PIN, and start accessing your inbox!

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose with Material Design 3 (M3) edge-to-edge layout.
- **Local Database**: Room Database with Coroutines & Flow for reactive state management.
- **HTTP Server**: Ktor Embedded Netty Engine with JSON serialization.
- **Media Playback**: AndroidX Media3 ExoPlayer.
- **Image Loading**: Coil Compose.
- **Dependency Management**: Gradle Kotlin DSL with Version Catalog.

---

## ☕ Support

Found this useful? A coffee goes a long way ☕

<a href='https://ko-fi.com/P5P21ZQGK2' target='_blank'><img height='72' style='border:0px;height:72px;' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.
