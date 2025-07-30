from utils.context_helper import get_context_param
from services.firestore_service import db
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
    try:
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
        return response
    except Exception as e:
        logging.error(f"Firestore Error: {e}")
        return {
            "fulfillmentText": "Terjadi kesalahan saat mengambil materi."
        }
