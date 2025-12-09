# 🤖 Backend Chatbot LearnAble (FastAPI Version)

Proyek ini adalah backend chatbot edukasi berbasis **FastAPI** yang terintegrasi dengan **Dialogflow**, **Gemini AI**, dan sistem **Email Notifikasi** yang lengkap. Mendukung webhook intent, chat AI, token Dialogflow, dan pengiriman email otomatis secara asinkron.

## 🚀 Fitur Utama

### 🤖 Chatbot & AI
- ✅ Integrasi webhook dengan Dialogflow (async)
- ✅ Pilihan jenjang SD, SMP, SMA
- ✅ Navigasi pelajaran, topik, subtopik
- ✅ Integrasi Gemini AI untuk pertanyaan custom
- ✅ Redis caching untuk response AI
- ✅ Context management yang canggih

### 📧 Sistem Email Notifikasi
- ✅ **3 jenis email otomatis**: Admin notification, User approval, User rejection
- ✅ **Template HTML profesional** dengan design responsive
- ✅ **Background task** untuk pengiriman non-blocking
- ✅ **SMTP Gmail** dengan SSL/TLS support
- ✅ **Retry mechanism** dengan exponential backoff
- ✅ **Logo inline** menggunakan CID (Content-ID)
- ✅ **Fallback plain text** untuk email client lama

### 🗄️ Data & Storage
- ✅ Dukungan Firestore
- ✅ Redis client untuk caching
- ✅ Autentikasi token Dialogflow
- ✅ Struktur modular, siap production

## 📁 Struktur Proyek

```
backend-android/
├── chatbot/                    # Core chatbot functionality
│   ├── handlers/              # Intent handlers
│   │   ├── general.py         # Welcome & menu utama
│   │   ├── subject.py         # Jenjang pendidikan
│   │   ├── lessons.py         # Topik pelajaran
│   │   ├── subbab.py          # Subbab pembelajaran
│   │   ├── custom_question.py # Pertanyaan custom ke AI
│   │   └── theory_with_gemini.py # Teori dengan Gemini
│   ├── services/              # External services
│   │   ├── firestore_service.py    # Database operations
│   │   ├── gemini_service_async.py # Gemini AI integration
│   │   └── redis_client.py         # Caching layer
│   └── utils/                 # Utility functions
│       ├── context_helper.py       # Context management
│       ├── dialogflow_token.py     # Token authentication
│       └── sync_dialogflow.py      # Dialogflow sync
├── send_email/                # Email notification system
│   ├── config.py              # SMTP configuration
│   ├── background_task.py     # Background task handler
│   ├── send_email.py          # Core email functions
│   └── templates/             # HTML email templates
│       ├── registration_notification.html  # Admin notification
│       ├── approve_notification.html       # User approval
│       ├── unapprove_notification.html     # User rejection
│       └── images/
│           └── logo-learnable.png          # Brand logo
├── approval/                  # Approval system
├── main.py                    # FastAPI entry point
├── requirements.txt           # Python dependencies
└── README.md                  # Documentation
```

## ⚙️ Konfigurasi Environment

### 1. Buat file `.env` di root project:

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
GOOGLE_APPLICATION_CREDENTIALS=credentials.json
```

### 2. Setup Gmail App Password

Untuk menggunakan Gmail SMTP, Anda perlu:

1. **Aktifkan 2-Step Verification** di Google Account
2. **Generate App Password**:
   - Buka [Google Account Settings](https://myaccount.google.com/)
   - Security → 2-Step Verification → App passwords
   - Generate password untuk "Mail"
   - Gunakan password yang di-generate (bukan password login biasa)

### 3. File Rahasia

Upload file berikut di server/deployment:
- `credentials.json` → file service account Google Cloud

## 📦 Instalasi & Setup

### Prerequisites
- Python 3.8+
- Redis server (optional, untuk caching)
- Google Cloud account (untuk Firestore)

### Instalasi Lokal

```bash
# Clone repository
git clone <your-repo-url>
cd machine-learning/backend-android

# Install dependencies
pip install -r requirements.txt

# Setup environment variables
cp .env.example .env
# Edit .env dengan konfigurasi yang sesuai

# Jalankan server development
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Production Deployment

```bash
# Install production dependencies
pip install -r requirements.txt

# Jalankan dengan Gunicorn
gunicorn main:app -w 4 -k uvicorn.workers.UvicornWorker --bind 0.0.0.0:8000
```

## 📚 Dokumentasi API Endpoints

### 🤖 Chatbot Endpoints

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

**Response:**
```json
{
  "fulfillmentMessages": [
    {
      "text": {
        "text": ["Selamat datang di LearnAble! 🎓"]
      }
    }
  ]
}
```

#### 2. Check Gemini Result
```http
GET /check-gemini-result?cache_key=gemini_abc123
```

**Response:**
```json
{
  "status": "ready",
  "fulfillmentMessages": [
    {
      "text": {
        "text": ["🤖 Gemini Bot:\nJawaban dari AI..."]
      }
    }
  ]
}
```

#### 3. Get Dialogflow Token
```http
GET /get-dialogflow-token
```

**Response:**
```json
{
  "access_token": "ya29.a0AfH6SMC..."
}
```

### 📧 Email Notification Endpoints

#### 1. Admin Notification (Pendaftaran Baru)
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

#### 2. User Approval Notification
```http
POST /email-approve-user
Content-Type: application/json

{
  "email": "user@example.com",
  "role": "guru",
  "name": "Nama Guru"
}
```

#### 3. User Rejection Notification
```http
POST /email-unapprove-user
Content-Type: application/json

{
  "email": "user@example.com",
  "role": "orang tua",
  "name": "Nama User"
}
```

