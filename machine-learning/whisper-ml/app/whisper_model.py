import whisper
import logging

logger = logging.getLogger(__name__)

try:
    model = whisper.load_model("base")
    logger.info("✅ Whisper model loaded successfully")
except Exception as e:
    logger.error(f"❌ Failed to load Whisper model: {e}")
    raise


def transcribe(path: str) -> dict:
    return model.transcribe(
        path,
        language="id",
        task="transcribe",
        fp16=False,
        initial_prompt="Transkripsi audio bahasa Indonesia",
        temperature=0.0,
        best_of=1
    )