package net.mine_diver.macula;

import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ColorBuffer {
    public static int colorAttachments = 0;
    static IntBuffer defaultDrawBuffers = null;
    static IntBuffer defaultTextures = null;
    static IntBuffer defaultRenderBuffers = null;
    static int defaultFramebuffer = 0;
    public static final int DEPTH_ATTACHMENT_INDEX = 1;
    static int defaultDepthBuffer = 0;

    static void setupFrameBuffer() {
        if (defaultFramebuffer != 0) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(defaultFramebuffer);
            EXTFramebufferObject.glDeleteRenderbuffersEXT(defaultRenderBuffers);
        }

        defaultFramebuffer = EXTFramebufferObject.glGenFramebuffersEXT();
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, defaultFramebuffer);

        EXTFramebufferObject.glGenRenderbuffersEXT(defaultRenderBuffers);

        for (int i = 0; i < colorAttachments; ++i) {
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    defaultRenderBuffers.get(i));
            // Depth buffer
            if (i == DEPTH_ATTACHMENT_INDEX) {
                EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                        ARBTextureFloat.GL_RGB32F_ARB, ShaderCore.renderWidth, ShaderCore.renderHeight);
            } else {
                EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL11.GL_RGBA,
                        ShaderCore.renderWidth, ShaderCore.renderHeight);
            }
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    defaultDrawBuffers.get(i), EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    defaultRenderBuffers.get(i));
            EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    defaultDrawBuffers.get(i), GL11.GL_TEXTURE_2D,
                    defaultTextures.get(i), 0);
        }

        if (defaultDepthBuffer != 0)
            EXTFramebufferObject.glDeleteRenderbuffersEXT(defaultDepthBuffer);
        defaultDepthBuffer = GLUtils.glCreateDepthBuffer(ShaderCore.renderWidth, ShaderCore.renderHeight);

        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                defaultDepthBuffer);

        int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
        if (status != EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT)
            System.err.println("Failed creating framebuffer! (Status " + status + ")");
    }

    static void setupRenderTextures() {
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