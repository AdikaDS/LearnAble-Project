from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
import numpy as np
import json
from speech_service import SpeechToTextService
import asyncio
import base64

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
speech_service = SpeechToTextService()

@app.get("/")
async def root():
    return {"message": "Speech to Text API is running"}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            # Receive audio data from client
            data = await websocket.receive_text()
            audio_data = json.loads(data)
            
            # Convert base64 audio to numpy array
            audio_bytes = base64.b64decode(audio_data["audio"])
            audio_array = np.frombuffer(audio_bytes, dtype=np.float32)
            
            # Process audio and get transcription
            text = speech_service.process_audio_chunk(audio_array)
            
            if text:
                # Send transcription back to client
                await websocket.send_json({"text": text})
                
    except WebSocketDisconnect:
        print("Client disconnected")
    except Exception as e:
        print(f"Error: {str(e)}")
        await websocket.close()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
