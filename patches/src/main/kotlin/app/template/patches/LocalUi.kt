package app.template.patches

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*

private const val FRAME = "Lcom/coui/appcompat/poplist/RoundFrameLayout;"

@Suppress("unused")
val localUi = bytecodePatch(
    name="Nhạc Local - compact shortcuts and popup corners",
    description="Hide local shortcut strip and align popup clipping, blur and shadow geometry to 20dp. Experimental exact-version patch.",
    default=true
) {
    dependsOn(musicLocal)
    execute {
        val adapter = mutableClassDefBy("Lcom/allsaints/music/local/adapter/LocalHostTopAdapter;")
        check(adapter.methods.none { it.name == "getItemCount" })
        val count = ImmutableMethod(adapter.type,"getItemCount",emptyList(),"I",AccessFlags.PUBLIC.value,
            emptySet(),emptySet(),MutableMethodImplementation(2)).toMutable()
        count.addInstructions(0,"const/4 v0, 0x0\nreturn v0")
        adapter.methods.add(count)

        val host = mutableClassDefBy("Lcom/allsaints/music/local/LocalHostFragment;").methods.single { it.name == "initViews" }
        val at = host.implementation!!.instructions.indexOfFirst {
            ((it as? ReferenceInstruction)?.reference as? MethodReference)?.let { r ->
                r.definingClass == "Landroidx/recyclerview/widget/RecyclerView;" && r.name == "setAdapter"
            } == true
        }
        check(at >= 0) { "Missing local shortcut RecyclerView binding" }
        host.addInstructions(at+1,"const/16 v7, 0x8\ninvoke-virtual {v3, v7}, Landroid/view/View;->setVisibility(I)V")

        val frame = mutableClassDefBy(FRAME)
        check(frame.methods.none { it.name == "localMenuRadius" })
        val radius = ImmutableMethod(FRAME,"localMenuRadius",emptyList(),"F",AccessFlags.PRIVATE.value,
            emptySet(),emptySet(),MutableMethodImplementation(3)).toMutable()
        radius.addInstructions(0,"""
            invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;
            move-result-object v0
            invoke-virtual {v0}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;
            move-result-object v0
            iget v0, v0, Landroid/util/DisplayMetrics;->density:F
            const/high16 v1, 0x41a00000
            mul-float/2addr v0, v1
            return v0
        """.trimIndent())
        frame.methods.add(radius)
        frame.methods.filter { it.name != "localMenuRadius" }.forEach { m ->
            val impl = m.implementation ?: return@forEach
            impl.instructions.toList().withIndex().reversed().forEach { (index, ins) ->
                val field = (ins as? ReferenceInstruction)?.reference as? FieldReference
                if (ins.opcode == Opcode.IGET && field?.definingClass == FRAME && field.name in setOf("j","k")) {
                    val regs = ins as TwoRegisterInstruction
                    m.replaceInstruction(index,"invoke-direct {v${regs.registerB}}, $FRAME->localMenuRadius()F")
                    m.addInstructions(index+1,"move-result v${regs.registerA}")
                }
            }
        }
        // All generated shadow/spotlight shapes use exactly [20dp, zero smoothing].
        frame.methods.single { it.name == "getSmoothRadiusAndWeight" }.addInstructions(0,"""
            const/4 v0, 0x2
            new-array v0, v0, [F
            invoke-direct {p0}, $FRAME->localMenuRadius()F
            move-result v1
            const/4 v2, 0x0
            aput v1, v0, v2
            return-object v0
        """.trimIndent())
        frame.methods.single { it.name == "c" && it.returnType == "Landroid/graphics/Path;" }.addInstructions(0,"""
            iget-object v0, p0, $FRAME->g:Landroid/graphics/Path;
            invoke-virtual {v0}, Landroid/graphics/Path;->reset()V
            iget-object v1, p0, $FRAME->i:Landroid/graphics/RectF;
            invoke-direct {p0}, $FRAME->localMenuRadius()F
            move-result v2
            sget-object v3, Landroid/graphics/Path${'$'}Direction;->CW:Landroid/graphics/Path${'$'}Direction;
            invoke-virtual {v0, v1, v2, v2, v3}, Landroid/graphics/Path;->addRoundRect(Landroid/graphics/RectF;FFLandroid/graphics/Path${'$'}Direction;)V
            return-object v0
        """.trimIndent())
        val blur = mutableClassDefBy("Lth/a;").methods.single { it.name == "b" && it.returnType == "Lcom/oplus/graphics/OplusBlurParam;" }
        val ret = blur.implementation!!.instructions.indexOfLast { it.opcode == Opcode.RETURN_OBJECT }
        check(ret >= 0)
        blur.addInstructions(ret,"""
            iget-object v1, p0, Lth/a;->c:Landroid/view/View;
            instance-of v1, v1, $FRAME
            if-eqz v1, :local_keep_smoothing
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Lcom/oplus/graphics/OplusBlurParam;->setSmoothCornerWeight(F)V
            invoke-virtual {v0, v1}, Lcom/oplus/graphics/OplusBlurParam;->setSmoothCornerType(I)V
            :local_keep_smoothing
            nop
        """.trimIndent())
    }
}
