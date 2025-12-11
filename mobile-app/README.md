# LearnAble Mobile App

LearnAble adalah aplikasi mobile pembelajaran berbasis Android yang dirancang untuk memfasilitasi proses belajar mengajar dengan fitur lengkap untuk siswa, guru, dan administrator. Aplikasi ini dibangun menggunakan Kotlin dengan arsitektur MVVM dan mengintegrasikan berbagai layanan cloud untuk memberikan pengalaman belajar yang optimal.

## 📱 Tentang Aplikasi

LearnAble adalah platform pembelajaran digital yang menyediakan:
- Sistem pembelajaran terstruktur dengan mata pelajaran, bab, dan sub-bab
- Konten pembelajaran multimedia (video dan PDF)
- Sistem kuis interaktif dengan berbagai tipe pertanyaan
- Pelacakan progress pembelajaran
- Chatbot AI untuk membantu pembelajaran
- Sistem notifikasi untuk mengingatkan aktivitas belajar
- Fitur aksesibilitas untuk pengguna dengan kebutuhan khusus

## 🎯 Fitur Utama

### 👨‍🎓 Fitur untuk Siswa

#### 1. Dashboard Siswa
- Tampilan ringkasan progress pembelajaran
- Daftar mata pelajaran berdasarkan tingkat sekolah (SD, SMP, SMA)
- Progress sub-bab yang sedang dipelajari
- Rekomendasi video pembelajaran
- Badge notifikasi
- Filter berdasarkan kelas/tingkat sekolah

#### 2. Daftar Mata Pelajaran
- Tampilan grid mata pelajaran yang tersedia
- Filter berdasarkan tingkat sekolah
- Navigasi ke daftar pelajaran per mata pelajaran

#### 3. Sistem Pembelajaran
- **Daftar Pelajaran**: Menampilkan semua pelajaran dalam suatu mata pelajaran
- **Daftar Sub-Bab**: Menampilkan sub-bab dalam suatu pelajaran
- **Detail Sub-Bab**: 
  - Daftar langkah pembelajaran (steps)
  - Materi pembelajaran (video dan PDF)
  - Tombol mulai kuis
  - Progress indicator
  - Bookmark materi

#### 4. Pemutar Video
- Pemutar video menggunakan ExoPlayer
- Kontrol playback lengkap
- Picture-in-Picture mode
- Thumbnail video
- Rekomendasi video terkait

#### 5. PDF Viewer
- Tampilan PDF materi pembelajaran
- Zoom in/out

#### 6. Sistem Kuis
- **Tipe Pertanyaan**:
  - Pilihan ganda (Multiple Choice)
  - Esai (Essay)
- Timer countdown
- Navigasi antar pertanyaan
- Indikator pertanyaan yang sudah dijawab
- Hasil kuis dengan penjelasan jawaban
- Skor dan statistik hasil

#### 7. Pelacakan Progress
- Progress per mata pelajaran
- Progress per pelajaran
- Progress per sub-bab
- History progress harian dan mingguan
- Visualisasi progress dengan bar
- Filter berdasarkan kelas/tingkat

#### 8. Chatbot AI (LearnBot)
- Chatbot berbasis Dialogflow dan Gemini AI
- Quick reply chips untuk pertanyaan umum
- Input teks bebas untuk pertanyaan custom
- Animasi typing indicator
- Cache untuk response
- Retry mechanism untuk error handling
- Mendukung berbagai topik pembelajaran

#### 9. Bookmark Materi
- Simpan materi favorit
- Daftar semua bookmark
- Hapus bookmark

#### 10. Notifikasi
- Notifikasi harian untuk mengingatkan belajar
- Notifikasi ketika menyelesaikan pembelajaran
- Badge notifikasi di dashboard
- Pengaturan notifikasi on/off

### 👨‍🏫 Fitur untuk Guru

#### 1. Dashboard Guru
- Tampilan semua pelajaran yang dibuat
- Filter berdasarkan tingkat sekolah dan mata pelajaran
- Pencarian pelajaran
- Tombol tambah pelajaran baru

