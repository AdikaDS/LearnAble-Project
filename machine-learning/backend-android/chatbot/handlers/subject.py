from chatbot.services.firestore_service import db
from chatbot.services.redis_client import redis_client
import json
import logging

async def handle_subjects_by_level(level: str, req):
    logging.info(f"Mengambil pelajaran untuk jenjang {level}")
    cache_key = f"subjects:{level}"
    
    # Validasi input
    if not level or level not in ["sd", "smp", "sma"]:
        return {"fulfillmentText": "Jenjang pendidikan tidak valid."}
    
    # Validasi koneksi database
    if not db:
        return {"fulfillmentText": "Terjadi kesalahan koneksi database."}
    
    try:
        # 🔍 Cek apakah data sudah ada di Redis
        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                logging.info("📦 Mengambil data dari Redis cache.")
                return json.loads(cached)
    
        docs = db.collection("subjects").where("schoolLevel", "==", level).stream()
        chips = []
        for doc in docs:
            data = doc.to_dict()
            nama_materi = data.get("name")
            if nama_materi:
                logging.debug(f"Ditemukan pelajaran: {nama_materi}")
                chips.append({"text": nama_materi})

        if not chips:
            return {"fulfillmentText": f"Belum ada pelajaran untuk jenjang {level.upper()}."}
        response = {
            "fulfillmentMessages": [
                {"text": {"text": [f"Berikut pelajaran untuk jenjang {level.upper()} yang tersedia:"]}},
                {"payload": {"richContent": [[{"type": "chips", "options": chips}]]}}
            ],
            "outputContexts": [
                {
                    "name": f"{req.session}/contexts/pilihjenjang-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "school_level": level
                    }
                }
            ]
        }
        if redis_client:
            await redis_client.set(cache_key, json.dumps(response), ex=3600)  # Simpan ke Redis dengan expire 1 jam
            logging.info("🧠 Data pelajaran disimpan ke Redis.")
        return response
    except Exception as e:
        logging.error(f"Firestore Error: {e}")
        return {"fulfillmentText": "Terjadi kesalahan saat mengambil data pelajaran."}

