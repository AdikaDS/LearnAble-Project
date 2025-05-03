# Whisper Speech Recognition API

This project implements OpenAI's Whisper model for automatic speech recognition (ASR) as a REST API service. Whisper is a powerful speech recognition model that can transcribe audio files with high accuracy.

## Features

- REST API endpoint for audio transcription
- Support for various audio formats
- Optimized for Indonesian language transcription
- Simple and easy to use
- Azure App Service deployment ready (with GitHub integration)

## Requirements

- Python 3.9 or higher
- PyTorch
- OpenAI Whisper
- FastAPI
- Uvicorn
- Gunicorn
- Azure subscription
- GitHub account

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

## Azure App Service Deployment with GitHub

### Prerequisites

1. Azure subscription
2. GitHub account
3. Repository containing this project

### Deployment Steps

1. **Create Resource Group and App Service**:
   - Login to [Azure Portal](https://portal.azure.com)
   - Click "Create a resource"
   - Search for "Web App"
   - Click "Create"
   - Fill in:
     - Subscription: Your subscription
     - Resource Group: Create new (e.g., "whisper-api-rg")
     - Name: e.g., "whisper-api-app"
     - Publish: Code
     - Runtime stack: Python 3.9
     - Operating System: Linux
     - Region: Choose "Southeast Asia" or your preferred region
     - App Service Plan: Create new (P1v2 recommended)
   - Click "Review + create" then "Create"

2. **Configure GitHub Deployment**:
   - Go to your Web App in Azure Portal
   - Under "Deployment" > "Deployment Center"
   - Select "GitHub" as source
   - Click "Authorize" to connect your GitHub account
   - Select:
     - Organization: Your GitHub organization
     - Repository: Your repository
     - Branch: main (or your preferred branch)
   - Click "Save"

3. **Configure App Settings**:
   - Go to "Configuration" > "Application settings"
   - Add these settings:
     ```
     PYTHON_VERSION=3.9
     SCM_DO_BUILD_DURING_DEPLOYMENT=true
     ```
   - Click "Save"

4. **Enable Continuous Deployment**:
   - In Deployment Center, go to "Continuous Deployment"
   - Enable "Continuous Deployment"
   - Every push to your selected branch will trigger a deployment

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