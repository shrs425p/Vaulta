# Security Policy

Vaulta is an open-source, zero-knowledge vault application designed to safeguard sensitive files, credentials, and two-factor authentication tokens. We take security vulnerabilities seriously and welcome responsible disclosures from researchers, developers, and users.

---

## Supported Versions

Only the latest release receive security patches and cryptographic updates.

| Version | Supported | Notes |
| :--- | :---: | :--- |
| **1.0.x** | Yes | Current active stable release |
| **< 1.0.0** | No | Deprecated development builds |

---

## Threat Model & Scope

### In-Scope Vulnerabilities:
- Cryptographic design weaknesses in Argon2id derivation or AES-256-GCM chunk streaming.
- Decrypted plaintext leaking to persistent flash storage, temporary cache files, or system logs.
- Bypass of Android Keystore biometric validation (TEE / StrongBox) or `FLAG_SECURE` window shields.
- Authentication tag forgery, chunk reordering, or truncation attacks against `.vault` or `vault.key` files.
- Unauthorized data leakage or token exposure in Google Drive synchronization.

### Out-of-Scope:
- Attacks requiring physical device compromise with unlocked root access (tampered kernels or Magisk).
- Attacks on modified, untrusted custom ROMs without hardware-backed Keystore support.
- Denial of service attacks requiring direct malware execution inside the host process sandbox.

---

## Reporting a Vulnerability

**Please DO NOT open a public GitHub Issue for security vulnerabilities.**

To report a vulnerability responsibly:

1. **Submit via GitHub Private Security Advisories:**  
   Go to [**Report a Vulnerability**](https://github.com/shrs425p/Vaulta/security/advisories/new).
2. **Include Technical Details:**
   - Detailed description of the defect or cryptographic weakness.
   - Proof-of-concept (PoC) code or steps to reproduce.
   - Target Android version, device model, and Vaulta edition (Offline or Online).

---

## Our Commitment

- **Acknowledgment:** We will acknowledge receipt of your report within **48 hours**.
- **Assessment:** We will validate the issue and coordinate with you on a severity assessment.
- **Fix & Disclosure:** We will work on a patch and coordinate a public release timeline with appropriate attribution and credit.
