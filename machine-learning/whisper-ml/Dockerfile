FROM python:3.10-slim

# Install git dan ffmpeg (dibutuhkan whisper)
RUN apt-get update && apt-get install -y git ffmpeg && apt-get clean

# Set direktori kerja
WORKDIR /app

# Salin file requirements dan install dependencies
COPY requirements.txt .
RUN pip install --upgrade pip && pip install -r requirements.txt

# Salin semua file ke dalam container
COPY . .

# Ekspose port untuk aplikasi
EXPOSE 8000

# Jalankan aplikasi dengan gunicorn + uvicorn worker
CMD ["gunicorn", "app:app", "-w", "1", "-k", "uvicorn.workers.UvicornWorker", "--bind", "0.0.0.0:8000"]