package net.mine_diver.macula.mixin;

import net.mine_diver.macula.GLUtils;
import net.mine_diver.macula.ShaderPack;
import net.mine_diver.macula.PositionBuffer;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(
            method = "renderSky(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/level/Level;method_286(F)F",
                    shift = At.Shift.AFTER
            )
    )
    private void onGetStarBrightness(float par1, CallbackInfo ci) {
        if (!ShaderPack.shaderPackLoaded) return;
        PositionBuffer.updateCelestialPosition();
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"
            )
    )
    private void onGlEnable(int i) {
        if (!ShaderPack.shaderPackLoaded) {
            GL11.glEnable(i);
            return;
        }
        GLUtils.glEnableWrapper(i);
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"
            )
    )
    private void onGlDisable(int i) {
        if (!ShaderPack.shaderPackLoaded) {
            GL11.glDisable(i);
            return;
        }
        GLUtils.glDisableWrapper(i);
    }
}
