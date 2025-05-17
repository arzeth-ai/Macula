package net.mine_diver.macula;

import net.minecraft.block.BlockBase;
import net.minecraft.item.ItemInstance;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.opengl.ARBShaderObjects.glDeleteObjectARB;

public class ShaderProgram {
    public static final EnumMap<ShaderProgramType, Integer> shaderProgramId = new EnumMap<>(ShaderProgramType.class);
    public static ShaderProgramType activeShaderProgram = ShaderProgramType.NONE;

    public static final int NO_PROGRAM_ID = 0;

    public static void initializeShaders() {
        shaderProgramId.put(ShaderProgramType.NONE, NO_PROGRAM_ID);

        ShaderProgramType[] shaderProgramTypes = ShaderProgramType.values();
        int shaderProgramTypesLength = shaderProgramTypes.length;

        for (int i = 1; i < shaderProgramTypesLength; i++) {
            ShaderProgramType shaderProgramType = shaderProgramTypes[i];
            String shaderProgramName = shaderProgramType.fileName;
            shaderProgramId.put(shaderProgramType,
                    createShaderProgram(shaderProgramName + ".vsh", shaderProgramName + ".fsh"));
        }
    }

    public static void resolveFallbacks() {
        for (ShaderProgramType shaderProgramType : ShaderProgramType.values()) {
            ShaderProgramType current = shaderProgramType;
            while (shaderProgramId.get(current) == NO_PROGRAM_ID) {
                if (current.fallback == null || current == current.fallback) break;
                current = current.fallback;
            }
            shaderProgramId.put(shaderProgramType, shaderProgramId.get(current));
        }
    }

    private static int createShaderProgram(String vertShaderPath, String fragShaderPath) {
        int programId = ARBShaderObjects.glCreateProgramObjectARB();

        if (programId == NO_PROGRAM_ID) return NO_PROGRAM_ID;

        int vertShaderId = ShaderCompiler.createVertShader(vertShaderPath);
        int fragShaderId = ShaderCompiler.createFragShader(fragShaderPath);

        if (vertShaderId != NO_PROGRAM_ID || fragShaderId != NO_PROGRAM_ID) {
            if (vertShaderId != NO_PROGRAM_ID) ARBShaderObjects.glAttachObjectARB(programId, vertShaderId);
            if (fragShaderId != NO_PROGRAM_ID) ARBShaderObjects.glAttachObjectARB(programId, fragShaderId);
            if (ShaderCore.entityAttrib >= 0)
                ARBVertexShader.glBindAttribLocationARB(programId, ShaderCore.entityAttrib, "mc_Entity");
            ARBShaderObjects.glLinkProgramARB(programId);
            ARBShaderObjects.glValidateProgramARB(programId);
            GLUtils.printLogInfo(programId);
        } else {
            ARBShaderObjects.glDeleteObjectARB(programId);
            return NO_PROGRAM_ID;
        }

        return programId;
    }

