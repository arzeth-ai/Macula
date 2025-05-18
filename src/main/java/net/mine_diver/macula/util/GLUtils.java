package net.mine_diver.macula.util;

import net.mine_diver.macula.shader.ShaderCore;
import net.mine_diver.macula.shader.program.ShaderProgram;
import net.mine_diver.macula.shader.program.ShaderProgramType;
import net.mine_diver.macula.shader.program.Uniform;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class GLUtils {
    public static final String glVersionString = GL11.glGetString(GL11.GL_VERSION);
    public static final String glVendorString = GL11.glGetString(GL11.GL_VENDOR);
    public static final String glRendererString = GL11.glGetString(GL11.GL_RENDERER);

    public static void glEnableWrapper(int cap) {
        GL11.glEnable(cap);
        if (cap == GL11.GL_TEXTURE_2D) {
            if (ShaderProgram.activeShaderProgram == ShaderProgramType.BASIC) ShaderProgram.useShaderProgram(
                    ShaderProgramType.TEXTURED);
        } else if (cap == GL11.GL_FOG) {
            ShaderCore.fogEnabled = true;
            UniformUtils.setProgramUniform1i(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram),
                    Uniform.FOG_MODE, GL11.glGetInteger(GL11.GL_FOG_MODE));
        }
    }

    public static void glDisableWrapper(int cap) {
        GL11.glDisable(cap);
        if (cap == GL11.GL_TEXTURE_2D) {
            if (ShaderProgram.activeShaderProgram == ShaderProgramType.TEXTURED || ShaderProgram.activeShaderProgram == ShaderProgramType.TEXTURED_LIT)
                ShaderProgram.useShaderProgram(ShaderProgramType.BASIC);
        } else if (cap == GL11.GL_FOG) {
            ShaderCore.fogEnabled = false;
            UniformUtils.setProgramUniform1i(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram),
                    Uniform.FOG_MODE, 0);
        }
    }

    public static void glDrawQuad() {
        GL11.glBegin(GL11.GL_TRIANGLES);

        // First triangle
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(0.0f, 0.0f, 0.0f);

        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex3f(1.0f, 0.0f, 0.0f);

        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(1.0f, 1.0f, 0.0f);

        // Second triangle
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(0.0f, 0.0f, 0.0f);

        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex3f(1.0f, 1.0f, 0.0f);

        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(0.0f, 1.0f, 0.0f);

        GL11.glEnd();
    }

    public static boolean printLogInfo(int program) {
        int logLength = GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH);

        if (logLength == 0) return true;

        String log = GL20.glGetProgramInfoLog(program, logLength);
        if (!log.trim().isEmpty()) {
            System.out.println("Info log:\n" + log);
            return false;
        }
        return true;
    }

    public static void glClearBuffer(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public static void glSetupOrthographicProjection(float left, float right, float bottom, float top, float near, float far) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GL11.glOrtho(left, right, bottom, top, near, far);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    public static int glCreateDepthBuffer(int width, int height) {
        int depthBuffer = ARBFramebufferObject.glGenRenderbuffers();
        ARBFramebufferObject.glBindRenderbuffer(ARBFramebufferObject.GL_RENDERBUFFER, depthBuffer);
        ARBFramebufferObject.glRenderbufferStorage(ARBFramebufferObject.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT,
                width, height);
        return depthBuffer;
    }
}