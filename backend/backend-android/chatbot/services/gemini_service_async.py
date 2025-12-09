import httpx
from os import getenv
from dotenv import load_dotenv
import logging

load_dotenv()

GEMINI_API_KEY = getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    logging.error("❌ GEMINI_API_KEY tidak ditemukan di environment variables")
    GEMINI_ENDPOINT = None
else:
    GEMINI_ENDPOINT = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={GEMINI_API_KEY}"

async def chat_with_gemini_api(user_message: str) -> str:
    if not user_message:
        return "❗ Pertanyaan tidak boleh kosong."
    
    if not GEMINI_ENDPOINT:
        return "❌ Konfigurasi Gemini API tidak valid."

    payload = {"contents": [{"parts": [{"text": user_message}]}]}
    headers = {"Content-Type": "application/json"}

    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(GEMINI_ENDPOINT, json=payload, headers=headers, timeout=30)
            response.raise_for_status()
            data = response.json()
            return data["candidates"][0]["content"]["parts"][0]["text"]
        except httpx.TimeoutException:
            logging.error("⏰ Timeout saat memanggil Gemini API")
            return "⏰ Maaf, server sedang sibuk. Silakan coba lagi dalam beberapa saat."
        except httpx.RequestError as e:
            logging.error(f"🌐 Error koneksi ke Gemini API: {str(e)}")
            return "🌐 Maaf, terjadi masalah koneksi. Silakan coba lagi."
        except (KeyError, IndexError) as e:
            logging.error(f"📄 Error parsing response Gemini: {str(e)}")
            return "📄 Maaf, terjadi kesalahan dalam memproses jawaban."
        except Exception as e:
            logging.error(f"❌ Error tidak terduga di Gemini API: {str(e)}")
            return "❌ Maaf, terjadi kesalahan. Silakan coba lagi."