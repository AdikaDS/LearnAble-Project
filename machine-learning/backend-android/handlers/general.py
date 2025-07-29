from flask import jsonify


def handle_welcome():
    chips = [{
        "text": "Jenjang SD"
    }, {
        "text": "Jenjang SMP"
    }, {
        "text": "Jenjang SMA"
    }]
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
    return jsonify(response)
