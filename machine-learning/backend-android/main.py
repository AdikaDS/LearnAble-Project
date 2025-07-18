from flask import Flask, request, jsonify
from google.cloud import firestore

app = Flask(__name__)

# Inisialisasi Firestore
db = firestore.Client()

@app.route("/webhook", methods=["POST"])
def webhook():
    req = request.get_json()
    intent = req.get("queryResult", {}).get("intent", {}).get("displayName", "")

    if intent == "Welcome" or intent == "Mulai":
        return handle_welcome
    elif intent == "Pilih Jenjang SD":
        return handle_subjects_by_level("sd")
    elif intent == "Pilih Jenjang SMP":
        return handle_subjects_by_level("smp")
    elif intent == "Pilih Jenjang SMA":
        return handle_subjects_by_level("sma")
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
    chips = [
        {"text": "Pilih Jenjang SD"},
        {"text": "Pilih Jenjang SMP"},
        {"text": "Pilih Jenjang SMA"}
    ]
    response = {
        "fulfillmentMessages": [
            {"text": {"text": ["Halo! Selamat datang di LearnAble. Silakan pilih jenjang pendidikan:"]}},
            {"payload": {"richContent": [[{"type": "chips", "options": chips}]]}}
        ]
    }
    return jsonify(response)
    
def handle_subjects_by_level(level):
    try:
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
            return jsonify({"fulfillmentText": f"Tidak ditemukan pelajaran {subject_name} untuk jenjang {level.upper()}."})

        subject_data = subject_doc.to_dict()
        subject_id = subject_data.get("idSubject")

        # Filter lesson berdasarkan idSubject
        lesson_query = db.collection("lessons").where("idSubject", "==", subject_id)

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
            ]
        }

        return jsonify(response)
    except Exception as e:
        print("Firestore Error:", e)
        return jsonify({"fulfillmentText": "Terjadi kesalahan saat mengambil materi."})

def handle_subbab_by_lessonid(req):
    parameters = req["queryResult"].get("parameters", {})
    lesson_name = parameters.get("lesson_name") 

    if not lesson_name:
        return jsonify({"fulfillmentText": "Mohon pilih nama materi terlebih dahulu."})

    try:
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
    except Exception as e:
        print("Firestore Error:", e)
        return jsonify({"fulfillmentText": "Terjadi kesalahan saat mengambil sub-bab."})

if __name__ == "__main__":
    from os import getenv
    app.run(host="0.0.0.0", port=int(getenv("PORT", 5000)))
