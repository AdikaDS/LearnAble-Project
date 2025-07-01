from fastapi import FastAPI, UploadFile, File, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import tempfile
import shutil
import os
import logging

from whisper_model import transcribe
from config import MAX_FILE_SIZE, ALLOWED_EXTENSIONS, HOST, PORT, LOG_FILE
from utils import is_allowed_file, get_word_count, get_timestamp

# Logging setup
logging.basicConfig(
    handlers=[logging.FileHandler(LOG_FILE, encoding="utf-8")],
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

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
async def root():
    return {"message": "Whisper API is running", "status": "healthy"}


@app.post("/transcribe", summary="Transcribe Audio", description="Upload audio file dan dapatkan hasil transkripsi")
async def transcribe_audio(file: UploadFile = File(...)):
    temp_dir = None
    try:
        content = await file.read()

        if len(content) > MAX_FILE_SIZE:
            return JSONResponse(
                content={"error": "File terlalu besar. Maksimum 20MB."},
                status_code=status.HTTP_400_BAD_REQUEST
            )

        if not file.filename or not is_allowed_file(file.filename, ALLOWED_EXTENSIONS):
            return JSONResponse(
                content={"error": "Format file tidak didukung."},
                status_code=status.HTTP_400_BAD_REQUEST
            )

        # Simpan ke file sementara
        temp_dir = tempfile.mkdtemp()
        file_path = os.path.join(temp_dir, file.filename)

        with open(file_path, "wb") as f:
            f.write(content)

        logger.info(f"✅ File received: {file.filename} ({len(content)} bytes)")

        result = transcribe(file_path)

        logger.info(f"📝 Transcription result: {result['text']}")

        return {
            "text": result["text"],
            "language": result["language"],
            "word_count": get_word_count(result["text"]),
            "timestamp": get_timestamp()
        }

    except Exception as e:
        logger.error(f"❌ Error processing file: {e}")
        return JSONResponse(content={"error": str(e)}, status_code=status.HTTP_500_INTERNAL_SERVER_ERROR)
    finally:
        if temp_dir and os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
            logger.info("🧹 Cleaned up temporary files")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host=HOST, port=PORT, reload=True)
