import whisper
import logging
import numpy as np

# Setup logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

class SpeechToTextService:
    def __init__(self):
        try:
            logger.info("📦 Loading Whisper model...")
            self.model = whisper.load_model("base")
            logger.info("✅ Whisper model loaded successfully.")
        except Exception as e:
            logger.error(f"❌ Error loading Whisper model: {e}")
            raise e

    def process_audio_chunk(self, audio_array: np.ndarray):
        try:
            logger.info("🔊 Processing audio chunk...")
            # Convert the audio array to a format acceptable by Whisper
            audio_tensor = whisper.utils.audio_to_input_tensor(audio_array)

            # Perform transcription using Whisper
            logger.info("🔄 Running transcription...")
            result = self.model.transcribe(audio_tensor)
            
            transcription = result.get('text', '')
            if transcription:
                logger.info(f"💬 Transcription: {transcription}")
            else:
                logger.warning("⚠️ No transcription result returned.")
            return transcription
        except Exception as e:
            logger.error(f"❌ Error processing audio chunk: {e}")
            return None
