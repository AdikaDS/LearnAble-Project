from services.firestore_service import db
from services.gemini_service_async import chat_with_gemini_api
from utils.context_helper import get_context_param
from services.redis_client import redis_client
from fastapi import BackgroundTasks
import hashlib
import logging

def generate_cache_key(level: str, materi: str) -> str:
    key_string = f"{level}:{materi}"
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
        if jawaban and not jawaban.startswith("❌") and not jawaban.startswith("⏰") and not jawaban.startswith("🌐") and not jawaban.startswith("📄"):
            await redis_client.set(cache_key, jawaban, ex=60 * 60 * 6) # Simpan ke Redis dengan expire 6 jam
            logging.info(f"✅ Jawaban Gemini disimpan ke Redis untuk key: {cache_key}")
        else:
            logging.warning("❌ Jawaban dari Gemini gagal atau error. Tidak disimpan ke Redis.")
        logging.info("✅ Respons dari Gemini berhasil diproses.")
    except Exception as e:
        logging.error(f"❌ Gagal generate jawaban Gemini: {str(e)}")

async def get_theory_from_subbab(req, background_task: BackgroundTasks):
    logging.info("➡️ Memulai proses get_theory_from_subbab")

    # Ambil nama subbab dari input langsung user
    subbab_name = req.queryResult.get("queryText", "").strip()
    logging.info("📥 Nama subbab dari user: '%s'", subbab_name)

    # Ambil jenjang dari context
    level = get_context_param(req.dict(), "pilihpelajaran-followup", "school_level")
    logging.info("🏫 Jenjang pendidikan dari context: '%s'", level)

    # Ambil data subbab dari Firestore
    try:
        subbab_docs = db.collection("sub_bab").where("title", "==", subbab_name).stream()
        subbab_data = next((doc.to_dict() for doc in subbab_docs), None)
        logging.info("🟾 Data subbab ditemukan: %s", subbab_data is not None)

        if not subbab_data:
            logging.warning("❌ Subbab '%s' tidak ditemukan di Firestore", subbab_name)
            return {"fulfillmentText": "Subbab tidak ditemukan."}

        materi = subbab_data.get("content", "")
        if not materi:
            logging.warning("⚠️ Konten 'content' kosong di subbab '%s'", subbab_name)
            return {"fulfillmentText": "Konten materi belum tersedia."}

        # Buat prompt Gemini
        prompt = f"Jelaskan dengan sederhana kepada siswa {level}: {materi}. Berikan 1 contoh soal sederhana juga."
        logging.debug("🧠 Prompt ke Gemini: %s", prompt)
        cache_key = generate_cache_key(level, materi)

        # Panggil Gemini API
        # Cek di Redis
        cached = await redis_client.get(cache_key)
        if cached:
            logging.info("📦 Jawaban diambil dari Redis cache.")
            jawaban = cached
            # Kirim langsung ke user
            return make_response(jawaban)
        
        # Jika belum ada, kirim respon awal
        logging.info("🕐 Jawaban belum tersedia. Kirim respon awal ke Dialogflow.")
        background_task.add_task(generate_and_cache_gemini_answer, prompt, cache_key)
        
        return {
            "fulfillmentText": "🤖 Jawaban sedang diproses... Mohon tunggu sebentar.",
            "outputContexts": [
                {
                    "name": f"{req.session}/contexts/waiting_theory_answer",
                    "lifespanCount": 3,
                    "parameters": {
                        "cache_key": cache_key,
                        "school_level": level,
                        "subbab_name": subbab_name
                    }
                }
            ]
        }

    except Exception as e:
        logging.exception("🔥 Terjadi exception saat ambil teori dari subbab")
        return {"fulfillmentText": f"Terjadi kesalahan: {str(e)}"}
    
