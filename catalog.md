# Vaulta Release and Dependency Catalog

This file records the published 1.0.0 baseline, the changes prepared for Vaulta 1.1.0, and the resolved third-party runtime dependencies for both Android editions.

## Release history

| Version | Date | Status | Version code | Android target | Source reference |
|---|---|---|---:|---:|---|
| 1.0.0 | November 5, 2026 | First published release | 1 | API 35 | Git tag `v1.0.0` (`9fda59c`) |
| 1.1.0 | November 6, 2026 | Build verified | 2 | API 36 | Release branch `vaulta/release-v1.1.0`; planned tag `v1.1.0` |

Version 1.1.0 retains the Cloud and Offline application IDs and minimum API level from 1.0.0. The app keeps the same IDs: Cloud `com.vaulta.shrs` and Offline `com.vaulta.shrs.offline`. Both retain minimum API 26.

## What shipped in 1.0.0

The `v1.0.0` tag is the source baseline for the first release. Its principal product features were:

- An encrypted local file vault with a browser, encrypted index and blob storage, and an in-app viewer for supported media and documents.
- A portable version-1 `vault.key` workflow with password-based vault unlock and recovery-code support. The tagged source defines a 32-byte master key and salt, Argon2id defaults of 64 MiB / 3 iterations / 4 lanes, and 64 KiB AES-GCM file chunks with 128-bit tags.
- A password manager with credential create/edit/delete, password generation, and general CSV import.
- An integrated TOTP authenticator.
- Biometric unlock, automatic locking, screen-capture controls, and clipboard cleanup settings.
- A Cloud edition that adds Google sign-in and Google Drive synchronization with `drive.file` and `drive.appdata` scopes, and a separate Offline edition whose sync provider reports sync as disabled.
- Android backup restrictions and local-first file handling.

At that baseline, Cloud's flavor manifest declared Internet and network-state permissions. The Offline flavor excluded the Cloud sync module and removed network-state permission; the later 1.1.0 preparation explicitly strips both permissions from Offline's merged manifest. The v1.0 viewer decoded images in memory but decrypted audio, video, PDF, and some binary previews to app-cache files. Viewer export/print/external handoff paths were also present; the 1.1.0 release removes or disables these plaintext-dependent paths and adds authenticated range-based audio/video playback.

## Changes from 1.0.0 to the 1.1.0 release

### Added

- Password manager export and import using a password-protected, encrypted Vaulta archive designed for moving Vaulta credentials between vaults and accounts.
- Authenticated range-based encrypted media reads for audio and video playback.
- CI and repository security checks, contribution guidance, issue templates, screenshots, and updated privacy/security documentation.
- Release build configuration that reads signing settings from environment variables or local properties.

### Improved

- Credential save, delete, and archive import paths now use stronger rollback and failure handling.
- Key-file parameter validation, encrypted stream versioning, biometric fallback, clipboard ownership cleanup, and stale viewer-cache cleanup.
- Cloud deletion error reporting and privacy-oriented log messages.
- Offline edition isolation: its merged release manifest removes Internet and network-state permissions, and cloud-only controls are hidden where sync is unavailable.
- The viewer keeps supported image, text, audio, and video previews within Vaulta. The release disables content paths that require writing plaintext for preview or handoff.
- Release signing and build metadata; the release is version code 2, version name 1.1.0, compile/target API 36, AGP 8.13.2, and Gradle 8.13.

### Removed or replaced

- General plaintext CSV password import was replaced by encrypted Vaulta archive import/export.
- Decrypted viewer export, printing, external-app handoff, the temporary plaintext FileProvider path, and PDF/binary preview paths that require plaintext files were removed or disabled.
- The unused Coil declaration and unused OkHttp logging-interceptor dependency were removed. No application code referenced the logging interceptor.
- Unused entries were removed from the Gradle version catalog, and remaining hard-coded Argon2 and test JSON dependency versions were moved into it.

## Detailed change inventory

### Password manager and viewer

