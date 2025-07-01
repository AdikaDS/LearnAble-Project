from flask import Flask, request, jsonify
from google.cloud import firestore

app = Flask(__name__)

# Inisialisasi Firestore
db = firestore.Client()

@app.route("/webhook", methods=["POST"])
def webhook():
    req = request.get_json()
    intent = req["queryResult"]["intent"]["displayName"]

    if intent == "Tanya Disabilitas":
        return handle_disability_selection(req)
    elif intent == "Pilih Jenjang SD":
        return handle_subjects_by_level(req, "sd")
    elif intent == "Pilih Jenjang SMP":
        return handle_subjects_by_level(req, "smp")
    elif intent == "Pilih Jenjang SMA":
        return handle_subjects_by_level(req, "sma")
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

def handle_disability_selection(req):
    parameters = req["queryResult"]["parameters"]
    disability = parameters.get("disability_type")

    if not disability:
        return jsonify({"fulfillmentText": "Tolong pilih jenis disabilitas kamu."})
    
    response = {
        "fulfillmentText": f"Kamu memilih disabilitas {disability}. Sekarang, pilih jenjang pendidikan:",
        "outputContexts": [
            {
                "name": f"{req['session']}/contexts/disability_context",
                "lifespanCount": 20,
                "parameters": {
                    "disability_type": disability
                }
            }
        ],
        "payload": {
            "richContent": [
                [
                    {
                        "type": "chips",
                        "options": [
                            {"text": "SD"},
                            {"text": "SMP"},
                            {"text": "SMA"}
                        ]
                    }
                ]
            ]
        }
    }
    return jsonify(response)

def handle_subjects_by_level(req, level):
    disability = get_context_param(req, "disability_context", "disability_type")
    
    docs = db.collection("subjects").where("schoolLevel", "==", level).stream()

    chips = []
    for doc in docs:
        data = doc.to_dict()
        nama_materi = data.get("name")
        if nama_materi:
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
                "name": f"{req['session']}/contexts/disability_context",
                "lifespanCount": 20,
                "parameters": {
                    "disability_type": disability
                }
            }
        ]
    }

    return jsonify(response)

def handle_lessons_by_subject_name_level(req):
    parameters = req["queryResult"]["parameters"]
    subject_name = parameters.get("subject_name")  # e.g. "Matematika"
    level = parameters.get("school_level")         # e.g. "sd"
    disability = get_context_param(req, "disability_context", "disability_type")

    if not subject_name or not level:
        return jsonify({"fulfillmentText": "Mohon pilih topik dan jenjang terlebih dahulu."})

    # Cari subject berdasarkan name + schoolLevel
    subject_query = db.collection("subjects") \
                      .where("name", "==", subject_name) \
                      .where("schoolLevel", "==", level) \
                      .limit(1).stream()

    subject_doc = next(subject_query, None)
    if not subject_doc:
        return jsonify({"fulfillmentText": f"Tidak ditemukan pelajaran {subject_name} untuk jenjang {level.upper()}."})

    subject_data = subject_doc.to_dict()
    subject_id = subject_data.get("idSubject")

    # Filter lesson berdasarkan idSubject dan disability
    lesson_query = db.collection("lessons").where("idSubject", "==", subject_id)

    if disability:
        lesson_query = lesson_query.where("disabilityTypes", "array_contains", disability)

    lessons = lesson_query.stream()

    chips = []
    for doc in lessons:
        title = doc.to_dict().get("title")
        if title:
            chips.append({"text": title})

    if not chips:
        return jsonify({"fulfillmentText": f"Belum ada materi untuk {subject_name} jenjang {level.upper()}."})

    response = {
        "fulfillmentMessages": [
            {"text": {"text": [f"Materi untuk {subject_name} jenjang {level.upper()} untuk {disability}:"]}},
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
        ]
    }

    return jsonify(response)

def handle_subbab_by_lessonid(req):
    parameters = req["queryResult"]["parameters"]
    lesson_name = parameters.get("lesson_name") 

    if not lesson_name:
        return jsonify({"fulfillmentText": "Mohon pilih nama materi terlebih dahulu."})

    # Cari ID lesson berdasarkan title
    lesson_query = db.collection("lessons").where("title", "==", lesson_name).limit(1).stream()
    lesson_doc = next(lesson_query, None)

    if not lesson_doc:
        return jsonify({"fulfillmentText": f"Materi '{lesson_name}' tidak ditemukan."})

    lesson_id = lesson_doc.id

    # Cari semua subbab yang punya lessonId sesuai
    subbab_query = db.collection("sub_bab").where("lessonId", "==", lesson_id).stream()

    chips = []
    for doc in subbab_query:
        data = doc.to_dict()
        title = data.get("title")
        if title:
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
        ]
    }
    return jsonify(response)


if __name__ == "__main__":
    from os import getenv
    app.run(host="0.0.0.0", port=int(getenv("PORT", 5000)))
