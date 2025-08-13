import re
import time
import ssl
import smtplib
from pathlib import Path
from typing import Optional
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from email.mime.image import MIMEImage
from email.utils import formatdate, make_msgid

from jinja2 import Environment, FileSystemLoader, select_autoescape
from datetime import datetime

from send_email.config import get_email_config, validate_email_config

# --- Konstanta & util bersama ---
EMAIL_REGEX = r"^[^@]+@[^@]+\.[^@]+$"

BASE_DIR = Path(__file__).resolve().parent
TEMPLATES_DIR = BASE_DIR / "templates"
LOGO_PATH = TEMPLATES_DIR / "images" / "logo-learnable.png"

def _validate_email(addr: str, label: str) -> None:
    if not addr or not re.match(EMAIL_REGEX, addr):
        raise ValueError(f"{label} tidak valid: {addr}")

def _load_logo_bytes(path: Path) -> bytes:
    if not path.exists():
        raise FileNotFoundError(f"Logo tidak ditemukan: {path.resolve()}")
    return path.read_bytes()

def _get_jinja_env() -> Environment:
    return Environment(
        loader=FileSystemLoader(TEMPLATES_DIR),
        autoescape=select_autoescape(["html", "xml"]),
    )

def _get_config_or_fail():
    """Ambil config sekali dan validasi minimal."""
    if not validate_email_config():
        raise RuntimeError("Konfigurasi email belum lengkap. Harap isi .env / secret manager.")
    cfg = get_email_config()
    # Validasi minimum untuk bisa login SMTP
    _validate_email(cfg["smtp_user"], "SMTP user")
    if not cfg.get("smtp_pass"):
        raise ValueError("SMTP pass tidak di-set.")
    if not cfg.get("smtp_server"):
        raise ValueError("SMTP server tidak di-set.")
    if not cfg.get("smtp_port"):
        raise ValueError("SMTP port tidak di-set.")
    return cfg

def _compose_message_with_logo(
    *,
    from_name: str,
    from_email: str,
    to_email: str,
    subject: str,
    html_template: str,
    html_ctx: dict,
    plain_fallback: str,
    logo_bytes: bytes,
) -> MIMEMultipart:
    """Buat email multipart/related (HTML + plain) dgn logo inline (CID)."""
    msg_root = MIMEMultipart("related")
    msg_root["From"] = f"{from_name} <{from_email}>"
    msg_root["To"] = to_email
    msg_root["Subject"] = subject
    msg_root["Date"] = formatdate(localtime=True)
    msg_root["Message-ID"] = make_msgid(domain=from_email.split("@")[-1])

    alt = MIMEMultipart("alternative")
    alt.attach(MIMEText(plain_fallback, "plain", "utf-8"))

    env = _get_jinja_env()
    tpl = env.get_template(html_template)
    # Pastikan template pakai <img src="cid:logo">
    html_ctx = {**html_ctx, "logo_cid": "cid:logo"}
    html_body = tpl.render(**html_ctx)
    alt.attach(MIMEText(html_body, "html", "utf-8"))

    msg_root.attach(alt)

    img = MIMEImage(logo_bytes, _subtype="png")
    img.add_header("Content-ID", "<logo>")
    img.add_header("Content-Disposition", "inline", filename="logo-learnable.png")
    msg_root.attach(img)

    return msg_root

def _send_with_retry(
    *,
    smtp_host: str,
    smtp_port: int,
    smtp_user: str,
    smtp_pass: str,
    msg: MIMEMultipart,
    to_addrs: list[str],
    use_ssl: bool = True,
    starttls_fallback: bool = True,
    max_retries: int = 3,
    backoff_secs: int = 2,
    timeout: int = 12,
) -> bool:
    """
    Kirim email dengan SSL (port 465) atau STARTTLS (port 587) + retry.
    """
    for attempt in range(1, max_retries + 1):
        try:
            if use_ssl:
                server = smtplib.SMTP_SSL(smtp_host, smtp_port, context=ssl.create_default_context(), timeout=timeout)
            else:
                server = smtplib.SMTP(smtp_host, smtp_port, timeout=timeout)

            with server:
                if not use_ssl and starttls_fallback:
                    server.starttls(context=ssl.create_default_context())
                server.login(smtp_user, smtp_pass)
                server.sendmail(msg["From"], to_addrs, msg.as_string())
            return True
        except Exception as e:
            print(f"⚠️ Gagal kirim (attempt {attempt}/{max_retries}): {e}")
            if attempt < max_retries:
                time.sleep(backoff_secs * attempt)
            else:
                print("❌ Email gagal dikirim setelah retry.")
                return False

# --- Fungsi publik yang simpel ---

