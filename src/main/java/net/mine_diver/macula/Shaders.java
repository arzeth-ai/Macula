// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package net.mine_diver.macula;

import net.mine_diver.macula.util.MinecraftInstance;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.GL_MAX_DRAW_BUFFERS;
import static org.lwjgl.opengl.GL20.glDrawBuffers;

public class Shaders {

    public static final Minecraft MINECRAFT = MinecraftInstance.get();

    public static boolean isInitialized = false;

    public static int renderWidth = 0;
    public static int renderHeight = 0;

    private static final float[] clearColor = new float[3];

    public static float rainStrength = 0.0f;

    public static boolean fogEnabled = true;

    public static int entityAttrib = -1;

    static {
        if (!ShaderConfig.configDir.exists())
            if (!ShaderConfig.configDir.mkdirs())
                throw new RuntimeException();
        ShaderConfig.loadConfig();
    }

    public static void init() {
        if (!(ShaderPack.shaderPackLoaded = !ShaderPack.currentShaderName.equals(ShaderPack.SHADER_DISABLED))) return;

        MatrixBuffer.initMatrixBuffer();

        int maxDrawBuffers = glGetInteger(GL_MAX_DRAW_BUFFERS);

        System.out.println("GL_MAX_DRAW_BUFFERS = " + maxDrawBuffers);

        ColorBuffer.colorAttachments = 4;

        ShaderProgram.initializeShaders();

        if (ColorBuffer.colorAttachments > maxDrawBuffers) System.out.println("Not enough draw buffers!");

        ShaderProgram.resolveFallbacks();

        ColorBuffer.defaultDrawBuffers = BufferUtils.createIntBuffer(ColorBuffer.colorAttachments);
        for (int i = 0; i < ColorBuffer.colorAttachments; ++i) ColorBuffer.defaultDrawBuffers.put(i, GL_COLOR_ATTACHMENT0_EXT + i);

        ColorBuffer.defaultTextures = BufferUtils.createIntBuffer(ColorBuffer.colorAttachments);
        ColorBuffer.defaultRenderBuffers = BufferUtils.createIntBuffer(ColorBuffer.colorAttachments);

        resize();
        ShadowBuffer.setupShadowFrameBuffer();
        isInitialized = true;
    }

    public static void setClearColor(float red, float green, float blue) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;

        if (ShadowBuffer.isShadowPass) {
            GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);
            return;
        }

        glDrawBuffers(ColorBuffer.defaultDrawBuffers);
        GLUtils.glClearBuffer(0f, 0f, 0f, 0f);

        glDrawBuffers(GL_COLOR_ATTACHMENT0_EXT);
        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        glDrawBuffers(GL_COLOR_ATTACHMENT1_EXT);
        GLUtils.glClearBuffer(1f, 1f, 1f, 1f);

        glDrawBuffers(ColorBuffer.defaultDrawBuffers);
    }

    public static void beginRender(Minecraft minecraft, float f, long l) {
        rainStrength = minecraft.level.getRainGradient(f);

        if (ShadowBuffer.isShadowPass) return;

        if (!isInitialized) init();
        if (!ShaderPack.shaderPackLoaded) return;
        if (MINECRAFT.actualWidth != renderWidth || MINECRAFT.actualHeight != renderHeight)
            resize();

        if (ShadowBuffer.shadowEnabled) {
            // do shadow pass
            boolean preShadowPassThirdPersonView = MINECRAFT.options.thirdPerson;
            MINECRAFT.options.thirdPerson = true;

            ShadowBuffer.isShadowPass = true;

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, ShadowBuffer.shadowFramebuffer);
            ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
            MINECRAFT.gameRenderer.delta(f, l);
            glFlush();

            ShadowBuffer.isShadowPass = false;
            MINECRAFT.options.thirdPerson = preShadowPassThirdPersonView;
        }

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, ColorBuffer.defaultFramebuffer);

        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    private static void bindTextures() {
        for (byte i = 0; i < ColorBuffer.colorAttachments; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, ColorBuffer.defaultTextures.get(i));
        }

        if (ShadowBuffer.shadowEnabled) {
            glActiveTexture(GL_TEXTURE7);
            glBindTexture(GL_TEXTURE_2D, ShadowBuffer.shadowDepthTexture);
        }

        glActiveTexture(GL_TEXTURE0);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }


    public static void endRender() {
        if (ShadowBuffer.isShadowPass) return;

        glPushMatrix();

        GLUtils.glSetupOrthographicProjection(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        // composite

        glDisable(GL_BLEND);

        ShaderProgram.useShaderProgram(ShaderProgramType.COMPOSITE);

        glDrawBuffers(ColorBuffer.defaultDrawBuffers);

        bindTextures();
        GLUtils.glDrawQuad();

        // final

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

        ShaderProgram.useShaderProgram(ShaderProgramType.FINAL);

        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        bindTextures();
        GLUtils.glDrawQuad();

        glEnable(GL_BLEND);

        glPopMatrix();
        ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
    }

    private static void bindTerrainTextures() {
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_nh.png"));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_s.png"));
        glActiveTexture(GL_TEXTURE0);
    }

    public static void beginTerrain() {
        ShaderProgram.useShaderProgram(ShaderProgramType.TERRAIN);
        bindTerrainTextures();
    }

    public static void endTerrain() {
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    public static void beginWater() {
        ShaderProgram.useShaderProgram(ShaderProgramType.WATER);
        bindTerrainTextures();
    }

    public static void endWater() {
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    public static void beginHand() {
        glEnable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.HAND);
    }

    public static void endHand() {
        glDisable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);

        if (ShadowBuffer.isShadowPass)
            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, ShadowBuffer.shadowFramebuffer); // was set to 0 in beginWeather()
    }

    public static void beginWeather() {
        glEnable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.WEATHER);

        if (ShadowBuffer.isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0); // will be set to sbf in endHand()
    }

    public static void endWeather() {
        glDisable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    private static void resize() {
        renderWidth = MINECRAFT.actualWidth;
        renderHeight = MINECRAFT.actualHeight;
        ColorBuffer.setupRenderTextures();
        ColorBuffer.setupFrameBuffer();
    }

}
