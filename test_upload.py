import requests
import time
import sys
import json

BASE_URL = "http://localhost:8080/api/v1"

def test_pipeline():
    print("1. Logging in...")
    login_resp = requests.post(f"{BASE_URL}/auth/login", json={
        "email": "test_e2e@example.com",
        "password": "password123"
    })
    
    if login_resp.status_code != 200:
        print(f"Login failed: {login_resp.text}")
        sys.exit(1)
        
    token = login_resp.json().get('data', {}).get('token')
    if not token:
        print("No token received")
        sys.exit(1)
        
    headers = {"Authorization": f"Bearer {token}"}
    
    print("4. Uploading Document...")
    files = {'file': ('dummy.pdf', open('dummy.pdf', 'rb'), 'application/pdf')}
    upload_resp = requests.post(f"{BASE_URL}/documents/upload", headers=headers, files=files)
    
    print(f"Upload Resp: {upload_resp.status_code} - {upload_resp.text}")
    if upload_resp.status_code not in [200, 201]:
        sys.exit(1)
        
    doc = upload_resp.json().get('data', {})
    if isinstance(doc, str):
        # Wait maybe data is the ID?
        doc_id = upload_resp.json().get('data', {}).get('id') if isinstance(upload_resp.json().get('data'), dict) else None
    else:
        doc_id = doc.get('id')
    
    # If doc is missing, try to get the list and take the first
    if not doc_id:
        print("Fetching all documents to find ID...")
        docs = requests.get(f"{BASE_URL}/documents", headers=headers).json().get('data', [])
        if docs:
            doc_id = docs[0].get('id')
    
    if not doc_id:
        print("Could not retrieve document ID.")
        sys.exit(1)

    print(f"Polling Document Status for ID {doc_id}...")
    while True:
        doc_resp = requests.get(f"{BASE_URL}/documents", headers=headers)
        docs = doc_resp.json().get('data', [])
        if not isinstance(docs, list):
            print(f"Unexpected /documents response: {docs}")
            sys.exit(1)
        
        my_doc = next((d for d in docs if d['id'] == doc_id), None)
        
        if not my_doc:
            print("Document not found in list")
            sys.exit(1)
            
        status = my_doc.get('status')
        print(f"Current Status: {status}")
        
        if status in ['READY', 'FAILED']:
            print(f"Terminal status reached: {status}")
            break
            
        time.sleep(3)

if __name__ == "__main__":
    test_pipeline()
