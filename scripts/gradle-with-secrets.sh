#!/bin/sh
# Runs Gradle with the plugin signing secrets and, with --publish, the JetBrains Marketplace token.
#
#   scripts/gradle-with-secrets.sh [--publish] <gradle tasks...>
#
# Secrets are read from $SIGNING_DIR: chain.crt, private_encrypted.pem and, optionally, publish-token. The key
# passphrase (and the token, when there is no publish-token file) is prompted for unless PRIVATE_KEY_PASSWORD
# (or PUBLISH_TOKEN) is already set.
#
# Gradle records task inputs, which include the passphrase and the token, in its project cache (.gradle/). So that
# they are never written outside $SIGNING_DIR, the build uses a temporary project cache inside $SIGNING_DIR that is
# deleted when the build ends, and a daemon that exits with the build.
set -eu

cd "$(dirname "$0")/.."

publish=false
if [ "${1:-}" = "--publish" ]; then
  publish=true
  shift
fi

fail() {
  echo "error: $*" >&2
  exit 1
}

[ -n "${SIGNING_DIR:-}" ] || fail "SIGNING_DIR is not set; it must point at the directory holding the signing key"
[ -f "$SIGNING_DIR/chain.crt" ] || fail "$SIGNING_DIR/chain.crt not found"
[ -f "$SIGNING_DIR/private_encrypted.pem" ] || fail "$SIGNING_DIR/private_encrypted.pem not found"

# Prints the answer to a prompt, without echoing what is typed.
prompt_secret() {
  printf '%s' "$1" >&2
  stty -echo 2>/dev/null || true
  IFS= read -r answer || true
  stty echo 2>/dev/null || true
  echo >&2
  printf '%s' "$answer"
}

if [ -z "${PRIVATE_KEY_PASSWORD:-}" ]; then
  PRIVATE_KEY_PASSWORD=$(prompt_secret "Signing key passphrase: ")
fi
if $publish && [ -z "${PUBLISH_TOKEN:-}" ]; then
  if [ -f "$SIGNING_DIR/publish-token" ]; then
    PUBLISH_TOKEN=$(cat "$SIGNING_DIR/publish-token")
  else
    PUBLISH_TOKEN=$(prompt_secret "JetBrains Marketplace token: ")
  fi
fi

cache=$(mktemp -d "$SIGNING_DIR/gradle-project-cache.XXXXXX")
trap 'rm -rf "$cache"' EXIT
trap 'exit 130' INT TERM

export CERTIFICATE_CHAIN_FILE="$SIGNING_DIR/chain.crt"
export PRIVATE_KEY_FILE="$SIGNING_DIR/private_encrypted.pem"
export PRIVATE_KEY_PASSWORD
if $publish; then
  export PUBLISH_TOKEN
fi

./gradlew --no-daemon --project-cache-dir "$cache" "$@"
