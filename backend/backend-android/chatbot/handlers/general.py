async def handle_welcome(session: str = None):
    """
    Handler untuk Welcome/Menu Utama.
    Reset semua context dan kembali ke awal (pilih jenjang).
    """
    import logging
    logging.info("🏠 Reset semua context dan kembali ke menu utama")
    
    chips = [
        {"text": "Jenjang SD"},
        {"text": "Jenjang SMP"},
        {"text": "Jenjang SMA"}
    ]
    
    # Clear semua context yang mungkin aktif dengan set lifespanCount: 0
    # Context yang perlu di-clear:
    # - waiting_custom_answer
    # - pilihjenjang-followup
    # - pilihpelajaran-followup
    # - pilihsubbab-followup
    # - waiting_theory_answer
    
    contexts_to_clear = [
        "waiting_custom_answer",
        "pilihjenjang-followup",
        "pilihpelajaran-followup",
        "pilihsubbab-followup",
        "waiting_theory_answer"
    ]
    
    output_contexts = []
    if session:
        for context_name in contexts_to_clear:
            output_contexts.append({
                "name": f"{session}/contexts/{context_name}",
                "lifespanCount": 0  # Clear context dengan set lifespanCount: 0
            })
        logging.info(f"🧹 Clearing {len(output_contexts)} contexts")
    
    response = {
        "fulfillmentMessages": [{
            "text": {
                "text": [
                    "👋 Halo! Selamat datang di LearnAble! 📚 Yuk mulai petualangan belajarmu bersama kami. Silakan pilih jenjang pendidikan di bawah ini:"
                ]
            }
        }, {
            "payload": {
                "richContent": [[{
                    "type": "chips",
                    "options": chips
                }]]
            }
        }]
    }
    
    if output_contexts:
        response["outputContexts"] = output_contexts
    
    return response
