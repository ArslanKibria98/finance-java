#!/usr/bin/env bash
# Restart QA services after a rebuild.
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ $# -eq 0 ]; then
  pm2 restart ecosystem.config.js --update-env
else
  pm2 restart "${1}-qa" --update-env
fi
pm2 ls
