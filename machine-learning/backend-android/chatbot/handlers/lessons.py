from chatbot.utils.context_helper import get_context_param, get_previous_chips
from chatbot.services.firestore_service import db
from chatbot.services.redis_client import redis_client
import json
import logging

async def handle_lessons_by_subject_name_level(req):
    subject_name = req.queryResult.get("queryText", "").strip()
    level = get_context_param(req.dict(), "pilihjenjang-followup", "school_level")
    logging.info(f"Mencari materi untuk subject: {subject_name}, level: {level}")
    if not subject_name or not level:
        chips = [
            {"text": "Jenjang SD"},
            {"text": "Jenjang SMP"},
            {"text": "Jenjang SMA"}
        ]
        return {
            "fulfillmentMessages": [{
                "text": {
                    "text": [
                        "👋 Silakan pilih ulang jenjang pendidikan terlebih dahulu:"
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
                "name": f"{req.session}/contexts/pilihjenjang-followup",
                "lifespanCount": 5
            }]
        }
    
    # Buat cache key Redis
    cache_key = f"lessons:{level}:{subject_name}"

    try:
        # Cek cache Redis
        cached = await redis_client.get(cache_key)
        if cached:
            logging.info("✅ Materi ditemukan di Redis")
            return json.loads(cached)
        
        subject_query = db.collection("subjects") \
                        .where("name", "==", subject_name) \
                        .where("schoolLevel", "==", level) \
                        .limit(1).stream()
        subject_doc = next(subject_query, None)
        if not subject_doc:
            logging.warning(f"Pelajaran {subject_name} tidak ditemukan untuk level {level}")
            fallback_subjects = db.collection("subjects").where(
                "schoolLevel", "==", level).stream()
            fallback_chips = [{
                "text": doc.to_dict().get("name")
            } for doc in fallback_subjects]
            return {
                "fulfillmentMessages": [{
                    "text": {
                        "text": [
                            f"❗ Pelajaran '{subject_name}' tidak ditemukan untuk jenjang {level.upper()}.\nBerikut pelajaran yang tersedia:"
                        ]
                    }
                }, {
                    "payload": {
                        "richContent": [[{
                            "type": "chips",
                            "options": fallback_chips
                        }]]
                    }
                }],
                "outputContexts": [{
                    "name": f"{req.session}/contexts/pilihjenjang-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "school_level": level
                    }
                }]
            }
        subject_data = subject_doc.to_dict()
        subject_id = subject_data.get("idSubject")
        logging.debug(f"subject_id = {subject_id}")
        lesson_query = db.collection("lessons").where("idSubject", "==", subject_id).stream()
        chips = []
        for doc in lesson_query:
            title = doc.to_dict().get("title")
            if title:
                logging.debug(f"Ditemukan materi: {title}")
                chips.append({"text": title})
        if not chips:
            # Kembali ke chip sebelumnya (subject chips)
            logging.info("⚠️ Tidak ada materi, kembali ke chip subject sebelumnya")
            previous = await get_previous_chips(req, db)
            if previous:
                response = {
                    "fulfillmentMessages": [{
                        "text": {
                            "text": [
                                f"❗ Belum ada materi untuk {subject_name} jenjang {level.upper()}.\n{previous['message']}"
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
                f"Belum ada materi untuk {subject_name} jenjang {level.upper()}."
            }
        response = {
            "fulfillmentMessages": [{
                "text": {
                    "text":
                    [f"Materi untuk {subject_name} jenjang {level.upper()}:"]
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
                "name": f"{req.session}/contexts/pilihpelajaran-followup",
                "lifespanCount": 5,
                "parameters": {
                    "subject_name": subject_name,
                    "school_level": level
                }
            }]
        }
         # Simpan ke Redis (expire 1 jam)
        await redis_client.set(cache_key, json.dumps(response), ex=3600)
        logging.info("🧠 Data materi disimpan ke Redis.")
        return response
    except Exception as e:
        logging.error(f"Firestore Error: {e}")
        # Kembali ke chip sebelumnya (subject chips)
        logging.info("⚠️ Error terjadi, kembali ke chip subject sebelumnya")
        previous = await get_previous_chips(req, db)
        if previous:
            response = {
                "fulfillmentMessages": [{
                    "text": {
                        "text": [
                            f"❗ Terjadi kesalahan saat mengambil materi.\n{previous['message']}"
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
            "fulfillmentText": "Terjadi kesalahan saat mengambil materi."
        }
