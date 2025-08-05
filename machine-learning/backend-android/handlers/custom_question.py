from services.gemini_service_async import chat_with_gemini_api
import hashlib
from services.redis_client import redis_client
from fastapi import BackgroundTasks
import logging

def generate_cache_key(session: str, message: str) -> str:
    key_string = f"{session}:{message}"
    return hashlib.sha256(key_string.encode()).hexdigest()

def make_response(jawaban: str):
    logging.info("📤 Membuat respons untuk Dialogflow")
    # Kirim respons ke user
    chips = [
        {"text": "💬 Tanya Lagi ke AI"},
        {"text": "🏠 Menu Utama"}
    ]
    response = {
        "fulfillmentMessages": [
            {"text": {"text": [f"🤖 Gemini Bot:\n{jawaban}"]}},
            {"text": {"text": ["🤖 Chatbot:\nIngin bertanya lagi atau kembali ke menu?:"]}},
            {"payload": {"richContent": [[{"type": "chips", "options": chips}] ]}}
        ]
    }
    logging.info("📤 Mengirim respons teori subbab + chips ke user.")
    return response

async def generate_and_cache_gemini_answer(prompt: str, cache_key: str):
    try:
        jawaban = await chat_with_gemini_api(prompt)
        if jawaban:  # hanya simpan jika jawaban valid
            await redis_client.set(cache_key, jawaban, ex=60) # Simpan ke Redis dengan expire 1 menit
        else:
            logging.warning("❌ Jawaban dari Gemini gagal. Tidak disimpan ke Redis.")
            return {
                "fulfillmentText": "🤖 Maaf, saya belum bisa memberikan jawaban saat ini. Silakan coba lagi nanti."
            }
        logging.info(f"✅ Jawaban Gemini disimpan ke Redis untuk key: {cache_key}")
        logging.info("✅ Respons dari Gemini berhasil didapat.")
    except Exception as e:
        logging.error(f"❌ Gagal generate jawaban Gemini: {str(e)}")

async def handle_custom_question(req, background_task: BackgroundTasks):
    user_question = req.queryResult.get("queryText", "").strip()
    session = req.session

    if not user_question:
        return {
            "fulfillmentText": "❗ Pertanyaan tidak boleh kosong."
        }
    try:
        # Kalau hanya klik chip "Tanya Lagi ke AI", kirim prompt awal saja
        if user_question == "💬 Tanya Lagi ke AI":
            return {
                "fulfillmentText": "Silakan ketik pertanyaan yang ingin kamu tanyakan 😊"
            }
        
        cache_key = generate_cache_key(session, user_question)

        # Panggil Gemini API
        # Cek di Redis
        cached = await redis_client.get(cache_key)
        if cached:
            logging.info("📦 Jawaban diambil dari Redis cache.")
            # Kirim langsung ke user
            return make_response(cached)
        
        # Jika belum ada, kirim respon awal
        logging.info("🕐 Jawaban belum tersedia. Kirim respon awal ke Dialogflow.")
        background_task.add_task(generate_and_cache_gemini_answer, user_question, cache_key)
        
        return {
            "fulfillmentText": "🤖 Jawaban sedang diproses... Mohon tunggu sebentar.",
            "outputContexts": [
            {
                "name": f"{req.session}/contexts/waiting_custom_answer",
                "lifespanCount": 5,
                "parameters": {
                    "cache_key": cache_key
                }
            }
        ]
        }
    except Exception as e:
        return {
            "fulfillmentText": f"Terjadi kesalahan: {str(e)}"
        }