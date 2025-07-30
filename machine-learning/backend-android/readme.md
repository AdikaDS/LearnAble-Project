# 🤖 Backend Chatbot LearnAble (FastAPI Version)

Proyek ini adalah backend chatbot edukasi berbasis **FastAPI** yang terintegrasi dengan **Dialogflow** dan **Gemini AI**. Mendukung webhook intent, chat AI, dan token Dialogflow secara asinkron.

## 🚀 Fitur
- ✅ Integrasi webhook dengan Dialogflow (async)
- ✅ Pilihan jenjang, pelajaran, topik, subtopik
- ✅ Dukungan Firestore
- ✅ Autentikasi token Dialogflow
- ✅ Integrasi Gemini AI (async, httpx)
- ✅ Struktur modular, siap production

## 📁 Struktur Proyek

backend-android/
├── handlers/ # Penanganan intent
│   ├── general.py
│   ├── subject.py
│   ├── lessons.py
│   ├── subbab.py
│   └── custom_question.py
├── services/
│   ├── firestore_service.py
│   └── gemini_service_async.py # Koneksi Gemini AI (async)
├── utils/
│   ├── context_helper.py
│   ├── sync_dialogflow.py
│   └── dialogflow_token.py
├── main.py # Entry point FastAPI
├── requirements.txt
├── .env
├── .gitignore
└── README.md

## ⚙️ Konfigurasi `.env`
Buat file `.env` di root project, contoh:
```
GEMINI_API_KEY=xxx
PORT=5000
```

## 🔐 File Rahasia
Upload file berikut di server/deployment:
- `credentials.json` → file service account Google Cloud

## 📦 Instalasi Lokal
```bash
# Clone repo
cd machine-learning/backend-android
pip install -r requirements.txt

# Jalankan server
uvicorn main:app --reload
```

## 📚 Dokumentasi Endpoint

### 1. Webhook Dialogflow
- **POST** `/webhook`
- **Body:**
```json
{
  "queryResult": { ... },
  "session": "..."
}
```
- **Response:**
```json
{
  "fulfillmentMessages": [...]
}
```

### 2. Chat Gemini
- **POST** `/chat-gemini`
- **Body:**
```json
{
  "message": "Apa itu AI?"
}
```
- **Response:**
```json
{
  "reply": "AI adalah ..."
}
```

### 3. Get Dialogflow Token
- **GET** `/get-dialogflow-token`
- **Response:**
```json
{
  "access_token": "..."
}
```

## 📝 Catatan
- Semua endpoint async, siap untuk beban tinggi.
- Untuk deployment production, gunakan Uvicorn/Gunicorn.
- Kode handler dan service mudah dikembangkan.

## 🚦 Dokumentasi Otomatis (Swagger & Redoc)
FastAPI menyediakan dokumentasi otomatis berbasis OpenAPI:

- **Swagger UI:** [http://localhost:8000/docs](http://localhost:8000/docs)
- **Redoc:** [http://localhost:8000/redoc](http://localhost:8000/redoc)

Cukup jalankan server, lalu buka salah satu URL di atas di browser untuk eksplorasi dan testing API secara interaktif.

---

**Kontribusi, bug report, dan saran sangat diterima!**


