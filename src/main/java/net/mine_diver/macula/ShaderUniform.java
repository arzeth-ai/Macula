package net.mine_diver.macula;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class ShaderUniform {
    private static final int MATRIX_SIZE = 16;
    private static final boolean TRANSPOSE = false;

    private static final Map<Integer, Integer> uniformLocationCache = new HashMap<>();

    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(MATRIX_SIZE);

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

    public static void setProgramUniform3f(int programId, String name, Vector3f vec3) {
        int uniform = getUniformLocation(programId, name);
        if (uniform != -1) ARBShaderObjects.glUniform3fARB(uniform, vec3.x, vec3.y, vec3.z);
    }

    public static void setProgramUniformMatrix4(int programId, String name, Matrix4f mat4) {
        int uniform = getUniformLocation(programId, name);

        if (uniform == -1) return;

        matrixBuffer.clear();
        mat4.get(matrixBuffer);

        ARBShaderObjects.glUniformMatrix4ARB(uniform, TRANSPOSE, matrixBuffer);
    }

    public static void clearUniformLocation() {
        uniformLocationCache.clear();
    }
}