- Adds a password-protected encrypted Vaulta archive containing credential fields, password history, tags, custom fields, and templates. It supports portability between Vaulta vaults and does not use the source vault's master key.
- Replaces general CSV password import with encrypted archive import/export. Adds export/import password prompts, validation, cancellation cleanup, status feedback, delete confirmation, serialized writes, and rollback on failed credential changes.
- Reworks audio/video playback to decrypt authenticated ranges in memory. Removes plaintext viewer export, print, external-app handoff, FileProvider sharing, and PDF/binary previews that need plaintext temp files.
- Keeps supported image and text previews in app memory; cleans additional stale viewer cache files and attempts overwrite-before-delete where possible. Physical erasure is not guaranteed.

### Encryption, vault, and privacy protections

- Adds stream format versioning and authenticates the version with encrypted chunks. Version 1.1 writes stream version 3; version 1.0.0 clients only read versions 1 and 2, so Cloud devices must be updated before syncing newly written files between them. The reader accepts stream versions 1 and 2, but importing real user datasets from 1.0.0 was not part of this verification; keep a backup before upgrading.
- Rejects vault key files with excessive Argon2 memory, iteration, or parallelism parameters to reduce resource-exhaustion risks.
- Removes failed StrongBox key entries before standard Android Keystore fallback and moves key-file references to encrypted preferences with migration handling.
- Clears decrypted index bytes and more sensitive media buffers/keys; reduces log details about filenames, sizes, blob names, MIME types, sync errors, biometric failures, and exceptions.
- Adds ownership labels to Vaulta clipboard entries so cleanup only clears content Vaulta itself copied. Adds startup cleanup for stale Vaulta clipboard content and viewer cache.
- Reports cloud deletion failures instead of proceeding as if remote deletion succeeded.
- Preserves Android backup restrictions and applies screen-capture protection before the first rendered frame when enabled.

### Edition and interaction changes

- Adds confirmation prompts before file/folder deletion and remote-copy removal; guards cloud-only actions when sync is unavailable and defaults unavailable-sync imports to local-only.
- Hides cloud sync controls in Offline and clarifies in onboarding that Offline does not use cloud storage.
- Removes nonfunctional theme preview buttons and revises security-setting explanations.
- Adds shared clipboard handling for recovery-code copying.
- Explicitly strips both Internet and network-state permissions from Offline release manifest merges. Cloud continues to declare network access for Google sign-in and Drive sync.

### Build, testing, and project changes

- Sets the release to 1.1.0 / version code 2 and API 36; upgrades AGP/Gradle to 8.13.2/8.13 and compiles every Android module against API 36.
- Moves remaining hard-coded Argon2 and test JSON dependency versions into the TOML version catalog and removes unused dependency/plugin entries, including the unused logging interceptor.
- Reads release signing values from local properties or environment variables and configures signing only when all values and the keystore file are present.
- Adds CI tests, release lint, debug/release assembly, an Offline network-permission assertion, CodeQL, secret scanning, dependency review, and Dependabot updates.
- Adds open-source licensing, build/use guidance, security reporting, contribution guidance, issue templates, screenshots, and product artwork.
- Updates the privacy and security wording to distinguish Offline and Cloud data paths and to state limits around deletion, metadata, memory clearing, screen capture, and third-party services.

## Release build verification (November 6, 2026)

- JDK 17, Gradle 8.13, AGP 8.13.2, Android SDK 36, and Build Tools 36.0.0 were used.
- Local Gradle verification passed: unit tests, Cloud and Offline release lint, and debug and release APK assembly for both editions (760 actionable Gradle tasks in the verification run).
- The test run completed 120 tests across 36 suites with no failures or skipped tests.
- Both release APK signatures were verified with `apksigner`; both match the v1.0.0 certificate SHA-256 fingerprint documented in the README.
- The Offline release merged manifest was checked and contains neither `INTERNET` nor `ACCESS_NETWORK_STATE`.
- Both release APKs installed and launched on a Google Play API 37 emulator. No Android runtime exceptions were recorded during the launch smoke check; this did not exercise sign-in, sync, archive import/export, or the full UI workflows.
- Built release APK sizes: Offline 4.99 MB; Cloud 5.91 MB.
- Physical-phone compatibility, Google sign-in/sync, and archive import/export workflows were not covered by this verification run.

## Offline edition

