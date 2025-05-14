package net.mine_diver.macula;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

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
            Shaders.fogEnabled = true;
            ShaderUniform.setProgramUniform1i(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram),
                    "fogMode", GL11.glGetInteger(GL11.GL_FOG_MODE));
        }
    }

    public static void glDisableWrapper(int cap) {
        GL11.glDisable(cap);
        if (cap == GL11.GL_TEXTURE_2D) {
            if (ShaderProgram.activeShaderProgram == ShaderProgramType.TEXTURED || ShaderProgram.activeShaderProgram == ShaderProgramType.TEXTURED_LIT)
                ShaderProgram.useShaderProgram(ShaderProgramType.BASIC);
        } else if (cap == GL11.GL_FOG) {
            Shaders.fogEnabled = false;
            ShaderUniform.setProgramUniform1i(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram),
                    "fogMode", 0);
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

    public static boolean printLogInfo(int obj) {
        IntBuffer iVal = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);

        int length = iVal.get();
        if (length > 1) {
            ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
            iVal.flip();
            ARBShaderObjects.glGetInfoLogARB(obj, iVal, infoLog);
            byte[] infoBytes = new byte[length];
            infoLog.get(infoBytes);
            String out = new String(infoBytes);
            System.out.println("Info log:\n" + out);
            return false;
        }
        return true;
    }

    static void glClearBuffer(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }
}