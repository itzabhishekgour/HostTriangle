# Security Policy

## Official Releases & Authenticity

Official builds of **HostTriangle** are distributed exclusively through this GitHub repository's Releases page.

To verify that your APK has not been tampered with or injected with malware, always verify the release checksums and the official certificate signing fingerprint:

* **Official Signing Key SHA-256:** `[YOUR_OFFICIAL_RELEASE_KEY_SHA256_FINGERPRINT]`

**Warning:** Never install HostTriangle APKs from unofficial Telegram channels, third-party APK mirror sites, or unverified forks.

---

## Supported Versions

Only the latest active release of **HostTriangle** receives security patches and critical bug fixes:

| Version | Supported |
| :--- | :--- |
| Latest (`v0.1.x` / Beta) | Yes |
| Older versions | No |

---

## Reporting a Vulnerability

Because HostTriangle manages local sandboxed execution runtimes (`PRoot`, `cloudflared`, JVM) and handles user `.env` secrets on-device, security is taken seriously.

**Please DO NOT report security vulnerabilities through public GitHub Issues.**

Instead, please report them privately via one of the following channels:
1. **GitHub Private Vulnerability Reporting:** Navigate to the **Security** tab of this repository and select **"Report a vulnerability"** (Recommended).
2. **Email:** Send details directly to `support@tinexus.in` with the subject `[SECURITY] HostTriangle Vulnerability`.

Please include:
* A clear description of the vulnerability and its potential impact.
* Detailed reproduction steps or proof-of-concept payload.
* Affected Android OS version and device architecture (e.g., `arm64-v8a`).

You will receive an initial acknowledgment within **48–72 hours**. Once verified and resolved, you will be credited in the security advisory and release notes (unless you prefer to remain anonymous).
