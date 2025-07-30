from flask import jsonify
from services.firestore_service import db
import logging

def handle_subjects_by_level(level, req):
    logging.info("Mengambil pelajaran untuk jenjang %s", level)
    try:
        docs = db.collection("subjects").where("schoolLevel", "==", level).stream()

        chips = []
        for doc in docs:
            data = doc.to_dict()
            nama_materi = data.get("name")
            if nama_materi:
                logging.debug("Ditemukan pelajaran: %s", nama_materi)
                chips.append({"text": nama_materi})

        if not chips:
            return jsonify({"fulfillmentText": f"Belum ada pelajaran untuk jenjang {level.upper()}."})

        response = {
            "fulfillmentMessages": [
                {"text": {"text": [f"Berikut pelajaran untuk jenjang {level.upper()} yang tersedia:"]}},
                {"payload": {"richContent": [[{"type": "chips", "options": chips}]]}}
            ],
            "outputContexts": [
                {
                    "name": f"{req['session']}/contexts/pilihjenjang-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "school_level": level
                    }
                }
            ]
        }

        return jsonify(response)
    except Exception as e:
        print("Firestore Error:", e)
        return jsonify({"fulfillmentText": "Terjadi kesalahan saat mengambil data pelajaran."})

