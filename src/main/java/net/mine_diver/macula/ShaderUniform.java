package net.mine_diver.macula;

import org.lwjgl.opengl.ARBShaderObjects;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class ShaderUniform {
    private static final Map<Integer, Integer> uniformLocationCache = new HashMap<>();

    static int getUniformLocation(int programId, String name) {
        int nameHash = name.hashCode() & 0xFFFF;
        int key = (programId << 16) | nameHash;

        return uniformLocationCache.computeIfAbsent(key,
                k -> ARBShaderObjects.glGetUniformLocationARB(programId, name));
    }

    public static void setProgramUniform1i(int programId, String name, int n) {
        int uniform = getUniformLocation(programId, name);
        if (uniform != -1) ARBShaderObjects.glUniform1iARB(uniform, n);
    }

    public static void setProgramUniform1f(int programId, String name, float x) {
        int uniform = getUniformLocation(programId, name);
        if (uniform != -1) ARBShaderObjects.glUniform1fARB(uniform, x);
    }

    public static void setProgramUniform3f(int programId, String name, float[] vec3) {
        int uniform = getUniformLocation(programId, name);
        if (uniform != -1) ARBShaderObjects.glUniform3fARB(uniform, vec3[0], vec3[1], vec3[2]);
    }

    public static void setProgramUniformMatrix4(int programId, String name, FloatBuffer mat4) {
        int uniform = getUniformLocation(programId, name);
        final boolean TRANSPOSE = false;
        if (uniform != -1) ARBShaderObjects.glUniformMatrix4ARB(uniform, TRANSPOSE, mat4);
    }

    public static void clearUniformLocation() {
        uniformLocationCache.clear();
    }
}