from flask import Flask, request, jsonify
from services import gemini_service
from handlers import general, lessons, subject, subbab, custom_question, theory_with_gemini
from utils.dialogflow_token import get_dialogflow_token
from dotenv import load_dotenv
from os import getenv

# Load konfigurasi dari .env
load_dotenv()

# Inisialisasi aplikasi Flask
app = Flask(__name__)

@app.route("/chat-gemini", methods=["POST"])
def chat_with_gemini():
    data = request.get_json()
    user_message = data.get("message", "")
    return jsonify({"reply": gemini_service.chat_with_gemini_api(user_message)})

@app.route("/webhook", methods=["POST"])
def webhook()   :
    req = request.get_json()
    intent = req.get("queryResult", {}).get("intent", {}).get("displayName", "")

    if intent in ["Welcome","Mulai"]:
        return general.handle_welcome()
    elif intent == "Pilih Jenjang SD":
        return subject.handle_subjects_by_level("sd", req)
    elif intent == "Pilih Jenjang SMP":
        return subject.handle_subjects_by_level("smp", req)
    elif intent == "Pilih Jenjang SMA":
        return subject.handle_subjects_by_level("sma", req)
    elif intent == "Pilih Topik Pelajaran":
        return lessons.handle_lessons_by_subject_name_level(req)
    elif intent == "Pilih Subbab":
        return subbab.handle_subbab_by_lessonid(req)
    elif intent == "Pilih Teori Subbab":
        return theory_with_gemini.get_theory_from_subbab(req)
    elif intent == "Tanya Lagi ke AI":
        return custom_question.handle_custom_question(req)
    elif intent == "Menu Utama":
        return general.handle_welcome()

    return jsonify({"fulfillmentText": "Maaf, permintaan tidak dikenali."})

@app.route("/get-dialogflow-token", methods=["GET"])
def dialogflow_token():
    try:
        token = get_dialogflow_token()
        return jsonify({"access_token": token})
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(getenv("PORT", 5000)))
