# 🎓 LearnAble - Platform Pembelajaran Interaktif

<div align="center">

![LearnAble Logo](https://img.shields.io/badge/LearnAble-Education-blue?style=for-the-badge&logo=graduation-cap)

**Platform pembelajaran digital yang menggabungkan chatbot AI, materi pembelajaran interaktif, dan sistem manajemen untuk siswa, guru, dan admin.**

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Python](https://img.shields.io/badge/Python-3776AB?style=flat&logo=python&logoColor=white)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=flat&logo=fastapi)](https://fastapi.tiangolo.com/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat&logo=firebase&logoColor=black)](https://firebase.google.com/)

</div>

---

## 📋 Daftar Isi

- [✨ Fitur Utama](#-fitur-utama)
- [🏗️ Arsitektur Proyek](#️-arsitektur-proyek)
- [🛠️ Tech Stack](#️-tech-stack)
- [📁 Struktur Proyek](#-struktur-proyek)
- [🚀 Instalasi & Setup](#-instalasi--setup)
- [📱 Penggunaan](#-penggunaan)
- [🔌 API Documentation](#-api-documentation)
- [🤝 Kontribusi](#-kontribusi)
- [📄 Lisensi](#-lisensi)

---

## ✨ Fitur Utama

### 🤖 Chatbot AI Cerdas
- **Dialogflow Integration**: Chatbot berbasis natural language processing
- **Gemini AI**: Integrasi dengan Google Gemini untuk pertanyaan custom
- **Context Management**: Sistem konteks canggih untuk percakapan yang lebih natural
- **Redis Caching**: Optimasi performa dengan caching response AI
- **Multi-level Navigation**: Navigasi jenjang SD, SMP, SMA dengan mudah

### 📚 Sistem Pembelajaran
- **Materi Interaktif**: Video pembelajaran dengan ExoPlayer, PDF viewer, dan konten multimedia
- **Quiz & Assessment**: Sistem kuis interaktif dengan penjelasan detail
- **Progress Tracking**: Pelacakan progress pembelajaran per siswa
- **Bookmark System**: Sistem bookmark untuk materi favorit
- **Multi-format Content**: Dukungan video, PDF, dan konten teks

### 👥 Multi-Role System
- **👨‍🎓 Dashboard Siswa**: 
  - Akses materi pembelajaran
  - Tracking progress belajar
  - Interaksi dengan chatbot
  - Bookmark materi
  
- **👨‍🏫 Dashboard Guru**:
  - Manajemen materi pembelajaran
  - Upload konten (video, PDF)
  - Buat dan kelola quiz
  - Monitoring progress siswa
  
- **👨‍💼 Dashboard Admin**:
  - Manajemen user (approval/rejection)
  - Monitoring sistem
  - Manajemen konten global

### 📧 Sistem Notifikasi Email
- **Email Otomatis**: 3 jenis email notifikasi (Admin, Approval, Rejection)
- **HTML Templates**: Template email profesional dan responsive
- **Background Tasks**: Pengiriman email non-blocking
- **Retry Mechanism**: Sistem retry dengan exponential backoff

### 🎨 Fitur Tambahan
- **Multi-language Support**: Dukungan bahasa Indonesia
- **Accessibility Features**: Text scaling, vibration feedback
- **Push Notifications**: Sistem notifikasi untuk update penting
- **Offline Support**: Beberapa fitur dapat diakses offline
- **AWS S3 Integration**: Penyimpanan konten di AWS S3
- **Firebase Integration**: Authentication, Firestore, Storage

---

## 🏗️ Arsitektur Proyek

```
LearnAble-Project/
├── 📱 mobile-app/          # Android Application (Kotlin)
│   ├── app/
│   │   └── src/main/
│   │       ├── java/       # Source code Kotlin
│   │       └── res/       # Resources (layouts, drawables, etc.)
│   └── build.gradle.kts
│
└── 🔧 backend/             # Backend API (Python FastAPI)
    └── backend-android/
        ├── chatbot/        # Chatbot handlers & services
        ├── send_email/     # Email notification system
        └── main.py         # FastAPI entry point
```

---

## 🛠️ Tech Stack

### 📱 Mobile App (Android)
- **Language**: Kotlin 2.1.0
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt (Dagger)
- **UI Framework**: Material Design Components
- **Navigation**: Navigation Component
- **Networking**: Retrofit 2, OkHttp3
- **Image Loading**: Glide
- **Video Player**: ExoPlayer (Media3)
- **PDF Viewer**: Pdf-Viewer
- **Animation**: Lottie
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth, Google Sign-In
- **Storage**: Firebase Storage, AWS S3
- **Cloud Messaging**: Firebase Cloud Messaging

### 🔧 Backend API
- **Framework**: FastAPI
- **Language**: Python 3.8+
- **AI Integration**: 
  - Google Dialogflow (NLP)
  - Google Gemini AI
- **Database**: Google Cloud Firestore
- **Caching**: Redis
- **Email**: SMTP (Gmail)
- **Authentication**: Google Cloud Authentication
- **Deployment**: Render.com

### ☁️ Cloud Services
- **Firebase**: Authentication, Firestore, Storage, Analytics
- **Google Cloud**: Dialogflow, Gemini AI, Firestore
- **AWS**: S3 (Content Storage)
- **Redis**: Caching layer

---

## 📁 Struktur Proyek

### 📱 Mobile App Structure

```
mobile-app/app/src/main/java/com/adika/learnable/
├── 📂 adapter/              # RecyclerView adapters
├── 📂 api/                  # API service interfaces
├── 📂 di/                   # Dependency Injection (Hilt)
├── 📂 model/                # Data models
├── 📂 repository/           # Data repositories
├── 📂 service/              # Background services
├── 📂 util/                 # Utility classes
└── 📂 view/                 # UI components
    ├── auth/                # Authentication screens
    ├── dashboard/
    │   ├── admin/           # Admin dashboard
    │   ├── student/         # Student dashboard
    │   └── teacher/         # Teacher dashboard
    ├── chatbot/             # Chatbot interface
    ├── profile/             # Profile management
    └── settings/            # App settings
```

### 🔧 Backend Structure

```
backend/backend-android/
├── 📂 chatbot/
│   ├── handlers/            # Intent handlers
│   │   ├── general.py      # Welcome & menu
│   │   ├── subject.py      # Education level
│   │   ├── lessons.py      # Lesson topics
│   │   ├── subbab.py       # Subtopics
│   │   ├── custom_question.py  # Custom AI questions
│   │   └── theory_with_gemini.py  # Theory with Gemini
│   ├── services/           # External services
│   │   ├── firestore_service.py
│   │   ├── gemini_service_async.py
│   │   └── redis_client.py
│   └── utils/              # Utilities
│       ├── context_helper.py
│       ├── dialogflow_token.py
│       └── sync_dialogflow.py
├── 📂 send_email/          # Email system
│   ├── config.py
│   ├── background_task.py
│   ├── send_email.py
│   └── templates/          # HTML email templates
└── main.py                 # FastAPI app
```

---

## 🚀 Instalasi & Setup

### Prerequisites

#### Untuk Mobile App:
- Android Studio Hedgehog atau lebih baru
- JDK 17
- Android SDK (minSdk 24, targetSdk 35)
- Google Services JSON file (`google-services.json`)

#### Untuk Backend:
- Python 3.8 atau lebih baru
- Redis server (optional, untuk caching)
- Google Cloud account (untuk Firestore & Dialogflow)
- Gmail account (untuk email notifications)

### 📱 Setup Mobile App

1. **Clone repository**
```bash
git clone <repository-url>
cd LearnAble-Project/mobile-app
```

2. **Setup Google Services**
   - Download `google-services.json` dari Firebase Console
   - Place di `mobile-app/app/google-services.json`

3. **Build project**
```bash
./gradlew build
```

4. **Run di Android Studio**
   - Buka project di Android Studio
   - Sync Gradle
   - Run aplikasi di emulator atau device

### 🔧 Setup Backend

1. **Clone dan masuk ke direktori backend**
```bash
cd LearnAble-Project/backend/backend-android
```

2. **Install dependencies**
```bash
pip install -r requirements.txt
```

3. **Setup Environment Variables**
   Buat file `.env` di root backend:
```env
# Gemini AI
GEMINI_API_KEY=your_gemini_api_key

# Redis (optional)
REDIS_URL=redis://localhost:6379

# Email Configuration
SMTP_USER=your_email@gmail.com
SMTP_PASS=your_gmail_app_password
ADMIN_EMAIL=admin@learnable.com

# Google Cloud (untuk Firestore)
GOOGLE_APPLICATION_CREDENTIALS=path/to/credentials.json
```

4. **Setup Gmail App Password**
   - Aktifkan 2-Step Verification di Google Account
   - Generate App Password: Security → 2-Step Verification → App passwords
   - Gunakan password yang di-generate (bukan password login)

5. **Jalankan server**
```bash
# Development
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# Production
gunicorn main:app -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000
```

6. **Akses dokumentasi API**
   - Swagger UI: http://localhost:8000/docs
   - ReDoc: http://localhost:8000/redoc

---

## 📱 Penggunaan

### Untuk Siswa 👨‍🎓

1. **Login/Register**: Daftar atau login dengan email atau Google Sign-In
2. **Pilih Jenjang**: Pilih jenjang pendidikan (SD/SMP/SMA)
3. **Pilih Mata Pelajaran**: Pilih mata pelajaran yang ingin dipelajari
4. **Akses Materi**: Buka materi pembelajaran (video, PDF, teks)
5. **Kerjakan Quiz**: Uji pemahaman dengan quiz interaktif
6. **Chat dengan AI**: Tanya jawab dengan chatbot AI
7. **Lihat Progress**: Pantau progress pembelajaran

### Untuk Guru 👨‍🏫

1. **Login sebagai Guru**: Login dengan akun guru yang sudah disetujui admin
2. **Upload Materi**: Upload video, PDF, atau konten pembelajaran
3. **Buat Quiz**: Buat quiz dengan pertanyaan dan jawaban
4. **Kelola Subbab**: Organisir materi per subbab
5. **Monitor Siswa**: Lihat progress dan aktivitas siswa

### Untuk Admin 👨‍💼

1. **Login sebagai Admin**: Login dengan akun admin
2. **Approve/Reject Users**: Kelola pendaftaran user baru
3. **Monitor Sistem**: Pantau aktivitas sistem
4. **Kelola Konten**: Manajemen konten global

---

## 🔌 API Documentation

### Chatbot Endpoints

#### 1. Webhook Dialogflow
```http
POST /webhook
Content-Type: application/json

{
  "queryResult": {
    "intent": {
      "displayName": "Welcome"
    },
    "queryText": "Halo",
    "outputContexts": []
  },
  "session": "projects/learnable/agent/sessions/abc123"
}
```

#### 2. Check Gemini Result
```http
GET /check-gemini-result?cache_key=gemini_abc123
```

#### 3. Get Dialogflow Token
```http
GET /get-dialogflow-token
```

### Email Endpoints

#### 1. Admin Notification
```http
POST /email-admin-verification
Content-Type: application/json

{
  "email": "user@example.com",
  "role": "orang tua",
  "name": "Nama User",
  "phone": "08123456789"
}
```

#### 2. User Approval
```http
POST /email-approve-user
Content-Type: application/json

{
  "email": "user@example.com",
  "role": "guru",
  "name": "Nama Guru"
}
```

#### 3. User Rejection
```http
POST /email-unapprove-user
Content-Type: application/json

{
  "email": "user@example.com",
  "role": "orang tua",
  "name": "Nama User"
}
```

### Utility Endpoints

#### Clear Redis Cache
```http
GET /clear-all-cache
```

📖 **Dokumentasi lengkap**: Akses Swagger UI di `http://localhost:8000/docs` saat server berjalan

---

## 🎯 Fitur Teknis

### Performance Optimizations
- ✅ Redis caching untuk response AI
- ✅ Lazy loading untuk konten
- ✅ Image caching dengan Glide
- ✅ Background tasks untuk email
- ✅ Efficient Firestore queries

### Security Features
- ✅ Firebase Authentication
- ✅ Environment variables untuk secrets
- ✅ Input validation
- ✅ HTTPS only di production
- ✅ Secure token management

### User Experience
- ✅ Material Design 3
- ✅ Smooth animations dengan Lottie
- ✅ Accessibility support (text scaling, vibration)
- ✅ Offline support untuk beberapa fitur
- ✅ Push notifications

---

## 🤝 Kontribusi

Kontribusi sangat diterima! Untuk berkontribusi:

1. Fork repository
2. Buat feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit perubahan (`git commit -m 'Add some AmazingFeature'`)
4. Push ke branch (`git push origin feature/AmazingFeature`)
5. Buka Pull Request

### Guidelines
- Ikuti coding style yang sudah ada
- Tambahkan komentar untuk kode kompleks
- Update dokumentasi jika diperlukan
- Test perubahan sebelum submit PR

---

## 📄 Lisensi

Proyek ini dibuat untuk Tugas Akhir Sistem. Semua hak cipta dilindungi.

---

## 👥 Tim Pengembang

Dikembangkan dengan ❤️ untuk kemajuan pendidikan Indonesia.

---

## 📞 Kontak & Support

- **Issues**: Gunakan GitHub Issues untuk melaporkan bug atau request fitur
- **Email**: support@learnable.com (contoh)

---

<div align="center">

**Made with ❤️ for Education**

⭐ Star repository ini jika project ini membantu!

</div>

