# talloc Source & Build Documentation

* **Component:** `libtalloc.so`
* **Upstream:** [Samba talloc](https://talloc.samba.org/)
* **License:** GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later)
* **Exact Version:** `2.5.0`
* **Source Archive:** [https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz](https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz)

## Build Instructions (Reproducing Binaries)
talloc uses waf build system.
Cross-compilation is executed with Python and Android NDK toolchain:
```bash
./configure --prefix=/usr --cross-compile --cross-execute="" --disable-python
make -j$(nproc)
```
See [`build.sh`](build.sh) for details.
