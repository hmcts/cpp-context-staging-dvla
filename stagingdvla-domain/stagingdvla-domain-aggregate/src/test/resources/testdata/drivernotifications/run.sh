#!/usr/bin/env bash
# Recursively finds all hearing input files in the given directory and runs
# OutboundDvlaNotification for each one, writing the result alongside the input.
#
# Input files must be named:  <name>-resulted-hearing.json
#   e.g. my-case-resulted-hearing.json
# Output files are written as: <name>-resulted.json
#   e.g. my-case-resulted.json  (same directory as the input file)
#
# Usage:
#   ./run-all.sh <input-directory>          — process all matching files recursively
#   ./run-all.sh <path/to/file-hearing.json> — process a single file
#
# Configuration:
#   OUTBOUND_DVLA_DIR — path to the OutboundDvlaNotification project directory.
#   Update this if the project is moved or cloned to a different location.
set -euo pipefail

OUTBOUND_DVLA_DIR="cpp-context-azure-legalaidagency/azure-functions/durable-functions/OutboundDvlaNotification"
export OUTBOUND_DVLA_DIR

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
RUN_JS="$SCRIPT_DIR/generate-notification-payload.js"
if [ -z "${1:-}" ]; then
    echo "Usage: run.sh <input-directory|input-file>"
    exit 1
fi
INPUT="$1"

if [ -f "$INPUT" ]; then
    echo "Processing: $INPUT"
    node "$RUN_JS" "$INPUT"
elif [ -d "$INPUT" ]; then
    found=0
    while IFS= read -r -d '' file; do
        found=$((found + 1))
        echo "Processing: $file"
        node "$RUN_JS" "$file"
    done < <(find "$INPUT" -name "*-resulted-hearing.json" -print0)

    if [ "$found" -eq 0 ]; then
        echo "No *-resulted-hearing.json files found under: $INPUT"
    fi
else
    echo "Error: '$INPUT' is not a valid file or directory"
    exit 1
fi