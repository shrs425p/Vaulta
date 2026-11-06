# Security Policy

Vaulta is an open-source, local-first vault application designed to safeguard sensitive files, credentials, and two-factor authentication tokens. We take security vulnerabilities seriously and welcome responsible disclosures from researchers, developers, and users.

---

## Supported Versions

Security fixes are made in the project repository and included in releases at the maintainers' discretion. No release support or response-time SLA is currently guaranteed by this policy. Check the repository's latest release and commit history before choosing a version; prefer the latest maintained build.

---

## Threat Model & Scope

### In-Scope Vulnerabilities:
- Cryptographic design weaknesses in Argon2id derivation or AES-256-GCM chunk streaming.
- Decrypted plaintext leaking to persistent flash storage, temporary cache files, or system logs.
- Bypass of Android Keystore biometric validation (TEE / StrongBox) or `FLAG_SECURE` window shields.
- Authentication tag forgery, chunk reordering, or truncation attacks against `.vault` or `vault.key` files.
- Unauthorized data leakage or token exposure in Google Drive synchronization.
- Sensitive information exposed through Android logs, temporary files, caches, backups, or system integrations.
- Vulnerabilities in third-party dependencies, build plugins, release signing, or dependency update processes.

### Out-of-Scope:
- Attacks requiring physical device compromise with unlocked root access (tampered kernels or Magisk).
- Attacks on modified, untrusted custom ROMs without hardware-backed Keystore support.
- Denial of service attacks requiring direct malware execution inside the host process sandbox.
- Vulnerabilities in Google, Android, OEM, or third-party services outside Vaulta's control (these may still affect Vaulta's security and should be reported if they expose a Vaulta-specific weakness).

## Security and privacy limits

- Vaulta's Offline and Cloud editions have different capabilities. The Offline edition removes `INTERNET` and `ACCESS_NETWORK_STATE` in its source manifest and excludes the sync module; verify the merged manifest and release dependency graph for every release.
- The Cloud edition uses Google sign-in and Drive APIs. Google and network providers can process account, connection, timing, and usage metadata. Vaulta does not operate the Google service and cannot control its handling of that information.
- The repository contains Android diagnostic log calls. Avoid logging secrets, tokens, plaintext filenames, or credential values; review log changes as part of security review. Device compromise, root access, modified Android builds, external cameras, and hostile accessibility/input services are outside the protection Vaulta can guarantee.
- Cryptographic and privacy claims describe the reviewed source implementation, not a formal security certification. Review the code and test the exact signed release artifact before relying on it for high-impact data.

## Third-party dependencies and open-source licensing

Vaulta uses open-source libraries for Android UI, storage, cryptography, and media. The Cloud build additionally includes Google Drive/OAuth and Google Play Services Auth components. Their licenses are not all the same: most AndroidX/Kotlin/Tink components use Apache-2.0, Argon2Kt uses MIT, test dependencies include other terms, and Google Play Services Auth is subject to Google's SDK/service terms.

Dependency versions and transitive dependencies can change. Before release, review the resolved runtime dependency graph for both variants, generate and verify a complete per-release software bill of materials (SBOM), scan for known vulnerabilities, and preserve required third-party notices. The current APK packaging configuration excludes some `META-INF` license resources, so confirm the required notices are available through another distribution channel or an in-app notice screen.

Vaulta's root `LICENSE` is Apache-2.0. That applies to covered Vaulta code; it does not replace third-party license obligations or prove ownership/provenance of every contribution, image, or other asset.

---

## Reporting a Vulnerability

Please do not open a public GitHub Issue for security vulnerabilities.

To report a vulnerability responsibly:

1. **Submit via GitHub Private Security Advisories:**  
   Go to [Report a Vulnerability](https://github.com/shrs425p/Vaulta/security/advisories/new).
2. **Include Technical Details:**
   - Detailed description of the defect or cryptographic weakness.
   - Proof-of-concept (PoC) code or steps to reproduce.
   - Target Android version, device model, and Vaulta edition (Offline or Online).

---

## Our Commitment

- **Acknowledgment:** We will acknowledge receipt of your report within 48 hours.
- **Assessment:** We will validate the issue and coordinate with you on a severity assessment.
- **Fix & Disclosure:** We will work on a patch and coordinate a public release timeline with appropriate attribution and credit.
