from services.firestore_service import db
import logging

async def handle_subjects_by_level(level: str, req):
    logging.info(f"Mengambil pelajaran untuk jenjang {level}")
    try:
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
        return response
    except Exception as e:
        logging.error(f"Firestore Error: {e}")
        return {"fulfillmentText": "Terjadi kesalahan saat mengambil data pelajaran."}

