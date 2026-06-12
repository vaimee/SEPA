#!/usr/bin/env bash
set -euo pipefail

HTML_FILE="index.html"
SRC_DIR="js"
DEPLOY_DIR="deploy"

DOCKER_USERNAME=""
DOCKER_PASSWORD=""
VERSION=""

usage() {
  cat <<EOF
Usage: $0 -v VERSION -du DOCKER_USERNAME -dp DOCKER_PASSWORD

Options:
  -v   Version tag (required)
  -du  Docker Hub username (required)
  -dp  Docker Hub password (required)
  -h   Show this help and exit
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v) VERSION="${2:-}"; shift 2 ;;
    -du) DOCKER_USERNAME="${2:-}"; shift 2 ;;
    -dp) DOCKER_PASSWORD="${2:-}"; shift 2 ;;
    -h) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
done

if [[ -z "$VERSION" || -z "$DOCKER_USERNAME" || -z "$DOCKER_PASSWORD" ]]; then
  echo "Error: missing required arguments"
  usage
  exit 1
fi

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v) VERSION="${2:-}"; shift 2 ;;
    -du) DOCKER_USERNAME="${2:-}"; shift 2 ;;
    -dp) DOCKER_PASSWORD="${2:-}"; shift 2 ;;
    -h) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
done

if [ ! -f "$HTML_FILE" ]; then
  echo "Error: file HTML '$HTML_FILE' not found"
  exit 1
fi

if [ ! -d "$SRC_DIR" ]; then
  echo "Error: directory '$SRC_DIR' not found"
  exit 1
fi

rm -rf "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR"

# Timestamp tipo 20251210194530
TIMESTAMP=$(date +"%Y%m%d%H%M%S")

html_base=$(basename "$HTML_FILE")
html_copy="$DEPLOY_DIR/$html_base"

echo "Copy index.html in $DEPLOY_DIR"
cp "$HTML_FILE" "$html_copy"

# Update version in HTML
sed -i '' "s|__VERSION__|$VERSION|g" "$html_copy"

# Scorro tutti i file nella cartella sorgente
for f in "$SRC_DIR"/*; do
  [ -f "$f" ] || continue   # salta se non è un file

  base=$(basename "$f")

  # Gestione file con o senza estensione
  if [[ "$base" == *.js ]]; then
    name="${base%.*}"
    ext="${base##*.}"
    new="${name}-${VERSION}-${TIMESTAMP}.${ext}"
  else
    name="$base"
    new="${name}-${VERSION}-${TIMESTAMP}"
  fi

  echo "Copy $f -> $DEPLOY_DIR/$new"
  cp "$f" "$DEPLOY_DIR/$new"

  # Escape per sed
  escaped_new=${new//&/\\&}

  echo "Update references in $html_copy: $base -> $new"
  # Sed macOS: -i '' per modifica in-place
  sed -i '' "s|$base|$escaped_new|g" "$html_copy"
done

echo "Logging into Docker Hub as ${DOCKER_USERNAME}..."
echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin

echo "Build Docker with tag vaimee/sepa-dashboard:${VERSION}"

docker buildx build \
  --platform linux/arm64,linux/amd64 \
  -t "vaimee/sepa-dashboard:${VERSION}" \
  --push .

echo "done."
