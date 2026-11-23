@echo off
REM HostTriangle - Download Corresponding Source Code Archives (Windows)
REM Downloads exact upstream sources for GPL/LGPL binary components

set "ARCHIVE_DIR=%~dp0archives"
if not exist "%ARCHIVE_DIR%" mkdir "%ARCHIVE_DIR%"

echo === Downloading HostTriangle Third-Party Source Archives ===

echo [1/3] Downloading PRoot v5.1.107.102 source...
curl -fL -o "%ARCHIVE_DIR%\proot-v5.1.107.102.tar.gz" "https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz"

echo [2/3] Downloading talloc 2.5.0 source...
curl -fL -o "%ARCHIVE_DIR%\talloc-2.5.0.tar.gz" "https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz"

echo [3/3] Downloading libandroid-shmem v0.7 source...
curl -fL -o "%ARCHIVE_DIR%\libandroid-shmem-v0.7.tar.gz" "https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz"

echo === All source archives successfully saved to %ARCHIVE_DIR% ===
