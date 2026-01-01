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
from send_email.send_email import send_email_to_admin, send_email_approve_to_user, send_email_unapprove_to_user
from send_email.background_task import _enqueue_email
from chatbot.utils.sync_dialogflow import sync_subjects_to_entity, sync_lessons_to_entity, sync_all_training_phrases

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

@app.on_event("startup")
async def startup_event():
    """
    Event handler yang dijalankan saat aplikasi FastAPI start.
    Menjalankan sync_dialogflow untuk sinkronisasi entity dan training phrases.
    """
    import asyncio
    try:
        logging.info("🚀 Aplikasi sedang starting up...")
        logging.info("🔄 Memulai sinkronisasi Dialogflow...")
        
        # Jalankan sync_dialogflow di thread pool karena fungsi-fungsi sync adalah synchronous
        loop = asyncio.get_event_loop()
        await loop.run_in_executor(None, sync_subjects_to_entity)
        await loop.run_in_executor(None, sync_lessons_to_entity)
        await loop.run_in_executor(None, sync_all_training_phrases)
        
        logging.info("✅ Sinkronisasi Dialogflow selesai!")
        logging.info("✅ Aplikasi siap menerima request")
    except Exception as e:
        logging.error(f"❌ Error saat startup sync_dialogflow: {str(e)}")
        logging.warning("⚠️ Aplikasi tetap berjalan meskipun sync_dialogflow gagal")

class DialogflowRequest(BaseModel):
    queryResult: dict
    session: str

class ChatGeminiRequest(BaseModel):
    message: str

class UserRegistrationRequest(BaseModel):
    email: str
    role: str  # "orang tua" atau "guru"
    name: str
    phone: str = None

@app.post("/webhook", tags=["Chatbot"])
async def webhook(req: DialogflowRequest, background_task: BackgroundTasks):
    """
    Endpoint untuk menangani webhook dari Dialogflow.
    Menerima request dari Dialogflow, memproses intent, dan mengembalikan response.
    """
    try:
        logging.info("🔄 Webhook dipanggil")
        logging.info(f"📥 Request session: {req.session}")
        
        if not req.queryResult:
            logging.error("❌ queryResult kosong")
            raise HTTPException(status_code=400, detail="Invalid request: queryResult is required")
        
        intent = req.queryResult.get("intent", {}).get("displayName", "")
        query_text = req.queryResult.get("queryText", "").strip()
        output_contexts = req.queryResult.get("outputContexts", [])
        input_contexts = req.queryResult.get("inputContexts", [])
        
        logging.info(f"🎯 Intent yang diterima: '{intent}'")
        logging.info(f"💬 Query text: '{query_text}'")
        
        # Cek apakah ada context waiting_custom_answer di inputContexts ATAU outputContexts
        # Context yang dibuat di response sebelumnya akan muncul sebagai inputContexts di request berikutnya
        all_contexts = input_contexts + output_contexts
        has_waiting_context = any(
            "waiting_custom_answer" in context.get("name", "")
            for context in all_contexts
        )
        
        if has_waiting_context:
            logging.info(f"✅ Context waiting_custom_answer ditemukan (inputContexts: {len(input_contexts)}, outputContexts: {len(output_contexts)})")
        
        # Handle Default Fallback Intent dengan context waiting_custom_answer
        if intent == "Default Fallback Intent" and has_waiting_context and query_text:
            logging.info("💭 Fallback intent dengan context waiting_custom_answer - treat as Custom Pertanyaan")
            return await handle_custom_question(req, background_task)
        
        # PRIORITAS: Jika ada context waiting_custom_answer aktif, semua input diarahkan ke custom question
        # Kecuali intent khusus yang harus diabaikan (Welcome, Menu Utama, Tanya Lagi ke AI)
        if has_waiting_context and query_text and intent not in ["Welcome", "Mulai", "Menu Utama", "Tanya Lagi ke AI"]:
            logging.info(f"💬 Context waiting_custom_answer aktif - mengarahkan intent '{intent}' ke Custom Pertanyaan")
            return await handle_custom_question(req, background_task)
        
        if not intent:
            logging.warning("⚠️ Intent kosong")
            return {"fulfillmentText": "Maaf, intent tidak dikenali."}
            
        if intent in ["Welcome", "Mulai", "Menu Utama"]:
            logging.info("🏠 Menangani intent Welcome/Mulai/Menu Utama - Reset semua context")
            return await handle_welcome(req.session)
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

@app.get("/check-gemini-result", tags=["Chatbot"])
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

@app.get("/clear-all-cache", tags=["Reddis"])
async def clear_all_cache():
    """
    Endpoint untuk menghapus semua cache Redis.
    """
    await redis_client.flushall()
    return {"status": "✅ Semua cache Redis telah dihapus"}

@app.get("/get-dialogflow-token", tags=["Chatbot"])
async def dialogflow_token():
    """
    Endpoint untuk mengambil token Dialogflow yang digunakan untuk autentikasi API.
    """
    try:
        token = get_dialogflow_token()
        return {"access_token": token}
    except Exception as e:
        return {"error": str(e)}

@app.post("/email-admin-verification", tags=["Send Email"])
async def email_admin(user_data: UserRegistrationRequest, background_task: BackgroundTasks):
    """
    Endpoint untuk mengirim notifikasi ke admin
    """
    ok = _enqueue_email(
        background_task,
        send_email_to_admin,
        user_data.name, user_data.email, user_data.role
    )
    if not ok:
        raise HTTPException(status_code=500, detail="Gagal menjadwalkan email ke Admin.")
    return {
        "status": "success",
        "message": "Email notifikasi telah dijadwalkan untuk admin.",
        "user": user_data.model_dump()
    }

@app.post("/email-approve-user", tags=["Send Email"])
async def email_approve_user(user_data: UserRegistrationRequest, background_task: BackgroundTasks):
    """
    Endpoint untuk mengirim notifikasi ke user bahwa akun mereka telah disetujui
    """
    ok = _enqueue_email(
        background_task,
        send_email_approve_to_user,
        user_data.name, user_data.email, user_data.role
    )
    if not ok:
        raise HTTPException(status_code=500, detail="Gagal menjadwalkan email Approval.")
    return {
        "status": "success",
        "message": "Email Approval telah dijadwalkan untuk user.",
        "user": user_data.model_dump()
    }

@app.post("/email-unapprove-user", tags=["Send Email"])
async def email_unapprove_user(user_data: UserRegistrationRequest, background_task: BackgroundTasks):
    """
    Endpoint untuk mengirim notifikasi ke user bahwa akun mereka belum disetujui
    """
    ok = _enqueue_email(
        background_task,
        send_email_unapprove_to_user,
        user_data.name, user_data.email, user_data.role
    )
    if not ok:
        raise HTTPException(status_code=500, detail="Gagal menjadwalkan email Unapproval.")
    return {
        "status": "success",
        "message": "Email Unapproval telah dijadwalkan untuk user.",
        "user": user_data.model_dump()
    }