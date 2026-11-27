# Rap Zone - Music Streaming Platform

A complete, production-ready music streaming platform specialized in Arabic rap music, built with modern technologies.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Rap Zone Platform                        │
├──────────────────┬──────────────────────┬──────────────────────┤
│   Android App    │    Admin Panel       │    Backend API       │
│  (Kotlin +       │   (Vue 3 +          │   (NestJS +          │
│   Jetpack        │    Element Plus)     │    TypeORM +         │
│   Compose)       │                      │    SQLite/PostgreSQL)│
└──────────────────┴──────────────────────┴──────────────────────┘
```

## 📁 Project Structure

```
Rap Zone Open Code/
├── android/                 # Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/java/com/rapzone/app/
│   │   │   ├── model/           # Data models (Artist, Album, Song, Playlist)
│   │   │   ├── network/         # API client (Retrofit + Moshi)
│   │   │   ├── repository/      # Repository layer
│   │   │   ├── database/        # Room database + DAOs
│   │   │   ├── media/           # Media playback (ExoPlayer + MediaSession)
│   │   │   ├── ui/              # Compose UI screens
│   │   │   │   ├── home/        # Home feed
│   │   │   │   ├── artist/      # Artist detail
│   │   │   │   ├── album/       # Album detail
│   │   │   │   ├── player/      # Full-screen player
│   │   │   │   ├── search/      # Search screen
│   │   │   │   ├── playlist/    # Playlist detail
│   │   │   │   └── settings/    # Settings screen
│   │   │   ├── di/              # Hilt dependency injection
│   │   │   └── utils/           # Image loading, formatters
│   │   └── build.gradle.kts
│   └── gradle/
│
├── server/                   # Backend API (NestJS + TypeORM)
│   ├── src/
│   │   ├── entities/           # TypeORM entities
│   │   ├── auth/               # JWT authentication
│   │   ├── artists/            # Artists CRUD
│   │   ├── albums/             # Albums CRUD
│   │   ├── songs/              # Songs CRUD + play/download tracking
│   │   ├── playlists/          # Playlists CRUD
│   │   ├── favorites/          # User favorites
│   │   ├── user-playlists/     # User personal playlists
│   │   ├── stats/              # Dashboard stats + user management
│   │   ├── upload/             # File upload (audio/image)
│   │   ├── media/              # Media validation & storage
│   │   ├── home/               # Home feed + search
│   │   ├── seed/               # Database seeding
│   │   ├── common/             # Guards, interceptors
│   │   ├── app.module.ts
│   │   ├── main.ts
│   │   └── database.ts
│   ├── package.json
│   └── tsconfig.json
│
├── admin/                    # Admin Panel (Vue 3 + Element Plus)
│   ├── src/
│   │   ├── views/            # Page components
│   │   │   ├── Login.vue
│   │   │   ├── Dashboard.vue
│   │   │   ├── artists/      # List + Form
│   │   │   ├── albums/       # List + Form
│   │   │   ├── songs/        # List + Form
│   │   │   ├── playlists/    # List + Form
│   │   │   ├── users/        # User management
│   │   │   ├── Stats.vue
│   │   │   └── Settings.vue
│   │   ├── services/         # API client (Axios)
│   │   ├── router/           # Vue Router + auth guards
│   │   ├── utils/            # Formatters
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
│
├── scripts/                  # Utility scripts
│   ├── generate-media.mjs    # Generate sample MP3 + images
│   └── package.json
│
├── start.ps1                 # Windows startup script
└── README.md
```

## 🚀 Quick Start

### Prerequisites

- **Node.js 18+** - [Download](https://nodejs.org/)
- **Java 17+** - [Download](https://adoptium.net/)
- **Android Studio** (for Android app) - [Download](https://developer.android.com/studio)
- **PostgreSQL 14+** (optional, SQLite used by default) - [Download](https://www.postgresql.org/download/)

### Windows (PowerShell)

```powershell
# Clone or navigate to project
cd "Rap Zone Open Code"

# Run the startup script (opens 3 windows)
.\start.ps1
```

### Manual Start

**1. Generate Sample Media (once)**
```bash
cd scripts
npm install
npm run generate:media
```

**2. Start Backend API**
```bash
cd server
npm install
npm run build
npm run start
# API runs on http://localhost:4000
```

**3. Start Admin Panel**
```bash
cd admin
npm install
npm run dev
# Admin panel on http://localhost:8080
```

**4. Build Android App**
```bash
cd android
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Default Credentials

