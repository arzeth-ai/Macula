package net.mine_diver.macula;

import net.mine_diver.macula.util.MatrixUtil;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;

public class ShadowBuffer {
    // configuration
    public static boolean shadowEnabled = false;
    public static int shadowResolution = 1024;
    public static float shadowMapHalfPlane = 30.0f;
    public static final float NEAR = 0.05f;
    public static final float FAR = 256.0f;
    public static boolean isShadowPass = false;
    static int shadowFramebuffer = 0;
    static int shadowDepthTexture = 0;
    static int shadowDepthBuffer = 0;

    public static void setupShadowViewport(float f, float x, float y, float z) {
        GL11.glViewport(0, 0, shadowResolution, shadowResolution);

        GLUtils.glSetupOrthographicProjection(-shadowMapHalfPlane, shadowMapHalfPlane, -shadowMapHalfPlane,
                shadowMapHalfPlane,
                NEAR,
                FAR);

        GL11.glTranslatef(0.0f, 0.0f, -100.0f);
        GL11.glRotatef(90.0f, 0.0f, 0.0f, -1.0f);
        float angle = Shaders.MINECRAFT.level.method_198(f) * 360.0f;
        // night time
        // day time
        if (angle < 90.0 || angle > 270.0) GL11.glRotatef(angle - 90.0f, -1.0f, 0.0f, 0.0f);
        else GL11.glRotatef(angle + 90.0f, -1.0f, 0.0f, 0.0f);

        // reduces jitter
        GL11.glTranslatef(x % 10.0f - 5.0f, y % 10.0f - 5.0f, z % 10.0f - 5.0f);
    }

    static void setupShadowFrameBuffer() {
        if (!shadowEnabled) return;

        setupShadowRenderTexture();

        if (shadowFramebuffer != 0) EXTFramebufferObject.glDeleteFramebuffersEXT(shadowFramebuffer);

        shadowFramebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, shadowFramebuffer);

        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        if (shadowDepthBuffer != 0) EXTFramebufferObject.glDeleteRenderbuffersEXT(shadowDepthBuffer);
        shadowDepthBuffer = GLUtils.glCreateDepthBuffer(shadowResolution, shadowResolution);

        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                shadowDepthBuffer);

        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, GL11.GL_TEXTURE_2D, shadowDepthTexture, 0);

        int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
        if (status != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT)
            System.err.println("Failed creating shadow framebuffer! (Status " + status + ")");
    }

    static void setupShadowRenderTexture() {
        if (!shadowEnabled) return;

        GL11.glDeleteTextures(shadowDepthTexture);
        shadowDepthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowDepthTexture);

        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        ByteBuffer buffer = ByteBuffer.allocateDirect(shadowResolution * shadowResolution * 4);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, shadowResolution, shadowResolution, 0,
                GL11.GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT, buffer);
    }
}