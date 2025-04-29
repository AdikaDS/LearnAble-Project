from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import numpy as np
import json
import asyncio
import base64
import logging

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
logger.info("🔥 Starting FastAPI application...")

from speech_service import SpeechToTextService

app = FastAPI()

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize speech service
try:
    logger.info("🔁 Initializing SpeechToTextService...")
    speech_service = SpeechToTextService()
    logger.info("✅ SpeechToTextService initialized successfully.")
except Exception as e:
    logger.error(f"❌ Failed to initialize SpeechToTextService: {e}")
    raise e

@app.get("/")
async def root():
    logger.info("📥 Received GET request at '/'")
    return {"message": "Speech to Text API is running"}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    logger.info("📡 WebSocket connection attempt...")
    await websocket.accept()
    logger.info("✅ WebSocket connected.")

    try:
        while True:
            # Receive audio data from client
            data = await websocket.receive_text()
            logger.debug(f"🔊 Received data from client: {data[:30]}...")

            audio_data = json.loads(data)

            # Convert base64 audio to numpy array
            audio_bytes = base64.b64decode(audio_data["audio"])
            audio_array = np.frombuffer(audio_bytes, dtype=np.float32)

            # Process audio and get transcription
            text = speech_service.process_audio_chunk(audio_array)

            if text:
                logger.info(f"💬 Transcription result: {text}")
                await websocket.send_json({"text": text})

    except WebSocketDisconnect:
        logger.info("⚠️ WebSocket client disconnected")
    except Exception as e:
        logger.error(f"❌ Error in WebSocket handler: {e}")
        await websocket.close()

if __name__ == "__main__":
    import uvicorn
    logger.info("🚀 Running Uvicorn server...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
