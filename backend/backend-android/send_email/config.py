import os
from dotenv import load_dotenv

load_dotenv()

EMAIL_CONFIG = {
    "smtp_server": "smtp.gmail.com",
    "smtp_port": 465,
    "smtp_user": os.getenv("SMTP_USER", "noreply@learnable.com"),
    "smtp_pass": os.getenv("SMTP_PASS", "your_app_password"),
    "admin_email": os.getenv("ADMIN_EMAIL", "admin@learnable.com"),
    "from_name": "LearnAble Super Admin"
}

def get_email_config():
    return EMAIL_CONFIG

def validate_email_config():
    required_fields = ["smtp_user", "smtp_pass", "admin_email"]
    missing_fields = []
    
    for field in required_fields:
        if not EMAIL_CONFIG.get(field) or EMAIL_CONFIG[field] == "your_app_password":
            missing_fields.append(field)
    
    if missing_fields:
        print(f"⚠️ Konfigurasi email belum lengkap. Field yang perlu diisi: {missing_fields}")
        print("📧 Silakan buat file .env dengan konfigurasi berikut:")
        print("SMTP_USER=your_email@gmail.com")
        print("SMTP_PASS=your_app_password")
        print("ADMIN_EMAIL=admin@learnable.com")
        return False
    
    return True 