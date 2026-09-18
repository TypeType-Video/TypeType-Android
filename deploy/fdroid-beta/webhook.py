#!/usr/bin/env python3
import hashlib
import hmac
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


SECRET_FILE = Path("/srv/fdroid-beta/secrets/github-webhook-secret")
TRIGGER_FILE = Path("/run/typetype-fdroid-beta-webhook/sync")
MAX_PAYLOAD_SIZE = 1024 * 1024


class WebhookHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/hooks/fdroid-beta":
            self.send_error(404)
            return

        try:
            content_length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_error(400)
            return

        if content_length <= 0 or content_length > MAX_PAYLOAD_SIZE:
            self.send_error(413)
            return

        payload = self.rfile.read(content_length)
        supplied_signature = self.headers.get("X-Hub-Signature-256", "")
        expected_signature = "sha256=" + hmac.new(
            SECRET_FILE.read_bytes().strip(),
            payload,
            hashlib.sha256,
        ).hexdigest()

        if not hmac.compare_digest(supplied_signature, expected_signature):
            self.send_error(403)
            return

        event = self.headers.get("X-GitHub-Event", "")
        if event not in {"ping", "release"}:
            self.send_response(204)
            self.end_headers()
            return

        try:
            data = json.loads(payload)
        except json.JSONDecodeError:
            self.send_error(400)
            return

        release = data.get("release", {})
        if (
            data.get("action") == "published"
            and not release.get("draft", True)
            and release.get("prerelease", False)
        ):
            TRIGGER_FILE.touch(exist_ok=True)
            self.send_response(202)
        else:
            self.send_response(204)
        self.end_headers()

    def do_GET(self):
        self.send_error(405)

    def log_message(self, message, *args):
        print(f"{self.address_string()} - {message % args}", flush=True)


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 19091), WebhookHandler).serve_forever()
