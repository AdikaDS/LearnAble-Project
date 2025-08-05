from google.cloud import firestore
from os import getenv
from dotenv import load_dotenv
import logging

load_dotenv()

try:
    # Cek apakah ada credentials file
    credentials_path = getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if credentials_path:
        db = firestore.Client.from_service_account_json(credentials_path)
    else:
        # Fallback ke default credentials (untuk development)
        db = firestore.Client()
    logging.info("✅ Firestore client berhasil diinisialisasi")
except Exception as e:
    logging.error(f"❌ Gagal menginisialisasi Firestore client: {str(e)}")
    db = None