# Vaulta

Open-source, local-first Android vault for encrypted files, passwords, and 2FA with in-memory RAM streaming and air-gapped security.

[![Release](https://img.shields.io/github/v/release/shrs425p/Vaulta?label=Release&color=24634d)](https://github.com/shrs425p/Vaulta/releases/latest)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com)
[![Website](https://img.shields.io/badge/Website-vaultaaa.web.app-17231e.svg)](https://vaultaaa.web.app)

Official Website: [vaultaaa.web.app](https://vaultaaa.web.app) | Downloads Hub: [vaultaaa.web.app/download](https://vaultaaa.web.app/download)

<p align="center">
  <img src="docs/screenshots/screen_home.jpg" width="22%" alt="Vaulta Home Screen" />
  <img src="docs/screenshots/screen_totp.jpg" width="22%" alt="2FA Authenticator" />
  <img src="docs/screenshots/screen_passwords.jpg" width="22%" alt="Password Manager" />
  <img src="docs/screenshots/screen_settings.jpg" width="22%" alt="Settings and Export" />
</p>

---

## Why Vaulta?

Vaulta solves a specific problem that existing security tools leave fragmented across separate applications:

- **Bitwarden & Proton Pass:**
  Strong general-purpose password managers, but designed primarily around cloud infrastructure with active internet permissions, network syncing dependencies, and remote accounts. Vaulta's offline edition omits the network stack entirely at the Android OS manifest layer.

- **KeePassDX:**
  A capable offline credential manager, but restricted strictly to .kdbx password databases. It requires external file-provider sync plugins or manual file-picker workflows and does not offer an integrated RFC 6238 2FA authenticator or media streaming.

- **Aegis Authenticator:**
  An excellent offline 2FA app, but focused exclusively on one-time code tokens. It does not store encrypted documents, photos, notes, or credentials.

- **Vaulta's Architectural Differences:**
  - **Air-Gapped Offline Build:** The offline edition declares no `android.permission.INTERNET`. The Android OS prevents the process from creating network sockets.
  - **In-Memory RAM Streaming:** Encrypted photos, audio, videos, and PDFs stream through authenticated 64 KB cipher chunks directly into RAM buffers. Decrypted plaintext is never written to flash storage or temporary cache files.
  - **Unified Security Boundary:** 2FA TOTP/HOTP codes, password credentials, sensitive notes, and encrypted files live within a single Argon2id + AES-256-GCM hardware-backed vault.
  - **Zero Vendor Lock-In:** A complete one-click batch export option in Settings allows users to decrypt and restore all original media and credentials at any time.

---

## Security Audit Status

Vaulta is in active open-source development and has not yet undergone an independent third-party security audit. The complete source code, threat model, and cryptographic implementation are open for peer review and reproducible verification.

If you identify a security defect, please disclose it responsibly via [GitHub Security Advisories](https://github.com/shrs425p/Vaulta/security/advisories).

---

## Downloads

Vaulta is released in two distinct editions to match your personal operational security requirements. Both editions share the exact same cryptographic core:

- **Offline Edition (Air-Gapped):**
  - Package: `com.vaulta.shrs.offline`
  - Completely omits `android.permission.INTERNET`.
  - Zero network libraries compiled into the binary.
  - [Download Offline APK](https://github.com/shrs425p/Vaulta/releases/latest/download/app-offline-release.apk) (5.08 MB)

- **Online Edition (Google Drive Sync):**
  - Package: `com.vaulta.shrs`
  - Direct client-to-Google-Drive encrypted synchronization with zero intermediate servers.
  - Pre-encrypts all data locally before transmission (Google only receives ciphertext blobs).
  - Supports Stealth Mode (`appDataFolder`) and an itemized Cloud Storage Inspector.
  - [Download Online APK](https://github.com/shrs425p/Vaulta/releases/latest/download/app-cloud-release.apk) (6.11 MB)

### One-Command Sideload (via ADB)
```bash
adb install -r app-offline-release.apk
```

---

## Key Capabilities

- **In-Memory RAM Streaming:**
  Encrypted video, audio, photos, and PDF files stream directly from AES-256-GCM cipher chunks into memory buffers. Plaintext data never touches Android storage or flash memory cache.

- **Encrypted Password Manager:**
  Client-side credential store with zxcvbn entropy strength estimation, automatic clipboard purges after 30 seconds, and Diceware passphrase generation.

- **Integrated 2FA Authenticator:**
  RFC 6238 TOTP engine supporting 6-digit and 8-digit codes with visual countdown rings and encrypted URI exports.

- **Hardware Root of Trust:**
  Biometric unlock keys backed by Android Keystore (TEE / StrongBox). Screen surveillance protection enforced via `FLAG_SECURE`.

- **Batch Export:**
  Full batch-export capability to decrypt and restore all original media, documents, and credentials back to unencrypted files at any time from Settings.

- **Disaster Recovery via portable `vault.key`:**
  Encrypted salt and parameter container file allows full vault migration across phones without third-party servers.

---

## Cryptographic Architecture

```
[User Passphrase] + [vault.key (Salt & KDF Parameters)]
                       │
                       ▼
            Argon2id Key Derivation
       (Memory: 64 MB, Iterations: 3, Threads: 4)
                       │
                       ▼
               [Master Key (256-bit)]
                       │
       ┌───────────────┼───────────────┐
       ▼               ▼               ▼
 AES-256-GCM      AES-256-GCM      Android Keystore
 Media Stream      Room Database     (TEE / StrongBox)
 (64 KB Chunks)   (Credentials)    (Hardware Master Key)
```

### Specifications
- **Key Derivation Function:** Argon2id (m=65536 KB, t=3, p=4) with PBKDF2 fallback on unsupported platforms.
- **Symmetric Cipher:** AES-256 in Galois/Counter Mode (GCM) with 128-bit authentication tag.
- **Chunk Size:** 64 KB per authenticated chunk with monotonic index counters to guard against reordering attacks.
- **Biometric Enclave:** Android Keystore with Hardware-backed StrongBox / TEE validation.

---

## Release Signing & Integrity Verification

Official releases are signed with a 4096-bit RSA release key. You can independently verify the cryptographic authenticity of any downloaded APK using Android's official `apksigner` tool:

```bash
apksigner verify --print-certs app-offline-release.apk
```

Expected Output:
```text
Signer #1 certificate DN: CN=Vaulta Lead Developer, OU=Vaulta Security, O=Vaulta, L=Mumbai, ST=Maharashtra, C=IN
Signer #1 certificate SHA-256 digest: a51b31b9fa153c0a102acf49615af4666b6bf0cbc79175d329fb08db35ded269
Signer #1 certificate SHA-1 digest: f422277bd1ab457e24b38073c7bb57a0f9ba10f2
```

---

## Building from Source

### Prerequisites
- JDK 17+
- Android SDK (API 35, Build-Tools 34.0.0+)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/shrs425p/Vaulta.git
cd Vaulta

# Run unit and cryptographic tests
./gradlew test

# Build offline debug APK
./gradlew assembleOfflineDebug

# Build cloud debug APK
./gradlew assembleCloudDebug
```

Compiled APKs will be located at:
`app/build/outputs/apk/<flavor>/debug/`

---

## Contributing

Contributions, bug reports, and code audits are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on code standards, local testing, and submission guidelines.

---

## License

Vaulta is licensed under the [Apache License, Version 2.0](LICENSE).
