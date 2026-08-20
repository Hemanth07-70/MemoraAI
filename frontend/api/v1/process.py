from http.server import BaseHTTPRequestHandler
import json, base64, os, tempfile


class handler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", 0))
            body = json.loads(self.rfile.read(length))
            job_type = body.get("jobType", "")
            result = _handle_ocr(body) if job_type == "OCR" else _handle_non_ocr(body)
            self._json(200, result)
        except Exception as e:
            self._json(500, {"success": False, "status": "FAILED", "message": str(e)})

    def _json(self, code, data):
        payload = json.dumps(data).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *_):
        pass


def _handle_ocr(body):
    import fitz

    file_content = body.get("fileContent")
    if not file_content:
        return {"success": False, "status": "FAILED", "message": "No file content provided"}

    file_bytes = base64.b64decode(file_content)
    tmp = tempfile.NamedTemporaryFile(suffix=".pdf", delete=False)
    try:
        tmp.write(file_bytes)
        tmp.close()

        doc = fitz.open(tmp.name)
        if doc.is_encrypted:
            return {"success": False, "status": "FAILED", "message": "PDF is encrypted"}

        pages = [doc[i].get_text("text") for i in range(len(doc))]
        doc.close()

        full_text = "\n".join(pages).strip()
        return {
            "success": True,
            "status": "COMPLETED",
            "message": "Text extracted successfully",
            "pageCount": len(pages),
            "wordCount": len(full_text.split()),
            "characterCount": len(full_text),
            "text": full_text,
        }
    finally:
        try:
            os.unlink(tmp.name)
        except OSError:
            pass


def _handle_non_ocr(body):
    return {"success": True, "status": "PROCESSING", "message": "Job accepted"}
