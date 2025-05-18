// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package net.mine_diver.macula.shader;

import net.mine_diver.macula.ShaderPack;
import net.mine_diver.macula.option.ShaderConfig;
import net.mine_diver.macula.shader.program.ShaderProgram;
import net.mine_diver.macula.shader.program.ShaderProgramType;
import net.mine_diver.macula.util.GLUtils;
import net.mine_diver.macula.util.MinecraftInstance;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public class ShaderCore {

    public static final Minecraft MINECRAFT = MinecraftInstance.get();

    public static boolean isInitialized = false;

    public static int renderWidth = 0;
    public static int renderHeight = 0;
    public static float aspectRatio = 0;

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

        int maxDrawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);

        System.out.println("GL_MAX_DRAW_BUFFERS = " + maxDrawBuffers);

        Framebuffer.colorAttachments = 4;

        ShaderProgram.initializeShaders();

        if (Framebuffer.colorAttachments > maxDrawBuffers) System.out.println("Not enough draw buffers!");

        ShaderProgram.resolveFallbacks();

        Framebuffer.defaultDrawBuffers = BufferUtils.createIntBuffer(Framebuffer.colorAttachments);
        for (int i = 0; i < Framebuffer.colorAttachments; ++i)
            Framebuffer.defaultDrawBuffers.put(i, EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + i);

        Framebuffer.defaultTextures = BufferUtils.createIntBuffer(Framebuffer.colorAttachments);
        Framebuffer.defaultRenderBuffers = BufferUtils.createIntBuffer(Framebuffer.colorAttachments);

        resize();
        ShadowMap.initializeShadowMap();
        isInitialized = true;
    }

    public static void setClearColor(float red, float green, float blue) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;

        if (ShadowMap.isShadowPass) {
            GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);
            return;
        }

        GL20.glDrawBuffers(Framebuffer.defaultDrawBuffers);
        GLUtils.glClearBuffer(0f, 0f, 0f, 0f);

        GL20.glDrawBuffers(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        GL20.glDrawBuffers(EXTFramebufferObject.GL_COLOR_ATTACHMENT1_EXT);
        GLUtils.glClearBuffer(1f, 1f, 1f, 1f);

        GL20.glDrawBuffers(Framebuffer.defaultDrawBuffers);
    }

    private static void resize() {
        renderWidth = MINECRAFT.actualWidth;
        renderHeight = MINECRAFT.actualHeight;

        aspectRatio = (float) renderWidth / (float) renderHeight;

        Framebuffer.setupRenderTextures();
        Framebuffer.setupFrameBuffer();
    }

    public static void beginRender(Minecraft minecraft, float f, long l) {
        rainStrength = minecraft.level.getRainGradient(f);

        if (ShadowMap.isShadowPass) return;

        if (!isInitialized) init();
        if (!ShaderPack.shaderPackLoaded) return;
        if (MINECRAFT.actualWidth != renderWidth || MINECRAFT.actualHeight != renderHeight)
            resize();

        if (ShadowMap.shadowEnabled) {
            // do shadow pass
            boolean preShadowPassThirdPersonView = MINECRAFT.options.thirdPerson;
            MINECRAFT.options.thirdPerson = true;

            ShadowMap.isShadowPass = true;

            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    ShadowMap.shadowFramebufferId);
            ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
            MINECRAFT.gameRenderer.delta(f, l);
            GL11.glFlush();

            ShadowMap.isShadowPass = false;
            MINECRAFT.options.thirdPerson = preShadowPassThirdPersonView;
        }

        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                Framebuffer.defaultFramebufferId);

        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    public static void endRender() {
        if (ShadowMap.isShadowPass) return;

        GL11.glPushMatrix();

        GLUtils.glSetupOrthographicProjection(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // composite

        GL11.glDisable(GL11.GL_BLEND);

        ShaderProgram.useShaderProgram(ShaderProgramType.COMPOSITE);

        GL20.glDrawBuffers(Framebuffer.defaultDrawBuffers);

        bindTextures();
        GLUtils.glDrawQuad();

        // final

        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

        ShaderProgram.useShaderProgram(ShaderProgramType.FINAL);

        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        bindTextures();
        GLUtils.glDrawQuad();

        GL11.glEnable(GL11.GL_BLEND);

        GL11.glPopMatrix();
        ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
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
        GL11.glEnable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.HAND);
    }

    public static void endHand() {
        GL11.glDisable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);

        if (ShadowMap.isShadowPass)
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    ShadowMap.shadowFramebufferId); // was set to 0 in beginWeather()
    }

    public static void beginWeather() {
        GL11.glEnable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.WEATHER);

        if (ShadowMap.isShadowPass)
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    0); // will be set to sbf in endHand()
    }

    public static void endWeather() {
        GL11.glDisable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    private static void bindTextures() {
        for (byte i = 0; i < Framebuffer.colorAttachments; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, Framebuffer.defaultTextures.get(i));
        }

        if (ShadowMap.shadowEnabled) {
            GL13.glActiveTexture(GL13.GL_TEXTURE7);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShadowMap.shadowDepthTextureId);
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void bindTerrainTextures() {
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_nh.png"));
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_s.png"));
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

}
