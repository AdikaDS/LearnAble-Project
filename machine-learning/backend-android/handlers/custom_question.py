from services.gemini_service_async import chat_with_gemini_api

async def handle_custom_question(req):
    user_question = req.queryResult.get("queryText", "").strip()
    chips = [
        {"text": "💬 Tanya Lagi ke AI"},
        {"text": "🏠 Kembali ke Menu"}
    ]
    if not user_question:
        return {
            "fulfillmentText": "❗ Pertanyaan tidak boleh kosong."
        }
    try:
        jawaban = await chat_with_gemini_api(user_question)
        return {
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
        }
    except Exception as e:
        return {
            "fulfillmentText": f"Terjadi kesalahan: {str(e)}"
        }
