# 🤖 Backend Chatbot LearnAble

Proyek ini merupakan backend berbasis Flask untuk chatbot edukasi yang terintegrasi dengan **Dialogflow** dan **Gemini AI**.  
Digunakan untuk menangani webhook, memberikan token akses, dan merespons pertanyaan secara dinamis.


## 🚀 Fitur

- ✅ Integrasi webhook dengan Dialogflow
- ✅ Pilihan jenjang, mata pelajaran, topik, hingga subtopik
- ✅ Dukungan Firestore (opsional)
- ✅ Autentikasi token Dialogflow
- ✅ Integrasi dengan Gemini AI untuk respon berbasis AI
- ✅ Struktur kode modular dan bersih


## 📁 Struktur Proyek

backend-android/
├── handlers/ # Penanganan setiap intent
│ ├── general.py
│ ├── subject.py
│ ├── lessons.py
│ └── subbab.py
│
├── services/
│ ├── firestore_service.py
│ └── gemini_service.py # Koneksi ke Gemini AI
│
├── utils/
│  ├── context_helper.py
│  ├── sync_dialogflow.py
│  └── dialogflow_token.py # Token akses Dialogflow
│
├── main.py # Entry point Flask
├── requirements.txt
├── .env
├── .gitignore
└── README.md


## ⚙️ Konfigurasi `.env`

Buat file `.env` di root project


## 🔐 File Rahasia

Upload file berikut di dashboard **Render > Environment > Secret Files**:

- `credentials.json` → file service account dari Google Cloud


## 📦 Instalasi Lokal

```bash
# Clone repo
git clone https://github.com/AdikaDS/LearnAble-Project.git
cd machine-learning/backend-android

# Install dependency
pip install -r requirements.txt

# Jalankan server
python main.py