def send_email_to_admin(
    user_name: str,
    user_email: str,
    user_role: str,
    admin_email: Optional[str] = None,
    smtp_user: Optional[str] = None,
    smtp_pass: Optional[str] = None,
    template_name: str = "registration_notification.html",
    subject_prefix: str = "Pendaftaran Baru",
) -> bool:
    cfg = _get_config_or_fail()
    admin_email = admin_email or cfg["admin_email"]
    smtp_user = smtp_user or cfg["smtp_user"]
    smtp_pass = smtp_pass or cfg["smtp_pass"]
    smtp_host = cfg["smtp_server"]
    smtp_port = int(cfg["smtp_port"])
    from_name = cfg.get("from_name", "LearnAble")

    _validate_email(admin_email, "Email admin")
    _validate_email(user_email, "Email pendaftar")  # untuk ditampilkan di konten
    # compose
    plain = (
        "Halo Admin!\n\n"
        "Ada pendaftaran baru di sistem LearnAble.\n\n"
        f"Nama Pendaftar: {user_name}\n"
        f"Email Pendaftar: {user_email}\n"
        f"Role Pendaftar: {user_role.title()}\n"
        f"Waktu Pendaftaran: {datetime.now().strftime('%d %B %Y %H:%M:%S')}\n\n"
        "Silakan cek dashboard admin untuk detail.\n\n"
        "Salam,\nSistem LearnAble\n"
    )
    msg = _compose_message_with_logo(
        from_name=from_name,
        from_email=smtp_user,
        to_email=admin_email,
        subject=f"{subject_prefix} sebagai {user_role.title()} - LearnAble",
        html_template=template_name,
        html_ctx={
            "user_name": user_name,
            "user_email": user_email,
            "user_role": user_role.title(),
            "registration_time": datetime.now().strftime("%d %B %Y %H:%M:%S"),
        },
        plain_fallback=plain,
        logo_bytes=_load_logo_bytes(LOGO_PATH),
    )
    return _send_with_retry(
        smtp_host=smtp_host,
        smtp_port=smtp_port,
        smtp_user=smtp_user,
        smtp_pass=smtp_pass,
        msg=msg,
        to_addrs=[admin_email],
        use_ssl=(smtp_port == 465),
        starttls_fallback=(smtp_port == 587),
    )

def send_email_approve_to_user(
    user_name: str,
    user_email: str,
    user_role: str,
    admin_email: Optional[str] = None,  # opsional: untuk Reply-To
    smtp_user: Optional[str] = None,
    smtp_pass: Optional[str] = None,
    template_name: str = "approve_notification.html",
    subject_prefix: str = "Selamat Datang di LearnAble",
) -> bool:
    cfg = _get_config_or_fail()
    smtp_user = smtp_user or cfg["smtp_user"]
    smtp_pass = smtp_pass or cfg["smtp_pass"]
    smtp_host = cfg["smtp_server"]
    smtp_port = int(cfg["smtp_port"])
    from_name = cfg.get("from_name", "LearnAble")

    _validate_email(user_email, "Email pendaftar")

    plain = (
        f"Halo {user_name}!\n\n"
        "Terima kasih telah bergabung dengan LearnAble! Akun kamu sudah aktif.\n\n"
        f"Nama Pendaftar: {user_name}\n"
        f"Email Pendaftar: {user_email}\n"
        f"Role Pendaftar: {user_role.title()}\n"
        "Yuk mulai eksplorasi fitur-fitur kami!\n\n"
        "Salam,\nSistem LearnAble\n"
    )
    msg = _compose_message_with_logo(
        from_name=from_name,
        from_email=smtp_user,
        to_email=user_email,
        subject=subject_prefix,
        html_template=template_name,
        html_ctx={
            "user_name": user_name,
            "user_email": user_email,
            "user_role": user_role.title(),
        },
        plain_fallback=plain,
        logo_bytes=_load_logo_bytes(LOGO_PATH),
    )
    # Opsional: set Reply-To admin, kalau mau
    if admin_email:
        _validate_email(admin_email, "Email admin")
        msg["Reply-To"] = admin_email

    return _send_with_retry(
        smtp_host=smtp_host,
        smtp_port=smtp_port,
        smtp_user=smtp_user,
        smtp_pass=smtp_pass,
        msg=msg,
        to_addrs=[user_email],  # <= ✅ FIX: kirim ke user
        use_ssl=(smtp_port == 465),
        starttls_fallback=(smtp_port == 587),
    )

def send_email_unapprove_to_user(
    user_name: str,
    user_email: str,
    user_role: str,
    admin_email: Optional[str] = None,  # opsional: Reply-To admin
    smtp_user: Optional[str] = None,
    smtp_pass: Optional[str] = None,
    template_name: str = "unapprove_notification.html",
    subject_prefix: str = "Pendaftaran",
) -> bool:
    cfg = _get_config_or_fail()
    smtp_user = smtp_user or cfg["smtp_user"]
    smtp_pass = smtp_pass or cfg["smtp_pass"]
    smtp_host = cfg["smtp_server"]
    smtp_port = int(cfg["smtp_port"])
    from_name = cfg.get("from_name", "LearnAble")

    _validate_email(user_email, "Email pendaftar")

    subject = f"{subject_prefix} sebagai {user_role.title()} tidak disetujui - LearnAble"  # <= typo diperbaiki
    plain = (
        f"Halo {user_name}!\n\n"
        "Terima kasih telah mendaftar di LearnAble.\n"
        "Sayangnya, pendaftaran kamu belum disetujui oleh admin.\n\n"
        f"Nama Pendaftar: {user_name}\n"
        f"Email Pendaftar: {user_email}\n"
        f"Role Pendaftar: {user_role.title()}\n\n"
        "Jika ada pertanyaan, silakan balas email ini.\n\n"
        "Salam,\nSistem LearnAble\n"
    )
    msg = _compose_message_with_logo(
        from_name=from_name,
        from_email=smtp_user,
        to_email=user_email,
        subject=subject,
        html_template=template_name,
        html_ctx={
            "user_name": user_name,
            "user_email": user_email,
            "user_role": user_role.title(),
        },
        plain_fallback=plain,
        logo_bytes=_load_logo_bytes(LOGO_PATH),
    )
    if admin_email:
        _validate_email(admin_email, "Email admin")
        msg["Reply-To"] = admin_email

    return _send_with_retry(
        smtp_host=smtp_host,
        smtp_port=smtp_port,
        smtp_user=smtp_user,
        smtp_pass=smtp_pass,
        msg=msg,
        to_addrs=[user_email],  # <= ✅ FIX: kirim ke user
        use_ssl=(smtp_port == 465),
        starttls_fallback=(smtp_port == 587),
    )
