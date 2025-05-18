package net.mine_diver.macula.shader;

import net.mine_diver.macula.util.GLUtils;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Framebuffer {
    public static int colorAttachments = 0;

    public static IntBuffer defaultDrawBuffers = null;
    public static IntBuffer defaultTextures = null;
    public static IntBuffer defaultRenderBuffers = null;

    public static int defaultFramebufferId = 0;
    private static int defaultDepthBufferId = 0;

    private static final int DEPTH_ATTACHMENT_INDEX = 1;

    public static void setupFrameBuffer() {
        if (defaultFramebufferId != 0) {
            ARBFramebufferObject.glDeleteFramebuffers(defaultFramebufferId);
            ARBFramebufferObject.glDeleteRenderbuffers(defaultRenderBuffers);
        }

        defaultFramebufferId = ARBFramebufferObject.glGenFramebuffers();
        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER, defaultFramebufferId);

        ARBFramebufferObject.glGenRenderbuffers(defaultRenderBuffers);

        for (int i = 0; i < colorAttachments; ++i) {
            ARBFramebufferObject.glBindRenderbuffer(ARBFramebufferObject.GL_RENDERBUFFER,
                    defaultRenderBuffers.get(i));
            // Depth buffer
            if (i == DEPTH_ATTACHMENT_INDEX) {
                ARBFramebufferObject.glRenderbufferStorage(ARBFramebufferObject.GL_RENDERBUFFER,
                        ARBTextureFloat.GL_RGB32F_ARB, ShaderCore.renderWidth, ShaderCore.renderHeight);
            } else {
                ARBFramebufferObject.glRenderbufferStorage(ARBFramebufferObject.GL_RENDERBUFFER, GL11.GL_RGBA,
                        ShaderCore.renderWidth, ShaderCore.renderHeight);
            }
            ARBFramebufferObject.glFramebufferRenderbuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                    defaultDrawBuffers.get(i), ARBFramebufferObject.GL_RENDERBUFFER,
                    defaultRenderBuffers.get(i));
            ARBFramebufferObject.glFramebufferTexture2D(ARBFramebufferObject.GL_FRAMEBUFFER,
                    defaultDrawBuffers.get(i), GL11.GL_TEXTURE_2D,
                    defaultTextures.get(i), 0);
        }

        if (defaultDepthBufferId != 0)
            ARBFramebufferObject.glDeleteRenderbuffers(defaultDepthBufferId);
        defaultDepthBufferId = GLUtils.glCreateDepthBuffer(ShaderCore.renderWidth, ShaderCore.renderHeight);

        ARBFramebufferObject.glFramebufferRenderbuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                ARBFramebufferObject.GL_DEPTH_ATTACHMENT, ARBFramebufferObject.GL_RENDERBUFFER,
                defaultDepthBufferId);

        int status = ARBFramebufferObject.glCheckFramebufferStatus(ARBFramebufferObject.GL_FRAMEBUFFER);
        if (status != ARBFramebufferObject.GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Failed creating framebuffer! (Status " + status + ")");
    }

    public static void setupRenderTextures() {
        GL11.glDeleteTextures(defaultTextures);
        GL11.glGenTextures(defaultTextures);

        for (int i = 0; i < colorAttachments; ++i) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, defaultTextures.get(i));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            if (i == DEPTH_ATTACHMENT_INDEX) { // depth buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(ShaderCore.renderWidth * ShaderCore.renderHeight * 4 * 4);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, ARBTextureFloat.GL_RGB32F_ARB, ShaderCore.renderWidth,
                        ShaderCore.renderHeight, 0, GL11.GL_RGBA, GL11.GL_FLOAT,
                        buffer);
            } else {
                ByteBuffer buffer = ByteBuffer.allocateDirect(ShaderCore.renderWidth * ShaderCore.renderHeight * 4);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, ShaderCore.renderWidth, ShaderCore.renderHeight, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                        buffer);
            }
        }
    }
}