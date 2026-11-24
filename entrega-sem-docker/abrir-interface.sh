#!/usr/bin/env bash
# Abre a interface web local (requer Python 3).
cd "$(dirname "$0")" || exit 1
if command -v python3 >/dev/null 2>&1; then
    exec python3 servidor.py "$@"
fi
exec python servidor.py "$@"
