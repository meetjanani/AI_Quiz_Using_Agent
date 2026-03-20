```python
import os
import json
import requests
import re
import subprocess

API_KEY = os.environ.get("LLM_API_KEY")
GH_TOKEN = os.environ.get("GH_TOKEN")
PR_NUMBER = os.environ.get("PR_NUMBER")
REPO_NAME = os.environ.get("REPO_NAME")
# GitHub Actions sets this environment variable automatically
HEAD_REF = os.environ.get("GITHUB_HEAD_REF")

LLM_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

def read_file(filepath):
    with open(filepath, 'r') as file:
        return file.read()

def run_command(command):
    """Utility to run shell commands (like git)"""
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Command failed: {command}\nError: {result.stderr}")
    return result.returncode == 0

def main():
    if not API_KEY:
        print("Error: LLM_API_KEY is missing.")
        return

    master_agent_prompt = read_file(".github/scripts/master-reviewer-agent.md")

    try:
        pr_diff = read_file("pr_diff.patch")
    except FileNotFoundError:
        print("No diff found. Skipping review.")
        return

    print("Sending code to Gemini Agent for review & fixing...")

    system_instruction = f"{master_agent_prompt}\n\nReview the following code diff. If you find issues, output the complete corrected file using the <file_update> tag."
    user_prompt = f"Here is the git diff:\n\n```diff\n{pr_diff}\n```"

    headers = {
        "Content-Type": "application/json",
        "x-goog-api-key": API_KEY
    }

    payload = {
        "systemInstruction": {"parts": [{"text": system_instruction}]},
        "contents": [{"role": "user", "parts": [{"text": user_prompt}]}],
        "generationConfig": {"temperature": 0.1}
    }

    response = requests.post(LLM_API_URL, headers=headers, json=payload)
    response_data = response.json()
    
    # Re-introducing the API error check
    if "error" in response_data:
        print(f"API Error: {response_data['error']['message']}")
        return

    try:
        review_comment = response_data["candidates"][0]["content"]["parts"][0]["text"]
    except KeyError:
        print("Error parsing Gemini response. Unexpected format or missing candidates.")
        print(response_data) # Print full response for debugging
        return

    print("Searching for auto-fixes in AI response...")
    # Regex to find <file_update path="..."> ...content...