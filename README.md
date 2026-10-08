# SAFEPLAN 0.2 — Android-first, shared Compose Multiplatform UI

This is an **experimental source-code MVP**, based on the supplied SAFEPLAN Product Brief, **not a vetted safeguarding product**. Do not enter real sensitive personal information until independent security, safeguarding, legal, privacy and device testing are complete.

## Implemented in source
- **Safety Check:** immediate-danger guidance for UK users, without automatic contact/reporting.
- **Personal Safety Plan:** load, edit and save safer places, essential items, escalation steps and trusted people.
- **Incident Journal:** create dated incident entries, chronological timeline and per-entry deletion. Date/time is user-entered; no attachments or evidence authenticity guarantee.
- **Trusted People:** add, list and delete local contacts; no automatic calls, texts or device contacts permission.
- **Find Support:** offline informational UK service list (verify links and service details before release).
- **Privacy Centre:** explanation, confirmation-based local data and key deletion.

## Architecture
- `composeApp`: **shared Compose Multiplatform UI in `commonMain`** (currently built for Android; future iOS target can be added).
- `shared`: platform-neutral models and repository interface.
- `features/{safety,plan,journal,contacts,support}`: independent KMP feature/domain modules.
- `androidApp`: Android host and Keystore-backed encrypted local JSON repository adapter.

The repository uses AES-256-GCM with Android Keystore key material, fresh random IV per write, atomic file replacement and app-private storage. Android backup/device transfer extraction disabled; screenshots/recents preview blocked via `FLAG_SECURE`. No internet, location, contacts or SMS permissions are requested. All writes happen off the main thread.

## Build
1. Open `safeplan-kmp` in Android Studio with Android SDK 35 and JDK 17.
2. Use Gradle 8.9 (wrapper not included; generate one if Gradle is available).
3. Sync Gradle and run `androidApp` on Android API 26+.
4. Verify compilation and exercise create/restart/read/delete flows on a test emulator **using dummy data only**.

**This environment did not have Gradle or an Android SDK, so compilation and runtime tests have not been performed.** Dependencies may need version alignment during first sync.

## Remaining release blockers
- Professional safeguarding and UK GDPR/DPIA review; trauma-informed content and accessibility testing.
- Independent cryptographic/security review, threat model (shared/compromised devices, coercive control, deletion/forensics), secure lifecycle testing.
- Authentication/app lock design, safe exit behaviour and background exposure assessment.
- Real evidence attachments, validated timestamps, optional export and robust migration/restore strategy.
- UI instrumentation/unit tests, localization, verified support service details, Play Store compliance.
- iOS target and iOS encrypted-storage implementation are **not** included yet.

Deletion removes the app-private encrypted file and Android Keystore alias, but cannot guarantee erasure of all OS/device forensic remnants. There is no cloud sync or remote backup. No automatic reporting or external communication occurs.
