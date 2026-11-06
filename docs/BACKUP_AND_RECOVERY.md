# Vaulta Backup and Recovery

Vaulta has several distinct kinds of data. A backup of one kind does not automatically back up the others. Android app-data backup is disabled, and a `vault.key` file alone does not contain the encrypted vault files.

## What to protect

| Item | What it contains or enables | Keep in mind |
|---|---|---|
| `vault.key` | Salt, derivation parameters, and wrapped key slots used to unlock the vault | Keep protected copies. It is needed with the password or recovery code; it does not contain vault files or the encrypted index. |
| Recovery code | Unlocks the recovery-wrapped slot in the matching `vault.key` and allows setting a new password | Store it separately from the key file and device. It cannot recreate a missing key file. |
| Offline vault data | Encrypted index and encrypted file blobs in app-private storage | There is no automatic cloud sync in Offline. A key-file copy alone cannot restore these data. |
| Cloud-synced data | Eligible encrypted index and file payloads in Drive | Only configured synchronized data is covered. Confirm sync completed; local-only items are excluded. |
| Credential archive | Encrypted Vaulta password-manager records | Separate from the file vault. Its archive password is needed to import it. |
| Exported files | Decrypted files written to a chosen destination | These copies are outside Vaulta's encrypted storage and follow the destination's backup and access rules. |

## Before changing phones or reinstalling

1. Keep the master password and recovery code available in a secure place. Do not store them together with the only copy of `vault.key`.
2. Make a separate protected copy of `vault.key`. Confirm that the copy is readable from the destination you intend to use.
3. In Cloud, confirm a recent successful sync and verify that the files you need are set to sync. Cloud sync does not include local-only files or every piece of app state.
4. If you need password-manager records independently, export a Vaulta credential archive and keep its archive password separately.
5. Do not clear app data or uninstall until you have confirmed that all required encrypted data and recovery materials exist elsewhere.

## Unlocking and recovering access

- **You have the key file and current password:** Open the matching edition, select/use the key file, and unlock with the password.
- **You have the matching key file and recovery code, but forgot the password:** Use recovery for that vault and set a new password. The recovery code and key file must both be available.
- **You have the password but lost the key file:** The password does not recreate the random master key or the wrapped key file. Restore the matching key file from a protected copy.
- **You lost both the key file and recovery material:** Vaulta cannot derive the original master key from a newly created vault or from the password alone. Any encrypted file data without its original key remains inaccessible.
- **You are moving an Offline vault:** You need both the matching key file and the encrypted index and file blobs. The key file alone is not a complete backup, and Offline provides no automatic cross-device sync.
- **You are moving a Cloud vault:** Install the Cloud edition, sign in to the same Google account, use the matching key file and password/recovery code, then allow sync and on-demand downloads to complete. Google sign-in alone does not unlock encrypted content.

## Credential archives

A credential archive can move Vaulta password-manager records between Vaulta vaults or accounts. It is independent of the source vault master key, but it is protected by the archive password. Import it only into Vaulta. Review matching records before importing; records with colliding identifiers may be replaced by the import behavior in Vaulta 1.1.0. Keep a copy of important destination data before importing.

The credential archive does not include file-vault blobs, the encrypted file index, recovery codes, or the vault key file.

## Deletion and retention

Deleting a local item or uninstalling Vaulta does not guarantee immediate physical erasure from flash storage, nor removal of copies held by Android, a document provider, Google Drive, or a provider's backups. Cloud remote deletion requires a functioning Cloud connection and remains subject to Drive behavior and retention. Check the relevant destination directly when you need to confirm whether a copy remains.
