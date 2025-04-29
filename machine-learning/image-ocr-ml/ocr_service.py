from PIL import Image
from transformers import AutoProcessor, GotOcr2ForConditionalGeneration, TextStreamer

class OCRService:
    def __init__(self):
         # Memuat model dan processor hanya sekali saat kelas diinisialisasi
        self.device = "cuda"
        self.model = GotOcr2ForConditionalGeneration.from_pretrained("stepfun-ai/GOT-OCR-2.0-hf").to(self.device)
        self.processor = AutoProcessor.from_pretrained("stepfun-ai/GOT-OCR-2.0-hf")

    def detect_text (self, image: Image.Image) -> str:

        def filter_text(text: str) -> str:
            # Jika teks terlalu pendek atau hanya karakter aneh, anggap tidak ada teks
            if len(text) < 3 or text.isnumeric():  # Bisa disesuaikan
                return "Tidak ada teks yang terdeteksi pada gambar."
            return text
    
         # Memproses gambar menjadi input untuk model
        inputs = self.processor(image, return_tensors="pt").to(self.device)

        # Generate
        streamer = TextStreamer(self.processor.tokenizer, skip_prompt=True, skip_special_tokens=True)
        generate_ids = self.model.generate(
            **inputs,
            do_sample=False,
            tokenizer=self.processor.tokenizer,
            stop_strings=["<|im_end|>"],
            streamer=streamer,
            max_new_tokens=4096,
        )

        # Mendecode hasil untuk mendapatkan teks yang terdeteksi
        generated_text = self.processor.tokenizer.decode(generate_ids[0], skip_special_tokens=True)
        return filter_text(generated_text.strip())
