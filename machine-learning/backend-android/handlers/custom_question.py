from flask import jsonify

from services.gemini_service import chat_with_gemini_api

def handle_custom_question(req):
    # Ambil pertanyaan dari user
    user_question = req.get("queryResult", {}).get("queryText", "").strip()

    chips = [{
        "text": "💬 Tanya Lagi ke AI"
    }, {
        "text": "🏠 Kembali ke Menu"
    }],

    # Validasi input
    if not user_question:
        return jsonify({
            "fulfillmentText": "❗ Pertanyaan tidak boleh kosong."
        })

    try:
        # Kirim ke Gemini API
        jawaban = chat_with_gemini_api(user_question)

        return jsonify({
            "fulfillmentMessages": [
                {
                    "text": {
                        "text": [f"🤖 Gemini Bot:\n{jawaban}"]
                    }
                },
                {
                    "text": {
                        "text": ["Ingin bertanya lagi atau kembali ke menu?"]
                    }
                },
                {
                    "payload": {
                        "richContent": [[{
                        "type": "chips",
                        "options": chips
                    }]]
                    }
                }
            ]
        })

    except Exception as e:
        return jsonify({
            "fulfillmentText": f"Terjadi kesalahan: {str(e)}"
        }), 500
