import whisper
import numpy as np
import sounddevice as sd
import torch
from typing import Optional, Callable

class SpeechToTextService:
    def __init__(self, model_size: str = "small", language: str = "Indonesian"):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model = whisper.load_model(model_size).to(self.device)
        self.language = language
        
        # Audio settings
        self.samplerate = 16000
        self.block_duration = 3  # seconds
        self.block_size = int(self.samplerate * self.block_duration)
        self.silence_threshold = 0.01
        
        # Buffer
        self.audio_buffer = np.zeros((0,), dtype=np.float32)
        
    def is_speaking(self, audio_chunk: np.ndarray, threshold: float) -> bool:
        energy = np.sqrt(np.mean(audio_chunk ** 2))
        return energy > threshold
    
    def process_audio_chunk(self, audio_chunk: np.ndarray) -> Optional[str]:
        if self.is_speaking(audio_chunk, self.silence_threshold):
            result = self.model.transcribe(audio_chunk, language=self.language, fp16=False)
            return result["text"]
        return None
    
    def start_listening(self, callback: Optional[Callable[[str], None]] = None):
        def audio_callback(indata, frames, time, status):
            if status:
                print("Status mic:", status)
            audio_chunk = indata[:, 0]
            self.audio_buffer = np.concatenate((self.audio_buffer, audio_chunk))
            
            if len(self.audio_buffer) >= self.block_size:
                chunk = self.audio_buffer[:self.block_size]
                self.audio_buffer = self.audio_buffer[self.block_size:]
                chunk = np.clip(chunk, -1, 1)
                
                text = self.process_audio_chunk(chunk)
                if text and callback:
                    callback(text)
        
        with sd.InputStream(samplerate=self.samplerate, channels=1, 
                          dtype='float32', callback=audio_callback):
            print("🎤 Real-Time STT + Silence Detection aktif")
            print("Ctrl+C untuk berhenti\n")
            
            try:
                while True:
                    pass
            except KeyboardInterrupt:
                print("\n🛑 Dihentikan oleh pengguna.")