| Service | Email | Password |
|---------|-------|----------|
| Admin Panel | admin@rapzone.app | Admin@123 |
| Backend API | (JWT via login) | - |

## 📱 Android App Features

- **Home Feed**: Featured artists, latest albums, latest/popular songs, featured playlists
- **Artist Profile**: Bio, all songs, albums, play counts
- **Album Detail**: Cover, tracklist, play all
- **Full-screen Player**: Play/pause, seek, next/prev, shuffle, repeat, lyrics, queue
- **Search**: Real-time search across artists, albums, songs
- **Offline Mode**: Download songs (MP3), manage storage, play offline
- **Favorites**: Heart icon, synced with server
- **Personal Playlists**: Create, edit, reorder
- **Listening History**: Local + server sync
- **Settings**: Dark mode (default), download quality, WiFi-only, background playback
- **RTL Support**: Full Arabic RTL layout
- **MediaSession**: Lock screen controls, notification, Android Auto ready

## 🎛️ Admin Panel Features

- **Dashboard**: Real-time stats, charts (ECharts), top songs/artists, recent activity
- **Artists Management**: CRUD, image upload, publish/unpublish, search, pagination
- **Albums Management**: CRUD, cover upload, artist linking, year, publish state
- **Songs Management**: CRUD, MP3 upload + duration detection, cover, lyrics, album linking, play/download stats
- **Playlists Management**: CRUD, cover, song ordering, publish state
- **Users Management**: Create/edit/delete, roles (admin/user), activate/deactivate
- **Statistics**: Plays/downloads, daily charts, top content, active users
- **File Upload**: Drag-drop, progress, validation (MIME, size, magic bytes)
- **RTL Arabic UI**: Element Plus with Cairo font

## 🔧 Backend API Endpoints

### Public
```
GET  /home              # Home feed (artists, albums, songs, playlists)
GET  /home/search?q=    # Search across all content
GET  /artists           # List artists (paginated, searchable)
GET  /artists/:id       # Artist detail with songs/albums
GET  /albums            # List albums (filter by artist)
GET  /albums/:id        # Album detail with tracks
GET  /songs             # List songs (filter by artist/album, sort)
GET  /songs/popular     # Most played songs
GET  /songs/latest      # Latest releases
GET  /songs/:id         # Song detail
GET  /playlists         # Published playlists
GET  /playlists/:id     # Playlist with tracks
```

### Authenticated (JWT Bearer)
```
POST   /auth/register           # Register
POST   /auth/login              # Login
GET    /auth/profile            # Current user
POST   /auth/change-password    # Change password

GET    /favorites               # User favorites
POST   /favorites/:songId       # Add favorite
DELETE /favorites/:songId       # Remove favorite

GET    /me/playlists            # User playlists
POST   /me/playlists            # Create playlist
PATCH  /me/playlists/:id        # Update playlist
DELETE /me/playlists/:id        # Delete playlist
POST   /me/playlists/:id/songs  # Add song
DELETE /me/playlists/:id/songs/:songId

GET    /stats/history           # Listening history
POST   /songs/:id/play          # Record play
POST   /songs/:id/download      # Record download
```

### Admin Only (JWT + Role: admin)
```
POST   /artists                 # Create artist
PATCH  /artists/:id             # Update artist
DELETE /artists/:id             # Delete artist

POST   /albums                  # Create album
PATCH  /albums/:id              # Update album
DELETE /albums/:id              # Delete album

POST   /songs                   # Create song
PATCH  /songs/:id               # Update song
DELETE /songs/:id               # Delete song

POST   /playlists               # Create playlist
PATCH  /playlists/:id           # Update playlist
DELETE /playlists/:id           # Delete playlist
POST   /playlists/:id/songs     # Add song to playlist
DELETE /playlists/:id/songs/:songId

GET    /stats/dashboard         # Admin dashboard stats
GET    /stats/users             # List all users
POST   /stats/users             # Create user
PATCH  /stats/users/:id         # Update user
DELETE /stats/users/:id         # Delete user

POST   /upload/audio            # Upload MP3 (returns duration, URL)
POST   /upload/image            # Upload image (JPG/PNG/WEBP)
```

## 🎵 Media Features

### Audio Validation
- Validates MP3 magic bytes (ID3 or frame sync)
- Parses duration from MPEG headers (CBR/VBR with Xing support)
- Rejects corrupted/invalid files

### Image Validation
- Validates JPEG, PNG, WEBP magic bytes
- Size limit: 10MB

