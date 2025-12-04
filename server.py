# Copyright 2025 A^3
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from flask import Flask, request, jsonify
from flask_cors import CORS
from pyngrok import ngrok
import google.generativeai as genai
import json
import threading
import uuid
import time
import re

NGROK_TOKEN = "enter_your_ngroktoken here"
GEMINI_KEY  = "enter_api_key_here"

ngrok.set_auth_token(NGROK_TOKEN)
genai.configure(api_key=GEMINI_KEY)
model = genai.GenerativeModel('gemini-2.5-flash')

app = Flask(__name__)
CORS(app)

def clean_json_string(text):
    start = text.find('[')
    end = text.rfind(']')
    if start != -1 and end != -1:
        return text[start:end+1]
    return text


@app.route('/generate-quiz', methods=['POST'])
def generate_quiz():
    try:
        data = request.json
        topic = data.get('topic', 'General Knowledge')

        prompt = f"""
        You are a quiz API. Return a raw JSON Array of 5 multiple-choice questions about: {topic}.

        Strict Rules:
        1. Return ONLY valid JSON.
        2. Do not use Markdown formatting (no ```json).
        3. The output must be a list of objects.

        JSON Structure:
        [
            {{
                "question": "Question text?",
                "options": ["A", "B", "C", "D"],
                "correctIndex": 0
            }}
        ]
        """

        result_holder = {}

        def worker():
            try:
                response = model.generate_content(prompt)
                raw = response.text
                json_str = clean_json_string(raw)
                result_holder["result"] = json.loads(json_str)
            except Exception as e:
                result_holder["error"] = str(e)

        thread = threading.Thread(target=worker)
        thread.start()

        thread.join(timeout=20) 

        
        if thread.is_alive():
            return jsonify([{
                "question": "Server Timeout",
                "options": ["Try Again", "Check Internet", "Reduce Topic Size", "Wait"],
                "correctIndex": 0
            }]), 500

      
        if "error" in result_holder:
            return jsonify([{
                "question": "Gemini Error",
                "options": ["Try Again", "Wrong API Key?", "Check Server Logs", "Unknown Error"],
                "correctIndex": 0
            }]), 500

        return jsonify(result_holder["result"])

    except Exception:
        return jsonify([{
            "question": "Unexpected Server Error",
            "options": ["Try Again", "Check Logs", "Restart Server", "Error"],
            "correctIndex": 0
        }]), 500


ngrok.kill()
public_url = ngrok.connect(5000).public_url
print("="*60)
print(" PUBLIC URL:", public_url)
print("="*60)

app.run(host="0.0.0.0", port=5000)
