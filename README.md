# oppo-music-no-add-patch

Experimental Morphe source bundle for `com.heytap.music`, exact version `48.15.1.60.5_007d1ba_261019`.

Select **Nhạc Local**. Leave the older standalone ad-placement patch unchecked (the combined patch already includes it).

Changes:
- Disable central advertising placement lookup (AdConfigHelper.g).
- Disable Taku/AT engine setup and readiness; remove eight SDK manifest activities/providers. Referenced SDK classes remain to avoid missing-class crashes. This is not complete physical removal of the SDK.
- Rename package to app.local.heytapmusic and package-dependent string constants/authorities/permissions; preserve Java class names.
- Set display name to Nhạc Local.
- Follow the existing local-music shortcut branch during startup.
- Return animation level 1 at all three identified readers. System settings are not modified.

## Status

These behaviors were implemented directly in the separate APK build and that build was installed on the phone. The Morphe translation has NOT compiled, patched an APK, or passed runtime tests. Local startup and ad coverage still need functional verification. No claim of full ad removal is made.

Build attempt fails resolving app.morphe.patches Gradle plugin 2.1.0. See the official template setup for GitHub Packages credentials. Do not paste access tokens into chat or commit them. Configure credentials locally, then run `./gradlew buildAndroid` with a supported JDK; expected output is patches/build/libs/*.mpp.

The original app is a split APK set. Use tooling that supports the full set and preserves cross-split resource references. Rename all split manifest package names to app.local.heytapmusic; preserve their versionCode 101501605, split IDs and original binary resource tables. Sign every split with the same key. Applying this bundle to base.apk alone does not produce a standalone installable application.

Use the ORIGINAL backup as patch input, not the manually patched clone. The patch changes the package and thus cannot match the clone as input.

## Previous packaging fixes

Preserve original compiled resources and META-INF/services metadata. Rebuilding decoded base resources alone previously replaced cross-split image references with @null. Dropping META-INF/services previously broke the Kotlin Main dispatcher.

## Popup and shortcut patch

Select **Nhạc Local - compact shortcuts and popup corners**. It depends on the combined Nhạc Local patch. It hides the local shortcut RecyclerView and makes its adapter report zero items. Popup clipping, blur and shadow use 20dp circular corners with zero smoothing; the system animation-level setting is untouched. These changes were manually tested in a separately rebuilt APK; this Morphe translation still requires compilation and runtime validation.

## GitHub Actions

`Build Morphe patches` runs on main/dev pushes, pull requests and manual dispatch. On success, download the `.mpp` bundle and SHA256SUMS from the run artifacts. Logs are uploaded even on failure. This workflow builds a patch bundle, not a patched Music APK; it never uploads phone backups, signing keys or copyrighted APKs.

GitHub Packages authentication uses the repository token by default. If Morphe registry access is denied, configure Actions secrets MORPHE_PACKAGES_TOKEN (read:packages) and MORPHE_PACKAGES_USER for its owner. A token does not fix a nonexistent plugin version: plugin-resolution errors must be checked against the registry/template as well. Release automation is manual-only to avoid publishing unvalidated draft bundles automatically.

## Localized app name

The manifest uses @string/app_name. Vietnamese is Nhạc Local; English is Local Music. Additional translations cover French, German, Spanish, Portuguese, Italian, Russian, Japanese, Korean, Chinese, Thai, Indonesian, Malay and Turkish. Other languages fall back to Local Music. Existing localized app_name entries are updated to prevent the old name overriding the patch. Widget and license-screen labels are preserved.
