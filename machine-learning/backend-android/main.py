from flask import Flask, request, jsonify
from google.cloud import firestore
from google.oauth2 import service_account
import google.auth.transport.requests
import logging
from os import getenv

logging.basicConfig(level=logging.DEBUG, format="%(asctime)s [%(levelname)s] %(message)s")

app = Flask(__name__)

# Inisialisasi Firestore
db = firestore.Client()

@app.route("/get-dialogflow-token", methods=["GET"])
def get_dialogflow_token():
    # Path ke file service account JSON
    SERVICE_ACCOUNT_FILE = "credentials.json"
    SCOPES = ["https://www.googleapis.com/auth/cloud-platform"]

    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, scopes=SCOPES
    )
    request = google.auth.transport.requests.Request()
    credentials.refresh(request)
    access_token = credentials.token

    return jsonify({"access_token": access_token})

@app.route("/webhook", methods=["POST"])
def webhook():
    req = request.get_json()
    logging.debug("REQ JSON: %s", req)
    intent = req.get("queryResult", {}).get("intent", {}).get("displayName", "")
    logging.info("INTENT DITERIMA: %s", intent)

    if intent == "Welcome" or intent == "Mulai":
        return handle_welcome()
    elif intent == "Pilih Jenjang SD":
        return handle_subjects_by_level("sd", req)
    elif intent == "Pilih Jenjang SMP":
        return handle_subjects_by_level("smp", req)
    elif intent == "Pilih Jenjang SMA":
        return handle_subjects_by_level("sma", req)
    elif intent == "Pilih Topik Pelajaran":
        return handle_lessons_by_subject_name_level(req)
    elif intent == "Pilih Subbab":
        return handle_subbab_by_lessonid(req)


    return jsonify({"fulfillmentText": "Maaf, permintaan tidak dikenali."})

def get_context_param(req, context_name, param_key):
    contexts = req["queryResult"].get("outputContexts", [])
    for ctx in contexts:
        if context_name in ctx["name"]:
            return ctx.get("parameters", {}).get(param_key)
    return None

def handle_welcome():
    logging.info("Menampilkan pesan Welcome dan pilihan jenjang")
    chips = [
        {"text": "Jenjang SD"},
        {"text": "Jenjang SMP"},
        {"text": "Jenjang SMA"}
    ]
    response = {
        "fulfillmentMessages": [
            {"text": {"text": ["👋 Halo! Selamat datang di LearnAble! 📚 Yuk mulai petualangan belajarmu bersama kami. Silakan pilih jenjang pendidikan di bawah ini:"]}},
            {"payload": {"richContent": [[{"type": "chips", "options": chips}]]}}
        ]
    }
    return jsonify(response)
    
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


def handle_lessons_by_subject_name_level(req):
    parameters = req["queryResult"].get("parameters", {})
    subject_name = parameters.get("subject_name")  # e.g. "Matematika"

   # Ambil jenjang dari context
    level = get_context_param(req, "pilihjenjang-followup", "school_level")

    logging.info("Mencari materi untuk subject: %s, level: %s", subject_name, level)

    if not subject_name or not level:
        return jsonify({"fulfillmentText": "Mohon pilih pelajaran dan jenjang terlebih dahulu."})
    
    try:
        # Cari subject berdasarkan name + schoolLevel
        subject_query = db.collection("subjects") \
                        .where("name", "==", subject_name) \
                        .where("schoolLevel", "==", level) \
                        .limit(1).stream()

        subject_doc = next(subject_query, None)
        if not subject_doc:
            logging.warning("Pelajaran %s tidak ditemukan untuk level %s", subject_name, level)
            # Fallback: tampilkan ulang semua subject untuk jenjang ini
            fallback_subjects = db.collection("subjects").where("schoolLevel", "==", level).stream()
            fallback_chips = [{"text": doc.to_dict().get("name")} for doc in fallback_subjects]

            return jsonify({
                "fulfillmentMessages": [
                    {
                        "text": {
                            "text": [
                                f"❗ Pelajaran '{subject_name}' tidak ditemukan untuk jenjang {level.upper()}.\nBerikut pelajaran yang tersedia:"
                            ]
                        }
                    },
                    {
                        "payload": {
                            "richContent": [
                                [
                                    {
                                        "type": "chips",
                                        "options": fallback_chips
                                    }
                                ]
                            ]
                        }
                    }
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
            })

        subject_data = subject_doc.to_dict()
        subject_id = subject_data.get("idSubject")
        logging.debug("subject_id = %s", subject_id)

        # Filter lesson berdasarkan idSubject
        lesson_query = db.collection("lessons").where("idSubject", "==", subject_id)

        lessons = lesson_query.stream()

        chips = []
        for doc in lessons:
            title = doc.to_dict().get("title")
            if title:
                logging.debug("Ditemukan materi: %s", title)
                chips.append({"text": title})

        if not chips:
            return jsonify({"fulfillmentText": f"Belum ada materi untuk {subject_name} jenjang {level.upper()}."})

        response = {
            "fulfillmentMessages": [
                {"text": {"text": [f"Materi untuk {subject_name} jenjang {level.upper()}:"]}},
                {
                    "payload": {
                        "richContent": [
                            [
                                {
                                    "type": "chips",
                                    "options": chips
                                }
                            ]
                        ]
                    }
                }
            ],
            "outputContexts": [
                {
                    "name": f"{req['session']}/contexts/pilihpelajaran-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "subject_name": subject_name,
                        "school_level": level
                    }
                }
            ]
        }

        return jsonify(response)
    except Exception as e:
        print("Firestore Error:", e)
        return jsonify({"fulfillmentText": "Terjadi kesalahan saat mengambil materi."})

