#!/usr/bin/env bash
# Generates the RSA key pair the app signs and verifies JWTs with.
# Development only: never use these keys anywhere real.
set -euo pipefail

dest="${1:-certs}"
mkdir -p "$dest"

if [[ -f "$dest/privatekey.pem" ]]; then
  echo "$dest/privatekey.pem already exists, leaving it alone"
  exit 0
fi

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$dest/privatekey.pem" 2>/dev/null
openssl rsa -pubout -in "$dest/privatekey.pem" -out "$dest/publickey.pem" 2>/dev/null
chmod 600 "$dest/privatekey.pem"

echo "wrote $dest/privatekey.pem and $dest/publickey.pem"
