# Vaulta Offline Edition

This guide describes Vaulta Offline 1.1.0.## Identity

- **Application ID:** `com.vaulta.shrs.offline`
- **Version:** `1.1.0-offline` (version code 2)
- **Android support:** API 26 and later; target API 36

## Network boundary

The Offline edition excludes the Cloud sync module. Its release manifest removes both Android's `INTERNET` and `ACCESS_NETWORK_STATE` permissions. Android therefore does not grant this app process ordinary network access. This is an artifact-level property of the build; it should be checked again for each release by inspecting the merged manifest and APK.

The app stores its vault data on the device. It does not offer Google sign-in, Drive sync, or cloud-only file actions. Password records, local file operations, supported previews, and Vaulta credential archive import/export work without Vaulta network services.

## Data and storage

Vault files and the encrypted index are stored in the app's private local storage. The portable `vault.key` is selected and managed through Android's document picker, so it may be stored outside the app's private directory. Keep a separate protected copy of it. The key file is wrapped, but possession of it together with the password or recovery code can enable vault access.

Android app-data backup is disabled in the app manifest. Uninstalling the app or clearing its data can remove the local encrypted index and file data; keeping `vault.key` alone does not restore those files. Use the backup guidance in [Backup and recovery](BACKUP_AND_RECOVERY.md) before changing devices or clearing app data.

Vaulta's credential archive is a separate, password-protected encrypted export for Vaulta password-manager records. It is not a backup of the file vault. When exporting individual files from the vault, the app writes decrypted content to a destination chosen in Android's document picker; that exported copy is outside the encrypted vault and follows the destination provider's storage and backup behavior.

## Practical limits

- A lost or corrupted `vault.key` cannot be recreated from the master password alone. Keep the key file and recovery code safe and separately backed up.
- Offline does not automatically copy data to another device. Copying only the key file is insufficient; the encrypted index and file blobs are also needed.
- Deleting app data, exported files, or copies held by a storage provider is not a guarantee of immediate physical erasure from flash storage or provider backups.
- The Offline build's no-network property does not control Android, the device manufacturer, other apps, or the storage provider used through the document picker.

## Release verification

For a release, inspect the merged Offline release manifest and confirm that it contains neither `INTERNET` nor `ACCESS_NETWORK_STATE`. Version 1.1.0 passed that check; future builds must be checked independently.
