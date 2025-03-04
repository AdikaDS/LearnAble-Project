from flask import Flask, request, jsonify
from PIL import Image, UnidentifiedImageError
from ocr_service import OCRService 
import os

# Inisialisasi Flask dan OCRService
app = Flask(__name__)
ocr_service = OCRService()  # Membuat instance dari OCRService

@app.route("/detect_text", methods=["POST"])
def detect_text():
    if 'image' not in request.files:
        return jsonify({"error": "Tidak ada file gambar yang dikirim"}), 400

    file = request.files['image']
    
    try:
        image = Image.open(file.stream)
    except UnidentifiedImageError:
        return jsonify({"error": "Format gambar tidak valid"}), 400

    # Panggil metode detect_text dari OCRService
    detected_text = ocr_service.detect_text(image)

    return jsonify({"text": detected_text})

if __name__ == "__main__":
    debug_mode = os.getenv("FLASK_DEBUG", "False").lower() == "true"
    app.run(host="0.0.0.0", port=5000, debug=debug_mode)