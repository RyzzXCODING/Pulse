# Pulse 🎵

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.5-orange)
![Java](https://img.shields.io/badge/Java-21-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

> **Pulse adalah mod Minecraft Fabric yang menghadirkan pemutar musik, widget media interaktif, integrasi platform musik, serta sistem Song Request TikTok LIVE melalui EulerStream. Atur musik, playlist, koneksi platform, dan antrean permintaan lagu langsung dari dalam Minecraft dengan antarmuka Dark Glow transparan yang minimalis.**

---

## ✨ Fitur Utama

### 🎵 Pemutar Musik Lengkap
- **Playback Engine** berbasis LavaPlayer (MP3, OGG, WAV, FLAC, M4A, AAC)
- Kontrol penuh: Play/Pause, Stop, Next, Previous, Seek, Volume
- Shuffle & Repeat (Off / All / One)
- Queue management dengan drag-drop (via UI)
- Album artwork, metadata lagu (title, artist, album, duration)

### 📚 Music Library
- Scan folder musik lokal otomatis/manual
- Pencarian real-time (judul, artis, album)
- Manajemen Playlist: buat, edit, hapus, tambah/hapus lagu
- Statistik library (jumlah lagu, durasi total)

### 🔗 Koneksi Platform Musik
- **YouTube / YouTube Music** via OAuth2
  - Browse playlist & liked videos
  - Search track metadata
  - *Catatan: Playback langsung tidak didukung (batasan YouTube Data API)*
- Arsitektur siap untuk Spotify, SoundCloud (coming soon)

### 🔴 TikTok LIVE Integration (EulerStream)
- Koneksi WebSocket real-time ke EulerStream
- Auto-reconnect dengan exponential backoff
- Event handling: Chat, Gift, Like, Follow, Join
- Konfigurasi aman: API Key & Username disimpan lokal (tidak di-hardcode)

### 🎤 Sistem Song Request (Core Feature)
- **Command**: `!req <nama lagu>` atau `!req <artis> - <judul>`
- **Validasi**: Duplicate prevention, cooldown per user (default 30s), max request/user (default 3), max queue (default 50)
- **Resolver**: Cari di library lokal → platform terhubung
- **State Machine**: RECEIVED → SEARCHING → AVAILABLE → QUEUED → PLAYING → COMPLETED/UNAVAILABLE/REJECTED/FAILED
- **Moderation**: `!skip` (mod), `!clear` (streamer), `!queue`, `!myreq`, `!song`, `!help`

### 📱 Widget System (Overlay In-Game)
| Widget | Fungsi |
|--------|--------|
| **Music Widget** | Mini player: artwork, title, artist, progress, kontrol |
| **Live Widget** | Status TikTok LIVE, username, connection indicator |
| **Queue Widget** | Now playing + upcoming requests dengan requester |
| **Chat Widget** | Real-time chat TikTok LIVE (badge mod/subscriber) |
| **Notifications** | Toast notifikasi: request baru, lagu dimulai, error, dll |

- **Editor Mode**: Drag-reposition, resize, toggle visibility
- Layout tersimpan otomatis (persist across restart)

### 🎨 Dark Glow UI (GlassUI-Inspired)
- **Translucent glass panels** dengan rounded corners
- **Dark charcoal/navy base** + **Purple & Cyan accents**
- **Subtle glow** pada elemen aktif/hover (bukan neon berlebihan)
- **Smooth animations**: hover, press, slide, fade-in
- **Responsive**: Bekerja di semua resolusi & GUI scale
- **Settings**: Opacity, Scale, Glow Intensity, Accent Color, Widget visibility/position

---

## 📥 Instalasi

### Prasyarat
- Minecraft **1.21.11**
- Fabric Loader **0.19.5+**
- Java **21**

### Cara Install
1. Download `pulse-1.0.0.jar` dari [Releases](https://github.com/RyzzXCODING/Pulse/releases)
2. Taruh file JAR ke folder `mods` di instance Minecraft Fabric kamu
3. Jalankan Minecraft dengan profile Fabric 1.21.11
4. Mod akan otomatis membuat folder config di `.minecraft/config/pulse_config.json`

---

## ⌨️ Keybinds Default

| Aksi | Key Default | Deskripsi |
|------|-------------|-----------|
| **Buka Dashboard** | `P` | Membuka Pulse Dashboard utama |
| **Toggle Music Widget** | `M` | Show/hide widget musik |
| **Toggle Request Widget** | `N` | Show/hide widget antrean request |
| **Play/Pause** | `K` | Toggle playback musik |

*Semua keybind bisa diubah di **Settings > Keybinds** atau via controls Minecraft.*

---

## 🚀 Quick Start

### 1. Setup Musik Lokal
1. Buka Dashboard (`P`) → **Library**
2. Klik **"🔍 Pindai Folder Musik"** (default: `~/Music`)
3. Atau set custom folder di **Settings > Local Music Folder**
4. Lagu akan muncul di Library, siap dimainkan

### 2. Koneksi YouTube (Metadata Only)
1. Dashboard → **Connections** → **YouTube** → **Hubungkan**
2. Browser akan terbuka untuk OAuth2 consent
3. Setelah authorize, playlist & liked videos tersedia di Library

### 3. Setup TikTok LIVE Song Request
1. Dapatkan **EulerStream API Key** dari [EulerStream](https://eulerstream.com)
2. Dashboard → **TikTok LIVE** → Masukkan:
   - **TikTok Username** (tanpa @)
   - **EulerStream API Key**
3. Klik **🔗 Hubungkan**
4. Status berubah jadi **"● Terhubung"** → siap terima request!

### 4. Konfigurasi Song Request (Opsional)
**Settings > Song Requests:**
- **Prefix Command**: Default `!` (bisa diganti)
- **Max Queue**: Default 50
- **Max Request/User**: Default 3
- **Cooldown**: Default 30 detik
- **Duplicate Prevention**: On/Off
- **Auto Clear Completed**: On/Off

---

## 🎮 Command Chat (Untuk Viewer)

| Command | Contoh | Deskripsi |
|---------|--------|-----------|
| `!req <lagu>` | `!req Night Changes` | Request lagu |
| `!req <artis> - <judul>` | `!req Joji - Glimpse of Us` | Request dengan artis |
| `!queue` | `!queue` | Lihat antrean saat ini |
| `!myreq` | `!myreq` | Lihat request sendiri |
| `!song` | `!song` | Lagu yang sedang diputar |
| `!skip` | `!skip` | Skip lagu (hanya mod) |
| `!clear` | `!clear` | Hapus antrean (streamer only) |
| `!help` | `!help` | Bantuan command |

---

## ⚙️ Konfigurasi Lanjutan

File config: `.minecraft/config/pulse_config.json`

```json
{
  "ui_opacity": 0.9,
  "widget_scale": 1.0,
  "glow_intensity": 0.5,
  "background_opacity": 0.7,
  "accent_color": "#BB86FC",
  "secondary_accent_color": "#03DAC6",
  "master_volume": 1.0,
  "music_volume": 0.8,
  "request_command_prefix": "!",
  "request_command": "req",
  "max_queue_size": 50,
  "max_requests_per_user": 3,
  "user_cooldown_seconds": 30,
  "duplicate_prevention": true,
  "auto_clear_completed": true,
  "eulerstream_api_key": "",
  "tiktok_username": "",
  "auto_reconnect": true,
  "local_music_directory": ""
}
```

---

## 🔒 Keamanan & Privasi

- ✅ **API Key & Token** disimpan **lokal** di config (tidak dikirim ke server manapun)
- ✅ **OAuth2 Flow** via browser - tidak ada password masuk ke Minecraft
- ✅ **Tidak ada telemetry/analytics** - mod sepenuhnya offline
- ✅ **Disconnect** menghapus token tersimpan
- ⚠️ **Catatan**: File config lokal bisa dibaca siapa pun yang punya akses ke komputer kamu

---

## 🐛 Troubleshooting

| Masalah | Solusi |
|---------|--------|
| **Musik tidak keluar suara** | Cek Volume Master & Music Volume di Settings. Pastikan output audio Minecraft benar. |
| **File musik tidak kebaca** | Pastikan format didukung (MP3, OGG, WAV, FLAC, M4A, AAC). Cek permission folder. |
| **YouTube tidak connect** | Pastikan OAuth2 consent diberikan. Cek koneksi internet. |
| **TikTok LIVE tidak connect** | Verifikasi API Key EulerStream valid. Pastikan username TikTok benar (tanpa @). Cek LIVE status. |
| **Song request tidak masuk** | Cek prefix command di Settings. Pastikan TikTok LIVE connected. Cek cooldown/limit. |
| **Widget tidak muncul** | Cek **Widgets** tab → toggle visibility. Cek Widget Editor mode (toggle off). |
| **Config reset tiap restart** | Pastikan Minecraft punya write permission ke `.minecraft/config/` |

---

## 📋 Roadmap / Coming Soon

- [ ] **Spotify Connector** (via Spotify Web API + playback SDK)
- [ ] **SoundCloud Connector**
- [ ] **Last.fm Scrobbling**
- [ ] **Lyrics Display** (via LRCLIB / Genius)
- [ ] **Equalizer / Audio Effects**
- [ ] **Crossfade antar lagu**
- [ ] **Widget Themes** (preset warna)
- [ ] **Import/Export Playlist** (JSON/M3U)
- [ ] **Discord Rich Presence** (now playing)
- [ ] **Multi-language** (more translations)

---

## 🤝 Kontribusi

1. Fork repository
2. Buat branch fitur: `git checkout -b fitur/awesome-fitur`
3. Commit changes: `git commit -m 'Add awesome fitur'`
4. Push: `git push origin fitur/awesome-fitur`
5. Buat Pull Request

---

## 📄 Lisensi

**MIT License** - Bebas digunakan, dimodifikasi, didistribusikan.

```
Copyright (c) 2024 Alpoyy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

---

## 🙏 Credits & Dependencies

| Library | License | Fungsi |
|---------|---------|--------|
| **Fabric Loader / API** | MIT/Apache 2.0 | Modding platform |
| **LavaPlayer** | Apache 2.0 | Audio playback engine |
| **OkHttp** | Apache 2.0 | HTTP/WebSocket client |
| **Gson** | Apache 2.0 | JSON parsing |
| **Cloth Config** | MIT | Config screen API |
| **Yarn Mappings** | MIT | Mojang mappings |

---

## 📞 Dukungan & Laporan Bug

- **Issues**: [GitHub Issues](https://github.com/RyzzXCODING/Pulse/issues)
- **Discussions**: [GitHub Discussions](https://github.com/RyzzXCODING/Pulse/discussions)
- **Wiki**: [GitHub Wiki](https://github.com/RyzzXCODING/Pulse/wiki) (coming soon)

---

## 👨‍💻 Author

**Alpoyy** - *Creator & Maintainer*

- GitHub: [@RyzzXCODING](https://github.com/RyzzXCODING)

---

<div align="center">

**Dibuat dengan ❤️ untuk komunitas Minecraft Indonesia**

*Enjoy your music while mining! ⛏️🎵*

</div>