- **Application ID:** `com.vaulta.shrs.offline`
- **Release version:** `1.1.0-offline` (version code 2); first release baseline was `1.0.0` (version code 1).
- **Android support:** minimum API 26; target API 36.
- **Connectivity:** the Offline app has neither `INTERNET` nor `ACCESS_NETWORK_STATE` in its merged release manifest. It does not include the Cloud sync module.
- **Available behavior:** files, password records, encrypted credential archives, and previews remain local. Cloud sign-in, sync controls, and cloud-only file actions are not part of this edition. Imports default to local-only.
- **Release artifact:** 4.99 MB APK, signed with the release certificate whose SHA-256 fingerprint is recorded in the README.
- **Resolved runtime graph:** 158 third-party modules, including direct and transitive dependencies.

## Cloud edition

- **Application ID:** `com.vaulta.shrs`
- **Release version:** `1.1.0` (version code 2); first release baseline was `1.0.0` (version code 1).
- **Android support:** minimum API 26; target API 36.
- **Connectivity and sync:** includes the Cloud sync module and requests network permissions for Google sign-in and Google Drive sync. It uses Google Drive app-data or user-visible file storage according to the selected storage mode.
- **Available behavior:** users can choose local-only or cloud sync policies; cloud-only files can be downloaded on demand. Vault file and index payloads are encrypted before synchronization. Google and network providers can still observe account, request, timing, and storage metadata.
- **Release artifact:** 5.91 MB APK, signed with the same release certificate as the Offline edition.
- **Resolved runtime graph:** 192 third-party modules, including direct and transitive dependencies.

## Shared release details

Both editions share the encrypted vault, credential manager and protected Vaulta credential archive, clipboard handling, biometrics, auto-lock, and in-app previews. Both are built with JDK 17, Gradle 8.13, AGP 8.13.2, SDK 36, and Build Tools 36.0.0. The version 1.1.0 release build was verified locally.

## Edition, security, and recovery guides

- [Offline edition](docs/OFFLINE_EDITION.md)
- [Cloud edition](docs/ONLINE_EDITION.md)
- [Security architecture](docs/SECURITY_ARCHITECTURE.md)
- [Backup and recovery](docs/BACKUP_AND_RECOVERY.md)

The runtime inventory below was resolved from the release variants with Gradle on November 6, 2026. It includes direct and transitive Maven modules. A component marked **Both** appears in both release graphs; **Cloud only** appears only in the Cloud graph. License names are taken from artifact POM metadata or inherited parent POM metadata. Where a component provides its own license, that license applies to that component; licenses differ across the graph.

### Runtime dependency inventory

