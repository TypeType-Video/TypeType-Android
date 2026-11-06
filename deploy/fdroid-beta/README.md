# TypeType F-Droid beta channel

This directory contains the system-side deployment for the separate beta repository.

## Install on the VPS

Upload this directory to the Debian host and run the installer as root:

```bash
./install.sh
```

The installer creates:

- `/srv/fdroid-beta/signing`
- `/srv/fdroid-beta/secrets`
- `/srv/fdroid-beta/public`
- the webhook, path, and sync systemd units

Copy these stable values before the first sync:

```bash
install -o debian -g debian -m 0600 \
  /srv/fdroid/signing/state/trusted-apk-cert-sha256 \
  /srv/fdroid-beta/signing/state/trusted-apk-cert-sha256
```

The installer also copies the build context of the patched `typetype-fdroidserver:beta-patched` image to
`/srv/fdroid-beta/signing/image`. That image exists only on this host, so each sync keeps an archive of it in
`/srv/fdroid-beta/signing/state` and reloads it, or rebuilds it from that context, when Docker no longer has it.

The beta repository signing key is intentionally separate from the stable repository key. The APK signing certificate remains identical, so users can move between stable and beta without reinstalling.

## Caddy

Insert `caddy.snippet` inside the `typetype.video` site block, then reload Caddy.

## GitHub webhook

Create a repository webhook with:

- URL: `https://typetype.video/hooks/fdroid-beta`
- Content type: `application/json`
- Secret: contents of `/srv/fdroid-beta/secrets/github-webhook-secret`
- Events: releases only

## First sync

Merging `dev` into `beta` starts Android Beta. The workflow publishes a signed prerelease, the webhook queues the sync, and F-Droid beta users receive the update.