### 🗄️ Utility Endpoints

#### Clear Redis Cache
```http
GET /clear-all-cache
```

**Response:**
```json
{
  "status": "✅ Semua cache Redis telah dihapus"
}
```

## 🎨 Sistem Email - Fitur Lengkap

### ✨ Keunggulan Sistem Email

1. **🎯 3 Jenis Email Otomatis**
   - **Admin Notification**: Saat ada pendaftaran baru
   - **User Approval**: Konfirmasi akun disetujui
   - **User Rejection**: Notifikasi akun ditolak

2. **🚀 Performance & Reliability**
   - Background task untuk non-blocking operation
   - Retry mechanism dengan exponential backoff
   - Multiple SMTP attempts (max 3x)
   - SSL/TLS support dengan fallback STARTTLS

3. **📱 Design & UX**
   - Template HTML responsive (mobile-friendly)
   - Logo inline menggunakan CID
   - Color scheme yang konsisten
   - Fallback plain text untuk email client lama

4. **🛡️ Security & Validation**
   - Environment variables untuk credentials
   - Email format validation
   - SMTP authentication
   - Input sanitization

### 🎨 Design System Email

#### Color Palette
- **Primary Blue**: `#048AAF` (LearnAble brand)
- **Success Green**: `#059669` (Approval)
- **Error Red**: `#DC2626` (Rejection)
- **Background**: `#F3F4F6` (Light gray)
- **Card**: `#FFFFFF` (White)

#### Typography
- **Font Family**: Arial, Helvetica, sans-serif
- **Responsive Sizing**: 12px - 18px
- **Line Height**: 1.3 - 1.6
- **Weight**: 400 (normal), 600 (semi-bold), 700 (bold)

### 📧 Template Email

#### 1. Registration Notification (Admin)
- **Subject**: "Pendaftaran Baru sebagai [Role] - LearnAble"
- **Content**: Info lengkap pendaftar + waktu pendaftaran
- **Action**: Cek dashboard admin

#### 2. Approval Notification (User)
- **Subject**: "Selamat Datang di LearnAble"
- **Content**: Konfirmasi akun aktif + info akun
- **Action**: Mulai eksplorasi fitur

#### 3. Rejection Notification (User)
- **Subject**: "Pendaftaran sebagai [Role] tidak disetujui - LearnAble"
- **Content**: Info akun + kontak support
- **Action**: Hubungi tim support

## 🔧 Integrasi & Penggunaan

### Menggunakan Email System di Handler Lain

```python
from send_email.send_email import send_email_to_admin
from send_email.background_task import _enqueue_email

# Cara 1: Direct call
success = send_email_to_admin(
    user_name="John Doe",
    user_email="john@example.com",
    user_role="guru"
)

# Cara 2: Background task (recommended)
@app.post("/register")
async def register_user(user_data: dict, background_task: BackgroundTasks):
    # Simpan data user
    save_user(user_data)
    
    # Kirim email di background
    ok = _enqueue_email(
        background_task,
        send_email_to_admin,
        user_data["name"],
        user_data["email"],
        user_data["role"]
    )
    
    if not ok:
        raise HTTPException(status_code=500, detail="Gagal menjadwalkan email")
    
    return {"status": "success"}
```

### Custom Email Template

```python
# Buat template baru di send_email/templates/
# Gunakan Jinja2 syntax: {{ variable_name }}

# Panggil dengan template custom
success = send_email_to_admin(
    user_name="User",
    user_email="user@example.com",
    user_role="admin",
    template_name="custom_template.html"
)
```

## 🚦 Dokumentasi Otomatis

FastAPI menyediakan dokumentasi interaktif:

- **Swagger UI**: [http://localhost:8000/docs](http://localhost:8000/docs)
- **Redoc**: [http://localhost:8000/redoc](http://localhost:8000/redoc)
- **OpenAPI JSON**: [http://localhost:8000/openapi.json](http://localhost:8000/openapi.json)

## 🐛 Troubleshooting

### Email Issues

#### "Konfigurasi email belum lengkap"
```bash
# Pastikan .env sudah dibuat dengan:
SMTP_USER=your_email@gmail.com
SMTP_PASS=your_app_password
ADMIN_EMAIL=admin@learnable.com
```

#### "Authentication failed"
- ✅ Gunakan App Password, bukan password login
- ✅ Pastikan 2-Step Verification aktif
- ✅ Cek email Gmail valid

#### "Connection refused"
- ✅ Cek koneksi internet
- ✅ Port 465/587 tidak diblokir firewall
- ✅ Coba port alternatif (587 dengan TLS)

### Chatbot Issues

#### Intent tidak dikenali
- ✅ Cek Dialogflow agent configuration
- ✅ Pastikan webhook URL benar
- ✅ Cek log untuk error detail

#### Gemini AI tidak merespon
- ✅ Cek GEMINI_API_KEY di .env
- ✅ Cek koneksi internet
- ✅ Cek Redis server (jika menggunakan caching)

## 🔒 Security Best Practices

- ✅ **Environment Variables**: Semua credentials di .env
- ✅ **Input Validation**: Validasi semua input user
- ✅ **SMTP Authentication**: Proper SMTP login
- ✅ **HTTPS Only**: Gunakan HTTPS di production
- ✅ **Rate Limiting**: Implement rate limiting untuk endpoints
- ✅ **Logging**: Comprehensive logging untuk monitoring

## 📊 Monitoring & Logging

### Log Structure
```python
# Log format yang digunakan
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('app.log', encoding='utf-8')
    ]
)