import logging

def get_context_param(req, context_name_suffix, param_key):
    contexts = req.get("queryResult", {}).get("outputContexts", [])
    for context in contexts:
        if context_name_suffix in context.get("name", ""):
            return context.get("parameters", {}).get(param_key)
    return None

async def get_previous_chips(req, db):
    """
    Helper function untuk mendapatkan chip sebelumnya berdasarkan context yang ada.
    Returns: dict dengan chips, message, context_name, context_params atau None jika tidak ada context sebelumnya
    """
    from chatbot.services.firestore_service import db as firestore_db
    
    if isinstance(req, dict):
        query_result = req.get("queryResult", {})
        session = req.get("session", "")
    else:
        query_result = req.queryResult if hasattr(req, 'queryResult') else {}
        session = req.session if hasattr(req, 'session') else ""
    
    output_contexts = query_result.get("outputContexts", [])
    input_contexts = query_result.get("inputContexts", [])
    all_contexts = output_contexts + input_contexts
    
    for context in all_contexts:
        if "pilihsubbab-followup" in context.get("name", ""):
            params = context.get("parameters", {})
            level = params.get("school_level")
            subject_name = params.get("subject_name")
            lesson_name = params.get("lesson_name")
            
            if level and subject_name and lesson_name:
                try:
                    subject_query = firestore_db.collection("subjects") \
                        .where("name", "==", subject_name) \
                        .where("schoolLevel", "==", level) \
                        .limit(1).stream()
                    subject_doc = next(subject_query, None)
                    if subject_doc:
                        subject_id = subject_doc.to_dict().get("idSubject")
                        lesson_query = firestore_db.collection("lessons") \
                            .where("title", "==", lesson_name) \
                            .where("idSubject", "==", subject_id) \
                            .limit(1).stream()
                        lesson_doc = next(lesson_query, None)
                        if lesson_doc:
                            lesson_id = lesson_doc.id
                            subbab_query = firestore_db.collection("sub_bab") \
                                .where("lessonId", "==", lesson_id).stream()
                            chips = []
                            for doc in subbab_query:
                                title = doc.to_dict().get("title")
                                if title:
                                    chips.append({"text": title})
                            if chips:
                                return {
                                    "chips": chips,
                                    "message": f"Berikut sub-bab dari {lesson_name}:",
                                    "context_name": f"{session}/contexts/pilihsubbab-followup",
                                    "context_params": {
                                        "school_level": level,
                                        "subject_name": subject_name,
                                        "lesson_name": lesson_name
                                    }
                                }
                except Exception as e:
                    logging.error(f"Error getting previous subbab chips: {e}")
    
    for context in all_contexts:
        if "pilihpelajaran-followup" in context.get("name", ""):
            params = context.get("parameters", {})
            level = params.get("school_level")
            subject_name = params.get("subject_name")
            
            if level and subject_name:
                try:
                    subject_query = firestore_db.collection("subjects") \
                        .where("name", "==", subject_name) \
                        .where("schoolLevel", "==", level) \
                        .limit(1).stream()
                    subject_doc = next(subject_query, None)
                    if subject_doc:
                        subject_id = subject_doc.to_dict().get("idSubject")
                        lesson_query = firestore_db.collection("lessons") \
                            .where("idSubject", "==", subject_id).stream()
                        chips = []
                        for doc in lesson_query:
                            title = doc.to_dict().get("title")
                            if title:
                                chips.append({"text": title})
                        if chips:
                            return {
                                "chips": chips,
                                "message": f"Materi untuk {subject_name} jenjang {level.upper()}:",
                                "context_name": f"{session}/contexts/pilihpelajaran-followup",
                                "context_params": {
                                    "subject_name": subject_name,
                                    "school_level": level
                                }
                            }
                except Exception as e:
                    logging.error(f"Error getting previous lesson chips: {e}")
    
    for context in all_contexts:
        if "pilihjenjang-followup" in context.get("name", ""):
            params = context.get("parameters", {})
            level = params.get("school_level")
            
            if level:
                try:
                    subject_query = firestore_db.collection("subjects") \
                        .where("schoolLevel", "==", level).stream()
                    chips = []
                    for doc in subject_query:
                        name = doc.to_dict().get("name")
                        if name:
                            chips.append({"text": name})
                    if chips:
                        return {
                            "chips": chips,
                            "message": f"Berikut pelajaran untuk jenjang {level.upper()} yang tersedia:",
                            "context_name": f"{session}/contexts/pilihjenjang-followup",
                            "context_params": {
                                "school_level": level
                            }
                        }
                except Exception as e:
                    logging.error(f"Error getting previous subject chips: {e}")
            
            chips = [
                {"text": "Jenjang SD"},
                {"text": "Jenjang SMP"},
                {"text": "Jenjang SMA"}
            ]
            return {
                "chips": chips,
                "message": "👋 Silakan pilih ulang jenjang pendidikan terlebih dahulu:",
                "context_name": None,
                "context_params": None
            }
    return None