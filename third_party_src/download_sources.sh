#!/usr/bin/env bash
# HostTriangle - Download Corresponding Source Code Archives
# Downloads exact upstream sources for GPL/LGPL binary components

set -e
ARCHIVE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/archives"
mkdir -p "$ARCHIVE_DIR"

echo "=== Downloading HostTriangle Third-Party Source Archives ==="

# 1. PRoot v5.1.107.102 (GPL-2.0-or-later)
echo "[1/3] Downloading PRoot v5.1.107.102 source..."
curl -fL -o "$ARCHIVE_DIR/proot-v5.1.107.102.tar.gz" \
  "https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz"

# 2. Samba talloc 2.5.0 (LGPL-3.0-or-later)
echo "[2/3] Downloading talloc 2.5.0 source..."
curl -fL -o "$ARCHIVE_DIR/talloc-2.5.0.tar.gz" \
  "https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz"

# 3. libandroid-shmem v0.7 (Apache-2.0 / LGPL)
echo "[3/3] Downloading libandroid-shmem v0.7 source..."
curl -fL -o "$ARCHIVE_DIR/libandroid-shmem-v0.7.tar.gz" \
  "https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz"

echo "=== All source archives successfully saved to $ARCHIVE_DIR ==="
