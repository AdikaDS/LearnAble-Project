from fastapi import FastAPI, BackgroundTasks, HTTPException
from pydantic import BaseModel
import logging
import sys
from chatbot.handlers.theory_with_gemini import get_theory_from_subbab
from chatbot.handlers.subject import handle_subjects_by_level
from chatbot.handlers.lessons import handle_lessons_by_subject_name_level
from chatbot.handlers.subbab import handle_subbab_by_lessonid
from chatbot.handlers.general import handle_welcome
from chatbot.handlers.custom_question import handle_custom_question
from chatbot.services.redis_client import redis_client
from chatbot.utils.dialogflow_token import get_dialogflow_token

# Konfigurasi logging yang lebih robust
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('app.log', encoding='utf-8')
    ]
)

# Set level untuk semua logger
logging.getLogger().setLevel(logging.INFO)

app = FastAPI()

class DialogflowRequest(BaseModel):
    queryResult: dict
    session: str

class ChatGeminiRequest(BaseModel):
    message: str

@app.post("/webhook")
async def webhook(req: DialogflowRequest, background_task: BackgroundTasks):
    try:
        logging.info("🔄 Webhook dipanggil")
        logging.info(f"📥 Request session: {req.session}")
        
        if not req.queryResult:
            logging.error("❌ queryResult kosong")
            raise HTTPException(status_code=400, detail="Invalid request: queryResult is required")
        
        intent = req.queryResult.get("intent", {}).get("displayName", "")
        query_text = req.queryResult.get("queryText", "").strip()
        output_contexts = req.queryResult.get("outputContexts", [])
        
        logging.info(f"🎯 Intent yang diterima: '{intent}'")
        logging.info(f"💬 Query text: '{query_text}'")
        
        # Cek apakah ada context waiting_custom_answer
        has_waiting_context = any(
            "waiting_custom_answer" in context.get("name", "")
            for context in output_contexts
        )
        
        # Handle Default Fallback Intent dengan context waiting_custom_answer
        if intent == "Default Fallback Intent" and has_waiting_context and query_text:
            logging.info("💭 Fallback intent dengan context waiting_custom_answer - treat as Custom Pertanyaan")
            return await handle_custom_question(req, background_task)
        
        if not intent:
            logging.warning("⚠️ Intent kosong")
            return {"fulfillmentText": "Maaf, intent tidak dikenali."}
            
        if intent in ["Welcome", "Mulai", "Menu Utama"]:
            logging.info("🏠 Menangani intent Welcome/Mulai/Menu Utama")
            return await handle_welcome()
        elif intent == "Pilih Jenjang SD":
            logging.info("📚 Menangani intent Pilih Jenjang SD")
            return await handle_subjects_by_level("sd", req)
        elif intent == "Pilih Jenjang SMP":
            logging.info("📚 Menangani intent Pilih Jenjang SMP")
            return await handle_subjects_by_level("smp", req)
        elif intent == "Pilih Jenjang SMA":
            logging.info("📚 Menangani intent Pilih Jenjang SMA")
            return await handle_subjects_by_level("sma", req)
        elif intent == "Pilih Topik Pelajaran":
            logging.info("📖 Menangani intent Pilih Topik Pelajaran")
            return await handle_lessons_by_subject_name_level(req)
        elif intent == "Pilih Subbab":
            logging.info("📝 Menangani intent Pilih Subbab")
            return await handle_subbab_by_lessonid(req)
        elif intent == "Pilih Teori Subbab":
            logging.info("🧠 Menangani intent Pilih Teori Subbab")
            return await get_theory_from_subbab(req, background_task)
        elif intent == "Tanya Lagi ke AI":
            logging.info("🤖 Menangani intent Tanya Lagi ke AI")
            return await handle_custom_question(req, background_task)
        elif intent == "Custom Pertanyaan":
            logging.info("💬 Menangani intent Custom Pertanyaan")
            return await handle_custom_question(req, background_task)
        
        logging.warning(f"⚠️ Intent tidak dikenali: '{intent}'")
        return {"fulfillmentText": "Maaf, intent tidak dikenali."}
    except Exception as e:
        logging.error(f"❌ Error in webhook: {str(e)}")
        return {"fulfillmentText": "Terjadi kesalahan internal. Silakan coba lagi."}

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