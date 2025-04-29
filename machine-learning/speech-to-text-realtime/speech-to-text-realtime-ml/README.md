# Real-Time Speech to Text API

API untuk konversi suara ke teks secara real-time menggunakan OpenAI Whisper.

## Fitur

- Konversi suara ke teks secara real-time
- Deteksi suara otomatis
- Dukungan untuk bahasa Indonesia
- WebSocket API untuk streaming audio

## Teknologi

- FastAPI
- OpenAI Whisper
- WebSocket
- PyTorch

## Instalasi Lokal

1. Buat virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
.\venv\Scripts\activate   # Windows
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Jalankan server:
```bash
python speech_api.py
```

## Deployment

API ini siap untuk di-deploy ke Railway. Pastikan file berikut ada di root project:
- `requirements.txt`
- `Procfile`
- `speech_api.py`
- `speech_service.py`

## Penggunaan API

### WebSocket Endpoint

```
ws://your-api-url/ws
```

Format data yang dikirim:
```json
{
    "audio": "base64_encoded_audio_data"
}
```

Format response:
```json
{
    "text": "transcribed_text"
}
```

## Lisensi

MIT 