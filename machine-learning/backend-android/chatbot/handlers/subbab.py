from chatbot.services.firestore_service import db
from chatbot.utils.context_helper import get_context_param, get_previous_chips
from chatbot.services.redis_client import redis_client
import json
import logging

async def handle_subbab_by_lessonid(req):
    lesson_name = req.queryResult.get("queryText", "").strip()
    level = get_context_param(req.dict(), "pilihpelajaran-followup", "school_level")
    subject_name = get_context_param(req.dict(), "pilihpelajaran-followup", "subject_name")
    logging.info(f"Mencari sub-bab untuk lesson: {lesson_name}")
    logging.info(f"Level: {level} | Subject: {subject_name}")

    if not lesson_name or not subject_name or not level:
        # Kembali ke home (level chips)
        logging.info("⚠️ Validasi gagal, kembali ke home")
        chips = [
            {"text": "Jenjang SD"},
            {"text": "Jenjang SMP"},
            {"text": "Jenjang SMA"}
        ]
        return {
            "fulfillmentMessages": [{
                "text": {
                    "text": [
                        "❗ Validasi gagal.\n👋 Silakan pilih jenjang pendidikan:"
                    ]
                }
            }, {
                "payload": {
                    "richContent": [[{
                        "type": "chips",
                        "options": chips
                    }]]
                }
            }]
        }
    
    cache_key = f"subbab:{level}:{subject_name}:{lesson_name}"

    try:
        cached = await redis_client.get(cache_key)
        if cached:
            logging.info("📦 Mengambil data dari Redis cache.")
            return json.loads(cached)
        
        subject_query = db.collection("subjects") \
            .where("name", "==", subject_name) \
            .where("schoolLevel", "==", level) \
            .limit(1).stream()
        subject_doc = next(subject_query, None)
        if not subject_doc:
            # Kembali ke chip sebelumnya (subject chips)
            logging.info("⚠️ Pelajaran tidak ditemukan, kembali ke chip subject sebelumnya")
            previous = await get_previous_chips(req, db)
            if previous:
                response = {
                    "fulfillmentMessages": [{
                        "text": {
                            "text": [
                                f"❗ Pelajaran tidak ditemukan.\n{previous['message']}"
                            ]
                        }
                    }, {
                        "payload": {
                            "richContent": [[{
                                "type": "chips",
                                "options": previous["chips"]
                            }]]
                        }
                    }]
                }
                if previous["context_name"]:
                    response["outputContexts"] = [{
                        "name": previous["context_name"],
                        "lifespanCount": 5,
                        "parameters": previous["context_params"]
                    }]
                return response
            return {"fulfillmentText": "Pelajaran tidak ditemukan."}
        subject_id = subject_doc.to_dict().get("idSubject")
        lesson_query = db.collection("lessons") \
            .where("title", "==", lesson_name) \
            .where("idSubject", "==", subject_id) \
            .limit(1).stream()
        lesson_doc = next(lesson_query, None)
        if not lesson_doc:
            logging.warning(f"Materi '{lesson_name}' tidak ditemukan")
            fallback_lessons = db.collection("lessons").where(
                "idSubject", "==", subject_id).stream()
            chips = [{
                "text": doc.to_dict().get("title")
            } for doc in fallback_lessons]
            return {
                "fulfillmentMessages": [{
                    "text": {
                        "text": [
                            f"❗ Materi '{lesson_name}' tidak ditemukan untuk pelajaran {subject_name}.\nSilakan pilih materi yang tersedia berikut ini:"
                        ]
                    }
                }, {
                    "payload": {
                        "richContent": [[{
                            "type": "chips",
                            "options": chips
                        }]]
                    }
                }],
                "outputContexts": [{
                    "name":
                    f"{req.session}/contexts/pilihpelajaran-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "school_level": level,
                        "subject_name": subject_name
                    }
                }]
            }
        lesson_id = lesson_doc.id
        logging.debug(f"lesson_id = {lesson_id}")
        subbab_query = db.collection("sub_bab").where("lessonId", "==", lesson_id).stream()
        chips = []
        for doc in subbab_query:
            data = doc.to_dict()
            title = data.get("title")
            if title:
                logging.debug(f"Ditemukan sub-bab: {title}")
                chips.append({"text": title})
        if not chips:
            # Kembali ke chip sebelumnya (lesson chips)
            logging.info("⚠️ Tidak ada sub-bab, kembali ke chip lesson sebelumnya")
            previous = await get_previous_chips(req, db)
            if previous:
                response = {
                    "fulfillmentMessages": [{
                        "text": {
                            "text": [
                                f"❗ Belum ada sub-bab untuk materi {lesson_name}.\n{previous['message']}"
                            ]
                        }
                    }, {
                        "payload": {
                            "richContent": [[{
                                "type": "chips",
                                "options": previous["chips"]
                            }]]
                        }
                    }]
                }
                if previous["context_name"]:
                    response["outputContexts"] = [{
                        "name": previous["context_name"],
                        "lifespanCount": 5,
                        "parameters": previous["context_params"]
                    }]
                return response
            return {
                "fulfillmentText":
                f"Belum ada sub-bab untuk materi {lesson_name}."
            }
        response = {
            "fulfillmentMessages": [{
                "text": {
                    "text": [f"Berikut sub-bab dari {lesson_name}:"]
                }
            }, {
                "payload": {
                    "richContent": [[{
                        "type": "chips",
                        "options": chips
                    }]]
                }
            }],
            "outputContexts": [{
                "name": f"{req.session}/contexts/pilihsubbab-followup",
                "lifespanCount": 5,
                "parameters": {
                    "school_level": level,
                    "subject_name": subject_name,
                    "lesson_name": lesson_name
                }
            }]
        }
        
        # Simpan hasil ke Redis (expire dalam 1 jam)
        await redis_client.set(cache_key, json.dumps(response), ex=3600)
        logging.info("🧠 Data subbab disimpan ke Redis.")
        return response
    except Exception as e:
        logging.error(f"Firestore Error: {e}")
        # Kembali ke chip sebelumnya (lesson chips)
        logging.info("⚠️ Error terjadi, kembali ke chip lesson sebelumnya")
        previous = await get_previous_chips(req, db)
        if previous:
            response = {
                "fulfillmentMessages": [{
                    "text": {
                        "text": [
                            f"❗ Terjadi kesalahan saat mengambil sub-bab.\n{previous['message']}"
                        ]
                    }
                }, {
                    "payload": {
                        "richContent": [[{
                            "type": "chips",
                            "options": previous["chips"]
                        }]]
                    }
                }]
            }
            if previous["context_name"]:
                response["outputContexts"] = [{
                    "name": previous["context_name"],
                    "lifespanCount": 5,
                    "parameters": previous["context_params"]
                }]
            return response
        return {"fulfillmentText": "Terjadi kesalahan saat mengambil sub-bab."}
