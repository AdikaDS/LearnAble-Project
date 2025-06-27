import os

# Ukuran maksimum file (20 MB)
MAX_FILE_SIZE = 20 * 1024 * 1024
ALLOWED_EXTENSIONS = {".wav", ".mp3", ".m4a"}

# Load port dari environment atau default
PORT = int(os.getenv("PORT", 8000))
HOST = os.getenv("HOST", "0.0.0.0")

# Logging
LOG_FILE = "whisper_api.log"