### Storage
- **Local**: Files served from `/media` via ServeStaticModule
- **S3 Compatible**: Configure via env vars (AWS, MinIO, etc.)
- **URL Rewriting**: Relative paths auto-converted to absolute via interceptor

## 🛡️ Security

- **JWT Authentication**: Access tokens, 7-day expiry
- **Role-based Access**: User vs Admin guards
- **Password Hashing**: bcrypt (10 rounds)
- **Rate Limiting**: 300 req/min global
- **Helmet**: Security headers
- **CORS**: Configurable origins
- **File Validation**: MIME + magic bytes + size limits
- **SQL Injection**: TypeORM parameterized queries
- **No Secrets in Client**: All keys server-side only

## 📊 Database Schema

```sql
users           -- Authentication, profiles, roles
artists         -- Artists (name, bio, image, playCount)
albums          -- Albums (title, cover, year, artistId)
songs           -- Songs (title, audio, duration, lyrics, artistId, albumId)
playlists       -- Curated playlists (name, cover, songs[])
user_playlists  -- Personal playlists per user
favorites       -- User favorites (userEmail + songId)
play_events     -- Listening history (user, song, duration, timestamp)
media_assets    -- Uploaded files (checksum, size, mime, path)
```

## ⚙️ Configuration

### Backend (.env)
```env
PORT=4000
PUBLIC_BASE_URL=http://localhost:4000
USE_EMBEDDED_DB=true          # false for external PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=rapzone
JWT_SECRET=your-secret-key
JWT_EXPIRES_IN=7d
ADMIN_EMAIL=admin@rapzone.app
ADMIN_PASSWORD=Admin@123
CORS_ORIGINS=http://localhost:8080
STORAGE_DRIVER=local          # or 's3'
MAX_UPLOAD_SIZE=62914560      # 60MB
```

### Android (gradle.properties)
```properties
rapzone.api.baseurl=http://10.0.2.2:4000  # Emulator localhost
```

## 🏃‍♂️ Development Commands

### Backend
```bash
cd server
npm run dev          # Watch mode (ts-node)
npm run build        # Compile TypeScript
npm run start        # Run compiled JS
npm run seed         # Run database seeder
```

### Admin
```bash
cd admin
npm run dev          # Dev server with HMR
npm run build        # Production build
npm run preview      # Preview production build
```

### Android
```bash
cd android
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK (needs signing)
./gradlew test                 # Unit tests
./gradlew connectedAndroidTest # Instrumented tests
```

## 📦 Deployment

### Backend (Production)
```bash
# 1. Set USE_EMBEDDED_DB=false
# 2. Configure PostgreSQL connection
# 3. Set strong JWT_SECRET
# 4. Configure S3 storage if needed
# 5. Run migrations (TypeORM synchronize: false, use migrations)
# 6. Build: npm run build
# 7. Run: NODE_ENV=production node dist/main.js
# 8. Use PM2 or systemd for process management
```

### Admin Panel
```bash
cd admin
npm run build
# Deploy dist/ to nginx/Apache/CDN
# Configure proxy to backend API
```

### Android
```bash
cd android
./gradlew bundleRelease
# Upload app/build/outputs/bundle/release/app-release.aab to Play Console
```

## 🧪 Testing

```bash
# Backend
cd server
npm run test           # Unit tests
npm run test:e2e       # E2E tests

# Admin
cd admin
npm run test:unit      # Vitest
npm run test:e2e       # Cypress

# Android
cd android
./gradlew test                 # Unit
./gradlew connectedAndroidTest # Instrumented
```

## 📝 License & Content Policy

**Important**: This platform is designed for content you own or have explicit rights to distribute.

- All uploads require license declaration (owned/licensed/pending)
- Admin can unpublish/delete any content
- No automatic copyright detection - manual compliance required
- Implement your local copyright laws and DMCA procedures

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📞 Support

- **Issues**: GitHub Issues
- **Email**: support@rapzone.app
- **Documentation**: See `/docs` folder

## 🙏 Acknowledgments

- **NestJS** - Backend framework
- **Vue 3 + Element Plus** - Admin panel
- **Jetpack Compose** - Android UI
- **ExoPlayer** - Media playback
- **Room** - Local database
- **TypeORM** - ORM
- **Retrofit + Moshi** - Networking
- **Hilt** - Dependency injection
- **ECharts** - Charts
- **Coil** - Image loading

---

**Rap Zone** - Your Arabic Rap Music Platform 🎵
Made with ❤️ for the culture