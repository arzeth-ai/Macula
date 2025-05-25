package net.mine_diver.macula.mixin;

import net.mine_diver.macula.ShaderPack;
import net.mine_diver.macula.shader.ShaderCore;
import net.mine_diver.macula.util.TessellatorAccessor;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.opengl.ARBVertexProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

@Mixin(Tessellator.class)
public class TessellatorMixin implements TessellatorAccessor {
    @Shadow private int mode;

    @Shadow private static boolean useTriangles;

    @Shadow private int addedVertexCount;

    @Shadow private boolean hasNormals;

    @Shadow private int[] buffer;

    @Shadow private int bufferPosition;

    @Inject(
            method = "<init>(I)V",
            at = @At("RETURN")
    )
    private void onCor(int var1, CallbackInfo ci) {
        shadersData = new short[] {-1, 0};
        shadersBuffer = GlAllocationUtils.allocateByteBuffer(var1 / 8 * 4);
        shadersShortBuffer = shadersBuffer.asShortBuffer();
    }

    @Inject(
            method = "draw()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glDrawArrays(III)V"
            )
    )
    private void onDraw1(CallbackInfo ci) {
        if (!ShaderPack.shaderPackLoaded) return;
        if (ShaderCore.entityAttrib >= 0) {
            ARBVertexProgram.glEnableVertexAttribArrayARB(ShaderCore.entityAttrib);
            ARBVertexProgram.glVertexAttribPointerARB(ShaderCore.entityAttrib, 2, false, false, 4, shadersShortBuffer.position(0));
        }
    }

    @Inject(
            method = "draw()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glDrawArrays(III)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onDraw2(CallbackInfo ci) {
        if (!ShaderPack.shaderPackLoaded) return;
        if (ShaderCore.entityAttrib >= 0)
            ARBVertexProgram.glDisableVertexAttribArrayARB(ShaderCore.entityAttrib);
    }

    @Inject(
            method = "reset()V",
            at = @At(value = "RETURN")
    )
    private void onReset(CallbackInfo ci) {
        if (!ShaderPack.shaderPackLoaded) return;
        shadersBuffer.clear();
    }

    @Inject(
            method = "vertex(DDD)V",
            at = @At(value = "HEAD")
    )
    private void onAddVertex(CallbackInfo ci) {
        if (!ShaderPack.shaderPackLoaded) return;
        if (mode == 7 && useTriangles && (addedVertexCount + 1) % 4 == 0 && hasNormals) {
            buffer[bufferPosition + 6] = buffer[(bufferPosition - 24) + 6];
            shadersBuffer.putShort(shadersData[0]).putShort(shadersData[1]);
            buffer[bufferPosition + 8 + 6] = buffer[(bufferPosition + 8 - 16) + 6];
            shadersBuffer.putShort(shadersData[0]).putShort(shadersData[1]);
        }
        shadersBuffer.putShort(shadersData[0]).putShort(shadersData[1]);
    }

    @Override
    @Unique
    public void setEntity(int id) {
        shadersData[0] = (short) id;
    }

    public ByteBuffer shadersBuffer;
    public ShortBuffer shadersShortBuffer;
    public short[] shadersData;
}
