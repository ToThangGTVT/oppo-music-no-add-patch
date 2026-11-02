# oppo-music-no-add-patch

Target: com.heytap.music 48.15.1.60.5_007d1ba_261019.

The patch inserts `const/4 v0, 0x0; return-object v0` at the start of AdConfigHelper.g(String): NewAdItem. The original method is nullable and looks up placements from IAdManager.getAdModel(). This deliberately disables placement resolution; coverage of other ad paths is not established.

Status: SOURCE ONLY. Not compiled, applied, or runtime tested. Build failed resolving the Morphe Gradle plugin. Configure GitHub Packages credentials as required by the upstream template, then run ./gradlew buildAndroid with a supported JDK. Output should be patches/build/libs/*.mpp.

Input is an installed split APK set. Preserve the architecture/resource splits when producing an installable package. A patched app needs signing and cannot normally replace the OEM-signed app directly. Do not uninstall the original or erase its data merely to test this draft.

No subscription, payment, or reward entitlement logic is modified.
