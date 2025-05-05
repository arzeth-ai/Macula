package net.mine_diver.macula;

import net.minecraft.block.BlockBase;
import net.minecraft.item.ItemInstance;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;

public class ShaderProgram {
    public enum ShaderProgramType {
        NONE("", null),
        BASIC("gbuffers_basic", NONE),
        TEXTURED("gbuffers_textured", BASIC),
        TEXTURED_LIT("gbuffers_textured_lit", TEXTURED),
        TERRAIN("gbuffers_terrain", TEXTURED_LIT),
        WATER("gbuffers_water", TERRAIN),
        HAND("gbuffers_hand", TEXTURED_LIT),
        WEATHER("gbuffers_weather", TEXTURED_LIT),
        COMPOSITE("composite", NONE),
        FINAL("final", NONE);

        public final String fileName;
        public final ShaderProgramType fallback;

        ShaderProgramType(String fileName, ShaderProgramType fallback) {
            this.fileName = fileName;
            this.fallback = fallback;
        }
    }

    public static final EnumMap<ShaderProgramType, Integer> shaderProgramId = new EnumMap<>(ShaderProgramType.class);
    public static ShaderProgramType activeShaderProgram = ShaderProgramType.NONE;;

    public static void initializeShaders() {
        shaderProgramId.put(ShaderProgramType.NONE, 0);

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
            while (shaderProgramId.get(current) == 0) {
                if (current.fallback == null || current == current.fallback) break;
                current = current.fallback;
            }
            shaderProgramId.put(shaderProgramType, shaderProgramId.get(current));
        }
    }

    public static int createShaderProgram(String vertShaderPath, String fragShaderPath) {
        int programId = ARBShaderObjects.glCreateProgramObjectARB();

        if (programId == 0) return 0;

        int vertShaderId = Shaders.createVertShader(vertShaderPath);
        int fragShaderId = Shaders.createFragShader(fragShaderPath);

        if (vertShaderId != 0 || fragShaderId != 0) {
            if (vertShaderId != 0) ARBShaderObjects.glAttachObjectARB(programId, vertShaderId);
            if (fragShaderId != 0) ARBShaderObjects.glAttachObjectARB(programId, fragShaderId);
            if (Shaders.entityAttrib >= 0)
                ARBVertexShader.glBindAttribLocationARB(programId, Shaders.entityAttrib, "mc_Entity");
            ARBShaderObjects.glLinkProgramARB(programId);
            ARBShaderObjects.glValidateProgramARB(programId);
            GLUtils.printLogInfo(programId);
        } else {
            ARBShaderObjects.glDeleteObjectARB(programId);
            return 0;
        }

        return programId;
    }

    public static void useShaderProgram(ShaderProgramType shaderProgramType) {
        if (activeShaderProgram == shaderProgramType) return;
        if (Shaders.isShadowPass) {
            activeShaderProgram = ShaderProgramType.NONE;
            ARBShaderObjects.glUseProgramObjectARB(shaderProgramId.get(ShaderProgramType.NONE));
            return;
        }
        activeShaderProgram = shaderProgramType;
        ARBShaderObjects.glUseProgramObjectARB(shaderProgramId.get(shaderProgramType));
        if (shaderProgramId.get(shaderProgramType) == 0) return;

        switch (shaderProgramType) {
            case TEXTURED:
                Shaders.setProgramUniform1i("texture", 0);
                break;
            case TEXTURED_LIT:
            case HAND:
            case WEATHER:
                Shaders.setProgramUniform1i("texture", 0);
                Shaders.setProgramUniform1i("lightmap", 1);
                break;
            case TERRAIN:
            case WATER:
                Shaders.setProgramUniform1i("texture", 0);
                Shaders.setProgramUniform1i("lightmap", 1);
                Shaders.setProgramUniform1i("normals", 2);
                Shaders.setProgramUniform1i("specular", 3);
                break;
            case COMPOSITE:
            case FINAL:
                Shaders.setProgramUniform1i("gcolor", 0);
                Shaders.setProgramUniform1i("gdepth", 1);
                Shaders.setProgramUniform1i("gnormal", 2);
                Shaders.setProgramUniform1i("composite", 3);
                Shaders.setProgramUniform1i("gaux1", 4);
                Shaders.setProgramUniform1i("gaux2", 5);
                Shaders.setProgramUniform1i("gaux3", 6);
                Shaders.setProgramUniform1i("shadow", 7);
                Shaders.setProgramUniformMatrix4("gbufferPreviousProjection", MatrixBuffer.previousProjection);
                Shaders.setProgramUniformMatrix4("gbufferProjection", MatrixBuffer.projection);
                Shaders.setProgramUniformMatrix4("gbufferProjectionInverse", MatrixBuffer.projectionInverse);
                Shaders.setProgramUniformMatrix4("gbufferPreviousModelView", MatrixBuffer.previousModelView);
                if (Shaders.shadowPassInterval > 0) {
                    Shaders.setProgramUniformMatrix4("shadowProjection", MatrixBuffer.shadowProjection);
                    Shaders.setProgramUniformMatrix4("shadowProjectionInverse", MatrixBuffer.shadowProjectionInverse);
                    Shaders.setProgramUniformMatrix4("shadowModelView", MatrixBuffer.shadowModelView);
                    Shaders.setProgramUniformMatrix4("shadowModelViewInverse", MatrixBuffer.shadowModelViewInverse);
                }
                break;
        }

        ItemInstance stack = Shaders.MINECRAFT.player.inventory.getHeldItem();
        Shaders.setProgramUniform1i("heldItemId", (stack == null ? -1 : stack.itemId));
        Shaders.setProgramUniform1i("heldBlockLightValue",
                (stack == null || stack.itemId >= BlockBase.BY_ID.length ? 0 : BlockBase.EMITTANCE[stack.itemId]));
        Shaders.setProgramUniform1i("fogMode", (Shaders.fogEnabled ? GL11.glGetInteger(GL11.GL_FOG_MODE) : 0));
        Shaders.setProgramUniform1f("rainStrength", Shaders.rainStrength);
        Shaders.setProgramUniform1i("worldTime", (int) (Shaders.MINECRAFT.level.getLevelTime() % 24000L));
        Shaders.setProgramUniform1f("aspectRatio", (float) Shaders.renderWidth / (float) Shaders.renderHeight);
        Shaders.setProgramUniform1f("viewWidth", (float) Shaders.renderWidth);
        Shaders.setProgramUniform1f("viewHeight", (float) Shaders.renderHeight);
        Shaders.setProgramUniform1f("near", 0.05F);
        Shaders.setProgramUniform1f("far", 256 >> Shaders.MINECRAFT.options.viewDistance);
        Shaders.setProgramUniform3f("sunPosition", VectorBuffer.sunPosition);
        Shaders.setProgramUniform3f("moonPosition", VectorBuffer.moonPosition);
        Shaders.setProgramUniform3f("previousCameraPosition", VectorBuffer.previousCameraPosition);
        Shaders.setProgramUniform3f("cameraPosition", VectorBuffer.cameraPosition);
        Shaders.setProgramUniformMatrix4("gbufferModelView", MatrixBuffer.modelView);
        Shaders.setProgramUniformMatrix4("gbufferModelViewInverse", MatrixBuffer.modelViewInverse);
    }
}