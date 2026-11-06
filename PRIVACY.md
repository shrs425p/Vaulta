# Privacy Policy for Vaulta

**Last Updated:** November 6, 2026  
**Effective Date:** November 6, 2026  
**Application:** Vaulta (Zero-Knowledge Local-First Encrypted Vault for Android)

---

## 1. Introduction & Core Philosophy

**Vaulta** is designed from the ground up on the principles of **Privacy by Design**, **Local-First Architecture**, and **Zero-Knowledge Cryptography**. 

Our fundamental belief is simple: **Your private files, photos, videos, documents, credentials, and cryptographic keys belong exclusively to you.** 

Vaulta is designed for local-first use and does not include a Vaulta-operated account service or analytics backend. The Offline build has no network permission. The Cloud build can contact Google services when you sign in or use Google Drive sync. Vaulta is open source; these statements describe the current repository and are not a guarantee that every future build or third-party service behaves identically.

---

## 2. Information We Do NOT Collect

Vaulta does not include an advertising or analytics SDK, and the repository does not implement a Vaulta-operated collection service. This is different from saying that the Cloud build never sends data: Google receives the requests needed for sign-in and Drive operations, under Google's own terms and privacy policy. The app also writes diagnostic messages through Android logging APIs; do not assume device logs can never contain operational details.

- **No Vaulta-run PII collection service:** The current repository has no Vaulta-operated server that collects these details. Google sign-in and Drive can expose account, connection, request, and usage metadata to Google. Android and other device services may also process data under their own policies.
- **No Analytics or Telemetry:** There are zero tracking SDKs, analytics libraries (e.g., Firebase Analytics, Google Analytics, Mixpanel), crash-reporting beacons, or behavioral monitoring tools integrated into Vaulta.
- **No Account Registration:** You do not need an account with us to use Vaulta. We run no user registration servers or authentication databases.
- **Local UI handling:** Vaulta processes decrypted content on the device while the vault is unlocked. The app's source and Android platform behavior determine which actions are available; this policy is not a guarantee against a compromised device, operating system, keyboard, accessibility service, or user-authorized system picker.

---

## 3. How Your Data is Handled & Protected

Vault creation, encryption, decryption, browsing, and local storage are performed on your device. If you use Cloud sync, encrypted data is also sent to Google Drive as described below.

### 3.1 Encryption & Key Derivation (Zero-Knowledge)
- **Master Key Derivation:** Your vault's master key is derived locally using **Argon2id** (via Argon2Kt) with memory-hard parameters ($64\text{ MiB}$ RAM, $3$ iterations, $4$ parallel lanes) or PBKDF2-HMAC-SHA256, salted with high-entropy cryptographic random bytes.
- **File & Credential Encryption:** Stored files, credentials, and metadata are encrypted on-device using authenticated **256-bit AES-GCM** (Galois/Counter Mode). Each file payload utilizes a unique, per-file derived cryptographic subkey via HKDF-SHA256 and unique nonces for every 64 KiB chunk to prevent chunk swapping, truncation, and tampering.
- **Traffic and size metadata:** The implementation pads encrypted file data, but a network or storage provider may still observe traffic timing, transfer volume, account usage, and request metadata. Padding does not hide all file-size or activity information.
- **Memory hygiene:** Some sensitive byte buffers and key material are cleared where the implementation and platform permit. Managed-runtime copies, immutable UI strings, OS clipboard contents, crash state, and memory snapshots cannot be guaranteed to disappear immediately.
- **In-memory index:** The app uses an in-memory database for decrypted index data while unlocked. Clearing process memory is best effort; it is not a forensic guarantee that every copy is immediately erased.

### 3.2 Biometric Authentication
- Biometric authentication uses Android's native biometric APIs and Android Keystore-backed key material where supported. Hardware security level varies by device; Vaulta cannot guarantee that every device uses StrongBox or a particular TEE.
- Biometric templates and sensor data never leave the secure hardware chip and are never accessible to Vaulta.
- Biometric unlock can be enabled or revoked by the user at any time from within the application's settings.

### 3.3 Screen Protection, Auto-Lock & Anti-Forensics
- **Screen capture protection:** The main activity applies Android `FLAG_SECURE` in configured app states. Android/OEM behavior and external cameras or a compromised device can bypass practical screenshot protection.
- **Auto-Lock Timer:** An adjustable auto-lock manager locks the vault and shreds in-memory keys after a designated period of inactivity or whenever the application is sent to the background.
- **Clipboard Auto-Purge:** Copies sensitive passwords and TOTP codes with Android 13+ `EXTRA_IS_SENSITIVE` flags and attempts to clear Vaulta-owned clipboard contents within 30 seconds. Android may retain or restrict clipboard data after process death or background access.
- **Source-file deletion:** Deletion of an imported original depends on the selected Android document provider and storage medium. Normal deletion does not securely erase flash storage or guarantee removal of copies, backups, thumbnails, or provider trash.
- **Directory Shielding (`.nomedia`):** Vaulta supports creating `.nomedia` shield files in user directories, instructing Android's system `MediaScanner` to skip scanning and indexing those folders.

