#!/usr/bin/env bash
# Stop QA services (does not remove them from PM2 list).
set -euo pipefail
cd "$(dirname "$0")/../.."

if [ $# -eq 0 ]; then
  pm2 stop ecosystem.config.js
else
  pm2 stop "${1}-qa"
fi
pm2 ls
