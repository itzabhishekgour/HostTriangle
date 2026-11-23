# PRoot and PRoot-Loader Source & Build Documentation

* **Component:** `libproot.so`, `libproot-loader.so`
* **Upstream:** [proot-me/proot](https://github.com/proot-me/proot)
* **Android Port:** [termux/proot](https://github.com/termux/proot)
* **License:** GNU General Public License v2.0 or later (GPL-2.0-or-later)
* **Exact Version:** `5.1.107.102` (`v5.1.107.102`)
* **Source Archive:** [https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz](https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz)

## Build Instructions (Reproducing Binaries)
The binary is built with the Android NDK for `aarch64-linux-android`.
Dependencies: `libtalloc`, `libandroid-shmem`.

See [`build.sh`](build.sh) for the complete Termux build recipe and configure flags used to produce `libproot.so` and `libproot-loader.so`.
