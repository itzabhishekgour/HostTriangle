# HostTriangle - Third-Party Open Source Software Notices

HostTriangle incorporates, bundles, or interfaces with several open-source software libraries, binaries, and dependencies. This document provides clear attribution, links to original source code repositories, and licensing notices in compliance with their respective licenses.

*(Note: First-party native components such as `app/src/main/cpp/hosttriangle_pty.c` are core project sources licensed under the project's primary GNU General Public License v3.0 (GPL-3.0-only), documented in `LICENSE` and `README.md`).*

---

## GPLv2 / GPLv3 Source Code Compliance & Written Offer

In strict compliance with **GNU General Public License v2.0 (GPLv2) Section 3** and **GNU General Public License v3.0 (GPLv3) Section 6**, HostTriangle provides the complete corresponding machine-readable source code for all GPL-covered and LGPL-covered binary components bundled within this application:

1. **In-Repository Sources & Build Recipes:**
   Complete build recipes, configuration files, upstream release archives, and reproduction instructions are maintained directly in this repository under the [`third_party_src/`](third_party_src/) directory.
   - Run `third_party_src/download_sources.sh` (or `third_party_src/download_sources.bat`) to fetch exact corresponding upstream source archives.
2. **Release Tarballs:**
   Pre-packaged complete source tarballs containing both HostTriangle application source and bundled third-party source packages are attached to each official GitHub Release.
3. **Formal Written Offer:**
   HostTriangle extends a binding **Written Offer**, valid for at least three (3) years from the date of binary distribution, to provide to any third party a complete machine-readable copy of the Corresponding Source code for any GPL or LGPL-covered software components included in this distribution, gratis via network server transfer or at cost of physical distribution media upon request.

---

## 1. PRoot and PRoot-Loader (`libproot.so`, `libproot-loader.so`)
* **Component:** Rootless chroot and Linux virtualization sandbox
* **Bundled Binaries:** `lib/arm64-v8a/libproot.so`, `lib/arm64-v8a/libproot-loader.so`
* **License:** GNU General Public License v2.0 or later (GPL-2.0-or-later)
* **Copyright:** Copyright (C) 2010-2023 Cedric VINCENT, Romain Bouqueau, and PRoot contributors
* **Exact Version:** `5.1.107.102` (`v5.1.107.102`)
* **Upstream Source Repository:** [https://github.com/proot-me/proot](https://github.com/proot-me/proot) and [https://github.com/termux/proot](https://github.com/termux/proot)
* **Local Source & Build Recipe:** See [`third_party_src/proot/`](third_party_src/proot/)
* **Direct Source Archive:** [https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz](https://github.com/termux/proot/archive/refs/tags/v5.1.107.102.tar.gz)

---

## 2. talloc (`libtalloc.so`)
* **Component:** Hierarchical, reference counted memory pool system
* **Bundled Binary:** `lib/arm64-v8a/libtalloc.so`
* **License:** GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later)
* **Copyright:** Copyright (C) Andrew Tridgell, Stefan Metzmacher, and Samba Team
* **Exact Version:** `2.5.0`
* **Upstream Source Repository:** [https://talloc.samba.org/](https://talloc.samba.org/)
* **Local Source & Build Recipe:** See [`third_party_src/talloc/`](third_party_src/talloc/)
* **Direct Source Archive:** [https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz](https://www.samba.org/ftp/talloc/talloc-2.5.0.tar.gz)

---

## 3. android-shmem (`libandroid-shmem.so`)
* **Component:** System V shared memory emulation for Android
* **Bundled Binary:** `lib/arm64-v8a/libandroid-shmem.so`
* **License:** Apache License, Version 2.0 / LGPL
* **Copyright:** Copyright (c) 2016-2021 Termux
* **Exact Version:** `0.7` (`v0.7`)
* **Upstream Source Repository:** [https://github.com/termux/libandroid-shmem](https://github.com/termux/libandroid-shmem)
* **Local Source & Build Recipe:** See [`third_party_src/libandroid-shmem/`](third_party_src/libandroid-shmem/)
* **Direct Source Archive:** [https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz](https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz)

---

## 4. Cloudflare Tunnel (`libcloudflared.so`)
* **Component:** Cloudflare Zero Trust Tunnel client daemon (`cloudflared`)
* **Bundled Binary:** `lib/arm64-v8a/libcloudflared.so`
* **License:** Apache License, Version 2.0
* **Copyright:** Copyright (c) Cloudflare, Inc.
* **Upstream Source Repository:** [https://github.com/cloudflare/cloudflared](https://github.com/cloudflare/cloudflared)
* **Notice:**
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at `http://www.apache.org/licenses/LICENSE-2.0`. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

---

## 5. Termux Terminal View & Emulator
* **Dependencies:**
  - `com.github.termux.termux-app:terminal-view:v0.118.0`
  - `com.github.termux.termux-app:terminal-emulator:v0.118.0`
* **License:** Apache License, Version 2.0 (Android Terminal Emulator base) and GNU General Public License v3.0 (Termux modifications)
* **Copyright:**
  - Copyright (C) 2011 Jack Palevich (Jackpal) and Steven Luo (Android Terminal Emulator)
  - Copyright (C) 2015-2023 Fredrik Fornwall and Termux contributors
* **Upstream Source Repositories:**
  - [https://github.com/jackpal/Android-Terminal-Emulator](https://github.com/jackpal/Android-Terminal-Emulator) (Base Terminal Engine, Apache-2.0)
  - [https://github.com/termux/termux-app](https://github.com/termux/termux-app) (Terminal View & Emulator, GPLv3-only with Apache-2.0 exceptions)

---

## 6. Apache Commons Compress
* **Dependency:** `org.apache.commons:commons-compress:1.26.1`
* **License:** Apache License, Version 2.0
* **Copyright:** Copyright 2002-2024 The Apache Software Foundation
* **Upstream Source Repository:** [https://commons.apache.org/proper/commons-compress/](https://commons.apache.org/proper/commons-compress/)

---

## 7. XZ for Java
* **Dependency:** `org.tukaani:xz:1.9`
* **License:** Public Domain
* **Author:** Lasse Collin
* **Upstream Repository:** [https://tukaani.org/xz/java.html](https://tukaani.org/xz/java.html)

---

## 8. Android Open Source Project (AOSP) & Jetpack Compose
* **Dependencies:**
  - `androidx.compose.*`
  - `androidx.core:core-ktx`
  - `androidx.lifecycle.*`
  - `androidx.activity:activity-compose`
* **License:** Apache License, Version 2.0
* **Copyright:** Copyright (C) The Android Open Source Project
* **Upstream Repository:** [https://android.googlesource.com/platform/frameworks/support](https://android.googlesource.com/platform/frameworks/support)

---

## 9. On-Demand Provisioned Runtimes & APT Mirrors (Downloaded at Runtime)

HostTriangle does **not** bundle the following packages inside the application APK; rather, it provisions them on-demand directly onto the user's device from official upstream package mirrors:

* **Eclipse Temurin OpenJDK (Adoptium):**
  - **Component:** Headless Java Runtime Environments (`temurin-8-jre`, `temurin-11-jre`, `temurin-17-jre`, `temurin-21-jre`)
  - **License:** GNU General Public License v2.0 with Classpath Exception (GPLv2+CE)
  - **Maintainer:** Eclipse Adoptium Working Group / Eclipse Foundation
  - **Public Repository & GPG Key:** [https://packages.adoptium.net/](https://packages.adoptium.net/)
  - **Notice:** The public GPG signing key is fetched strictly for package integrity verification pursuant to standard APT security practices. Eclipse Temurin and Adoptium are trademarks of the Eclipse Foundation.

* **Debian 12 Bookworm Userland:**
  - **Component:** Minimal ARM64 root filesystem and standard utility packages (`curl`, `ca-certificates`, `gpg`, `python3`)
  - **License:** Debian Free Software Guidelines (DFSG) compliant licenses (GPL, MIT, Apache 2.0)
  - **Upstream:** Debian Project ([https://www.debian.org/](https://www.debian.org/))

* **Node.js LTS:**
  - **License:** MIT License
  - **Upstream:** Node.js Project ([https://nodejs.org/](https://nodejs.org/))

---

## Full Apache License, Version 2.0 Text

```
                              Apache License
                        Version 2.0, January 2004
                     http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions.
   "License" shall mean the terms and conditions for use, reproduction,
   and distribution as defined by Sections 1 through 9 of this document.
   "Licensor" shall mean the copyright owner or entity authorized by
   the copyright owner that is granting the License.
   "Legal Entity" shall mean the union of the acting entity and all
   other entities that control, are controlled by, or are under common
   control with that entity.
   "You" (or "Your") shall mean an individual or Legal Entity
   exercising permissions granted by this License.
   "Source" form shall mean the preferred form for making modifications,
   including but not limited to software source code, documentation
   source, and configuration files.
   "Object" form shall mean any form resulting from mechanical
   transformation or translation of a Source form, including but
   not limited to compiled object code, generated documentation,
   and conversions to other media types.
   "Work" shall mean the work of authorship, whether in Source or
   Object form, made available under the License, as indicated by a
   copyright notice that is included in or attached to the work.
   "Derivative Works" shall mean any work, whether in Source or Object
   form, that is based on (or derived from) the Work and for which the
   editorial revisions, annotations, elaborations, or other modifications
   represent, as a whole, an original work of authorship.
   "Contribution" shall mean any work of authorship, including
   the original version of the Work and any modifications or additions
   to that Work or Derivative Works thereof, that is intentionally
   submitted to Licensor for inclusion in the Work by the copyright owner
   or by an individual or Legal Entity authorized to submit on behalf of
   the copyright owner.
   "Contributor" shall mean Licensor and any individual or Legal Entity
   on behalf of whom a Contribution has been received by Licensor and
   subsequently incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of
   this License, each Contributor hereby grants to You a perpetual,
   worldwide, non-exclusive, no-charge, royalty-free, irrevocable
   copyright license to reproduce, prepare Derivative Works of,
   publicly display, publicly perform, sublicense, and distribute the
   Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of
   this License, each Contributor hereby grants to You a perpetual,
   worldwide, non-exclusive, no-charge, royalty-free, irrevocable
   (except as stated in this section) patent license to make, have made,
   use, offer to sell, sell, import, and otherwise transfer the Work,
   where such license applies only to those patent claims licensable
   by such Contributor that are necessarily infringed by their
   Contribution(s) alone or by combination of their Contribution(s)
   with the Work to which such Contribution(s) was submitted.

4. Redistribution. You may reproduce and distribute copies of the
   Work or Derivative Works thereof in any medium, with or without
   modifications, and in Source or Object form, provided that You
   meet the following conditions:
   (a) You must give any other recipients of the Work or Derivative
       Works a copy of this License; and
   (b) You must cause any modified files to carry prominent notices
       stating that You changed the files; and
   (c) You must retain, in the Source form of any Derivative Works
       that You distribute, all copyright, patent, trademark, and
       attribution notices from the Source form of the Work; and
   (d) If the Work includes a "NOTICE" text file as part of its
       distribution, then any Derivative Works that You distribute must
       include a readable copy of the attribution notices contained
       within such NOTICE file.

5. Submission of Contributions. Unless You explicitly state otherwise,
   any Contribution intentionally submitted for inclusion in the Work
   by You to the Licensor shall be under the terms and conditions of
   this License, without any additional terms or conditions.

6. Trademarks. This License does not grant permission to use the trade
   names, trademarks, service marks, or product names of the Licensor,
   except as required for reasonable and customary use in describing the
   origin of the Work and reproducing the content of the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or
   agreed to in writing, Licensor provides the Work (and each
   Contributor provides its Contributions) on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
   implied, including, without limitation, any warranties or conditions
   of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
   PARTICULAR PURPOSE.

8. Limitation of Liability. In no event and under no legal theory,
   whether in tort (including negligence), contract, or otherwise,
   unless required by applicable law (such as deliberate and grossly
   negligent acts) or agreed to in writing, shall any Contributor be
   liable to You for damages, including any direct, indirect, special,
   incidental, or exemplary damages of any character arising as a
   result of this License or out of the use or inability to use the
   Work.

9. Accepting Warranty or Additional Liability. While redistributing
   the Work or Derivative Works thereof, You may choose to offer,
   and charge a fee for, acceptance of support, warranty, indemnity,
   or other liability obligations and/or rights consistent with this
   License.
```
