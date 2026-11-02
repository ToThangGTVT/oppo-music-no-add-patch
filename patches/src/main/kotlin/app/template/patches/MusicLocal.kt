package app.template.patches

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val OLD = "com.heytap.music"
private const val NEW = "app.local.heytapmusic"
private const val NS = "http://schemas.android.com/apk/res/android"
private val target = Compatibility(name="HeyTap Music", packageName=OLD,
    apkFileType=ApkFileType.APK, appIconColor=0xFF0045,
    targets=listOf(AppTarget(version="48.15.1.60.5_007d1ba_261019")))

private val localManifest = resourcePatch {
    execute {
        val file = get("AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(file)
        doc.documentElement.setAttribute("package", NEW)
        val nodes = doc.getElementsByTagName("*")
        val remove = mutableListOf<org.w3c.dom.Node>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as org.w3c.dom.Element
            val component = node.tagName in setOf("application", "activity", "activity-alias", "service", "receiver", "provider")
            // Preserve Java class names; expand relative names against the original package.
            for (key in listOf("name", "targetActivity", "parentActivityName")) {
                val v = node.getAttributeNS(NS, key)
                if (component && v.startsWith(".")) node.setAttributeNS(NS,"android:$key", OLD + v)
            }
            val name = node.getAttributeNS(NS,"name")
            if (node.tagName in setOf("activity","provider") && name.startsWith("com.secmtp.")) remove += node
            val attrs = node.attributes
            for (j in 0 until attrs.length) {
                val attr = attrs.item(j)
                if (attr.namespaceURI == NS && attr.nodeValue.contains(OLD)) {
                    val isClass = component && attr.localName in setOf("name","targetActivity","parentActivityName","appComponentFactory","backupAgent")
                    if (!isClass) attr.nodeValue = attr.nodeValue.replace(OLD,NEW)
                }
            }
            if (node.tagName == "application" || node.hasAttributeNS(NS,"label"))
                node.setAttributeNS(NS,"android:label","Nhạc Local")
        }
        remove.forEach { it.parentNode.removeChild(it) }
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc),StreamResult(file))
    }
}

@Suppress("unused")
val musicLocal = bytecodePatch(
    name="Nhạc Local",
    description="Local startup, renamed package, disabled AT initialization and ad placement lookup, animation level 1. Exact-version experimental patch.",
    default=true
) {
    compatibleWith(target)
    dependsOn(localManifest)
    execute {
        fun method(type:String, name:String) = mutableClassDefBy(type).methods.single { it.name == name }
        method("Lcom/allsaints/music/ad/business/AdConfigHelper;","g")
            .addInstructions(0,"const/4 v0, 0x0\nreturn-object v0")
        method("Lcom/polaris/ad/google/internal/engine/taku/TakuEngine;","c")
            .addInstructions(0,"sget-object v0, Lkotlin/u;->a:Lkotlin/u;\nreturn-object v0")
        method("Lcom/polaris/ad/google/internal/engine/taku/TakuEngine;","isReady")
            .addInstructions(0,"const/4 v0, 0x0\nreturn v0")
        method("Lth/e;","f").addInstructions(0,"const/4 v0, 0x1\nreturn v0")
        method("Lcom/coui/appcompat/COUIMaterialStateManager;","c")
            .addInstructions(0,"const/4 v0, 0x1\nreturn v0")
        val device = method("Lpg/a;","e")
        val di = device.implementation!!.instructions
        val property = di.indexOfFirst { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "persist.sys.oplus.anim_level" }
        check(property >= 0) { "Missing animation property reader" }
        // Replace only the value returned by the property read; preserve the surrounding cached logic.
        val result = (property+1 until di.size).first { di[it].opcode == Opcode.MOVE_RESULT }
        check(result - property <= 4) { "Unexpected property reader" }
        val register = (di[result] as OneRegisterInstruction).registerA
        device.replaceInstruction(result,"const/4 v$register, 0x1")

        val main = method("Lcom/allsaints/music/MainActivity;","onCreate")
        val ins = main.implementation!!.instructions
        val key = ins.indexOfFirst { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "shortcut_target" }
        check(key > 0) { "Missing local shortcut startup path" }
        val first = (key-1 downTo 0).first { ins[it].opcode == Opcode.IF_EQZ }
        val local = (key+1 until ins.size).first { ins[it].opcode == Opcode.IF_EQZ }
        // Force both checks down the existing local-music shortcut branch.
        for (idx in listOf(local, first)) {
            val reg = (ins[idx] as OneRegisterInstruction).registerA
            main.addInstructions(idx,"const/4 v$reg, 0x1")
        }
        // Rewrite package-dependent string constants, not class descriptors.
        getAllClassesWithStrings().toList().forEach { def ->
            if (def.methods.any { m -> m.implementation?.instructions?.any {
                ((it as? ReferenceInstruction)?.reference as? StringReference)?.string?.contains(OLD) == true
            } == true }) {
                mutableClassDefBy(def.type).methods.forEach { m ->
                    m.implementation?.instructions?.toList()?.forEachIndexed { index, instruction ->
                        val text = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                        if (text != null && text.contains(OLD)) {
                            val reg = (instruction as OneRegisterInstruction).registerA
                            m.replaceInstruction(index,BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,reg,ImmutableStringReference(text.replace(OLD,NEW))))
                        }
                    }
                }
            }
        }
    }
}