#### 2. Manajemen Pelajaran
- **Buat Pelajaran Baru**:
  - Input judul pelajaran
  - Pilih mata pelajaran
  - Pilih tingkat sekolah
  - Upload thumbnail
  - Deskripsi pelajaran
- **Edit Pelajaran**: Ubah detail pelajaran yang sudah ada
- **Hapus Pelajaran**: Hapus pelajaran dengan konfirmasi
- **Detail Pelajaran**: Lihat informasi lengkap pelajaran

#### 3. Manajemen Sub-Bab
- **Buat Sub-Bab Baru**:
  - Input judul sub-bab
  - Urutan sub-bab
  - Tambah langkah pembelajaran (steps)
  - Upload materi (video dari AWS S3 atau PDF)
  - Deskripsi sub-bab
- **Edit Sub-Bab**: Ubah detail sub-bab
- **Hapus Sub-Bab**: Hapus sub-bab dengan konfirmasi
- **Detail Sub-Bab**: Lihat informasi lengkap sub-bab

#### 4. Manajemen Kuis
- **Buat Kuis Baru**:
  - Input judul kuis
  - Tambah pertanyaan (pilihan ganda atau esai)
  - Set jawaban benar
  - Set waktu pengerjaan
  - Set skor per pertanyaan
- **Edit Kuis**: Ubah pertanyaan dan jawaban
- **Hapus Kuis**: Hapus kuis dengan konfirmasi
- **Detail Kuis**: Lihat semua pertanyaan dan jawaban

#### 5. Manajemen Materi
- Upload video ke AWS S3
- Upload PDF
- Preview materi sebelum publish

### 👨‍💼 Fitur untuk Admin

#### 1. Dashboard Admin
- Daftar semua pengguna (Siswa, Guru, Admin)
- Statistik pengguna
- Filter berdasarkan role dan status
- Pencarian pengguna
- Sort dan filter lanjutan

#### 2. Manajemen Pengguna
- **Detail Pengguna**: 
  - Informasi lengkap pengguna
  - Role pengguna
  - Status akun (aktif/nonaktif)
  - Data tambahan (tingkat sekolah, kelas, dll)
- **Approval System**:
  - Approve/reject pendaftaran guru baru
  - Konfirmasi approval dengan dialog
  - Notifikasi approval berhasil

### 🔐 Fitur Autentikasi

#### 1. Login
- Login dengan email dan password
- Login dengan Google Sign-In
- Validasi input
- Error handling

#### 2. Registrasi
- Daftar akun baru
- Pilih role (Siswa/Guru)
- Input data tambahan (tingkat sekolah, kelas untuk siswa)
- Validasi form
- Konfirmasi email

#### 3. Reset Password
- Lupa password
- Kirim email reset password
- Deep link untuk reset password
- Validasi token reset

#### 4. Logout
- Konfirmasi logout
- Clear session

### ⚙️ Fitur Pengaturan

#### 1. Pengaturan Umum
- **Bahasa**: Pilih bahasa (Indonesia/English)
- **Vibrasi**: On/Off feedback vibrasi
- **Notifikasi**: On/Off notifikasi
- **Text Scaling**: Atur ukuran teks untuk aksesibilitas

#### 2. Profil
- **Lihat Profil**: Informasi akun lengkap
- **Edit Profil**: 
  - Ubah nama
  - Ubah foto profil
  - Ubah data tambahan
  - Upload foto dari galeri atau kamera
- **Ganti Password**: Ubah password dengan validasi

#### 3. Bookmark
- Daftar semua materi yang di-bookmark
- Hapus bookmark

#### 4. Feedback
- Kirim feedback ke Google Sheets
- Form feedback dengan validasi

#### 5. Tentang Aplikasi
- Informasi aplikasi

### ♿ Fitur Aksesibilitas

#### 1. Text Scaling
- Atur ukuran teks global
- Preset ukuran (Kecil, Normal, Besar)
- Custom scaling
- Aplikasi otomatis ke semua fragment

#### 2. Vibrasi Feedback
- Feedback vibrasi untuk interaksi
- Vibrasi untuk fokus EditText
- Vibrasi untuk klik tombol
- Dapat diaktifkan/nonaktifkan

