import os
from datetime import datetime


def is_allowed_file(filename: str, allowed_extensions: set) -> bool:
    return os.path.splitext(filename)[1].lower() in allowed_extensions


def get_word_count(text: str) -> int:
    return len(text.strip().split())


def get_timestamp() -> str:
    return datetime.now().isoformat()