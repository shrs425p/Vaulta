# Vaulta Security Architecture

This document summarizes the security boundaries implemented in Vaulta 1.1.0. It describes the current release, not a guarantee that every device, storage provider, or future build has identical behavior.

## Trust boundaries

- **Vaulta process:** Handles the unlocked master key and plaintext while the vault is open. Android app-private storage helps isolate local files from other ordinary apps, but a compromised or rooted device can weaken that boundary.
- **Android and device:** Provide app isolation, Keystore-backed biometric mechanisms, document-picker access, and screen-capture controls. Vaulta cannot protect data from a fully compromised operating system or a person who can use an already-unlocked device.
- **Selected document provider:** Holds the user-selected `vault.key` and any file explicitly exported through Android's document picker. The provider may be local, removable, or cloud-backed; its storage and backup rules apply.
- **Google Drive (Cloud edition only):** Receives encrypted sync payloads and associated Drive metadata. Account identity, access requests, timing, and storage metadata remain visible to Google and relevant network providers.

## Vault key hierarchy

The vault master key is generated when a vault is created. The portable `vault.key` stores salt and key-derivation parameters plus wrapped key slots; it does not contain the unwrapped master key as plaintext. One slot is wrapped using a password-derived key, and a recovery slot is wrapped using the recovery code. The password and recovery code can therefore unlock the same vault master key through separate wrapped slots.

Password wrapping uses Argon2id with the release defaults of 64 MiB memory, 3 iterations, and 4 lanes, followed by authenticated encryption. The app validates imported derivation parameters and rejects excessive values to limit resource-exhaustion input. The recovery code is a separate secret; it is not a replacement for the `vault.key` file.

For vault file content, the app uses AES-GCM with 256-bit keys and 128-bit authentication tags. Per-file keys are derived using HKDF, and file data is processed in 64 KiB authenticated chunks. Stream version 3 authenticates the format version. Version 1.1.0 reads legacy versions 1 and 2 as well as version 3, while 1.0.0 clients cannot read version 3.

## Password-manager archives

Credential export creates a password-protected encrypted Vaulta archive. Its archive password derives the archive encryption key independently of the vault master key, allowing the archive to be imported into another Vaulta vault. The archive is for Vaulta password-manager records; it is not a file-vault export or a general-purpose password format.

Treat the archive and its password as sensitive. Anyone with both can read its contents through a compatible Vaulta importer. Store them separately and delete temporary copies when they are no longer needed, recognizing that deletion is not guaranteed physical erasure.

## Plaintext exposure

Vaulta must hold some decrypted data in process memory to display or use it. The release uses authenticated range reads for audio and video and keeps supported previews in Vaulta's in-app paths. External-app handoff, printing, PDF/binary preview paths that require plaintext temporary files, and viewer export actions have been removed or disabled.

Exporting an individual vault file through the Browser deliberately creates a decrypted copy at a destination chosen by the user. Once written, that copy is governed by Android and the selected provider. Memory clearing and cache cleanup are best-effort; they cannot guarantee secure physical erasure from flash storage, crash dumps, or provider backups.

Android app-data backup is disabled. This reduces automatic backup of app-private data but does not back up or erase files stored through a document provider, user exports, or Cloud Drive copies.

## Edition separation

The Offline release excludes the sync module and removes `INTERNET` and `ACCESS_NETWORK_STATE` from its merged manifest. The Cloud release includes network permission and Google sign-in/Drive sync. The Offline release's manifest was checked for the 1.1.0 release; this property should be revalidated for every release artifact.

## Security scope

These design controls reduce exposure but do not constitute a formal audit or guarantee. Protect the device, master password, recovery code, `vault.key`, archive passwords, and exported plaintext. Keep the app updated, and report suspected vulnerabilities through the project's security reporting channel.