#### 3. Navigasi Aksesibel
- Navigasi dengan keyboard
- Focus management

## 🛠️ Teknologi yang Digunakan

### Framework & Library Utama
- **Kotlin**: Bahasa pemrograman utama
- **Android SDK**: Platform development
- **Material Design**: UI/UX components
- **Jetpack Navigation**: Navigasi antar screen
- **ViewBinding**: Binding views
- **Hilt**: Dependency injection

### Arsitektur
- **MVVM (Model-View-ViewModel)**: Arsitektur aplikasi
- **Repository Pattern**: Data layer abstraction
- **LiveData & StateFlow**: Reactive data streams

### Backend & Cloud Services
- **Firebase**:
  - Authentication (Email, Google Sign-In)
  - Firestore (Database)
  - Storage (File storage)
  - Analytics
- **AWS S3**: Storage untuk video pembelajaran
- **Backend API**: REST API di `https://learnable-project.onrender.com/`
- **Dialogflow**: Chatbot AI
- **Gemini AI**: AI assistant untuk chatbot
- **Google Sheets API**: Feedback form

### Library Tambahan
- **Retrofit & OkHttp**: HTTP client
- **Gson**: JSON parsing
- **Glide**: Image loading
- **ExoPlayer**: Video player
- **Lottie**: Animasi
- **PDF Viewer**: Tampilan PDF
- **Calendar View**: Kalender untuk progress
- **Circle ImageView**: Avatar bulat
- **Image Picker**: Pilih gambar dari galeri/kamera

### Tools & Utilities
- **Gradle**: Build system
- **Kapt**: Annotation processing
- **Safe Args**: Type-safe navigation arguments

## 📋 Persyaratan Sistem

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 35)
- **Java Version**: Java 17
- **Kotlin Version**: 2.1.0
- **Gradle Version**: 8.6.0

## 🚀 Instalasi & Setup

### Prerequisites
1. Android Studio (Hedgehog atau lebih baru)
2. JDK 17 atau lebih tinggi
3. Android SDK dengan API 24-35
4. Google Services account (untuk Firebase)
5. AWS account (untuk S3 storage)

### Langkah Instalasi

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd mobile-app
   ```

2. **Setup Firebase**
   - Buat project Firebase di [Firebase Console](https://console.firebase.google.com/)
   - Download `google-services.json`
   - Letakkan di `app/google-services.json`

3. **Setup AWS S3**
   - Buat bucket S3 di AWS Console
   - Dapatkan Access Key dan Secret Key
   - Update di `app/build.gradle.kts`:
     ```kotlin
     buildConfigField("String", "AWS_ACCESS_KEY", "\"YOUR_ACCESS_KEY\"")
     buildConfigField("String", "AWS_SECRET_KEY", "\"YOUR_SECRET_KEY\"")
     buildConfigField("String", "S3_BUCKET_NAME", "\"YOUR_BUCKET_NAME\"")
     ```

4. **Setup Dialogflow**
   - Buat project Dialogflow
   - Dapatkan project ID
   - Update di `ChatbotFragment.kt`:
     ```kotlin
     private val projectId = "YOUR_PROJECT_ID"
     ```

5. **Setup Backend API**
   - Pastikan backend API berjalan di `https://learnable-project.onrender.com/`
   - Atau update BASE_URL di `app/build.gradle.kts`

6. **Build Project**
   ```bash
   ./gradlew build
   ```

7. **Run di Emulator/Device**
   - Buka project di Android Studio
   - Pilih device/emulator
   - Klik Run atau tekan `Shift + F10`

## 📁 Struktur Project

