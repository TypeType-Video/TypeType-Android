#!/usr/bin/env bash
set -euo pipefail
umask 077

repo_dir=/srv/fdroid-beta/signing
state_dir=$repo_dir/state
public_dir=/srv/fdroid-beta/public
github_repo=TypeType-Video/TypeType-Android
image=typetype-fdroidserver:beta-patched

exec 8>"$state_dir/sync.lock"
flock 8

release_json="$(
  curl -fsSL --retry 5 \
    "https://api.github.com/repos/${github_repo}/releases?per_page=100" |
    jq -c '[.[] | select(.draft == false and .prerelease == true)] | first // empty'
)"

if [ -z "$release_json" ]; then
  echo "No published TypeType Android prerelease was found." >&2
  exit 1
fi

release_id="$(jq -er '.id' <<<"$release_json")"
release_tag="$(jq -er '.tag_name' <<<"$release_json")"
release_notes="$(jq -er '.body // ""' <<<"$release_json")"

mapfile -t apk_assets < <(
  jq -er '.assets[] | select(.name | endswith(".apk")) | "\(.id)\t\(.name)"' <<<"$release_json"
)

if [ "${#apk_assets[@]}" -ne 1 ]; then
  echo "Expected exactly one APK asset in prerelease ${release_tag}." >&2
  exit 1
fi

IFS=$'\t' read -r apk_id apk_name <<<"${apk_assets[0]}"
checksum_name="${apk_name}.sha256"
download_url="https://github.com/${github_repo}/releases/download/${release_tag}/${apk_name}"
checksum_url="https://github.com/${github_repo}/releases/download/${release_tag}/${checksum_name}"

stage_dir="$(mktemp -d "$state_dir/release.XXXXXX")"
trap 'rm -rf "$stage_dir"' EXIT

curl -fL --retry 5 -o "${stage_dir}/${apk_name}" "$download_url"
curl -fL --retry 5 -o "${stage_dir}/${checksum_name}" "$checksum_url"
(
  cd "$stage_dir"
  sha256sum -c "$checksum_name"
)

actual_certificate=
while IFS= read -r line; do
  case "$line" in
    *SHA256:*) actual_certificate=${line#*SHA256: } ;;
  esac
done < <(keytool -printcert -jarfile "${stage_dir}/${apk_name}")

expected_certificate="$(<"${state_dir}/trusted-apk-cert-sha256")"
if [ "$actual_certificate" != "$expected_certificate" ]; then
  echo "APK certificate does not match the pinned TypeType signing certificate." >&2
  exit 1
fi

badging="$(/usr/bin/aapt dump badging "${stage_dir}/${apk_name}")"
package_name=
version_code=
version_name=
package_name="$(printf '%s\n' "$badging" | sed -nE "s/^package: name='([^']+)'.*/\1/p")"
version_code="$(printf '%s\n' "$badging" | sed -nE "s/^package:.*versionCode='([0-9]+)'.*/\1/p")"
version_name="$(printf '%s\n' "$badging" | sed -nE "s/^package:.*versionName='([^']+)'.*/\1/p")"

if [ "$package_name" != 'dev.typetype.android' ]; then
  echo "Unexpected package: ${package_name}" >&2
  exit 1
fi
if [[ ! "$version_code" =~ ^[0-9]+$ ]] || [ -z "$version_name" ]; then
  echo "Invalid APK version metadata." >&2
  exit 1
fi
if [[ "$version_name" != *-beta.* ]]; then
  echo "Prerelease APK must use a beta versionName: ${version_name}" >&2
  exit 1
fi

last_version_code=0
if [ -f "${state_dir}/published-beta-version-code" ]; then
  last_version_code="$(<"${state_dir}/published-beta-version-code")"
fi
if [ "$version_code" -le "$last_version_code" ]; then
  echo "versionCode ${version_code} is not newer than ${last_version_code}." >&2
  exit 1
fi

canonical_apk="${repo_dir}/repo/${package_name}_${version_code}.apk"
if [ -f "$canonical_apk" ]; then
  read -r existing_hash _ < <(sha256sum "$canonical_apk")
  read -r incoming_hash _ < <(sha256sum "${stage_dir}/${apk_name}")
  if [ "$existing_hash" != "$incoming_hash" ]; then
    echo "A different APK already exists for versionCode ${version_code}." >&2
    exit 1
  fi
else
  install -m 0644 "${stage_dir}/${apk_name}" "$canonical_apk"
fi

python3 - "${repo_dir}/metadata/dev.typetype.android.yml" "$version_name" "$version_code" <<'PYTHON'
import re
import sys
from pathlib import Path

path = Path(sys.argv[1])
version_name, version_code = sys.argv[2:]
metadata = path.read_text(encoding="utf-8")
metadata = re.sub(r"^CurrentVersion:.*$", f"CurrentVersion: {version_name}", metadata, flags=re.MULTILINE)
metadata = re.sub(r"^CurrentVersionCode:.*$", f"CurrentVersionCode: {version_code}", metadata, flags=re.MULTILINE)
metadata = re.sub(r"\nBuilds:\n(?:[ \t].*(?:\n|$))*", "\n", metadata)
metadata = metadata.rstrip() + (
    f"\nBuilds:\n"
    f"  - versionName: {version_name}\n"
    f"    versionCode: {version_code}\n"
)
path.write_text(metadata, encoding="utf-8")
PYTHON

changelog_dir="${repo_dir}/metadata/dev.typetype.android/en-US/changelogs"
install -d -m 0755 "$changelog_dir"
if [ -n "$release_notes" ]; then
  printf '%s\n' "$release_notes" > "${changelog_dir}/${version_code}.txt"
  chmod 0644 "${changelog_dir}/${version_code}.txt"
else
  rm -f "${changelog_dir}/${version_code}.txt"
fi

install -d -m 0755 "${repo_dir}/repo/icons"
if [ -f "${repo_dir}/metadata/dev.typetype.android/en-US/images/icon.png" ]; then
  install -m 0644 "${repo_dir}/metadata/dev.typetype.android/en-US/images/icon.png" "${repo_dir}/repo/icons/icon.png"
fi

docker run --rm --entrypoint python3 --user "$(id -u):$(id -g)" \
  -e PYTHONPATH=/home/vagrant/fdroidserver \
  -v "${repo_dir}:/repo" -w /repo "$image" \
  -c 'import sys; from fdroidserver import common, update; common.setup_status_output=lambda *_: {}; sys.argv=["fdroid update", "-c", "--pretty"]; update.main()'

repo_fingerprint=
while IFS= read -r line; do
  case "$line" in
    *SHA256:*) repo_fingerprint=${line#*SHA256: }; repo_fingerprint=${repo_fingerprint//:/} ;;
  esac
done < <(keytool -printcert -jarfile "${repo_dir}/repo/index-v1.jar")

expected_repo_fingerprint="$(<"${state_dir}/trusted-repo-fingerprint")"
if [ "$repo_fingerprint" != "$expected_repo_fingerprint" ]; then
  echo "F-Droid repository signing certificate does not match the pinned beta fingerprint." >&2
  exit 1
fi

install -d -m 0755 "$public_dir"
rsync -a --delete --delay-updates "${repo_dir}/repo/" "${public_dir}/repo/"

printf '%s\n' "$release_id" > "${state_dir}/published-beta-release-id"
printf '%s\n' "$version_code" > "${state_dir}/published-beta-version-code"
