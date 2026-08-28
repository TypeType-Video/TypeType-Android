#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root." >&2
  exit 1
fi

source_dir="$(cd "$(dirname "$0")" && pwd)"
signing_dir=/srv/fdroid-beta/signing
secrets_dir=/srv/fdroid-beta/secrets
public_dir=/srv/fdroid-beta/public
image=typetype-fdroidserver:beta-patched

install -d -o debian -g debian -m 0755 /srv/fdroid-beta
install -d -o debian -g debian -m 0755 "$signing_dir"
install -d -o debian -g debian -m 0700 "$signing_dir/state" "$secrets_dir" "$signing_dir/tmp"
install -d -o debian -g debian -m 0755 "$public_dir"
install -d -o debian -g debian -m 0755 "$signing_dir/metadata/dev.typetype.android/en-US/changelogs"
install -d -o debian -g debian -m 0755 "$signing_dir/repo"
install -d -o debian -g debian -m 0755 "$signing_dir/repo/icons"

docker build -t "$image" "$source_dir"

install -o debian -g debian -m 0755 "$source_dir/sync-github-beta-release.sh" "$signing_dir/sync-github-beta-release.sh"
install -o debian -g debian -m 0755 "$source_dir/webhook.py" "$signing_dir/typetype-fdroid-beta-webhook.py"

if [ ! -s "$secrets_dir/github-webhook-secret" ]; then
  openssl rand -hex 32 | install -o debian -g debian -m 0600 /dev/stdin "$secrets_dir/github-webhook-secret"
fi

if [ ! -s "$signing_dir/state/trusted-apk-cert-sha256" ]; then
  echo "Copy the stable trusted-apk-cert-sha256 file to $signing_dir/state." >&2
  exit 1
fi

if [ ! -s "$signing_dir/beta-keystore.p12" ]; then
  password="$(openssl rand -base64 36)"
  keytool -genkeypair \
    -storetype PKCS12 \
    -keystore "$signing_dir/beta-keystore.p12" \
    -storepass "$password" \
    -keypass "$password" \
    -alias typetype-beta \
    -dname 'CN=TypeType Beta F-Droid,OU=TypeType,O=TypeType,C=FR' \
    -keyalg RSA -keysize 3072 -validity 10950
  printf '%s' "$password" | install -o debian -g debian -m 0600 /dev/stdin "$secrets_dir/fdroid-repo-key-password"
fi

if [ ! -s "$signing_dir/config.yml" ]; then
  repo_password="$(cat "$secrets_dir/fdroid-repo-key-password")"
  install -o debian -g debian -m 0600 /dev/null "$signing_dir/config.yml"
  cat > "$signing_dir/config.yml" <<EOF
sdk_path: \$ANDROID_HOME
repo_keyalias: typetype-beta
keystore: beta-keystore.p12
keystorepass: $repo_password
keypass: $repo_password
keydname: CN=TypeType Beta F-Droid,OU=TypeType,O=TypeType,C=FR
repo_url: https://typetype.video/fdroid/android-beta/repo
repo_name: TypeType Beta
repo_description: Beta builds for the TypeType Android client.
EOF
fi

if grep -q '^repo_icon:' "$signing_dir/config.yml"; then
  sed -i 's#^repo_icon:.*#repo_icon: icon.png#' "$signing_dir/config.yml"
else
  printf '%s\n' 'repo_icon: icon.png' >> "$signing_dir/config.yml"
fi

stable_icon=/srv/fdroid/signing/metadata/dev.typetype.android/en-US/images/icon.png
if [ -s "$stable_icon" ]; then
  install -o debian -g debian -m 0644 "$stable_icon" \
    "$signing_dir/metadata/dev.typetype.android/en-US/images/icon.png"
  install -o debian -g debian -m 0644 "$stable_icon" "$signing_dir/icon.png"
fi

if [ ! -s "$signing_dir/metadata/dev.typetype.android.yml" ]; then
  echo "Copy the stable app metadata and icon into $signing_dir/metadata before first update." >&2
  exit 1
fi

install -o root -g root -m 0644 "$source_dir/typetype-fdroid-beta-webhook.service" /etc/systemd/system/
install -o root -g root -m 0644 "$source_dir/typetype-fdroid-beta-webhook.path" /etc/systemd/system/
install -o root -g root -m 0644 "$source_dir/typetype-fdroid-beta-sync.service" /etc/systemd/system/

systemctl daemon-reload
systemctl enable --now typetype-fdroid-beta-webhook.service
systemctl enable --now typetype-fdroid-beta-webhook.path
