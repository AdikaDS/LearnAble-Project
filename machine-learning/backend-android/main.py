from fastapi import FastAPI
from pydantic import BaseModel
import logging
from handlers.theory_with_gemini import get_theory_from_subbab
from handlers.subject import handle_subjects_by_level
from handlers.lessons import handle_lessons_by_subject_name_level
from handlers.subbab import handle_subbab_by_lessonid
from handlers.general import handle_welcome
from handlers.custom_question import handle_custom_question
from services.gemini_service_async import chat_with_gemini_api
from utils.dialogflow_token import get_dialogflow_token

app = FastAPI()
logging.basicConfig(level=logging.INFO)

class DialogflowRequest(BaseModel):
    queryResult: dict
    session: str

class ChatGeminiRequest(BaseModel):
    message: str

@app.post("/webhook")
async def webhook(req: DialogflowRequest):
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
        return await get_theory_from_subbab(req)
    elif intent == "Tanya Lagi ke AI":
        return await handle_custom_question(req)
    return {"fulfillmentText": "Maaf, intent tidak dikenali."}

@app.post("/chat-gemini")
async def chat_gemini(req: ChatGeminiRequest):
    reply = await chat_with_gemini_api(req.message)
    return {"reply": reply}

@app.get("/get-dialogflow-token")
async def dialogflow_token():
    try:
        token = get_dialogflow_token()
        return {"access_token": token}
    except Exception as e:
        return {"error": str(e)}