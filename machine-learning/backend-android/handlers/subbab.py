from flask import jsonify
from services.firestore_service import db
from utils.context_helper import get_context_param
import logging


def handle_subbab_by_lessonid(req):
    # Ambil nama materi (lesson) dari input langsung user
    lesson_name = req.get("queryResult", {}).get("queryText", "").strip()

    # Ambil jenjang dan subject dari context sebelumnya
    level = get_context_param(req, "pilihpelajaran-followup", "school_level")
    subject_name = get_context_param(req, "pilihpelajaran-followup",
                                     "subject_name")

    logging.info("Mencari sub-bab untuk lesson: %s", lesson_name)
    logging.info("Level: %s | Subject: %s", level, subject_name)

    if not lesson_name or not subject_name or not level:
        return jsonify(
            {"fulfillmentText": "Mohon pilih nama materi terlebih dahulu."})

    try:
        # Cari subject untuk mendapatkan idSubject
        subject_query = db.collection("subjects") \
            .where("name", "==", subject_name) \
            .where("schoolLevel", "==", level) \
            .limit(1).stream()
        subject_doc = next(subject_query, None)
        if not subject_doc:
            return jsonify({"fulfillmentText": "Pelajaran tidak ditemukan."})

        subject_id = subject_doc.to_dict().get("idSubject")

        # Cari lesson berdasarkan title dan idSubject
        lesson_query = db.collection("lessons") \
            .where("title", "==", lesson_name) \
            .where("idSubject", "==", subject_id) \
            .limit(1).stream()
        lesson_doc = next(lesson_query, None)

        if not lesson_doc:
            logging.warning("Materi '%s' tidak ditemukan", lesson_name)
            # Fallback → tampilkan daftar materi yang tersedia
            fallback_lessons = db.collection("lessons").where(
                "idSubject", "==", subject_id).stream()
            chips = [{
                "text": doc.to_dict().get("title")
            } for doc in fallback_lessons]

            return jsonify({
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
                    f"{req['session']}/contexts/pilihpelajaran-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "school_level": level,
                        "subject_name": subject_name
                    }
                }]
            })

        lesson_id = lesson_doc.id
        logging.debug("lesson_id = %s", lesson_id)

        # Ambil semua sub-bab berdasarkan lessonId
        subbab_query = db.collection("sub_bab").where("lessonId", "==",
                                                      lesson_id).stream()

        chips = []
        for doc in subbab_query:
            data = doc.to_dict()
            title = data.get("title")
            if title:
                logging.debug("Ditemukan sub-bab: %s", title)
                chips.append({"text": title})

        if not chips:
            return jsonify({
                "fulfillmentText":
                f"Belum ada sub-bab untuk materi {lesson_name}."
            })

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
                "name": f"{req['session']}/contexts/pilihsubbab-followup",
                "lifespanCount": 5,
                "parameters": {
                    "school_level": level,
                    "subject_name": subject_name,
                    "lesson_name": lesson_name
                }
            }]
        }
        return jsonify(response)
    except Exception as e:
        logging.error("Firestore Error: %s", e)
        return jsonify(
            {"fulfillmentText": "Terjadi kesalahan saat mengambil sub-bab."})
