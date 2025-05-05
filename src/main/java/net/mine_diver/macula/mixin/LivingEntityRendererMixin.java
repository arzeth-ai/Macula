package net.mine_diver.macula.mixin;

import net.mine_diver.macula.GLUtils;
import net.mine_diver.macula.ShaderPack;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
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
