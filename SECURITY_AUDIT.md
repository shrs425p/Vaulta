# Vaulta Security Audit

**Review date:** 2026-11-06
**Status:** Preliminary local review completed; fixes retested. This is not an independent penetration test, a formal audit, or a claim of OWASP MASVS certification.

## Scope and method

Reviewed the Android Offline and Cloud 1.1.0 release APKs and the release source prepared on `vaulta/release-v1.1.0`. The source review covered key derivation, authenticated vault encryption, password and recovery flows, biometric unlock and session handling, auto-lock, key-file parsing, lockout preferences, backup configuration, viewer-cache cleanup, network setup, and build dependencies. It did not audit Google Drive, Google sign-in, provider infrastructure, every source/dependency path, or perform a full dynamic security assessment.

MobSF 4.6.5 ran locally against both rebuilt APKs. The server listened on loopback; domain-malware lookup and VirusTotal upload were disabled. No APK or report was uploaded to a third party. A preliminary scan attempt invoked MobSF's package metadata lookup; the completed scans reported here were rerun with that lookup disabled.

| Edition | Package / version | MobSF score | High | Warnings | Info | Trackers |
|---|---|---:|---:|---:|---:|---:|
| Offline | `com.vaulta.shrs.offline` / `1.1.0-offline` | 69 | 0 | 8 | 2 | 0 |
| Cloud | `com.vaulta.shrs` / `1.1.0` | 62 | 0 | 12 | 2 | 0 |

MobSF scores are heuristic scanner outputs, not security grades. The completed MobSF runs were static APK analysis. I also installed both release APKs on the available Android emulator (API 37) and launched each: both reached the foreground with a running process, and the short launch checks found no fatal crash or app-process log output. This was only a startup smoke test; I did not unlock a vault, exercise biometric/auto-lock flows, intercept Cloud traffic, or run MobSF dynamic analysis. The emulator rejected `adb root` (`adbd cannot run as root in production builds`), and the release APKs are non-debuggable, so I could not inspect their private cache files directly. These checks do not establish absence of runtime vulnerabilities.

The release build command completed successfully:

```powershell
.\gradlew.bat --no-daemon test lintOfflineRelease lintCloudRelease assembleOfflineRelease assembleCloudRelease
```

Result: **120 tests, 0 failures, 0 errors, 0 skipped**; Offline and Cloud release lint and APK assembly passed. `apksigner verify` succeeded for both APKs. Both use the same RSA-4096 signing certificate, SHA-256 fingerprint:

`a51b31b9fa153c0a102acf49615af4666b6bf0cbc79175d329fb08db35ded269`

The Offline APK manifest has no `INTERNET` or `ACCESS_NETWORK_STATE` permission. The Cloud APK contains network permissions for its online functionality. The Cloud release runtime no longer resolves `google-http-client-apache-v2`/`httpclient`, and the rebuilt APK contains no Apache HTTP classes.

## Findings and changes

| ID | Initial finding | Action / current status |
|---|---|---|
| F-01 | Cloud MobSF scan reported a high-severity insecure-SSL finding in the optional Apache HTTP transport stack. | Removed the unused Apache transport/client dependencies; this app's Drive transport uses `NetHttpTransport`. Confirmed no Apache HTTP classes in the rebuilt Cloud APK and no matching dependency in its runtime classpath. The final MobSF scan reports **0 high findings**. Fixed for the scanned build. |
| F-02 | Key-file import read a document-provider stream without an input-size bound, allowing oversized input to consume memory. | Added a bounded stream read and a 2,107-byte serialized-file limit, with rejection tests. Included in the 1.1.0 release source. |
| F-03 | Malformed/oversized recovery-code input could reach validation/key-derivation work. | Validate exact supported raw or grouped format and allowed characters before derivation; cap recovery-code UI input. Added tests. Included in the 1.1.0 release source. |
| F-04 | Key-file parser accepted structurally invalid slot lengths and trailing bytes. | Enforce wrapped-key bounds and reject trailing data; added tests. Included in the 1.1.0 release source. |
| F-05 | New vault passwords did not have a strong minimum length. | New/create/change/recovery flows now require 15–1,024 characters. Existing vault unlock is not changed by this policy. Length alone does not guarantee a strong or unique password. Added tests. Included in the 1.1.0 release source. |

