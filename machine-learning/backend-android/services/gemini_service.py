import requests
from os import getenv
from dotenv import load_dotenv

load_dotenv()

# Konfigurasi API Gemini
GEMINI_API_KEY = getenv("GEMINI_API_KEY")
GEMINI_ENDPOINT = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key={GEMINI_API_KEY}"


def chat_with_gemini_api(user_message: str) -> str:
    if not user_message:
        return "❗ Pertanyaan tidak boleh kosong."

    payload = {"contents": [{"parts": [{"text": user_message}]}]}

    headers = {"Content-Type": "application/json"}

    try:
        response = requests.post(GEMINI_ENDPOINT,
                                 json=payload,
                                 headers=headers,
                                 timeout=10)
        response.raise_for_status()
        data = response.json()

        return data["candidates"][0]["content"]["parts"][0]["text"]

    except requests.exceptions.Timeout:
        return "⏱️ Waktu permintaan habis. Silakan coba lagi."

    except requests.exceptions.RequestException as e:
        return f"⚠️ Terjadi kesalahan jaringan: {str(e)}"

    except (KeyError, IndexError):
        return "🤖 Maaf, saya belum bisa memberikan jawaban saat ini."
