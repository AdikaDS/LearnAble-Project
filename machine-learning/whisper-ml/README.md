# Whisper Speech Recognition API

This project implements OpenAI's Whisper model for automatic speech recognition (ASR) as a REST API service. Whisper is a powerful speech recognition model that can transcribe audio files with high accuracy.

## Features

- REST API endpoint for audio transcription
- Support for various audio formats
- Optimized for Indonesian language transcription
- Simple and easy to use
- Azure App Service deployment ready (with or without Docker)

## Requirements

- Python 3.9 or higher
- PyTorch
- OpenAI Whisper
- FastAPI
- Uvicorn
- Gunicorn
- Azure subscription

## Installation

1. Create a virtual environment (recommended):
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Install the required packages:
```bash
pip install -r requirements.txt
```

## Running the Server Locally

To start the server locally:

```bash
python server.py
```

The server will start on `http://localhost:8000`

## API Usage

### Transcribe Audio

Send a POST request to `/transcribe` endpoint with an audio file:

```bash
curl -X POST "http://localhost:8000/transcribe" \
     -H "accept: application/json" \
     -H "Content-Type: multipart/form-data" \
     -F "file=@path/to/your/audio.wav"
```

Response:
```json
{
    "text": "transcribed text here",
    "language": "id"
}
```

## Azure App Service Deployment

### Option 1: Deploy Without Docker (Recommended for Development)

1. **Create Resource Group and App Service**:
   - Follow steps in Azure Portal as described in previous section
   - Make sure to select "Code" instead of "Docker" when creating Web App

2. **Deploy Using Git**:
   ```bash
   # Install Azure CLI if not already installed
   az login

   # Configure local git
   az webapp deployment source config-local-git --name your-app-name --resource-group your-resource-group

   # Deploy
   git remote add azure <your-git-url>
   git push azure main
   ```

### Option 2: Deploy With Docker (Recommended for Production)

1. **Create Container Registry**:
   - In Azure Portal, create an Azure Container Registry (ACR)
   - Note down the login server, username, and password

2. **Build and Push Docker Image**:
   ```bash
   # Login to ACR
   az acr login --name your-registry-name

   # Build image
   docker build -t your-registry-name.azurecr.io/whisper-api:latest .

   # Push image
   docker push your-registry-name.azurecr.io/whisper-api:latest
   ```

3. **Create Web App with Docker**:
   - In Azure Portal, create a new Web App
   - Select "Docker" instead of "Code"
   - Choose your ACR and image
   - Configure the following settings:
     - WEBSITES_PORT: 8000
     - WEBSITES_CONTAINER_START_TIME_LIMIT: 600

4. **Deploy**:
   - The app will automatically deploy when you push to ACR
   - You can also set up continuous deployment

### Configuration for Both Options

1. **App Settings**:
   - Add these settings in Azure Portal:
     - PYTHON_VERSION: 3.9
     - SCM_DO_BUILD_DURING_DEPLOYMENT: true (for non-Docker deployment)

2. **Scaling**:
   - For development: B1 plan (1.75GB RAM)
   - For production: P1v2 plan (3.5GB RAM) or higher
   - Set up auto-scaling rules if needed

### Using the Deployed API

After deployment, you can access your API at:
```
https://your-web-app-name.azurewebsites.net/transcribe
```

## Local Docker Deployment

1. Build the Docker image:
```bash
docker build -t whisper-api .
```

2. Run the container:
```bash
docker run -p 8000:8000 whisper-api
```

## Model Information

This implementation uses the "base" model of Whisper, which provides a good balance between accuracy and performance. The model is specifically optimized for Indonesian language transcription.

## Error Handling

The API includes comprehensive error handling and logging. If an error occurs during transcription, you will receive a detailed error message in the response.