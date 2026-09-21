FROM registry.gitlab.com/fdroid/docker-executable-fdroidserver@sha256:2ad114edee50aad92fcc83fe7d801d8922780a021db070ae9f4c49f6f647c037

COPY patch_androguard.py /tmp/patch_androguard.py
RUN python3 /tmp/patch_androguard.py && rm /tmp/patch_androguard.py
