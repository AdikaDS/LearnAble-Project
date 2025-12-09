import redis.asyncio as redis
from os import getenv
from dotenv import load_dotenv
import logging

load_dotenv()

try:
    redis_host = getenv("REDIS_HOST")
    redis_port = int(getenv("REDIS_PORT"))
    redis_password = getenv("REDIS_PASSWORD")
    
    redis_client = redis.Redis(
        host=redis_host,
        port=redis_port,
        password=redis_password,
        decode_responses=True,
        socket_connect_timeout=5,
        socket_timeout=5,
        retry_on_timeout=True,
        health_check_interval=30
    )
    logging.info("✅ Redis client berhasil diinisialisasi")
except Exception as e:
    logging.error(f"❌ Gagal menginisialisasi Redis client: {str(e)}")
    redis_client = None