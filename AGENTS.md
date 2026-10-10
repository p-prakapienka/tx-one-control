# AGENTS.md

Guidance for AI coding agents (Claude Code, Codex, Cursor, …) and humans working in
this repository. Read it before changing anything.

## What this is

`tx-one-control` is an Android app (Kotlin + Jetpack Compose) that controls an
**original IK Multimedia ToneX One** (the model without Bluetooth) over USB-OTG.

It is built on [wiz0floyd/Tonex-OTG](https://github.com/wiz0floyd/Tonex-OTG) (Apache-2.0),
whose full history this repo carries. Tonex-OTG's USB protocol layer is ported from
[Builty/TonexOneController](https://github.com/Builty/TonexOneController) (Apache-2.0) and
[vit3k/tonex_controller](https://github.com/vit3k/tonex_controller) (MIT).

The work specific to this repo is a hardware-style "rig" UI on top of Tonex-OTG: an LCD
preset readout, a signal-chain strip and rotary knobs. It's an original design. Never
copy code, artwork, fonts or assets from IK Multimedia's apps. This project is not
affiliated with IK Multimedia.

Hobby project: keep anything that protects the user's pedal from bad writes; don't
gold-plate the rest (latency targets, gig-grade soak tests, elaborate auto-recovery).

## Layout

| Path | What |
| --- | --- |
| `protocol/` | Pure Kotlin/JVM: USB framing (HDLC, CRC), message codecs, parameter catalog, controller. **No Android dependency, Maven Central only, must build with no Android SDK present.** |
| `app/` | Android/Compose app. `google()` is declared only in `app/build.gradle.kts`, never centrally. |
| `app/src/main/kotlin/dev/tonexotg/app/ui/` | Screens, components, theme. All color literals live in `ui/theme/Color.kt`. |
| `docs/design/` | Mockups, interaction spec, design tokens. |
| `docs/architecture/` | Architecture notes (e.g. `s23-ui-wiring.md`, the navigation graph). |
| `docs/upstream-agent-guide.md` | Tonex-OTG's original agent guide (formerly `CLAUDE.md`). Code comments that cite "CLAUDE.md" mean this file. |
| `CREDITS.md`, `CONTRIBUTING.md`, `LICENSES/` | Attribution and the source-header convention for ported code. Follow them for any ported logic. |

## Build and verify

Requirements: **JDK 21**, Android SDK with **platform 36** (AGP 8.13.2, Gradle 8.14.3
wrapper, Kotlin 2.2.21). AGP auto-downloads platform/build-tools 36 once SDK licences are
accepted.

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties   # if not already present

./gradlew :protocol:test
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --continue --stacktrace
# APK: app/build/outputs/apk/debug/app-debug.apk
```

These are exactly the checks CI runs. **Lint is not optional**: upstream once had
`:app:lintDebug` red on `main` for three stories because everyone only ran tests, and lint
was the tool that would have caught a real crash. Run all three.

Network hosts a local build needs: `dl.google.com` and `maven.google.com` (AGP, AndroidX,
Compose, SDK), `repo.maven.apache.org`, `plugins.gradle.org`, `services.gradle.org`.

### If your sandbox can't build

Some agent sandboxes block `dl.google.com`. Without it, Gradle can't resolve AGP, and
then **even `:protocol:test` fails**, because the root `build.gradle.kts` declares the
Android plugin. Don't fake results and don't swap in unofficial Maven mirrors. Push your
branch and let CI build it (see below), then read the run's logs and fix from there.
Tell the user which host was blocked so they can allow it.

## CI (`.github/workflows/`)

| Workflow | Trigger | What it does |
| --- | --- | --- |
| `ci.yml` | push to **any** branch, manual dispatch | `protocol` job: `:protocol:test` with the Android SDK removed (enforces the SDK-free guarantee). `android` job: assemble + unit tests + lint, then uploads the **`debug-apk-<run>`** artifact (30 days), plus lint/test reports on failure. Docs-only pushes skip both jobs. |
| `nightly.yml` | push to `main` | Signed nightly pre-release. **Off** until the repo variable `ENABLE_NIGHTLY=true` is set and the `RELEASE_KEYSTORE_*` secrets exist. |
| `release.yml` | push of a `v*` tag | Signed release APK attached to a GitHub Release. Needs the same `RELEASE_KEYSTORE_*` secrets. |
| `_build-signed-apk.yml` | called by the two above | Shared build/sign/verify steps. |

The APK is uploaded even when lint or tests fail (the job still goes red), so a build can
be tried on a phone while a lint finding is being fixed. A missing APK means
`assembleDebug` itself failed.

### Stable signing for CI builds

Android only installs an update over an existing app when both are signed with the same
key. By default every CI run signs with a fresh throwaway debug key, so you have to
uninstall the app (and lose its local data, such as preset aliases) between builds. To
avoid that, create one debug keystore and store it as a repo secret. Never commit it:
the repo is public and `.gitignore` excludes `*.keystore`.

```bash
keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
base64 -w0 debug.keystore   # macOS: base64 -i debug.keystore
```

Paste the output into **Settings → Secrets and variables → Actions → New repository
secret** as `DEBUG_KEYSTORE_BASE64`. The `android` job installs it as
`~/.android/debug.keystore` before building. You still have to uninstall once, when moving
from an APK signed with a different key.

Debug builds use applicationId `dev.tonexotg.app`, the same as upstream Tonex-OTG, so a
build from here replaces an installed Tonex-OTG.

## Hard rules

1. **`:protocol` stays Android-free and SDK-free.** No AndroidX, no `google()`. The CI
   `protocol` job enforces this.
2. **UI work doesn't change pedal write behaviour.** Every write goes through the existing
   view-model paths (throttler, first-write notice, revert, typed errors). A UI change
   that needs a new pedal command or a new write path isn't a UI change any more. Stop
   and treat it as protocol work (rule 6).
3. **Fail fast and loud.** Surface a typed error (`TonexError`/`TonexResult`) instead of
   guessing, retrying silently, or reporting success on an operation that didn't fully
   succeed.
4. **Prefer literal assertions over round-trip self-consistency** whenever a real captured
   or upstream reference literal exists. Self-consistent fixtures have hidden real
   wire-format bugs here before.
5. **Don't silently pin or drop what only hardware can settle.** There's no pedal in the
   cloud. Leave a skipped test with a clear reason and file the exact observation that
   would resolve it as a GitHub issue.
6. **Protocol or codec changes get an adversarial review** before merge. Score the diff
   with the rubric in `docs/upstream-agent-guide.md` ("Review rigor, scaled to blast
   radius"). A score of 4 or more needs a reviewer who checks literals against the
   actual upstream source, not against citations in the code.
7. **Attribution:** ported code follows the source-header convention in `CONTRIBUTING.md`.
   Keep `LICENSE`, `LICENSES/` and `CREDITS.md` intact.
8. Keep each color literal in `ui/theme/Color.kt`. Don't hard-code colors elsewhere.

## Git workflow

- One branch per task, branched from an up-to-date `main`. Never commit directly to
  `main`. Changes land through PRs.
- **Commit small and push right after every commit.** Agent containers are ephemeral and
  sessions can end without warning; unpushed work is lost. Commit the working change
  first, then verify as its own resumable step.
- A PR is mergeable when CI (`protocol` + `android`) is green on its head commit. Put the
  what/how/verification write-up in the PR description.
- UI changes need the owner's sign-off on a real phone, using the CI APK. Green CI isn't
  enough to judge how a screen looks or feels.
- Don't trust an earlier session's "tests pass" claim. Re-run the checks, or read the
  latest CI run.

### Pulling in upstream Tonex-OTG changes

```bash
git remote add upstream https://github.com/wiz0floyd/Tonex-OTG.git   # once
git fetch upstream
git checkout -b sync-upstream origin/main
git merge upstream/main     # merge, don't rebase: keeps upstream SHAs and attribution
```

Open that as a PR like any other change. Upstream workflow edits can conflict with this
repo's CI changes (all-branch trigger, APK artifact, nightly gate). Keep this repo's
behaviour when resolving those.
