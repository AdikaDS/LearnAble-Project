#!/bin/bash
pip install --upgrade pip
pip install -r requirements.txt
exec gunicorn -w 1 -k uvicorn.workers.UvicornWorker app:app --bind=0.0.0.0:8000
