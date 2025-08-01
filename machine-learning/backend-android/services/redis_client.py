import redis.asyncio as redis
from os import getenv
from dotenv import load_dotenv

load_dotenv()

redis_client = redis.Redis(
    host=getenv("REDIS_HOST"),
    port=int(getenv("REDIS_PORT")),
    password=getenv("REDIS_PASSWORD"),
    decode_responses=True
)