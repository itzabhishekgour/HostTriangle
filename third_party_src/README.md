# HostTriangle - Complete Corresponding Source Code for Third-Party Binaries

This directory contains the complete corresponding source code references, build recipes, configuration files, and automated acquisition scripts for all GPL-covered and LGPL-covered binary components bundled within HostTriangle APK distributions, in strict compliance with **GNU General Public License v2.0 (GPLv2) Section 3** and **GNU General Public License v3.0 (GPLv3) Section 6**.

---

## 1. Bundled Binary Inventory & License Classification

| Binary | Source Component | Upstream Version / Tag | Effective License |
| :--- | :--- | :--- | :--- |
| `lib/arm64-v8a/libproot.so` | PRoot | `5.1.107.102` (`v5.1.107-102`) | **GNU GPLv2 or later** |
| `lib/arm64-v8a/libproot-loader.so` | PRoot Loader | `5.1.107.102` (`v5.1.107-102`) | **GNU GPLv2 or later** |
| `lib/arm64-v8a/libtalloc.so` | Samba talloc | `2.5.0` | **GNU LGPLv3 or later** |
| `lib/arm64-v8a/libandroid-shmem.so` | Termux android-shmem | `0.7` (`v0.7`) | **Apache-2.0 / LGPL** |
| `lib/arm64-v8a/libcloudflared.so` | Cloudflare Tunnel | `2024.x` | **Apache-2.0** |

*(Note: First-party native code such as `libtermux.so` is compiled directly from `app/src/main/cpp/hosttriangle_pty.c` and is part of the core HostTriangle project source under GPLv3).*

---

## 2. Written Offer for Source Code (GPLv2 Sec. 3(b) & GPLv3 Sec. 6(b))

HostTriangle hereby extends a binding **Written Offer**, valid for at least three (3) years from the date of binary distribution or for as long as customer support/releases are provided for this version, to provide to any third party a complete machine-readable copy of the Corresponding Source code for any GPL or LGPL-covered software components included in this distribution, on a durable medium customarily used for software interchange or via network server download, at no charge beyond the actual physical cost of distribution (or gratis via network transfer).

Requests for corresponding source code may be submitted via:
- **Repository Issues / Releases:** File an issue or download the source tarballs directly from this repository's Releases page.
- **Automated Download Script:** Run `./download_sources.sh` (or `download_sources.bat` on Windows) in this directory.

---

## 3. Component Details & Source Locations

### A. PRoot & PRoot Loader (`libproot.so`, `libproot-loader.so`)
- **Upstream Project:** [proot-me/proot](https://github.com/proot-me/proot)
- **Termux Android Port:** [termux/proot](https://github.com/termux/proot)
- **Exact Version / Tag:** `v5.1.107.102` (commit tagged `v5.1.107.102`)
- **Direct Source Archive:** [https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz](https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz)
- **Build Recipe & Patches:** Located at [`third_party_src/proot/build.sh`](proot/build.sh)

### B. talloc (`libtalloc.so`)
- **Upstream Project:** [Samba talloc](https://talloc.samba.org/)
- **Exact Version:** `2.5.0`
- **Direct Source Archive:** [https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz](https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz)
- **Build Recipe & Patches:** Located at [`third_party_src/talloc/build.sh`](talloc/build.sh)

### C. libandroid-shmem (`libandroid-shmem.so`)
- **Upstream Project:** [termux/libandroid-shmem](https://github.com/termux/libandroid-shmem)
- **Exact Version / Tag:** `v0.7`
- **Direct Source Archive:** [https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz](https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz)
- **Build Recipe & Patches:** Located at [`third_party_src/libandroid-shmem/build.sh`](libandroid-shmem/build.sh)

---

## 4. How to Download All Corresponding Source Archives

To download all exact source archives in one step into an `archives/` folder:

### On Linux / macOS / WSL:
```bash
cd third_party_src
chmod +x download_sources.sh
./download_sources.sh
```

### On Windows PowerShell:
```powershell
cd third_party_src
.\download_sources.bat
```
