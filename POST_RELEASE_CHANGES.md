# Changes since Vaulta 1.0.0

**Comparison date:** November 6, 2026  
**Baseline:** Git tag `v1.0.0`, at commit `9fda59c` (November 5, 2026)  
**Compared with:** The current repository checkout, including all tracked changes since the tag and the present uncommitted working-tree changes.

This report describes changes in plain language and contains no implementation code. It compares source trees, not the installed or published APK/AAB binaries. It does not claim that every current change has been built, tested, or released. The comparison includes all differences through the current tree; some items may have been added in follow-up repository commits and some remain uncommitted.

## Summary

Since version 1.0.0, the current source contains:

- A new encrypted Vaulta-only password archive flow, replacing the previous general CSV import path.
- A new encrypted streaming media source and a viewer redesign aimed at avoiding plaintext media files.
- Stronger failure handling around saving/deleting credentials and removing cloud copies.
- Changes to cryptographic stream versioning, key-file validation, biometric handling, clipboard cleanup, viewer-cache cleanup, and log messages.
- Clearer Offline-edition network isolation and edition-specific UI.
- New project documentation, issue templates, CI configuration, tests, and screenshots.
- Removal of file-sharing infrastructure and features that exported, printed, or opened decrypted viewer content in external apps.
- Revised privacy and security documentation, including more qualified descriptions of Cloud data flows and deletion behavior.

## Added since 1.0.0

| New file | What it adds |
|---|---|
| `.github/ISSUE_TEMPLATE/bug_report.yml` | Structured bug reports that ask for edition, Android/device details, reproduction steps, and expected behavior. It directs security reports to private reporting. |
| `.github/ISSUE_TEMPLATE/config.yml` | Disables blank issues and provides a private security-advisory link. |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | Structured feature requests, including which edition the request affects. |
| `.github/workflows/ci.yml` | GitHub Actions workflow to run Gradle tests and assemble Cloud and Offline debug builds on pushes and pull requests. |
| `LICENSE` | Adds the repository's Apache License 2.0 license text. |
| `README.md` | Adds project overview, feature and edition descriptions, setup/build information, and project links. |
| `SECURITY.md` | Adds security-reporting instructions, threat-model scope, and security policy. |
| `CONTRIBUTING.md` | Adds contribution setup, architecture notes, and security-focused contributor guidance. |
| `docs/screenshots/screen_cloud_inspector.jpg` | Adds a Cloud inspector screenshot for project documentation. |
| `docs/screenshots/screen_home.jpg` | Adds a home-screen screenshot. |
| `docs/screenshots/screen_passwords.jpg` | Adds a password-manager screenshot. |
| `docs/screenshots/screen_settings.jpg` | Adds a settings screenshot. |
| `docs/screenshots/screen_totp.jpg` | Adds a TOTP screenshot. |
| `docs/vaulta-banner.png` | Adds the current banner image in the working tree. The older `docs/banner.png` is removed. |
| `feature-browser/src/main/java/com/vaulta/browser/credentials/CredentialArchive.kt` | Adds creation and reading of password-protected Vaulta credential archives, including password-based key derivation, encrypted archive payloads, format/version checks, size/count limits, and clearing temporary byte/password buffers where implemented. |
| `feature-browser/src/test/java/com/vaulta/browser/credentials/CredentialArchiveTest.kt` | Adds tests for credential archive behavior. |
| `feature-viewer/src/main/java/com/vaulta/viewer/EncryptedMediaDataSource.kt` | Adds a Media3 data source that decrypts encrypted media by requested ranges for in-app playback, avoiding a complete plaintext media temp file. |

## Removed

| Removed file or capability | Change |
|---|---|
| `app/src/main/res/xml/file_paths.xml` | Removes FileProvider cache-path configuration previously used to share temporary viewer files. |
| `docs/banner.png` | Removes the former banner asset; a differently named banner image now exists in the current working tree. |
| Viewer export, print, and external-app opening | The viewer no longer writes decrypted content to a share/export temp file or sends it to print/external apps. Those actions now show an explanation that the actions are disabled to keep decrypted content in Vaulta. |
| General CSV password import | Replaced with Vaulta encrypted archive import/export. This change removes the old plaintext CSV import parser/path; it does not add standard CSV or KeePass/KDBX import/export. |
| PDF and binary file preview paths that created plaintext temp files | Those paths now report that preview is disabled where rendering would require a plaintext file. |
| Coil image-loader customization | Coil's app-level image-loader factory and the direct Coil app dependency were removed. The version catalog may still declare Coil; a catalog declaration alone does not mean it is packaged. |

## Security, encryption, and local storage changes

