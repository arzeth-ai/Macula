package net.mine_diver.macula.mixin;

import net.mine_diver.macula.ShaderPack;
import net.mine_diver.macula.shader.ShaderCore;
import net.mine_diver.macula.util.TessellatorAccessor;
import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.world.chunk.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashSet;

@Mixin(ChunkBuilder.class)
public class ChunkBuilderMixin {
    @Inject(
            method = "rebuild()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/block/BlockRenderManager;render(Lnet/minecraft/block/Block;III)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onRenderBlockByRenderType(CallbackInfo ci, int var1 , int var2, int  var3, int  var4, int  var5, int  var6, HashSet var7, int  var8, ChunkCache var9, BlockRenderManager  var10, int  var11, int  var12, int  var13, int  var14, int  var15, int  var16, int  var17, int  var18, Block  var19) {
        if (!ShaderPack.shaderPackLoaded) return;
        if (ShaderCore.entityAttrib >= 0)
            ((TessellatorAccessor) Tessellator.INSTANCE).setEntity(var19.id);
    }

    @Inject(method = "rebuild()V", at = @At(value = "RETURN"))
    private void onUpdateRenderer(CallbackInfo ci) {
        if (!ShaderPack.shaderPackLoaded) return;
        if (ShaderCore.entityAttrib >= 0)
            ((TessellatorAccessor) Tessellator.INSTANCE).setEntity(-1);
    }
}