---

## 4. App Editions & Network Connectivity

Vaulta is released in two editions to make network and sync capabilities clear:

### A. FOSS / Offline Edition (`com.vaulta.shrs.offline`)
- **No app network permission:** The Offline source manifest removes `INTERNET` and `ACCESS_NETWORK_STATE`; check the merged release manifest whenever dependencies or build configuration change. Offline still uses third-party libraries for local UI, storage, cryptography, and media functions.

### B. Cloud Backup Edition (`com.vaulta.shrs`)
- **Optional Cloud features:** Cloud sync requires the Cloud build and a Google sign-in. The app contacts Google services for authentication and Drive operations. Background sync depends on user settings and Android WorkManager scheduling.
- **Google receives requests:** Sync uses Google APIs and Google Play Services Auth. The current code has no Vaulta-operated relay. Google and the device/network provider may process connection and account metadata under their respective terms.
- **Stealth & Visible Storage Modes:** Users can freely choose between:
  - **Stealth Mode (Hidden App Data):** Stores encrypted backups strictly in Google Drive's isolated Application Data folder (`DRIVE_APPDATA`), completely hidden from Drive search and web file lists.
  - **Visible Mode:** Stores encrypted backups in a dedicated `Vaulta Backups` folder in Google Drive (`DRIVE_FILE`), allowing user-managed backup verification.
- **Selective Sync Policies:** You can mark individual files as `LOCAL_ONLY` (retained exclusively on local device storage, never synced) or `AUTO_SYNC`.
- **On-Demand Cloud Restore:** When restoring onto a new device, only the lightweight encrypted index (`vault_index.enc`) is synchronized upfront; individual encrypted file blobs are pulled on-demand, preventing unwanted storage and mobile data consumption.
- **Encrypted sync payloads:** The sync implementation is intended to upload encrypted blobs and an encrypted index, not plaintext file contents or credentials. This depends on the current encryption and sync code and should be re-verified for releases. Google can still see account, request, timing, and storage metadata. Encryption does not protect data after it is unlocked on a compromised device.

---

## 5. Device Permissions Used

Vaulta requests only the minimal set of permissions strictly necessary to deliver its core security functions:

| Permission | Purpose |
|:---|:---|
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Authenticate biometric credentials via Android KeyStore to unlock the vault locally. |
| Storage Access Framework (SAF) | Allows you to select files to import, export, or store your portable `vault.key`. Uses standard Android system file pickers; Vaulta never requests broad, legacy external storage permissions. |
| `INTERNET` *(Cloud edition only)* | Google sign-in and Google Drive sync requests. |
| `ACCESS_NETWORK_STATE` *(Cloud edition only)* | Checks connectivity for sync work. |

---

## 6. Third-Party Libraries & Dependencies

Vaulta uses third-party dependencies for Android UI, storage, cryptography, media, and (Cloud only) Google Drive integration. The Offline runtime excludes `:core-sync`; Cloud includes Google API clients, Retrofit, OkHttp, WorkManager, and Google Play Services Auth. Most app dependencies use permissive open-source licenses such as Apache-2.0 or MIT. Google Play Services Auth is governed by Google's SDK/service terms, not Apache-2.0. Required third-party notices should be included with releases.

The current build does not declare an analytics or crash-reporting SDK. Some app code writes Android log messages; dependency behavior can change when versions change. Inspect the resolved release dependency graph before each release.

---

## 7. Data Retention & User Control

- **Full Data Ownership:** All data encrypted by Vaulta belongs solely to you.
- **Deleting vault items:** Vaulta removes the selected encrypted item from its local vault storage and updates the vault index. When Cloud sync is enabled, the app also requests deletion of the corresponding remote item from Google Drive.
- **Deleting app data or backups:** Android manages removal of Vaulta's private app data when app data is cleared or the app is uninstalled. Cloud backups can be removed through Vaulta's cloud controls or Google Drive. Google processes remote deletion under its own service and retention policies.

---

## 8. Children’s Privacy

Vaulta is not designed specifically for children. This policy is not a certification of compliance with COPPA, GDPR, or any other privacy law. The Cloud edition uses Google services, which may process account and connection data under Google's terms. Users and distributors should assess legal obligations for their use and jurisdictions.

---

## 9. Changes to This Privacy Policy

Because Vaulta does not collect email addresses or maintain user accounts, updates to this Privacy Policy will be published directly within the project's source repository and included with application releases. The "Last Updated" date at the top of this policy will reflect any revisions.

---

## 10. Open-Source Verification & Contact

Vaulta is fully open source. You are encouraged to inspect, review, and compile the source code yourself to verify every claim made in this policy:

- **Source Code Repository:** [https://github.com/shrs425p/Vaulta](https://github.com/shrs425p/Vaulta)
- **Security & Privacy Inquiries:** For questions regarding this policy or to report security vulnerabilities, open an issue on GitHub.