| File(s) | Change since 1.0.0 |
|---|---|
| `core-crypto/src/main/java/com/vaulta/crypto/StreamingEncryptionImpl.kt` | Advances the encrypted stream format version and includes the stream version in chunk authentication data. Existing files must remain readable through the decryption compatibility logic; confirm compatibility with actual version 1 data before distribution. |
| `core-crypto/src/test/java/com/vaulta/crypto/StreamingEncryptionTest.kt` | Adds or updates tests for the stream format and authenticated chunk behavior. |
| `core-storage/src/main/java/com/vaulta/storage/blob/BlobStore.kt` | Removes an API associated with decrypting blobs directly to a plaintext file. |
| `core-storage/src/main/java/com/vaulta/storage/blob/BlobStoreImpl.kt` | Removes the matching plaintext-to-file decryption implementation. |
| `core-storage/src/main/java/com/vaulta/storage/cleanup/ViewerCachePurge.kt` | Extends startup cleanup to cover additional preview cache files, overwrites data with random bytes before deletion where possible, and removes file-specific error details from logs. Filesystem/flash wear-leveling means overwrite is best effort, not a guarantee of physical erasure. |
| `core-storage/src/main/java/com/vaulta/storage/index/EncryptedIndexImpl.kt` | Clears decrypted index bytes after parsing and removes exception detail from selected log messages. |
| `core-vault/src/main/java/com/vaulta/vault/biometric/BiometricUnlockManager.kt` | Removes a failed StrongBox key entry before falling back to the regular Android Keystore, and reduces exception detail in biometric logs. |
| `core-vault/src/main/java/com/vaulta/vault/keyfile/VaultKeyFileSerializer.kt` | Rejects key files that request Argon2 settings above explicit memory, iteration, or parallelism limits, reducing the chance of malicious resource-exhaustion inputs. |
| `core-vault/src/test/java/com/vaulta/vault/VaultKeyFileSerializerTest.kt` | Adds or updates key-file serializer validation tests. |
| `core-vault/src/main/java/com/vaulta/vault/preferences/KeyFilePreferenceManager.kt` | Migrates key-file references away from ordinary preferences to encrypted preferences, while retaining fallback/migration handling and reducing sensitive details in logs. |
| `core-vault/src/main/java/com/vaulta/vault/session/AutoLockManager.kt` | Removes exception details from the log path used when clearing the in-memory index during vault lock. |
| `core-vault/src/main/java/com/vaulta/vault/clipboard/ClipboardAutoPurgeManager.kt` | Marks app-owned sensitive clips with unique ownership labels and clears only the clip Vaulta itself placed on the clipboard, avoiding clearing a newer clip copied by another app. Also supports clearing a Vaulta-owned clip at startup. Android may still restrict clipboard access. |
| `app/src/main/java/com/vaulta/shrs/BaseVaultaApp.kt` | Removes verbose startup/crash logging and the custom uncaught-exception logger; clears Vaulta-owned clipboard content at startup and invokes stale viewer-cache cleanup. |
| `app/src/main/java/com/vaulta/shrs/MainActivity.kt` | Applies or clears screen-capture protection based on the current preference, including before the first rendered frame. |
| `core-sync/src/main/java/com/vaulta/sync/drive/DriveSyncClientImpl.kt` | Reduces log detail so Drive blob names, sizes, and raw exceptions are less likely to enter logs; error status is still surfaced through the sync error path. |
| `core-sync/src/main/java/com/vaulta/sync/SyncEngine.kt` | Reports cloud deletion failure instead of silently proceeding as if the operation succeeded, and reduces sensitive details in logs. |

## Password manager and credential archive changes

| File(s) | Change since 1.0.0 |
|---|---|
| `feature-browser/src/main/java/com/vaulta/browser/credentials/CredentialArchive.kt` | Adds a Vaulta-specific archive format for password-manager records, protected by a separate export password. It serializes credential fields, password history, tags, custom fields, and templates into encrypted archive contents. |
| `feature-browser/src/main/java/com/vaulta/browser/credentials/CredentialsScreen.kt` | Replaces CSV import with encrypted Vaulta archive import and adds encrypted archive export. Adds password entry/confirmation for export and password entry for import, file-size checks, cancellation cleanup, user feedback, and delete confirmation. |
| `feature-browser/src/main/java/com/vaulta/browser/credentials/CredentialsViewModel.kt` | Serializes credential writes with a mutex, saves an encrypted index snapshot, rolls back a failed save/delete/import, and adds archive import/export operations. Replaces the plaintext CSV parser path. |
| `feature-browser/build.gradle.kts` | Adds JSON support for test/build implementation of credential archive behavior; verify its configuration remains test-only as intended. |
| `feature-browser/src/test/java/com/vaulta/browser/credentials/` | Adds tests for archive and credential behavior. |

The archive is intended for Vaulta-to-Vaulta password portability using its archive password, rather than depending on the source vault's master key or `vault.key`. This report describes the current design and does not claim independent cryptographic certification or interoperability with KeePass/KDBX.

## Browser, viewer, onboarding, and settings changes

