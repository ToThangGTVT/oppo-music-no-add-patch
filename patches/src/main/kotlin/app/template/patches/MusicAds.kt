package app.template.patches

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.ApkFileType

private object AdPlacement : Fingerprint(
    definingClass = "Lcom/allsaints/music/ad/business/AdConfigHelper;",
    name = "g",
    returnType = "Lcom/polaris/ad/base/entity/NewAdItem;",
    parameters = listOf("Ljava/lang/String;")
)

@Suppress("unused")
val disableMusicAds = bytecodePatch(
    name = "Disable HeyTap Music ad placements",
    description = "Return no advertising placement from the central placement lookup. Experimental, exact version only.",
    default = false
) {
    compatibleWith(Compatibility(name = "HeyTap Music", packageName = "com.heytap.music", apkFileType = ApkFileType.APK, appIconColor = 0xFF0045, targets = listOf(AppTarget(version = "48.15.1.60.5_007d1ba_261019"))))
    execute {
        AdPlacement.method.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
    }
}
