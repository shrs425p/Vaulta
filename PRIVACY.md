# Privacy Policy for Vaulta

**Last Updated:** November 3, 2026  
**Effective Date:** November 3, 2026  
**Application:** Vaulta (Zero-Knowledge Local-First Encrypted Vault for Android)

---

## 1. Introduction & Core Philosophy

**Vaulta** is designed from the ground up on the principles of **Privacy by Design**, **Local-First Architecture**, and **Zero-Knowledge Cryptography**. 

Our fundamental belief is simple: **Your private files, passwords, credit cards, and cryptographic keys belong exclusively to you.** 

Vaulta operates with no telemetry, no tracking, no advertising, and no centralized backend servers. The developers of Vaulta do not have, and will never have, access to your master password, your encryption keys, your stored files, or your credentials.

---

## 2. Information We Do NOT Collect

Vaulta is an offline-first, client-side application. We do **not** collect, transmit, sell, rent, or monetize any of the following:

- **No Personal Identifiable Information (PII):** We do not collect your name, email address, phone number, device identifiers (IMEI, Android ID, Advertising ID), IP address, or location.
- **No Analytics or Telemetry:** There are zero tracking SDKs, analytics libraries (e.g., Firebase Analytics, Google Analytics, Mixpanel), crash-reporting beacons, or behavioral monitoring tools integrated into Vaulta.
- **No Account Registration:** You do not need an account with us to use Vaulta. We run no user registration servers or authentication databases.
- **No Access to Unencrypted Content:** Plaintext files, notes, credentials, 2FA/TOTP seeds, and passwords are never visible to anyone other than you.

---

## 3. How Your Data is Handled & Protected

All data processing takes place strictly on your local device.

### 3.1 Encryption & Key Derivation (Zero-Knowledge)
- **Master Key Derivation:** Your vault's master key is derived locally using **Argon2id** (via Argon2Kt) with memory-hard parameters ($64\text{ MiB}$ RAM, $3$ iterations, $4$ parallel lanes) or PBKDF2-HMAC-SHA256, salted with high-entropy cryptographic random bytes.
- **File & Credential Encryption:** Stored files, credentials, and metadata are encrypted on-device using authenticated **256-bit AES-GCM** (Galois/Counter Mode). Each file payload utilizes a unique, per-file derived cryptographic subkey via HKDF-SHA256 and unique nonces for every 64 KiB chunk to prevent chunk swapping and tampering.
- **Memory Hygiene:** In-memory plaintext buffers, derived keys, and password character arrays are explicitly overwritten with zeroes using secure zeroization routines immediately after use.
- **In-Memory SQLite Index:** Decrypted directory trees and credential records exist exclusively within an in-memory SQLite database while the vault is actively unlocked. Locking the vault or closing the application immediately wipes the in-memory database.

### 3.2 Biometric Authentication
- Biometric authentication (fingerprint, face unlock) utilizes Android's native `BiometricPrompt` API and hardware-backed **Android KeyStore** (Trusted Execution Environment / StrongBox Keymaster HSM).
- Biometric templates and sensor data never leave the secure hardware chip and are never accessible to Vaulta.
- Biometric unlock can be enabled or revoked by the user at any time from within the application's settings.

### 3.3 Screen Protection & Privacy Controls
- Vaulta enforces Android's `WindowManager.LayoutParams.FLAG_SECURE` by default. This prevents screen recording, screen capture, and sensitive vault previews in Android's Recent Apps / App Switcher view.
- An adjustable **Auto-Lock Timer** automatically locks the vault and shreds in-memory keys after a designated period of inactivity or whenever the application is sent to the background.
- A **Clipboard Auto-Purge** manager copies sensitive passwords and TOTP codes with Android 13+ `EXTRA_IS_SENSITIVE` flags and automatically wipes the clipboard within 30 seconds.

---

## 4. Application Flavors & Network Connectivity

Vaulta is released in two distinct build flavors to provide absolute transparency and user choice:

