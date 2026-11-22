#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/signing.properties"
APK="${1:-$ROOT/app/build/outputs/apk/release/app-release-unsigned.apk}"
OUT="${2:-$ROOT/dist/AIChat-signed.apk}"

if [[ ! -f "$PROPS" ]]; then
  echo "signing.properties not found. Copy signing.properties.example and fill it in." >&2
  exit 1
fi

if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

store_file="$(grep -E '^storeFile=' "$PROPS" | head -1 | cut -d= -f2-)"
store_password="$(grep -E '^storePassword=' "$PROPS" | head -1 | cut -d= -f2-)"
key_alias="$(grep -E '^keyAlias=' "$PROPS" | head -1 | cut -d= -f2-)"
key_password="$(grep -E '^keyPassword=' "$PROPS" | head -1 | cut -d= -f2-)"

if [[ "$store_file" != /* ]]; then
  store_file="$ROOT/$store_file"
fi

if command -v apksigner >/dev/null 2>&1; then
  APKSIGNER="apksigner"
else
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$SDK" && -f "$ROOT/local.properties" ]]; then
    SDK="$(grep -E '^sdk.dir=' "$ROOT/local.properties" | head -1 | cut -d= -f2-)"
  fi
  if [[ -z "$SDK" ]]; then
    echo "apksigner not found. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
    exit 1
  fi
  APKSIGNER="$(find "$SDK/build-tools" -name apksigner -type f | sort -V | tail -1)"
fi

mkdir -p "$(dirname "$OUT")"
AICHAT_KS_PASS="$store_password" AICHAT_KEY_PASS="$key_password" "$APKSIGNER" sign \
  --ks "$store_file" \
  --ks-key-alias "$key_alias" \
  --ks-pass env:AICHAT_KS_PASS \
  --key-pass env:AICHAT_KEY_PASS \
  --out "$OUT" \
  "$APK"

echo "Signed APK: $OUT"
