// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package net.mine_diver.macula;

import net.mine_diver.macula.util.MatrixUtil;
import net.mine_diver.macula.util.MinecraftInstance;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBTextureFloat.GL_RGB32F_ARB;
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

    // Shadow stuff

    // configuration
    public static boolean shadowEnabled = false;
    public static int shadowResolution = 1024;
    public static float shadowMapHalfPlane = 30.0f;
    public static final float NEAR = 0.05f;
    public static final  float FAR = 256.0f;

    public static boolean isShadowPass = false;

    private static int shadowFramebuffer = 0;
    private static int shadowDepthTexture = 0;
    private static int shadowDepthBuffer = 0;

    // Color attachment stuff

    public static int colorAttachments = 0;

    private static IntBuffer defaultDrawBuffers = null;

    private static IntBuffer defaultTextures = null;
    private static IntBuffer defaultRenderBuffers = null;

    private static int defaultFramebuffer = 0;
    private static int defaultDepthBuffer = 0;

    private static final int DEPTH_ATTACHMENT_INDEX = 1;

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

        colorAttachments = 4;

        ShaderProgram.initializeShaders();

        if (colorAttachments > maxDrawBuffers) System.out.println("Not enough draw buffers!");

        ShaderProgram.resolveFallbacks();

        defaultDrawBuffers = BufferUtils.createIntBuffer(colorAttachments);
        for (int i = 0; i < colorAttachments; ++i) defaultDrawBuffers.put(i, GL_COLOR_ATTACHMENT0_EXT + i);

        defaultTextures = BufferUtils.createIntBuffer(colorAttachments);
        defaultRenderBuffers = BufferUtils.createIntBuffer(colorAttachments);

        resize();
        setupShadowFrameBuffer();
        isInitialized = true;
    }

    public static void setClearColor(float red, float green, float blue) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;

        if (isShadowPass) {
            GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);
            return;
        }

        glDrawBuffers(defaultDrawBuffers);
        GLUtils.glClearBuffer(0f, 0f, 0f, 0f);

        glDrawBuffers(GL_COLOR_ATTACHMENT0_EXT);
        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        glDrawBuffers(GL_COLOR_ATTACHMENT1_EXT);
        GLUtils.glClearBuffer(1f, 1f, 1f, 1f);

        glDrawBuffers(defaultDrawBuffers);
    }

    public static void setupShadowViewport(float f, float x, float y, float z) {
        glViewport(0, 0, shadowResolution, shadowResolution);

        setupOrthographicProjection(-shadowMapHalfPlane, shadowMapHalfPlane, -shadowMapHalfPlane, shadowMapHalfPlane,
                NEAR,
                FAR);

        glTranslatef(0.0f, 0.0f, -100.0f);
        glRotatef(90.0f, 0.0f, 0.0f, -1.0f);
        float angle = MINECRAFT.level.method_198(f) * 360.0f;
        // night time
        // day time
        if (angle < 90.0 || angle > 270.0) glRotatef(angle - 90.0f, -1.0f, 0.0f, 0.0f);
        else glRotatef(angle + 90.0f, -1.0f, 0.0f, 0.0f);

        // reduces jitter
        glTranslatef(x % 10.0f - 5.0f, y % 10.0f - 5.0f, z % 10.0f - 5.0f);
    }

    private static void setupOrthographicProjection(float left, float right, float bottom, float top, float near, float far) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        glOrtho(left, right, bottom, top, near, far);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    public static void setupShadowMatrix() {
        MatrixBuffer.getMatrixBuffer(GL_PROJECTION_MATRIX, MatrixBuffer.shadowProjection);
        MatrixUtil.invertMat4(MatrixBuffer.shadowProjection, MatrixBuffer.shadowProjectionInverse);

        MatrixBuffer.getMatrixBuffer(GL_MODELVIEW_MATRIX, MatrixBuffer.shadowModelView);
        MatrixUtil.invertMat4(MatrixBuffer.shadowModelView, MatrixBuffer.shadowModelViewInverse);
    }

    public static void beginRender(Minecraft minecraft, float f, long l) {
        rainStrength = minecraft.level.getRainGradient(f);

        if (isShadowPass) return;

        if (!isInitialized) init();
        if (!ShaderPack.shaderPackLoaded) return;
        if (MINECRAFT.actualWidth != renderWidth || MINECRAFT.actualHeight != renderHeight)
            resize();

        if (shadowEnabled) {
            // do shadow pass
            boolean preShadowPassThirdPersonView = MINECRAFT.options.thirdPerson;
            MINECRAFT.options.thirdPerson = true;

            isShadowPass = true;

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, shadowFramebuffer);
            ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
            MINECRAFT.gameRenderer.delta(f, l);
            glFlush();

            isShadowPass = false;
            MINECRAFT.options.thirdPerson = preShadowPassThirdPersonView;
        }

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, defaultFramebuffer);

        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    private static void bindTextures() {
        for (byte i = 0; i < colorAttachments; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, defaultTextures.get(i));
        }

        if (shadowEnabled) {
            glActiveTexture(GL_TEXTURE7);
            glBindTexture(GL_TEXTURE_2D, shadowDepthTexture);
        }

        glActiveTexture(GL_TEXTURE0);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }


    public static void endRender() {
        if (isShadowPass) return;

        glPushMatrix();

        setupOrthographicProjection(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        // composite

        glDisable(GL_BLEND);

        ShaderProgram.useShaderProgram(ShaderProgramType.COMPOSITE);

        glDrawBuffers(defaultDrawBuffers);

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

        if (isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, shadowFramebuffer); // was set to 0 in beginWeather()
    }

    public static void beginWeather() {
        glEnable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.WEATHER);

        if (isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0); // will be set to sbf in endHand()
    }

    public static void endWeather() {
        glDisable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    private static void resize() {
        renderWidth = MINECRAFT.actualWidth;
        renderHeight = MINECRAFT.actualHeight;
        setupFrameBuffer();
    }

    private static int createDepthBuffer(int width, int height) {
        int depthBuffer = glGenRenderbuffersEXT();
        glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, depthBuffer);
        glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH_COMPONENT, width, height);
        return depthBuffer;
    }

    private static void setupFrameBuffer() {
        setupRenderTextures();

        if (defaultFramebuffer != 0) {
            glDeleteFramebuffersEXT(defaultFramebuffer);
            glDeleteRenderbuffersEXT(defaultRenderBuffers);
        }

        defaultFramebuffer = glGenFramebuffersEXT();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, defaultFramebuffer);

        glGenRenderbuffersEXT(defaultRenderBuffers);

        for (int i = 0; i < colorAttachments; ++i) {
            glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, defaultRenderBuffers.get(i));
            // Depth buffer
            if (i == DEPTH_ATTACHMENT_INDEX) glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_RGB32F_ARB, renderWidth, renderHeight);
            else glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_RGBA, renderWidth, renderHeight);
            glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, defaultDrawBuffers.get(i), GL_RENDERBUFFER_EXT,
                    defaultRenderBuffers.get(i));
            glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, defaultDrawBuffers.get(i), GL_TEXTURE_2D, defaultTextures.get(i), 0);
        }

        if (defaultDepthBuffer != 0) glDeleteRenderbuffersEXT(defaultDepthBuffer);
        defaultDepthBuffer = createDepthBuffer(renderWidth, renderHeight);

        glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT,
                defaultDepthBuffer);

        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        if (status != GL_FRAMEBUFFER_COMPLETE_EXT)
            System.err.println("Failed creating framebuffer! (Status " + status + ")");
    }

    private static void setupShadowFrameBuffer() {
        if (!shadowEnabled) return;

        setupShadowRenderTexture();

        if (shadowFramebuffer != 0) glDeleteFramebuffersEXT(shadowFramebuffer);

        shadowFramebuffer = glGenFramebuffersEXT();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, shadowFramebuffer);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (shadowDepthBuffer != 0) glDeleteRenderbuffersEXT(shadowDepthBuffer);
        shadowDepthBuffer = createDepthBuffer(shadowResolution, shadowResolution);

        glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT,
                shadowDepthBuffer);

        glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_TEXTURE_2D, shadowDepthTexture, 0);

        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        if (status != GL_FRAMEBUFFER_COMPLETE_EXT)
            System.err.println("Failed creating shadow framebuffer! (Status " + status + ")");
    }

    private static void setupRenderTextures() {
        glDeleteTextures(defaultTextures);
        glGenTextures(defaultTextures);

        for (int i = 0; i < colorAttachments; ++i) {
            glBindTexture(GL_TEXTURE_2D, defaultTextures.get(i));
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            if (i == DEPTH_ATTACHMENT_INDEX) { // depth buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4 * 4);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F_ARB, renderWidth, renderHeight, 0, GL_RGBA, GL11.GL_FLOAT,
                        buffer);
            } else {
                ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, renderWidth, renderHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                        buffer);
            }
        }
    }

    private static void setupShadowRenderTexture() {
        if (!shadowEnabled) return;

        glDeleteTextures(shadowDepthTexture);
        shadowDepthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, shadowDepthTexture);

        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        ByteBuffer buffer = ByteBuffer.allocateDirect(shadowResolution * shadowResolution * 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowResolution, shadowResolution, 0, GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT, buffer);
    }
}
