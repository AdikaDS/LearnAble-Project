from flask import Flask, request, jsonify
from PIL import Image
from ocr_service import OCRService 

# Inisialisasi Flask dan OCRService
app = Flask(__name__)
ocr_service = OCRService()  # Membuat instance dari OCRService

@app.route("/detect_text", methods=["POST"])
def detect_text():
    # Mendapatkan gambar dari request
    file = request.files['image']
    image = Image.open(file.stream)

    # Panggil metode detect_text dari OCRService
    detected_text = ocr_service.detect_text(image)

    return jsonify({"text": detected_text})

if __name__ == "__main__":
    app.run(debug=True)
