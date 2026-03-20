{\rtf1\ansi\ansicpg1252\cocoartf2868
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\paperw11900\paperh16840\margl1440\margr1440\vieww11520\viewh8400\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural\partightenfactor0

\f0\fs24 \cf0 import os\
import json\
import requests\
\
# 1. Environment Variables setup by GitHub Actions\
API_KEY = os.environ.get("LLM_API_KEY")\
GH_TOKEN = os.environ.get("GH_TOKEN")\
PR_NUMBER = os.environ.get("PR_NUMBER")\
REPO_NAME = os.environ.get("REPO_NAME")\
\
# Gemini API Endpoint for Gemini 2.5 Flash\
LLM_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"\
\
def read_file(filepath):\
    with open(filepath, 'r') as file:\
        return file.read()\
\
def main():\
    if not API_KEY:\
        print("Error: LLM_API_KEY is missing. Please add your Gemini API Key to GitHub Secrets.")\
        return\
\
    print("Reading Agent Prompts and PR Diff...")\
    \
    # Load your Master Agent instructions\
    try:\
        master_agent_prompt = read_file(".github/scripts/master-reviewer-agent.md")\
    except FileNotFoundError:\
        print("Error: master-reviewer-agent.md not found.")\
        return\
    \
    # Load the actual code changes\
    try:\
        pr_diff = read_file("pr_diff.patch")\
    except FileNotFoundError:\
        print("No diff found. Skipping review.")\
        return\
\
    if not pr_diff.strip():\
        print("Diff is empty. Skipping review.")\
        return\
\
    print("Sending code to Gemini Agent for review...")\
    \
    # 2. Construct the prompt and payload for Gemini API\
    system_instruction = f"\{master_agent_prompt\}\\n\\nReview the following code diff based strictly on your role and constraints."\
    user_prompt = f"Here is the git diff for the PR:\\n\\n```diff\\n\{pr_diff\}\\n```"\
    \
    headers = \{\
        "Content-Type": "application/json",\
        "x-goog-api-key": API_KEY  # Gemini uses this header for auth\
    \}\
    \
    # Gemini requires a specific JSON payload structure\
    payload = \{\
        "systemInstruction": \{\
            "parts": [\{"text": system_instruction\}]\
        \},\
        "contents": [\
            \{\
                "role": "user",\
                "parts": [\{"text": user_prompt\}]\
            \}\
        ],\
        "generationConfig": \{\
            "temperature": 0.2  # Keeps the review strict, analytical, and less "creative"\
        \}\
    \}\
\
    # 3. Call the Gemini API\
    response = requests.post(LLM_API_URL, headers=headers, json=payload)\
    response_data = response.json()\
    \
    if "error" in response_data:\
        print(f"API Error: \{response_data['error']['message']\}")\
        return\
        \
    try:\
        # Extract the text response from Gemini's payload structure\
        review_comment = response_data["candidates"][0]["content"]["parts"][0]["text"]\
    except KeyError:\
        print("Error parsing Gemini response. Unexpected format.")\
        print(response_data)\
        return\
\
    print("Review complete. Posting comment to GitHub PR...")\
\
    # 4. Post the AI's response back to GitHub as a PR comment\
    gh_headers = \{\
        "Authorization": f"token \{GH_TOKEN\}",\
        "Accept": "application/vnd.github.v3+json"\
    \}\
    gh_url = f"https://api.github.com/repos/\{REPO_NAME\}/issues/\{PR_NUMBER\}/comments"\
    \
    gh_response = requests.post(gh_url, headers=gh_headers, json=\{"body": review_comment\})\
    \
    if gh_response.status_code == 201:\
        print("\uc0\u9989  Successfully posted Gemini Code Review to PR!")\
    else:\
        print(f"\uc0\u10060  Failed to post to GitHub: \{gh_response.text\}")\
\
if __name__ == "__main__":\
    main()}