# Contributing to Vaulta

Thank you for helping build Vaulta. We welcome contributions from developers, security researchers, and designers.

---

## Code of Conduct

Vaulta is committed to providing an open, welcoming, and harassment-free environment for all contributors.

---

## Getting Started

### Prerequisites
- JDK 17 (e.g. OpenJDK 17 or Eclipse Temurin)
- Android Studio Ladybug (or command-line Android SDK)
- Android SDK Platform 35 and Build-Tools 34.0.0+

### Development Setup
1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/Vaulta.git
   cd Vaulta
   ```
3. Open the project in Android Studio or verify via command line:
   ```bash
   ./gradlew test
   ```

---

## Architectural Guidelines

Vaulta follows a strict modular clean architecture:

- `core-crypto`: Pure Kotlin cryptographic algorithms (Argon2id, AES-256-GCM, PBKDF2). No Android UI dependencies.
- `core-storage`: Encrypted Room database, index serialization, and data layer.
- `core-sync`: Google Drive REST client, conflict resolution, and cloud worker engine.
- `core-vault`: Vault session lifecycle, master key management, and `vault.key` specification.
- `feature-*`: Jetpack Compose UI screens adhering to Material 3 design tokens.

### Security Rules for PRs
1. **Never cache decrypted plaintext on disk:** New previews must use memory-only buffers; do not re-enable file-backed media, PDF, binary, export, print, or external-app paths without a design and tests proving the security boundary.
2. **Never commit sensitive files:** Ensure `.gitignore` is respected (no `.jks`, `.keystore`, `.properties`, or credentials).
3. **Keep the Offline edition air-gapped:** No network libraries or internet permissions may ever be introduced into the `offline` product edition.
4. **Preserve test coverage:** Any modification to cryptographic or synchronization logic must include corresponding unit tests in `src/test/`.

---

## Submitting Pull Requests

1. Create a feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Commit your changes with clear, semantic commit messages:
   ```bash
   git commit -m "feat(crypto): add WebDAV synchronization adapter"
   ```
3. Run the test suite:
   ```bash
   ./gradlew test
   ```
4. Push to your fork and submit a Pull Request to `main`.
