from http.server import BaseHTTPRequestHandler
import json, os, urllib.request, urllib.error

_HF_URL = "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2"
_DIM = 384


class handler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", 0))
            body = json.loads(self.rfile.read(length))
            text = body.get("text", "")
            if not text.strip():
                raise ValueError("Empty text")

            embedding = _embed(text)
            self._json(200, {"success": True, "dimension": _DIM, "embedding": embedding})
        except Exception as e:
            self._json(500, {"success": False, "error": str(e), "embedding": []})

    def _json(self, code, data):
        payload = json.dumps(data).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *_):
        pass


def _embed(text: str) -> list:
    token = os.environ.get("HF_API_KEY", "")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    payload = json.dumps({"inputs": text, "options": {"wait_for_model": True}}).encode()
    req = urllib.request.Request(_HF_URL, data=payload, headers=headers, method="POST")

    with urllib.request.urlopen(req, timeout=30) as resp:
        result = json.loads(resp.read())

    # HF returns [[float x 384]] for single input
    if isinstance(result, list) and result and isinstance(result[0], list):
        return result[0]
    return result
