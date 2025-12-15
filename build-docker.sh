#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./build-docker.sh -v <version> \
    -gu <github_actor> -gt <github_token> \
    -du <docker_username> -dp <docker_password> \
    [-i <image>] [--push] [--no-latest]

Default:
  Build locale single-arch + --load (per test)

Options:
  -v   Versione (es. 1.0.3)                       [required]
  -gu  GITHUB_ACTOR (username GitHub)             [required]
  -gt  GITHUB_TOKEN (PAT GitHub Packages)         [required]
  -du  DOCKER_USERNAME (Docker Hub)               [required]
  -dp  DOCKER_PASSWORD (Docker Hub)               [required]
  -i   Nome immagine (repo)                       [default: vaimee/sepa]
  --push       Build multi-arch (amd64, arm64) e push su Docker Hub
  --no-latest  Non tagga/pusha :latest
  -h   Help

Examples:
  # Test locale
  ./build-docker.sh -v 1.0.3 -gu vaimee -gt ghp_xxx -du vaimee -dp '***'

  # Release su Docker Hub (multi-arch)
  ./build-docker.sh -v 1.0.3 -gu vaimee -gt ghp_xxx -du vaimee -dp '***' --push
EOF
}

IMAGE="vaimee/sepa"
DO_PUSH="false"
DO_LATEST="true"

VERSION=""
GITHUB_ACTOR=""
GITHUB_TOKEN=""
DOCKER_USERNAME=""
DOCKER_PASSWORD=""

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v) VERSION="${2:-}"; shift 2 ;;
    -gu) GITHUB_ACTOR="${2:-}"; shift 2 ;;
    -gt) GITHUB_TOKEN="${2:-}"; shift 2 ;;
    -du) DOCKER_USERNAME="${2:-}"; shift 2 ;;
    -dp) DOCKER_PASSWORD="${2:-}"; shift 2 ;;
    -i) IMAGE="${2:-}"; shift 2 ;;
    --push) DO_PUSH="true"; shift 1 ;;
    --no-latest) DO_LATEST="false"; shift 1 ;;
    -h) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
done

# Validate
if [[ -z "$VERSION" || -z "$GITHUB_ACTOR" || -z "$GITHUB_TOKEN" || -z "$DOCKER_USERNAME" || -z "$DOCKER_PASSWORD" ]]; then
  echo "Error: missing required arguments." >&2
  usage
  exit 2
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-].+)?$ ]]; then
  echo "Error: VERSION must look like semver (e.g. 1.0.3)." >&2
  exit 2
fi

if [[ ! -f "./settings.xml" ]]; then
  echo "Error: ./settings.xml not found." >&2
  exit 2
fi

# Tags
MAJOR="$(echo "$VERSION" | cut -d. -f1)"
MINOR="$(echo "$VERSION" | cut -d. -f2)"
MAJOR_MINOR="${MAJOR}.${MINOR}"

TAG_VERSION="${IMAGE}:${VERSION}"
TAG_MAJOR_MINOR="${IMAGE}:${MAJOR_MINOR}"
TAG_LATEST="${IMAGE}:latest"

export GITHUB_ACTOR
export GITHUB_TOKEN
export DOCKER_BUILDKIT=1

echo "Logging into Docker Hub as ${DOCKER_USERNAME}..."
echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin

# buildx builder
if ! docker buildx inspect sepa-builder >/dev/null 2>&1; then
  docker buildx create --name sepa-builder --use >/dev/null
else
  docker buildx use sepa-builder >/dev/null
fi

COMMON_ARGS=(
  --secret "id=maven_settings,src=./settings.xml"
  --secret "id=github_actor,env=GITHUB_ACTOR"
  --secret "id=github_token,env=GITHUB_TOKEN"
  --build-arg "REVISION=${VERSION}"
  -f "./Dockerfile"
  .
)

if [[ "$DO_PUSH" == "false" ]]; then
  # Local build
  ARCH="$(uname -m)"
  if [[ "$ARCH" == "x86_64" ]]; then
    PLATFORM="linux/amd64"
  elif [[ "$ARCH" == "arm64" || "$ARCH" == "aarch64" ]]; then
    PLATFORM="linux/arm64"
  else
    PLATFORM="linux/amd64"
  fi

  echo "Local build -> ${PLATFORM}, --load"
  docker buildx build \
    --platform "${PLATFORM}" \
    -t "${TAG_VERSION}" \
    --load \
    "${COMMON_ARGS[@]}"

  docker tag "${TAG_VERSION}" "${TAG_MAJOR_MINOR}"
  [[ "$DO_LATEST" == "true" ]] && docker tag "${TAG_VERSION}" "${TAG_LATEST}"

  echo "Local tags:"
  echo "  - ${TAG_VERSION}"
  echo "  - ${TAG_MAJOR_MINOR}"
  [[ "$DO_LATEST" == "true" ]] && echo "  - ${TAG_LATEST}"

  echo "Test with:"
  echo "  docker run --rm -p 8000:8000 ${TAG_VERSION}"

else
  # Multi-arch push
  TAGS=(-t "${TAG_VERSION}" -t "${TAG_MAJOR_MINOR}")
  [[ "$DO_LATEST" == "true" ]] && TAGS+=(-t "${TAG_LATEST}")

  echo "Multi-arch build + push (amd64, arm64)"
  docker buildx build \
    --platform "linux/amd64,linux/arm64" \
    "${TAGS[@]}" \
    --push \
    "${COMMON_ARGS[@]}"

  echo "Pushed tags:"
  echo "  - ${TAG_VERSION}"
  echo "  - ${TAG_MAJOR_MINOR}"
  [[ "$DO_LATEST" == "true" ]] && echo "  - ${TAG_LATEST}"
fi
