from flask import jsonify

from services.firestore_service import db
from services.gemini_service import chat_with_gemini_api
from utils.context_helper import get_context_param


def get_theory_from_subbab(req):
    # Ambil nama subbab dari input langsung user
    subbab_name = req.get("queryResult", {}).get("queryText", "").strip()
    # Ambil jenjang dari context
    level = get_context_param(req, "pilihpelajaran-followup", "school_level")

    subbab_docs = db.collection("sub_bab").where("title", "==",subbab_name).stream()

    subbab_data = None
    for doc in subbab_docs:
        subbab_data = doc.to_dict()
        break

    if not subbab_data:
        return jsonify({"fulfillmentText": "Subbab tidak ditemukan."})

    materi = subbab_data.get("content", "")
    if not materi:
        return jsonify({"fulfillmentText": "Konten materi belum tersedia."})

    try:
        # Buat prompt Gemini
        prompt = f"Jelaskan dengan sederhana kepada siswa {level}: {materi}. Berikan 1 contoh soal sederhana juga."

        jawaban = chat_with_gemini_api(prompt)
        
        chips = [
        {
        "text": "💬 Tanya Lagi ke AI"
        }, {
        "text": "🏠 Menu Utama"
        }]

        response = {
            "fulfillmentMessages": [{
                "text": {
                    "text": [f"🤖 Gemini Bot Response:\n{jawaban}:"]
                }
            }, {
                "text": {
                    "text": ["🤖 Chatbot (lanjutan):\nIngin bertanya lagi, lanjut belajar atau kembali ke menu?:"]
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
        return jsonify(response)

    except Exception as e:
        return jsonify({"jawabanBot": f"Terjadi kesalahan: {str(e)}"}), 500
