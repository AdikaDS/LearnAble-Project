from google.cloud import dialogflow_v2 as dialogflow
from google.cloud import firestore
from google.oauth2 import service_account
from os import getenv
import logging
from dotenv import load_dotenv

load_dotenv()


logging.basicConfig(level=logging.INFO)

# Konfigurasi
PROJECT_ID = getenv("PROJECT_ID")
CREDENTIALS_PATH = getenv(
    "GOOGLE_APPLICATION_CREDENTIALS")
AGENT_LANGUAGE = "id"
ENTITY_SUBJECT = "SubjectName"
ENTITY_LESSON = "LessonName"
INTENT_DISPLAY1 = "Pilih Topik Pelajaran"
INTENT_DISPLAY2 = "Pilih Subbab"
INTENT_DISPLAY3 = "Pilih Teori Subbab"
MAX_PHRASES = 10

# Inisialisasi kredensial dan klien
credentials = service_account.Credentials.from_service_account_file(
    CREDENTIALS_PATH)
firestore_client = firestore.Client(credentials=credentials,
                                    project=PROJECT_ID)
df_entity_client = dialogflow.EntityTypesClient(credentials=credentials)
df_intent_client = dialogflow.IntentsClient(credentials=credentials)
df_parent = f"projects/{PROJECT_ID}/agent"


def get_intent_by_display_name(display_name):
    intents = df_intent_client.list_intents(request={"parent": df_parent})
    return next((i for i in intents if i.display_name == display_name), None)


def sync_training_phrases(intent_display_name, phrases):
    intent = get_intent_by_display_name(intent_display_name)
    if not intent:
        logging.error("❌ Intent '%s' tidak ditemukan!", intent_display_name)
        return

    training_phrases = [
        dialogflow.Intent.TrainingPhrase(
            parts=[dialogflow.Intent.TrainingPhrase.Part(text=phrase)])
        for phrase in phrases
    ]

    updated_intent = dialogflow.Intent(name=intent.name,
                                       display_name=intent.display_name,
                                       training_phrases=training_phrases,
                                       messages=intent.messages,
                                       parameters=intent.parameters,
                                       webhook_state=dialogflow.Intent.WebhookState.WEBHOOK_STATE_ENABLED)

    df_intent_client.update_intent(intent=updated_intent,
                                   language_code=AGENT_LANGUAGE)
    logging.info("✅ Sinkronisasi training phrase ke intent '%s' selesai",
                 intent_display_name)


def sync_subjects_to_entity():
    subjects = firestore_client.collection("subjects").stream()
    entities = [
        dialogflow.EntityType.Entity(value=name, synonyms=[name])
        for doc in subjects if (name := doc.to_dict().get("name"))
    ]

    entity_types = df_entity_client.list_entity_types(parent=df_parent)
    matched_entity = next(
        (et for et in entity_types if et.display_name == ENTITY_SUBJECT), None)

    entity_type = dialogflow.EntityType(
        display_name=ENTITY_SUBJECT,
        kind=dialogflow.EntityType.Kind.KIND_MAP,
        entities=entities)

    if matched_entity:
        entity_type.name = matched_entity.name
        df_entity_client.update_entity_type(entity_type=entity_type)
        logging.info("🔄 Entity SubjectName diperbarui")
    else:
        df_entity_client.create_entity_type(parent=df_parent,
                                            entity_type=entity_type)
        logging.info("✨ Entity SubjectName dibuat")


def sync_lessons_to_entity():
    lessons = firestore_client.collection("lessons").stream()
    entities = [
        dialogflow.EntityType.Entity(value=title, synonyms=[title])
        for doc in lessons if (title := doc.to_dict().get("title"))
    ]

    entity_types = df_entity_client.list_entity_types(parent=df_parent)
    matched_entity = next(
        (et for et in entity_types if et.display_name == ENTITY_LESSON), None)

    entity_type = dialogflow.EntityType(
        display_name=ENTITY_LESSON,
        kind=dialogflow.EntityType.Kind.KIND_MAP,
        entities=entities)

    if matched_entity:
        entity_type.name = matched_entity.name
        df_entity_client.update_entity_type(entity_type=entity_type)
        logging.info("🔄 Entity LessonName diperbarui")
    else:
        df_entity_client.create_entity_type(parent=df_parent,
                                            entity_type=entity_type)
        logging.info("✨ Entity LessonName dibuat")


def sync_all_training_phrases():
    # Ambil 10 subject untuk intent "Pilih Topik Pelajaran"
    subject_docs = firestore_client.collection("subjects").limit(
        MAX_PHRASES).stream()
    subject_phrases = [
        doc.to_dict().get("name") for doc in subject_docs
        if doc.to_dict().get("name")
    ]
    sync_training_phrases(INTENT_DISPLAY1, subject_phrases)

    # Ambil 10 lesson untuk intent "Pilih Subbab"
    lesson_docs = firestore_client.collection("lessons").limit(
        MAX_PHRASES).stream()
    lesson_phrases = [
        doc.to_dict().get("title") for doc in lesson_docs
        if doc.to_dict().get("title")
    ]
    sync_training_phrases(INTENT_DISPLAY2, lesson_phrases)

    # Ambil 10 lesson untuk intent "Pilih Teori Subbab"
    subbab_docs = firestore_client.collection("sub_bab").limit(
        MAX_PHRASES).stream()
    subbab_phrases = [
        doc.to_dict().get("title") for doc in subbab_docs
        if doc.to_dict().get("title")
    ]
    sync_training_phrases(INTENT_DISPLAY3, subbab_phrases)


if __name__ == "__main__":
    sync_subjects_to_entity()
    sync_lessons_to_entity()
    sync_all_training_phrases()
    print("✅ Sinkronisasi entity & training phrase selesai!")
