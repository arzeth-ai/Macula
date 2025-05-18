package net.mine_diver.macula.shader.program;

import net.mine_diver.macula.util.GLUtils;
import net.mine_diver.macula.shader.uniform.MatrixUniforms;
import net.mine_diver.macula.shader.uniform.PositionUniforms;
import net.mine_diver.macula.shader.ShaderCore;
import net.mine_diver.macula.util.UniformUtils;
import net.mine_diver.macula.shader.ShadowMap;
import net.mine_diver.macula.shader.compiler.ShaderCompiler;
import net.minecraft.block.BlockBase;
import net.minecraft.item.ItemInstance;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;
import java.util.Map;

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
        int programId = GL20.glCreateProgram();

        if (programId == NO_PROGRAM_ID) return NO_PROGRAM_ID;

        int vertShaderId = ShaderCompiler.createVertShader(vertShaderPath);
        int fragShaderId = ShaderCompiler.createFragShader(fragShaderPath);

        if (vertShaderId != NO_PROGRAM_ID || fragShaderId != NO_PROGRAM_ID) {
            if (vertShaderId != NO_PROGRAM_ID) GL20.glAttachShader(programId, vertShaderId);
            if (fragShaderId != NO_PROGRAM_ID) GL20.glAttachShader(programId, fragShaderId);
            if (ShaderCore.entityAttrib >= 0)
                GL20.glBindAttribLocation(programId, ShaderCore.entityAttrib, "mc_Entity");
            GL20.glLinkProgram(programId);
            GL20.glValidateProgram(programId);
            GLUtils.printLogInfo(programId);
        } else {
            GL20.glDeleteProgram(programId);
            return NO_PROGRAM_ID;
        }

        return programId;
    }

    public static void useShaderProgram(ShaderProgramType shaderProgramType) {
        if (activeShaderProgram == shaderProgramType) return;

        if (ShadowMap.isShadowPass) {
            activeShaderProgram = ShaderProgramType.NONE;
            GL20.glUseProgram(shaderProgramId.get(ShaderProgramType.NONE));
            return;
        }

        activeShaderProgram = shaderProgramType;
        GL20.glUseProgram(shaderProgramId.get(shaderProgramType));

        if (shaderProgramId.get(shaderProgramType) == NO_PROGRAM_ID) return;

        int programId = ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram);
        switch (shaderProgramType) {
            case TEXTURED:
                UniformUtils.setProgramUniform1i(programId, Uniform.TEXTURE, 0);
                break;
            case TEXTURED_LIT:
            case HAND:
            case WEATHER:
                UniformUtils.setProgramUniform1i(programId, Uniform.TEXTURE, 0);
                UniformUtils.setProgramUniform1i(programId, Uniform.LIGHTMAP, 1);
                break;
            case TERRAIN:
            case WATER:
                UniformUtils.setProgramUniform1i(programId, Uniform.TEXTURE, 0);
                UniformUtils.setProgramUniform1i(programId, Uniform.LIGHTMAP, 1);
                UniformUtils.setProgramUniform1i(programId, Uniform.NORMALS, 2);
                UniformUtils.setProgramUniform1i(programId, Uniform.SPECULAR, 3);
                break;
            case COMPOSITE:
            case FINAL:
                UniformUtils.setProgramUniform1i(programId, Uniform.GCOLOR, 0);
                UniformUtils.setProgramUniform1i(programId, Uniform.GDEPTH, 1);
                UniformUtils.setProgramUniform1i(programId, Uniform.GNORMAL, 2);
                UniformUtils.setProgramUniform1i(programId, Uniform.COMPOSITE, 3);
                UniformUtils.setProgramUniform1i(programId, Uniform.GAUX1, 4);
                UniformUtils.setProgramUniform1i(programId, Uniform.GAUX2, 5);
                UniformUtils.setProgramUniform1i(programId, Uniform.GAUX3, 6);
                UniformUtils.setProgramUniform1i(programId, Uniform.SHADOW, 7);

                UniformUtils.setProgramUniformMatrix4(programId, Uniform.GB_PREVIOUS_PROJECTION, MatrixUniforms.previousProjection);
                UniformUtils.setProgramUniformMatrix4(programId, Uniform.GB_PROJECTION, MatrixUniforms.projection);
                UniformUtils.setProgramUniformMatrix4(programId, Uniform.GB_PROJECTION_INVERSE, MatrixUniforms.projectionInverse);

                UniformUtils.setProgramUniformMatrix4(programId, Uniform.GB_PREVIOUS_MODELVIEW, MatrixUniforms.previousModelView);

                if (ShadowMap.shadowEnabled) {
                    UniformUtils.setProgramUniformMatrix4(programId, Uniform.SHADOW_PROJECTION, MatrixUniforms.shadowProjection);
                    UniformUtils.setProgramUniformMatrix4(programId, Uniform.SHADOW_PROJECTION_INVERSE, MatrixUniforms.shadowProjectionInverse);

                    UniformUtils.setProgramUniformMatrix4(programId, Uniform.SHADOW_MODELVIEW, MatrixUniforms.shadowModelView);
                    UniformUtils.setProgramUniformMatrix4(programId, Uniform.SHADOW_MODELVIEW_INVERSE, MatrixUniforms.shadowModelViewInverse);
                }
                break;
        }

        ItemInstance stack = ShaderCore.MINECRAFT.player.inventory.getHeldItem();
        UniformUtils.setProgramUniform1i(programId, Uniform.HELD_ITEM_ID, stack == null ? -1 : stack.itemId);
        UniformUtils.setProgramUniform1i(programId, Uniform.HELD_BLOCK_LIGHT_VALUE,
                stack == null || stack.itemId >= BlockBase.BY_ID.length ? 0 : BlockBase.EMITTANCE[stack.itemId]);

        UniformUtils.setProgramUniform1i(programId, Uniform.FOG_MODE, ShaderCore.fogEnabled ? GL11.glGetInteger(GL11.GL_FOG_MODE) : 0);
        UniformUtils.setProgramUniform1f(programId, Uniform.RAIN_STRENGTH, ShaderCore.rainStrength);

        UniformUtils.setProgramUniform1i(programId, Uniform.WORLD_TIME, (int) (ShaderCore.MINECRAFT.level.getLevelTime() % 24000));

        UniformUtils.setProgramUniform1f(programId, Uniform.ASPECT_RATIO, ShaderCore.aspectRatio);
        UniformUtils.setProgramUniform1f(programId, Uniform.VIEW_WIDTH, (float) ShaderCore.renderWidth);
        UniformUtils.setProgramUniform1f(programId, Uniform.VIEW_HEIGHT, (float) ShaderCore.renderHeight);

        UniformUtils.setProgramUniform1f(programId, Uniform.NEAR, 0.05F);
        UniformUtils.setProgramUniform1f(programId, Uniform.FAR, 256 >> ShaderCore.MINECRAFT.options.viewDistance);

        UniformUtils.setProgramUniform3f(programId, Uniform.SUN_POSITION, PositionUniforms.sunPosition);
        UniformUtils.setProgramUniform3f(programId, Uniform.MOON_POSITION, PositionUniforms.moonPosition);

        UniformUtils.setProgramUniform3f(programId, Uniform.PREVIOUS_CAMERA_POSITION, PositionUniforms.previousCameraPosition);
        UniformUtils.setProgramUniform3f(programId, Uniform.CAMERA_POSITION, PositionUniforms.cameraPosition);

        UniformUtils.setProgramUniformMatrix4(programId, Uniform.GB_MODELVIEW, MatrixUniforms.modelView);
        UniformUtils.setProgramUniformMatrix4(programId, Uniform.GB_MODELVIEW_INVERSE, MatrixUniforms.modelViewInverse);
    }

    public static void deleteShaders() {
        for (Map.Entry<ShaderProgramType, Integer> shaderEntry : shaderProgramId.entrySet()) {
            int programId = shaderEntry.getValue();
            if (programId != NO_PROGRAM_ID) {
                GL20.glDeleteProgram(programId);
                shaderEntry.setValue(NO_PROGRAM_ID);
            }
        }
    }
}