| File(s) | Change since 1.0.0 |
|---|---|
| `feature-browser/src/main/java/com/vaulta/browser/BrowserScreen.kt` | Adds confirmation dialogs before deleting a file/folder or removing a remote copy; hides cloud-only download/sync actions when sync is unavailable. |
| `feature-browser/src/main/java/com/vaulta/browser/BrowserViewModel.kt` | Detects whether sync is available, defaults imports to local-only when it is not, guards cloud-only actions, and removes filenames, sizes, MIME types, and exception details from selected logs. |
| `feature-browser/src/main/java/com/vaulta/browser/credentials/CredentialsScreen.kt` | Adds archive controls and confirmation UI as described above. |
| `feature-browser/src/main/java/com/vaulta/browser/credentials/CredentialsViewModel.kt` | Adds transactional/rollback-oriented persistence and encrypted archive operations as described above. |
| `feature-onboarding/src/main/java/com/vaulta/onboarding/create/CreateVaultScreen.kt` | Routes recovery-code copying through the shared clipboard manager instead of maintaining a screen-local clipboard timer. |
| `feature-onboarding/src/main/java/com/vaulta/onboarding/create/CreateVaultViewModel.kt` | Adds a recovery-code copy action using the shared sensitive clipboard handling. |
| `feature-onboarding/src/main/java/com/vaulta/onboarding/welcome/WelcomeScreen.kt` | Shows Offline-specific welcome wording that clarifies that this edition does not use cloud storage. |
| `app/src/main/java/com/vaulta/shrs/navigation/VaultaNavHost.kt` | Passes the build's Offline-edition flag into the welcome screen. |
| `feature-settings/src/main/java/com/vaulta/settings/SecuritySettingsScreen.kt` | Hides cloud sync policy controls in the Offline edition and revises security setting descriptions. |
| `feature-settings/src/main/java/com/vaulta/settings/ThemeSettingsScreen.kt` | Removes nonfunctional sample buttons from the theme preview UI. |
| `feature-viewer/src/main/java/com/vaulta/viewer/ViewerViewModel.kt` | Moves audio/video playback to encrypted range-based reads, clears sensitive buffers/keys in more cleanup paths, removes media plaintext temp-file generation, routes copying through the clipboard manager, and disables PDF/binary previews where a plaintext file is required. |
| `feature-viewer/src/main/java/com/vaulta/viewer/ViewerScreen.kt` | Uses the encrypted media data source, changes viewer action availability, routes sensitive text copies through the clipboard manager, and disables export/print/external-app handoff. |
| `feature-viewer/build.gradle.kts` | Removes an unused JSON dependency declaration. |

## Build and edition behavior changes

| File(s) | Change since 1.0.0 |
|---|---|
| `app/build.gradle.kts` | Loads release signing values from environment variables as well as local properties, uses a release key only when its configuration is complete and the keystore file exists, and removes the direct Coil app dependency. Product editions remain Offline and Cloud. The version code/name were not changed in this file relative to the v1.0.0 baseline. |
| `app/src/main/AndroidManifest.xml` | Removes the app-level FileProvider previously used for temporary plaintext sharing. |
| `app/src/offline/AndroidManifest.xml` | Explicitly removes both Internet and network-state permissions from the Offline merged manifest. |
| `gradle.properties` | Removes a machine-specific Java installation path, so the build no longer depends on that one Windows machine's JDK location. |

## Policy, documentation, and project maintenance changes

| File(s) | Change since 1.0.0 |
|---|---|
| `PRIVACY.md` | Updates the policy date and distinguishes Offline local use from Cloud Google sign-in/Drive flows. Qualifies claims about metadata, memory clearing, screen capture, file deletion, uninstall behavior, and legal compliance; clarifies that app deletion and Google Drive deletion are separate. |
| `SECURITY.md` | Adds the security policy and reporting channel, threat-model boundaries, dependency/update guidance, and notes about edition differences, Google metadata, Android logs, and limitations. |
| `README.md` | Adds general project, feature, edition, build, and repository information. |
| `CONTRIBUTING.md` | Adds contributor setup and security-focused contribution guidance. |
| `LICENSE` | Adds Apache-2.0 repository license text. |
| `.github/ISSUE_TEMPLATE/*` | Adds public issue/feature templates and a private security-report link. |
| `.github/workflows/ci.yml` | Adds automated test and Cloud/Offline debug assembly jobs. |
| `docs/screenshots/*` | Adds five product screenshots. |
| `docs/banner.png`, `docs/vaulta-banner.png` | Removes the old banner path; adds the new banner image in the current tree. |

## Testing and verification status

This report was produced by comparing source against tag `v1.0.0` and reviewing the current file changes. It is not a test report. No test suite or release APK/AAB build was run specifically for this comparison. Before using these changes for a new release, build both editions, run relevant unit/instrumentation tests, validate old vault and key-file compatibility, check the merged Offline manifest, and test encrypted archive import/export and media playback on target devices.

## Repository state

This report was prepared in the working tree and is included with the release-preparation changes. Git history records the commit that contains it.