```
mobile-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/adika/learnable/
│   │   │   │   ├── adapter/          # RecyclerView adapters
│   │   │   │   ├── api/               # API services
│   │   │   │   ├── customview/        # Custom views
│   │   │   │   ├── di/                # Dependency injection
│   │   │   │   ├── model/             # Data models
│   │   │   │   ├── receiver/          # Broadcast receivers
│   │   │   │   ├── repository/        # Data repositories
│   │   │   │   ├── service/           # Background services
│   │   │   │   ├── util/              # Utility classes
│   │   │   │   ├── view/              # UI components
│   │   │   │   │   ├── auth/          # Authentication screens
│   │   │   │   │   ├── core/          # Main activity, base fragments
│   │   │   │   │   ├── dashboard/     # Dashboard screens
│   │   │   │   │   │   ├── admin/     # Admin dashboard
│   │   │   │   │   │   ├── student/   # Student dashboard
│   │   │   │   │   │   └── teacher/   # Teacher dashboard
│   │   │   │   │   ├── navigation/    # Navigation screens
│   │   │   │   │   ├── notification/  # Notification screens
│   │   │   │   │   ├── profile/       # Profile screens
│   │   │   │   │   └── settings/      # Settings screens
│   │   │   │   └── viewmodel/         # ViewModels
│   │   │   ├── res/                   # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                      # Unit tests
│   ├── build.gradle.kts
│   └── google-services.json
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml            # Dependency versions
└── settings.gradle.kts
```

## 🔑 Konfigurasi

### Build Config Fields
File `app/build.gradle.kts` berisi konfigurasi:
- `BASE_URL_BACKEND`: URL backend API
- `BASE_URL_DIALOGFLOW`: URL Dialogflow API
- `BASE_URL_REGION`: URL API wilayah Indonesia
- `BASE_URL_FEEDBACK`: URL Google Sheets untuk feedback
- `AWS_ACCESS_KEY`: AWS access key
- `AWS_SECRET_KEY`: AWS secret key
- `S3_BUCKET_NAME`: Nama S3 bucket

### Permissions
Aplikasi memerlukan permission berikut:
- `INTERNET`: Koneksi internet
- `RECORD_AUDIO`: Rekaman audio (untuk fitur masa depan)
- `WRITE_EXTERNAL_STORAGE`: Menyimpan file (Android < 10)
- `READ_EXTERNAL_STORAGE`: Membaca file (Android < 10)
- `READ_MEDIA_IMAGES`: Membaca gambar (Android 13+)
- `CAMERA`: Akses kamera
- `POST_NOTIFICATIONS`: Notifikasi (Android 13+)
- `ACCESS_FINE_LOCATION`: Lokasi (untuk fitur masa depan)
- `VIBRATE`: Vibrasi feedback

## 📦 Build APK

### Debug APK
```bash
./gradlew assembleDebug
```
APK akan berada di `app/build/outputs/apk/debug/`

### Release APK
```bash
./gradlew assembleRelease
```
APK akan berada di `app/build/outputs/apk/release/`

## 🐛 Troubleshooting

### Error: Google Services
- Pastikan `google-services.json` sudah ada di `app/`
- Pastikan plugin Google Services sudah ditambahkan di `build.gradle.kts`

### Error: AWS S3
- Pastikan credentials AWS sudah benar
- Pastikan bucket S3 sudah dibuat dan accessible
- Check IAM permissions untuk S3

### Error: Backend API
- Pastikan backend API sudah running
- Check BASE_URL di build.gradle.kts
- Check network connectivity

### Error: Dialogflow
- Pastikan project ID Dialogflow sudah benar
- Pastikan service account sudah di-setup
- Check API key untuk Dialogflow

## 📝 Lisensi

Proyek ini adalah bagian dari Tugas Akhir Sistem untuk program D4 Teknologi Rekayasa Perangkat Lunak.

## 👥 Kontributor

- **Developer**: [Adika Dwi Saputra]

## 📞 Kontak & Support

Untuk pertanyaan atau dukungan, silakan hubungi:
- Email: [adikadwis17@gmail.com]
- GitHub Issues: [LearnAble-Project]/issues

## 🔄 Changelog

### Version 1.0 (Current)
- ✅ Fitur autentikasi lengkap
- ✅ Dashboard untuk semua role
- ✅ Sistem pembelajaran dengan video dan PDF
- ✅ Sistem kuis interaktif
- ✅ Chatbot AI
- ✅ Pelacakan progress
- ✅ Notifikasi
- ✅ Fitur aksesibilitas
- ✅ Bookmark materi
- ✅ Manajemen pengguna (Admin)
- ✅ Manajemen konten (Guru)

---

**Dibuat dengan ❤️ untuk pendidikan Indonesia**