| Component | Version | Edition | Declared license |
|---|---|---|---|
| `androidx.activity:activity` | `1.9.3` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.activity:activity-compose` | `1.9.3` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.activity:activity-ktx` | `1.9.3` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.annotation:annotation` | `1.8.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.annotation:annotation-experimental` | `1.4.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.annotation:annotation-jvm` | `1.8.2` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.appcompat:appcompat` | `1.7.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.appcompat:appcompat-resources` | `1.7.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.arch.core:core-common` | `2.2.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.arch.core:core-runtime` | `2.2.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.autofill:autofill` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.biometric:biometric` | `1.2.0-alpha05` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.collection:collection` | `1.4.4` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.collection:collection-jvm` | `1.4.4` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.collection:collection-ktx` | `1.4.4` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose:compose-bom` | `2024.12.01` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.animation:animation` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.animation:animation-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.animation:animation-core` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.animation:animation-core-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.foundation:foundation` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.foundation:foundation-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.foundation:foundation-layout` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.foundation:foundation-layout-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material:material-icons-core` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material:material-icons-core-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material:material-icons-extended` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material:material-icons-extended-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material:material-ripple` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material:material-ripple-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material3:material3` | `1.3.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.material3:material3-android` | `1.3.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.runtime:runtime` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.runtime:runtime-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.runtime:runtime-saveable` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.runtime:runtime-saveable-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-geometry` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-geometry-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-graphics` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-graphics-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-text` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-text-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-tooling-preview` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-tooling-preview-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-unit` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-unit-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-util` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.compose.ui:ui-util-android` | `1.7.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.concurrent:concurrent-futures` | `1.1.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.concurrent:concurrent-futures-ktx` | `1.1.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.core:core` | `1.15.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.core:core-ktx` | `1.15.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.cursoradapter:cursoradapter` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.customview:customview` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.customview:customview-poolingcontainer` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-android` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-core` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-core-android` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-core-okio` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-core-okio-jvm` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-preferences` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-preferences-android` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-preferences-core` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.datastore:datastore-preferences-core-jvm` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.drawerlayout:drawerlayout` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.emoji2:emoji2` | `1.3.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.emoji2:emoji2-views-helper` | `1.3.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.exifinterface:exifinterface` | `1.3.6` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.fragment:fragment` | `1.5.4` | Offline only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.fragment:fragment` | `1.5.7` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.graphics:graphics-path` | `1.0.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.hilt:hilt-common` | `1.2.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.hilt:hilt-navigation` | `1.2.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.hilt:hilt-navigation-compose` | `1.2.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.hilt:hilt-work` | `1.2.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.interpolator:interpolator` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-common` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-common-java8` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-common-jvm` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-livedata` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-livedata-core` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-livedata-core-ktx` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-process` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-runtime` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-runtime-android` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-runtime-compose-android` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-runtime-ktx-android` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-service` | `2.8.7` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-viewmodel` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-viewmodel-android` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-viewmodel-compose-android` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | `2.8.7` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.loader:loader` | `1.0.0` | Offline only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.loader:loader` | `1.1.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media:media` | `1.7.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-common` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-container` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-database` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-datasource` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-decoder` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-exoplayer` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-extractor` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.media3:media3-ui` | `1.5.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.navigation:navigation-common` | `2.8.5` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.navigation:navigation-common-ktx` | `2.8.5` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.navigation:navigation-compose` | `2.8.5` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.navigation:navigation-runtime` | `2.8.5` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.navigation:navigation-runtime-ktx` | `2.8.5` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.profileinstaller:profileinstaller` | `1.3.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.recyclerview:recyclerview` | `1.3.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.resourceinspection:resourceinspection-annotation` | `1.0.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.room:room-common` | `2.6.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.room:room-ktx` | `2.6.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.room:room-runtime` | `2.6.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.savedstate:savedstate` | `1.2.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.savedstate:savedstate-ktx` | `1.2.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.security:security-crypto` | `1.1.0-alpha06` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.sqlite:sqlite` | `2.4.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.sqlite:sqlite-framework` | `2.4.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.startup:startup-runtime` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.tracing:tracing` | `1.2.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.tracing:tracing-ktx` | `1.2.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.vectordrawable:vectordrawable` | `1.1.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.vectordrawable:vectordrawable-animated` | `1.1.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.versionedparcelable:versionedparcelable` | `1.1.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.viewpager:viewpager` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.work:work-runtime` | `2.10.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `androidx.work:work-runtime-ktx` | `2.10.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.android.gms:play-services-auth` | `21.3.0` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.android.gms:play-services-auth-api-phone` | `18.0.2` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.android.gms:play-services-auth-base` | `18.0.10` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.android.gms:play-services-base` | `18.5.0` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.android.gms:play-services-basement` | `18.4.0` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.android.gms:play-services-fido` | `20.0.1` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.android.gms:play-services-tasks` | `18.2.0` | Cloud only | [Android SDK License](https://developer.android.com/studio/terms.html) |
| `com.google.api-client:google-api-client` | `2.7.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.api-client:google-api-client-android` | `2.7.1` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.apis:google-api-services-drive` | `v3-rev20241027-2.0.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.auth:google-auth-library-credentials` | `1.30.1` | Cloud only | [BSD 3-Clause (BSD New)](http://opensource.org/licenses/BSD-3-Clause) |
| `com.google.auth:google-auth-library-oauth2-http` | `1.30.1` | Cloud only | [BSD 3-Clause (BSD New)](http://opensource.org/licenses/BSD-3-Clause) |
| `com.google.auto.value:auto-value-annotations` | `1.11.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.code.findbugs:jsr305` | `3.0.2` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.code.gson:gson` | `2.10.1` | Offline only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.code.gson:gson` | `2.11.0` | Cloud only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.crypto.tink:tink-android` | `1.15.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.dagger:dagger` | `2.53.1` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.dagger:dagger-lint-aar` | `2.53.1` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.dagger:hilt-android` | `2.53.1` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.dagger:hilt-core` | `2.53.1` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.errorprone:error_prone_annotations` | `2.22.0` | Offline only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.errorprone:error_prone_annotations` | `2.36.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.guava:failureaccess` | `1.0.2` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.guava:guava` | `33.3.1-android` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.guava:listenablefuture` | `9999.0-empty-to-avoid-conflict-with-guava` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.http-client:google-http-client` | `1.45.2` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.http-client:google-http-client-gson` | `1.45.2` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.j2objc:j2objc-annotations` | `3.0.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.google.oauth-client:google-oauth-client` | `1.36.0` | Cloud only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.lambdapioneer.argon2kt:argon2kt` | `1.6.0` | Both | [MIT](https://github.com/lambdapioneer/argon2kt/blob/master/LICENSE) |
| `com.squareup.moshi:moshi` | `1.15.1` | Cloud only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.squareup.okhttp3:okhttp` | `4.12.0` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.squareup.okio:okio` | `3.9.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.squareup.okio:okio-jvm` | `3.9.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.squareup.retrofit2:converter-moshi` | `2.11.0` | Cloud only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `com.squareup.retrofit2:retrofit` | `2.11.0` | Cloud only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `commons-codec:commons-codec` | `1.17.0` | Cloud only | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `io.grpc:grpc-api` | `1.68.2` | Cloud only | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| `io.grpc:grpc-context` | `1.68.2` | Cloud only | [Apache-2.0](https://opensource.org/licenses/Apache-2.0) |
| `io.opencensus:opencensus-api` | `0.31.1` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `io.opencensus:opencensus-contrib-http-util` | `0.31.1` | Cloud only | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `jakarta.inject:jakarta.inject-api` | `2.0.1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `javax.inject:javax.inject` | `1` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.checkerframework:checker-qual` | `3.43.0` | Cloud only | [MIT](http://opensource.org/licenses/MIT) |
| `org.jetbrains:annotations` | `23.0.0` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-android-extensions-runtime` | `1.9.22` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-bom` | `1.8.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-parcelize-runtime` | `1.9.22` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-stdlib` | `2.1.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | `2.1.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | `1.8.22` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | `1.8.22` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.9.0` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-bom` | `1.9.0` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.9.0` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | `1.9.0` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-serialization-bom` | `1.6.3` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | `1.6.3` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm` | `1.6.3` | Both | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) |
| `org.jspecify:jspecify` | `1.0.0` | Both | [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) |

### Build and test-only dependencies

These are used to build or test Vaulta and are not part of the release APK runtime graph.

| Component | Version | Use | Declared license |
|---|---|---|---|
| Android Gradle Plugin | 8.13.2 | Build | Apache-2.0 |
| Gradle wrapper | 8.13 | Build | Apache-2.0 |
| Kotlin and Compose compiler plugins | 2.1.0 | Build | Apache-2.0 |
| KSP | 2.1.0-1.0.29 | Build/code generation | Apache-2.0 |
| Hilt Gradle plugin | 2.53.1 | Build/code generation | Apache-2.0 |
| Room Gradle plugin | 2.6.1 | Build/code generation | Apache-2.0 |
| JUnit 4 | 4.13.2 | Tests | Eclipse Public License 1.0 |
| MockK | 1.13.13 | Tests | Apache-2.0 |
| Turbine | 1.2.0 | Tests | Apache-2.0 |
| Truth | 1.4.4 | Tests | Apache-2.0 |
| Kotlin Coroutines Test | 1.9.0 | Tests | Apache-2.0 |
| `org.json:json` | 20240303 | Credential archive tests only | Public Domain per its POM; see [upstream license text](https://github.com/stleary/JSON-java/blob/master/LICENSE) |

## License notes

The runtime dependency graph is **not exclusively Apache-2.0**. It includes MIT-licensed Argon2kt in both editions, Google Play Services components under Google's Android SDK License in Cloud, and BSD-3-Clause Google Auth libraries in Cloud. The JSON library is test-only and declares Public Domain in its POM. Review each linked license before redistributing; this catalog is a dependency snapshot, not legal advice.

The build's version catalog is [`gradle/libs.versions.toml`](gradle/libs.versions.toml). The dependency table records the resolved release runtime graphs for the build date above.