## Remaining review items

These are open limitations or scanner findings requiring continued review; they are not represented as confirmed exploitable vulnerabilities:

- **Minimum Android version:** The app supports API 26 (Android 8). MobSF warns that older operating systems may no longer receive security updates. Users need a supported, patched Android device; consider raising the minimum supported API in a future compatibility decision.
- **Certificate trust:** MobSF reports that network configuration trusts Android system certificates. This is normal platform CA validation and is not a trust-all configuration. Cloud traffic still depends on Android's TLS and CA store; this review did not perform runtime interception or pinning analysis.
- **Exported library components:** MobSF flags AndroidX/Google services and receivers guarded by platform or library permissions. Verify the effective permission protection levels across supported Android/Google Play Services versions; the scanner cannot determine every external permission's protection level from the app manifest alone.
- **RNG, SQL, and possible hardcoded-secret heuristics:** MobSF flags dependency/API patterns, raw SQL, and strings as possible risks. Source inspection found security-sensitive key/recovery generation uses `SecureRandom`, and database query paths reviewed use fixed/internal SQL rather than user-built query fragments. These checks were not exhaustive across every dependency and every code path. Review the full findings in MobSF before treating any as a false positive. No suspected credential values are reproduced here.
- **Logging:** Release R8 rules remove calls to Android's `Log` API. MobSF still identifies logging code in the packaged dependency set. App log messages reviewed do not include password/key contents, but continue to review new logging and test release APKs when dependencies change.
- **Clipboard:** Copying a credential or recovery value is an intentional user action. Other apps or the operating system may observe clipboard contents before Vaulta clears them; clipboard use cannot provide the same isolation as data kept inside the vault.
- **Plaintext and deletion boundaries:** Decrypted previews, user-selected exports, Android/provider caches, and flash storage can create copies outside encrypted vault storage. Deletion is not a guarantee of immediate physical erasure or removal of every provider/backup copy. Cloud removal also depends on Google Drive and its retention behavior.
- **Cloud and runtime coverage:** Static APK scanning does not verify Google account flows, live Drive access controls, network behavior on devices, rooted-device behavior, or all cache/log lifecycle paths. No full MobSF dynamic run or independent MASVS review was completed. Unlock, biometric failure/enrollment, lockout timing under clock changes, background auto-lock, and cache contents on a rooted test device remain untested. The UI lockout uses a wall-clock timestamp, so it should be treated as a deterrent rather than a tamper-resistant rate limit; offline password resistance still depends on the key-file KDF and password strength.
- **Key-file provider behavior:** A user-selected document provider controls storage and replacement behavior for `vault.key`; an interrupted overwrite could leave a damaged key file. Keep a protected copy and test recovery before relying on a provider for the only copy.

### Release signing

The local keystore store/key passwords were rotated before publication. The signing key and RSA-4096 certificate are unchanged; the verified certificate SHA-256 fingerprint remains `a51b31b9fa153c0a102acf49615af4666b6bf0cbc79175d329fb08db35ded269`. New passwords remain in ignored local configuration and are not included in source control.

## External review

A public community review request is posted at [GitHub issue #4](https://github.com/shrs425p/Vaulta/issues/4). It provides build instructions and asks reviewers to examine the 1.1.0 release source. No external review results have been received. This remains community feedback, not a formal audit or certification.

Reviewers can build both editions with:

```powershell
.\gradlew.bat --no-daemon test lintOfflineRelease lintCloudRelease assembleOfflineRelease assembleCloudRelease
```

The issue asks reviewers to focus on password derivation and recovery, authenticated encryption, credential archives, local storage and backup boundaries, Cloud sync, logging/caching, and Offline network isolation. Suspected vulnerabilities should be reported privately through [GitHub Security Advisories](https://github.com/shrs425p/Vaulta/security/advisories/new).
