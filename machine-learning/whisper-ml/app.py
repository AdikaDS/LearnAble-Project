from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
import whisper
import tempfile
import os
import uvicorn
import shutil
import logging
from datetime import datetime

# Konfigurasi logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Whisper Speech Recognition API",
    description="API untuk transkripsi audio menggunakan OpenAI Whisper",
    version="1.0.0"
)

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load Whisper model
try:
    model = whisper.load_model("base")
    logger.info("✅ Whisper model loaded successfully")
except Exception as e:
    logger.error(f"❌ Failed to load Whisper model: {e}")
    raise

@app.get("/")
async def root():
    return {
        "message": "Whisper API is running",
        "status": "healthy"
    }

@app.post("/transcribe")
async def transcribe_audio(file: UploadFile = File(...)):
    """
    Endpoint untuk menerima file audio dan melakukan transkripsi
    """
    try:
        # Buat direktori temporary untuk menyimpan file
        temp_dir = tempfile.mkdtemp()
        temp_file_path = os.path.join(temp_dir, file.filename)
        
        # Simpan file yang diupload
        with open(temp_file_path, "wb") as buffer:
            content = await file.read()
            buffer.write(content)
        
        logger.info(f"✅ File received: {file.filename} ({len(content)} bytes)")
        
        # Transcribe dengan Whisper
        result = model.transcribe(
            temp_file_path,
            language="id",
            task="transcribe",
            fp16=False,
            initial_prompt="Transkripsi audio bahasa Indonesia",
            temperature=0.0,
            best_of=1
        )
        
        logger.info(f"📝 Transcription result: '{result['text']}'")
        
        return {
            "text": result["text"],
            "language": result["language"]
        }
        
    except Exception as e:
        logger.error(f"❌ Error processing file: {e}")
        return {"error": str(e)}
    finally:
        # Bersihkan file temporary
        try:
            if os.path.exists(temp_dir):
                shutil.rmtree(temp_dir)
                logger.info("🧹 Cleaned up temporary files")
        except Exception as e:
            logger.error(f"❌ Error cleaning up temporary files: {e}")

if __name__ == "__main__":
    # Gunakan port dari environment variable atau default ke 8000
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=port,
        log_level="info",
        access_log=True
    )
