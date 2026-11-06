# Vaulta Cloud Edition

This guide describes Vaulta Cloud 1.1.0.## Identity

- **Application ID:** `com.vaulta.shrs`
- **Version:** `1.1.0` (version code 2)
- **Android support:** API 26 and later; target API 36

## What uses the network

The Cloud edition includes the sync module and requests Android network permissions for Google sign-in and Google Drive synchronization. Cloud features require connectivity and a signed-in Google account. The edition can also keep files local; sync policy determines which eligible files are synchronized. Local-only files are not uploaded. Cloud-only file content may need to be downloaded before it is available on the device.

Vaulta encrypts vault file and index payloads before synchronizing them. Drive stores the encrypted payloads, while Google and network providers can still observe account identity, requests, timing, approximate sizes, and storage metadata. This is not anonymous storage, and the encryption does not conceal all metadata.

Drive storage mode can use app-data storage or user-visible Drive files, depending on the selected mode. Review the account and Drive permissions shown during sign-in and the selected storage mode in the app.

## Local security and cloud boundary

The same local vault encryption, password manager, biometric unlock, auto-lock, and supported in-app previews are available as in Offline. Google sign-in authorizes Drive access; it does not replace the vault password or recovery code. Cloud sync is not a whole-device backup and does not upload files configured as local-only.

Remote availability depends on the Google account, network, Drive state, and successful synchronization. Before relying on a newly added or changed file, confirm that synchronization completed. Removing a local copy or uninstalling the app does not itself prove that a remote copy was deleted. Remote deletion and retention are subject to Drive behavior and account-level policies.

## Version compatibility

Vaulta 1.1.0 writes encrypted file streams using format version 3. Vaulta 1.0.0 clients can read only versions 1 and 2. Update all Cloud devices before synchronizing files newly written by 1.1.0 between them; an older client cannot read those version-3 streams.

## Practical limits

- Network access creates a metadata and account trust boundary that does not exist in the Offline build.
- Cloud sync is not a substitute for keeping a protected copy of `vault.key` and a recovery code.
- Google Drive availability, deletion, account recovery, and retention are outside Vaulta's control.
- Android app-data backup is disabled. Drive sync only covers the data Vaulta is configured to synchronize; it is not a complete copy of app state.

See [Security architecture](SECURITY_ARCHITECTURE.md) for the encryption design and [Backup and recovery](BACKUP_AND_RECOVERY.md) for recovery planning.
