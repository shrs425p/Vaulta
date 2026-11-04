# Privacy Policy for Vaulta

**Last Updated:** November 4, 2026  
**Effective Date:** November 4, 2026  
**Application:** Vaulta (Zero-Knowledge Local-First Encrypted Vault for Android)

---

## 1. Introduction & Core Philosophy

**Vaulta** is designed from the ground up on the principles of **Privacy by Design**, **Local-First Architecture**, and **Zero-Knowledge Cryptography**. 

Our fundamental belief is simple: **Your private files, photos, videos, documents, credentials, and cryptographic keys belong exclusively to you.** 

Vaulta operates with no telemetry, no tracking, no advertising, and no centralized backend servers. The developers of Vaulta do not have, and will never have, access to your master password, your encryption keys, your stored files, or your credentials.

---

## 2. Information We Do NOT Collect

Vaulta is an offline-first, client-side application. We do **not** collect, transmit, sell, rent, or monetize any user data:

- **No Personal Identifiable Information (PII):** We do not collect names, email addresses, phone numbers, device identifiers (IMEI, Android ID, Advertising ID), IP addresses, or location coordinates.
- **No Analytics or Telemetry:** There are zero tracking SDKs, analytics libraries (e.g., Firebase Analytics, Google Analytics, Mixpanel), crash-reporting beacons, or behavioral monitoring tools integrated into Vaulta.
- **No Account Registration:** You do not need an account with us to use Vaulta. We run no user registration servers or authentication databases.
- **No Access to Unencrypted Content:** Plaintext files, notes, credentials, 2FA/TOTP seeds, and passwords are never visible to anyone other than you.

---

## 3. How Your Data is Handled & Protected

All data processing takes place strictly on your local device.

### 3.1 Encryption & Key Derivation (Zero-Knowledge)
- **Master Key Derivation:** Your vault's master key is derived locally using **Argon2id** (via Argon2Kt) with memory-hard parameters ($64\text{ MiB}$ RAM, $3$ iterations, $4$ parallel lanes) or PBKDF2-HMAC-SHA256, salted with high-entropy cryptographic random bytes.
- **File & Credential Encryption:** Stored files, credentials, and metadata are encrypted on-device using authenticated **256-bit AES-GCM** (Galois/Counter Mode). Each file payload utilizes a unique, per-file derived cryptographic subkey via HKDF-SHA256 and unique nonces for every 64 KiB chunk to prevent chunk swapping, truncation, and tampering.
- **Traffic & Size Fingerprinting Defense:** Encrypted file blocks are padded with cryptographically secure random bytes to align with standard 64 KiB boundaries, preventing eavesdroppers and cloud storage providers from deducing file types or identities through exact file size matching.
- **Memory Hygiene:** In-memory plaintext buffers, derived keys, and password character arrays are explicitly overwritten with zeroes using secure zeroization routines immediately after use.
- **In-Memory SQLite Index:** Decrypted directory trees and credential records exist exclusively within an in-memory SQLite database while the vault is actively unlocked. Locking the vault or closing the application immediately wipes the in-memory database.

### 3.2 Biometric Authentication
- Biometric authentication (fingerprint, face unlock) utilizes Android's native `BiometricPrompt` API and hardware-backed **Android KeyStore** (Trusted Execution Environment / StrongBox Keymaster HSM).
- Biometric templates and sensor data never leave the secure hardware chip and are never accessible to Vaulta.
- Biometric unlock can be enabled or revoked by the user at any time from within the application's settings.

### 3.3 Screen Protection, Auto-Lock & Anti-Forensics
- **FLAG_SECURE:** Vaulta enforces Android's `WindowManager.LayoutParams.FLAG_SECURE` across all windows, blocking screenshots, screen recording, and preventing sensitive previews in Android's Recent Apps / App Switcher view.
- **Auto-Lock Timer:** An adjustable auto-lock manager locks the vault and shreds in-memory keys after a designated period of inactivity or whenever the application is sent to the background.
- **Clipboard Auto-Purge:** Copies sensitive passwords and TOTP codes with Android 13+ `EXTRA_IS_SENSITIVE` flags and automatically wipes the Android clipboard within 30 seconds.
- **Original Source File Shredder:** When you import sensitive photos, videos, or documents, Vaulta can automatically or interactively delete the unencrypted source file from your device storage (Downloads, Camera Roll, SD card) via Android SAF, eliminating leftover plaintext traces and preventing OS media scanners from indexing the original file.
- **Directory Shielding (`.nomedia`):** Vaulta supports creating `.nomedia` shield files in user directories, instructing Android's system `MediaScanner` to skip scanning and indexing those folders.

---

## 4. Application Flavors & Network Connectivity

Vaulta is released in two distinct build flavors to provide absolute transparency and user choice:

### A. FOSS / Offline Edition (`com.vaulta.shrs.offline`)
- **Zero Network Permissions:** The `android.permission.INTERNET` permission is completely stripped from the application manifest.
- **100% Air-Gapped:** The application has no physical capability to initiate network calls, contact any server, or communicate over the web. All cloud synchronization libraries are completely excluded from the compiled binary.

### B. Cloud Backup Edition (`com.vaulta.shrs`)
- **Optional & User-Initiated:** Cloud sync is completely optional and disabled by default.
- **Direct Client-to-Cloud Connection:** Synchronization operates directly between your device and Google Drive via official Google APIs. There are no intermediary proxy servers, third-party relays, or developer-controlled servers.
- **Stealth & Visible Storage Modes:** Users can freely choose between:
  - **Stealth Mode (Hidden App Data):** Stores encrypted backups strictly in Google Drive's isolated Application Data folder (`DRIVE_APPDATA`), completely hidden from Drive search and web file lists.
  - **Visible Mode:** Stores encrypted backups in a dedicated `Vaulta Backups` folder in Google Drive (`DRIVE_FILE`), allowing user-managed backup verification.
- **Selective Sync Policies:** You can mark individual files as `LOCAL_ONLY` (retained exclusively on local device storage, never synced) or `AUTO_SYNC`.
- **On-Demand Cloud Restore:** When restoring onto a new device, only the lightweight encrypted index (`vault_index.enc`) is synchronized upfront; individual encrypted file blobs are pulled on-demand, preventing unwanted storage and mobile data consumption.
- **End-to-End Encrypted Ciphertext Only:** Plaintext files, filenames, passwords, and encryption keys are **never** transmitted. Only encrypted blobs (named with random UUIDs) and the encrypted index are synchronized. Even if cloud storage is compromised, your data remains cryptographically inaccessible without your master password and `vault.key` file.

---

## 5. Device Permissions Used

Vaulta requests only the minimal set of permissions strictly necessary to deliver its core security functions:

| Permission | Purpose |
|:---|:---|
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Authenticate biometric credentials via Android KeyStore to unlock the vault locally. |
| Storage Access Framework (SAF) | Allows you to select files to import, export, or store your portable `vault.key`. Uses standard Android system file pickers; Vaulta never requests broad, legacy external storage permissions. |
| `INTERNET` *(Cloud Flavor Only)* | Used solely to synchronize encrypted blobs directly between your device and your personal Google Drive account. |
| `ACCESS_NETWORK_STATE` *(Cloud Flavor Only)* | Verifies connectivity status before initiating scheduled WorkManager background synchronization tasks. |

---

## 6. Third-Party Libraries & Dependencies

Vaulta relies exclusively on trusted, open-source Android libraries:
- **AndroidX & Jetpack:** Core architecture, Jetpack Compose UI, Room Database, Navigation, WorkManager.
- **Argon2Kt:** Local JNI bindings for Argon2id key derivation.
- **Google Tink / AndroidX Security:** Authenticated cryptographic primitives and KeyStore management.
- **Coil:** Client-side in-memory image loading with zero disk caching.
- **AndroidX Media3 (ExoPlayer):** In-memory streaming media playback with zero unencrypted disk leaks.
- **Google API Client *(Cloud Flavor Only)*:** Official client for direct Google Drive synchronization.

None of these libraries collect personal information or transmit telemetry on behalf of Vaulta.

---

## 7. Data Retention & User Control

- **Full Data Ownership:** All data encrypted by Vaulta belongs solely to you.
- **Data Deletion:** You can delete individual files, credentials, or entire folders at any time. When a file is deleted from Vaulta, its encrypted payload on disk is deleted and its index record is purged.
- **Complete Erasure:** Clearing the application data or uninstalling Vaulta immediately and irreversibly deletes the local encrypted vault and all associated keys. If you use cloud sync, deleting the Google Drive backup folder or clearing hidden app data completely deletes all remote encrypted backups.

---

## 8. Children’s Privacy

Vaulta does not address or collect data from anyone, including children under the age of 13 (or under the applicable age of digital consent in your jurisdiction). Because Vaulta collects no personal data whatsoever, it is fully compliant with COPPA, GDPR, and global privacy frameworks.

---

## 9. Changes to This Privacy Policy

Because Vaulta does not collect email addresses or maintain user accounts, updates to this Privacy Policy will be published directly within the project's source repository and included with application releases. The "Last Updated" date at the top of this policy will reflect any revisions.

---

## 10. Open-Source Verification & Contact

Vaulta is fully open source. You are encouraged to inspect, review, and compile the source code yourself to verify every claim made in this policy:

- **Source Code Repository:** [https://github.com/shrs425p/Vaulta](https://github.com/shrs425p/Vaulta)
- **Security & Privacy Inquiries:** For questions regarding this policy or to report security vulnerabilities, open an issue on GitHub.
