from services.firestore_service import db
from services.gemini_service_async import chat_with_gemini_api
from utils.context_helper import get_context_param
from services.redis_client import redis_client
import hashlib
import logging
import time

def generate_cache_key(level: str, materi: str) -> str:
    key_string = f"{level}:{materi}"
    return hashlib.sha256(key_string.encode()).hexdigest()

async def get_theory_from_subbab(req):
    logging.info("➡️ Memulai proses get_theory_from_subbab")
    t0 = time.time()

    # Ambil nama subbab dari input langsung user
    subbab_name = req.queryResult.get("queryText", "").strip()
    logging.info("📥 Nama subbab dari user: '%s'", subbab_name)

    # Ambil jenjang dari context
    level = get_context_param(req.dict(), "pilihpelajaran-followup", "school_level")
    logging.info("🏫 Jenjang pendidikan dari context: '%s'", level)

    # Ambil data subbab dari Firestore
    try:
        t1 = time.time()
        subbab_docs = db.collection("sub_bab").where("title", "==", subbab_name).stream()
        subbab_data = next((doc.to_dict() for doc in subbab_docs), None)
        t2 = time.time()
        logging.info("⏱️ Firestore: %.2f detik", t2 - t1)
        logging.info("🟾 Data subbab ditemukan: %s", subbab_data is not None)

        if not subbab_data:
            logging.warning("❌ Subbab '%s' tidak ditemukan di Firestore", subbab_name)
            logging.info("⏱️ Total waktu: %.2f detik", time.time() - t0)
            return {"fulfillmentText": "Subbab tidak ditemukan."}

        materi = subbab_data.get("content", "")
        if not materi:
            logging.warning("⚠️ Konten 'content' kosong di subbab '%s'", subbab_name)
            logging.info("⏱️ Total waktu: %.2f detik", time.time() - t0)
            return {"fulfillmentText": "Konten materi belum tersedia."}

        # Buat prompt Gemini
        prompt = f"Jelaskan dengan sederhana kepada siswa {level}: {materi}. Berikan 1 contoh soal sederhana juga."
        logging.debug("🧠 Prompt ke Gemini: %s", prompt)
        cache_key = generate_cache_key(level, materi)

        t3 = time.time()
        # Panggil Gemini API
        # Cek di Redis
        cached = await redis_client.get(cache_key)
        if cached:
            logging.info("📦 Jawaban diambil dari Redis cache.")
            jawaban = cached
        else:
            logging.info("💬 Memanggil Gemini karena belum ada cache.")
            jawaban = await chat_with_gemini_api(prompt)
            await redis_client.set(cache_key, jawaban, ex=60 * 60 * 6)  # 6 jam
        t4 = time.time()
        logging.info("⏱️ Gemini: %.2f detik", t4 - t3)
        logging.info("✅ Respons dari Gemini berhasil didapat.")

        # Kirim respons ke user
        chips = [
            {"text": "💬 Tanya Lagi ke AI"},
            {"text": "🏠 Menu Utama"}
        ]

        response = {
            "fulfillmentMessages": [
                {"text": {"text": [f"🤖 Gemini Bot:\n{jawaban}"]}},
                {"text": {"text": ["🤖 Chatbot:\nIngin bertanya lagi, lanjut belajar atau kembali ke menu?:"]}},
                {"payload": {"richContent": [[{"type": "chips", "options": chips}] ]}}
            ]
        }

        logging.info("📤 Mengirim respons teori subbab + chips ke user.")
        logging.info("⏱️ Total waktu: %.2f detik", time.time() - t0)
        return response

    except Exception as e:
        logging.exception("🔥 Terjadi exception saat memproses teori dari subbab:")
        logging.info("⏱️ Total waktu: %.2f detik", time.time() - t0)
        return {"jawabanBot": f"Terjadi kesalahan: {str(e)}"}