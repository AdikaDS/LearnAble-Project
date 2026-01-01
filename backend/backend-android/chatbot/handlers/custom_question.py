from chatbot.services.gemini_service_async import chat_with_gemini_api
import hashlib
from chatbot.services.redis_client import redis_client
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
    intent = req.queryResult.get("intent", {}).get("displayName", "")
    output_contexts = req.queryResult.get("outputContexts", [])

    logging.info(f"🎯 Intent: {intent}, User Question: '{user_question}'")
    logging.info(f"📝 Session: {session}")

    has_waiting_context = any(
        "waiting_custom_answer" in context.get("name", "")
        for context in output_contexts
    )

    if intent == "Tanya Lagi ke AI":
        logging.info("💬 User klik chip 'Tanya Lagi ke AI' - Langsung masuk ke custom question")
        return {
            "fulfillmentText": "💬 Silakan ketik pertanyaan yang ingin kamu tanyakan ke AI 😊",
            "fulfillmentMessages": [
                {
                    "text": {
                        "text": ["💬 Silakan ketik pertanyaan yang ingin kamu tanyakan ke AI 😊"]
                    }
                }
            ],
            "outputContexts": [
                {
                    "name": f"{req.session}/contexts/waiting_custom_answer",
                    "lifespanCount": 5
                }
            ]
        }
    
    # Jika ada context waiting_custom_answer aktif, proses sebagai custom question
    # Terlepas dari intent yang diterima (karena diarahkan dari main.py)
    if has_waiting_context and user_question:
        logging.info(f"💭 User mengetik pertanyaan: '{user_question}' (Intent: {intent}) - Diproses sebagai Custom Pertanyaan")
        
        try:
            cache_key = generate_cache_key(session, user_question)
            logging.info(f"🔑 Cache key: {cache_key}")

            if redis_client:
                cached = await redis_client.get(cache_key)
                if cached:
                    logging.info("📦 Jawaban diambil dari Redis cache.")
                    return make_response(cached)
                else:
                    logging.info("🔄 Cache tidak ditemukan, akan generate jawaban baru")
            else:
                logging.warning("⚠️ Redis client tidak tersedia")
            
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
            logging.error(f"❌ Error dalam handle_custom_question: {str(e)}")
            return {
                "fulfillmentText": f"Terjadi kesalahan: {str(e)}"
            }
    
    # Fallback untuk intent Custom Pertanyaan atau Default Fallback Intent tanpa context
    elif intent in ["Custom Pertanyaan", "Default Fallback Intent"]:
        logging.info(f"💭 User mengetik pertanyaan: '{user_question}' (Intent: {intent})")
        
        if not user_question:
            logging.warning("⚠️ Pertanyaan kosong")
            return {
                "fulfillmentText": "❗ Pertanyaan tidak boleh kosong."
            }
        
        try:
            cache_key = generate_cache_key(session, user_question)
            logging.info(f"🔑 Cache key: {cache_key}")

            if redis_client:
                cached = await redis_client.get(cache_key)
                if cached:
                    logging.info("📦 Jawaban diambil dari Redis cache.")
                    return make_response(cached)
                else:
                    logging.info("🔄 Cache tidak ditemukan, akan generate jawaban baru")
            else:
                logging.warning("⚠️ Redis client tidak tersedia")
            
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
            logging.error(f"❌ Error dalam handle_custom_question: {str(e)}")
            return {
                "fulfillmentText": f"Terjadi kesalahan: {str(e)}"
            }
    
    else:
        logging.warning(f"⚠️ Intent tidak dikenali atau tidak ada context: '{intent}', has_waiting_context: {has_waiting_context}, user_question: '{user_question}'")
        return {
            "fulfillmentText": "Maaf, intent tidak dikenali."
        }