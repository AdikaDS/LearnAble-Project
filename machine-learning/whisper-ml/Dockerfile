FROM python:3.10-slim

# Install git dan ffmpeg (dibutuhkan whisper)
RUN apt-get update && apt-get install -y git ffmpeg && apt-get clean

# Set direktori kerja
WORKDIR /app

# Salin file requirements dan install dependencies
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

# Salin semua file ke dalam container
COPY ./app /app

# Jalankan aplikasi dengan gunicorn + uvicorn worker
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5000"]