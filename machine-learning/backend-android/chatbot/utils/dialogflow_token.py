from google.oauth2 import service_account
import google.auth.transport.requests


def get_dialogflow_token():

    SERVICE_ACCOUNT_FILE = "/etc/secrets/credentials.json"
    SCOPES = ["https://www.googleapis.com/auth/cloud-platform"]

    credentials = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, scopes=SCOPES)
    auth_req = google.auth.transport.requests.Request()
    credentials.refresh(auth_req)
    return credentials.token
