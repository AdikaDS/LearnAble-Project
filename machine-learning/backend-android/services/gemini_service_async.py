import httpx
from os import getenv
from dotenv import load_dotenv

load_dotenv()

# Konfigurasi API Gemini
GEMINI_API_KEY = getenv("GEMINI_API_KEY")
GEMINI_ENDPOINT = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={GEMINI_API_KEY}"

async def chat_with_gemini_api(user_message: str) -> str:
    if not user_message:
        return "❗ Pertanyaan tidak boleh kosong."

    payload = {"contents": [{"parts": [{"text": user_message}]}]}
    headers = {"Content-Type": "application/json"}

    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(GEMINI_ENDPOINT, json=payload, headers=headers, timeout=60)
            response.raise_for_status()
            data = response.json()
            return data["candidates"][0]["content"]["parts"][0]["text"]
        except httpx.TimeoutException:
            return None
        except httpx.RequestError as e:
            return None
        except (KeyError, IndexError):
            return None