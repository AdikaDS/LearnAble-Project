import whisper
import numpy as np
import torch
from typing import Optional

class SpeechToTextService:
    def __init__(self, model_size: str = "small", language: str = "Indonesian"):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model = whisper.load_model(model_size).to(self.device)
        self.language = language
        
        # Audio settings
        self.samplerate = 16000
        self.silence_threshold = 0.01
        
    def is_speaking(self, audio_chunk: np.ndarray, threshold: float) -> bool:
        energy = np.sqrt(np.mean(audio_chunk ** 2))
        return energy > threshold
    
    def process_audio_chunk(self, audio_chunk: np.ndarray) -> Optional[str]:
        if self.is_speaking(audio_chunk, self.silence_threshold):
            result = self.model.transcribe(audio_chunk, language=self.language, fp16=False)
            return result["text"]
        return None