def handle_subbab_by_lessonid(req):
    parameters = req["queryResult"].get("parameters", {})
    lesson_name = parameters.get("lesson_name") 

    # Ambil jenjang dan subject dari context sebelumnya
    level = get_context_param(req, "pilihpelajaran-followup", "school_level")
    subject_name = get_context_param(req, "pilihpelajaran-followup", "subject_name")

    logging.info("Mencari sub-bab untuk lesson: %s", lesson_name)
    logging.info("Mencari sub-bab untuk level: %s", level)
    logging.info("Mencari sub-bab untuk subject: %s", subject_name)

    if not lesson_name or not subject_name or not level:
        return jsonify({"fulfillmentText": "Mohon pilih nama materi terlebih dahulu."})

    try:
         # Cari lesson berdasarkan title, subject, dan level
        subject_query = db.collection("subjects") \
            .where("name", "==", subject_name) \
            .where("schoolLevel", "==", level).limit(1).stream()
        subject_doc = next(subject_query, None)
        if not subject_doc:
            return jsonify({"fulfillmentText": "Pelajaran tidak ditemukan."})

        subject_id = subject_doc.to_dict().get("idSubject")

         # Cari lesson dengan judul dan idSubject
        lesson_query = db.collection("lessons") \
            .where("title", "==", lesson_name) \
            .where("idSubject", "==", subject_id) \
            .limit(1).stream()
        lesson_doc = next(lesson_query, None)

        if not lesson_doc:
            logging.warning("Materi %s tidak ditemukan", lesson_name)
             # Fallback jika lesson tidak ditemukan → tampilkan daftar materi
            fallback_lessons = db.collection("lessons").where("idSubject", "==", subject_id).stream()
            chips = [{"text": doc.to_dict().get("title")} for doc in fallback_lessons]

            return jsonify({
                "fulfillmentMessages": [
                    {
                        "text": {
                            "text": [
                                f"❗ Materi '{lesson_name}' tidak ditemukan untuk pelajaran {subject_name}.\nSilakan pilih materi yang tersedia berikut ini:"
                            ]
                        }
                    },
                    {
                        "payload": {
                            "richContent": [
                                [
                                    {
                                        "type": "chips",
                                        "options": chips
                                    }
                                ]
                            ]
                        }
                    }
                ],
                "outputContexts": [
                    {
                        "name": f"{req['session']}/contexts/pilihpelajaran-followup",
                        "lifespanCount": 5,
                        "parameters": {
                            "school_level": level,
                            "subject_name": subject_name
                        }
                    }
                ]
            })

        lesson_id = lesson_doc.id
        logging.debug("lesson_id = %s", lesson_id)

        # Cari semua subbab yang punya lessonId sesuai
        subbab_query = db.collection("sub_bab").where("lessonId", "==", lesson_id).stream()

        chips = []
        for doc in subbab_query:
            data = doc.to_dict()
            title = data.get("title")
            if title:
                logging.debug("Ditemukan sub-bab: %s", title)
                chips.append({"text": title})

        if not chips:
            return jsonify({"fulfillmentText": f"Belum ada sub-bab untuk materi {lesson_name}."})

        response = {
            "fulfillmentMessages": [
                {"text": {"text": [f"Berikut sub-bab dari {lesson_name}:"]}},
                {
                    "payload": {
                        "richContent": [
                            [
                                {
                                    "type": "chips",
                                    "options": chips
                                }
                            ]
                        ]
                    }
                }
            ],
            "outputContexts": [
                {
                    "name": f"{req['session']}/contexts/pilihsubbab-followup",
                    "lifespanCount": 5,
                    "parameters": {
                        "school_level": level,
                        "subject_name": subject_name,
                        "lesson_name": lesson_name
                    }
                }
            ]
        }
        return jsonify(response)
    except Exception as e:
        print("Firestore Error:", e)
        return jsonify({"fulfillmentText": "Terjadi kesalahan saat mengambil sub-bab."})

if __name__ == "__main__":
    from os import getenv
    app.run(host="0.0.0.0", port=int(getenv("PORT", 5000)))