### A. FOSS / Offline Edition (`foss`)
- **Zero Network Permissions:** The `android.permission.INTERNET` permission is completely stripped from the application manifest.
- **100% Air-Gapped:** The application has no physical capability to initiate network calls or communicate with the internet.

### B. Cloud Backup Edition (`cloud`)
- **Optional & User-Initiated:** Cloud sync is completely optional and disabled by default.
- **Direct Client-to-Cloud Connection:** Synchronization operates directly between your device and Google Drive via official Google APIs. There are no intermediary proxy servers, third-party relays, or developer-controlled cloud services.
- **Restricted App-Data Scope (`DRIVE_APPDATA`):** Vaulta requests access strictly to Google Drive's hidden Application Data folder. Vaulta cannot view, modify, or access any other files on your Google Drive.
- **End-to-End Encrypted Ciphertext Only:** Plaintext files, filenames, passwords, and encryption keys are **never** transmitted. Only encrypted blobs and the encrypted index (`vault_index.enc`) are synchronized. Even if the cloud storage is compromised, your data remains cryptographically inaccessible without your master password and `vault.key` file.

---

## 5. Device Permissions Used

Vaulta requests only the minimal set of permissions strictly necessary to deliver its core security functions:

| Permission | Purpose |
|:---|:---|
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Authenticate biometric credentials via Android KeyStore to unlock the vault locally. |
| `STORAGE` / SAF (Storage Access Framework) | Allows you to select where to import files from or export files and `vault.key` to. Uses Android system file pickers; Vaulta never requests broad filesystem read/write access. |
| `INTERNET` *(Cloud Flavor Only)* | Used solely to synchronize encrypted blobs directly to your personal Google Drive application folder. |
| `ACCESS_NETWORK_STATE` *(Cloud Flavor Only)* | Verifies connectivity status before initiating scheduled WorkManager background synchronization tasks. |

---

## 6. Third-Party Libraries & Dependencies

Vaulta relies exclusively on trusted, open-source Android libraries:
- **AndroidX & Jetpack:** Core architecture, Jetpack Compose UI, Room Database, Navigation, WorkManager.
- **Argon2Kt:** Local JNI bindings for Argon2id key derivation.
- **Google Tink / AndroidX Security:** Authenticated cryptographic primitives and KeyStore management.
- **Coil:** Client-side secure image loading into memory.
- **Google API Client *(Cloud Flavor Only)*:** Official client for Google Drive AppData synchronization.

None of these libraries collect personal information or transmit telemetry on behalf of Vaulta.

---

## 7. Data Retention & User Control

- **Full Data Ownership:** All data encrypted by Vaulta belongs solely to you.
- **Data Deletion:** You can delete individual files, credentials, or entire folders at any time. When a file is deleted from Vaulta, its encrypted payload on disk is deleted and its index record is purged.
- **Complete Erasure:** Clearing the application data or uninstalling Vaulta immediately and irreversibly deletes the local encrypted vault and all associated keys. If you use cloud sync, deleting the Google Drive AppData folder completely deletes all remote encrypted backups.

---

## 8. Children’s Privacy

Vaulta does not address or collect data from anyone, including children under the age of 13 (or under the applicable age of digital consent in your jurisdiction). Because Vaulta collects no personal data whatsoever, it is compliant with COPPA, GDPR, and global privacy frameworks.

---

## 9. Changes to This Privacy Policy

Because Vaulta does not collect email addresses or maintain user accounts, updates to this Privacy Policy will be published directly within the project's source repository and included with application releases. The "Last Updated" date at the top of this policy will reflect any revisions.

---

## 10. Open-Source Verification & Contact

Vaulta is fully open source. You are encouraged to inspect, review, and compile the source code yourself to verify every claim made in this policy:

- **Source Code Repository:** [https://github.com/shrs/Vaulta](https://github.com/shrs/Vaulta)
- **Security & Privacy Inquiries:** For questions regarding this policy or to report security vulnerabilities, open an issue on GitHub or contact the maintainers at `security@vaulta.local` (or via GitHub Security Advisories).