    public static void useShaderProgram(ShaderProgramType shaderProgramType) {
        if (activeShaderProgram == shaderProgramType) return;
        if (ShadowMap.isShadowPass) {
            activeShaderProgram = ShaderProgramType.NONE;
            ARBShaderObjects.glUseProgramObjectARB(shaderProgramId.get(ShaderProgramType.NONE));
            return;
        }
        activeShaderProgram = shaderProgramType;
        ARBShaderObjects.glUseProgramObjectARB(shaderProgramId.get(shaderProgramType));
        if (shaderProgramId.get(shaderProgramType) == NO_PROGRAM_ID) return;

        int programId = ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram);
        switch (shaderProgramType) {
            case TEXTURED:
                ShaderUniform.setProgramUniform1i(programId, "texture", 0);
                break;
            case TEXTURED_LIT:
            case HAND:
            case WEATHER:
                ShaderUniform.setProgramUniform1i(programId, "texture", 0);
                ShaderUniform.setProgramUniform1i(programId, "lightmap", 1);
                break;
            case TERRAIN:
            case WATER:
                ShaderUniform.setProgramUniform1i(programId, "texture", 0);
                ShaderUniform.setProgramUniform1i(programId, "lightmap", 1);
                ShaderUniform.setProgramUniform1i(programId, "normals", 2);
                ShaderUniform.setProgramUniform1i(programId, "specular", 3);
                break;
            case COMPOSITE:
            case FINAL:
                ShaderUniform.setProgramUniform1i(programId, "gcolor", 0);
                ShaderUniform.setProgramUniform1i(programId, "gdepth", 1);
                ShaderUniform.setProgramUniform1i(programId, "gnormal", 2);
                ShaderUniform.setProgramUniform1i(programId, "composite", 3);
                ShaderUniform.setProgramUniform1i(programId, "gaux1", 4);
                ShaderUniform.setProgramUniform1i(programId, "gaux2", 5);
                ShaderUniform.setProgramUniform1i(programId, "gaux3", 6);
                ShaderUniform.setProgramUniform1i(programId, "shadow", 7);
                ShaderUniform.setProgramUniformMatrix4(programId, "gbufferPreviousProjection", MatrixBuffer.previousProjection);
                ShaderUniform.setProgramUniformMatrix4(programId, "gbufferProjection", MatrixBuffer.projection);
                ShaderUniform.setProgramUniformMatrix4(programId, "gbufferProjectionInverse", MatrixBuffer.projectionInverse);
                ShaderUniform.setProgramUniformMatrix4(programId, "gbufferPreviousModelView", MatrixBuffer.previousModelView);
                if (ShadowMap.shadowEnabled) {
                    ShaderUniform.setProgramUniformMatrix4(programId, "shadowProjection", MatrixBuffer.shadowProjection);
                    ShaderUniform.setProgramUniformMatrix4(programId, "shadowProjectionInverse", MatrixBuffer.shadowProjectionInverse);
                    ShaderUniform.setProgramUniformMatrix4(programId, "shadowModelView", MatrixBuffer.shadowModelView);
                    ShaderUniform.setProgramUniformMatrix4(programId, "shadowModelViewInverse", MatrixBuffer.shadowModelViewInverse);
                }
                break;
        }

        ItemInstance stack = ShaderCore.MINECRAFT.player.inventory.getHeldItem();
        ShaderUniform.setProgramUniform1i(programId, "heldItemId", (stack == null ? -1 : stack.itemId));
        ShaderUniform.setProgramUniform1i(programId, "heldBlockLightValue",
                (stack == null || stack.itemId >= BlockBase.BY_ID.length ? 0 : BlockBase.EMITTANCE[stack.itemId]));
        ShaderUniform.setProgramUniform1i(programId, "fogMode", (ShaderCore.fogEnabled ? GL11.glGetInteger(GL11.GL_FOG_MODE) : 0));
        ShaderUniform.setProgramUniform1f(programId, "rainStrength", ShaderCore.rainStrength);
        ShaderUniform.setProgramUniform1i(programId, "worldTime", (int) (ShaderCore.MINECRAFT.level.getLevelTime() % 24000L));
        ShaderUniform.setProgramUniform1f(programId, "aspectRatio", (float) ShaderCore.renderWidth / (float) ShaderCore.renderHeight);
        ShaderUniform.setProgramUniform1f(programId, "viewWidth", (float) ShaderCore.renderWidth);
        ShaderUniform.setProgramUniform1f(programId, "viewHeight", (float) ShaderCore.renderHeight);
        ShaderUniform.setProgramUniform1f(programId, "near", 0.05F);
        ShaderUniform.setProgramUniform1f(programId, "far", 256 >> ShaderCore.MINECRAFT.options.viewDistance);
        ShaderUniform.setProgramUniform3f(programId, "sunPosition", PositionBuffer.sunPosition);
        ShaderUniform.setProgramUniform3f(programId, "moonPosition", PositionBuffer.moonPosition);
        ShaderUniform.setProgramUniform3f(programId, "previousCameraPosition", PositionBuffer.previousCameraPosition);
        ShaderUniform.setProgramUniform3f(programId, "cameraPosition", PositionBuffer.cameraPosition);
        ShaderUniform.setProgramUniformMatrix4(programId, "gbufferModelView", MatrixBuffer.modelView);
        ShaderUniform.setProgramUniformMatrix4(programId, "gbufferModelViewInverse", MatrixBuffer.modelViewInverse);
    }

    public static void deleteShaders() {
        for (Map.Entry<ShaderProgramType, Integer> shaderEntry : shaderProgramId.entrySet()) {
            int programId = shaderEntry.getValue();
            if (programId != NO_PROGRAM_ID) {
                glDeleteObjectARB(programId);
                shaderEntry.setValue(NO_PROGRAM_ID);
            }
        }
    }
}