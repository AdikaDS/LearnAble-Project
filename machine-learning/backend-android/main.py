from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel
import logging
from handlers.theory_with_gemini import get_theory_from_subbab
from handlers.subject import handle_subjects_by_level
from handlers.lessons import handle_lessons_by_subject_name_level
from handlers.subbab import handle_subbab_by_lessonid
from handlers.general import handle_welcome
from handlers.custom_question import handle_custom_question
from services.redis_client import redis_client
from utils.dialogflow_token import get_dialogflow_token

app = FastAPI()
logging.basicConfig(level=logging.INFO)

class DialogflowRequest(BaseModel):
    queryResult: dict
    session: str

class ChatGeminiRequest(BaseModel):
    message: str

@app.post("/webhook")
async def webhook(req: DialogflowRequest, background_task: BackgroundTasks):
    intent = req.queryResult.get("intent", {}).get("displayName", "")
    if intent in ["Welcome", "Mulai", "Menu Utama"]:
        return await handle_welcome()
    elif intent == "Pilih Jenjang SD":
        return await handle_subjects_by_level("sd", req)
    elif intent == "Pilih Jenjang SMP":
        return await handle_subjects_by_level("smp", req)
    elif intent == "Pilih Jenjang SMA":
        return await handle_subjects_by_level("sma", req)
    elif intent == "Pilih Topik Pelajaran":
        return await handle_lessons_by_subject_name_level(req)
    elif intent == "Pilih Subbab":
        return await handle_subbab_by_lessonid(req)
    elif intent == "Pilih Teori Subbab":
        return await get_theory_from_subbab(req, background_task)
    elif intent == "Tanya Lagi ke AI":
        return await handle_custom_question(req, background_task)
    elif intent == "Custom Pertanyaan":
        return await handle_custom_question(req, background_task)
    return {"fulfillmentText": "Maaf, intent tidak dikenali."}

@app.get("/check-gemini-result")
async def check_gemini_result(cache_key: str):
    cached = await redis_client.get(cache_key)
    if cached:
        chips = [
            {"text": "💬 Tanya Lagi ke AI"},
            {"text": "🏠 Menu Utama"}
        ]
        return {
            "status": "ready",
            "fulfillmentMessages": [
                {"text": {"text": [f"🤖 Gemini Bot:\n{cached}"]}},
                {"text": {"text": ["🤖 Chatbot:\nIngin bertanya lagi atau kembali ke menu?:"]}},
                {"payload": {"richContent": [[{"type": "chips", "options": chips}]]}}
            ]
        }
    return {"status": "pending"}

@app.get("/clear-all-cache")
async def clear_all_cache():
    await redis_client.flushall()
    return {"status": "✅ Semua cache Redis telah dihapus"}

@app.get("/get-dialogflow-token")
async def dialogflow_token():
    try:
        token = get_dialogflow_token()
        return {"access_token": token}
    except Exception as e:
        return {"error": str(e)}