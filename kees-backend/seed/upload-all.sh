#!/usr/bin/env bash

set -euo pipefail

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
markdown_directory="${script_directory}/markdown"
base_url="${KEES_BASE_URL:-http://localhost:8080}"
collection_id="${1:-${KEES_COLLECTION_ID:-}}"

if [[ -z "${collection_id}" ]]; then
    echo "Usage: $0 <collection-public-id>" >&2
    echo "Alternatively, set KEES_COLLECTION_ID." >&2
    exit 64
fi

shopt -s nullglob
markdown_files=("${markdown_directory}"/*.md)

if (( ${#markdown_files[@]} == 0 )); then
    echo "No Markdown files found in ${markdown_directory}." >&2
    exit 1
fi

upload_url="${base_url%/}/api/collections/${collection_id}/sources"

echo "Uploading ${#markdown_files[@]} Markdown file(s) to ${upload_url}"

for file in "${markdown_files[@]}"; do
    echo
    echo "Uploading $(basename "${file}")..."

    curl \
        --silent \
        --show-error \
        --fail-with-body \
        --request POST \
        --form "file=@${file};type=text/markdown" \
        "${upload_url}"

    echo
done

echo
echo "Uploaded all ${#markdown_files[@]} Markdown file(s)."
