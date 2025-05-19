package net.mine_diver.macula.util;

import net.mine_diver.macula.shader.program.Uniform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

public class UniformUtils {
    private static final int MATRIX_SIZE = 16;
    private static final boolean TRANSPOSE = false;

    private static final Map<Integer, Integer> uniformLocationCache = new HashMap<>();

    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(MATRIX_SIZE);

    public static void cacheUniformLocations(int programId) {
        for (Uniform uniform : Uniform.values()) {
            int location = GL20.glGetUniformLocation(programId, uniform.getName());
            if (location != -1) {
                int key = (programId << 16) | uniform.ordinal();
                uniformLocationCache.put(key, location);
            }
        }
    }

    private static int getUniformLocation(int programId, Uniform uniform) {
        int key = (programId << 16) | uniform.ordinal();

        return uniformLocationCache.getOrDefault(key, -1);
    }

    public static void setProgramUniform1i(int programId, Uniform uniform, int n) {
        int location = getUniformLocation(programId, uniform);
        if (location != -1) GL20.glUniform1i(location, n);
    }

    public static void setProgramUniform1f(int programId, Uniform uniform, float x) {
        int location = getUniformLocation(programId, uniform);
        if (location != -1) GL20.glUniform1f(location, x);
    }

    public static void setProgramUniform3f(int programId, Uniform uniform, Vector3f vec3) {
        int location = getUniformLocation(programId, uniform);
        if (location != -1) GL20.glUniform3f(location, vec3.x, vec3.y, vec3.z);
    }

    public static void setProgramUniformMatrix4(int programId, Uniform uniform, Matrix4f mat4) {
        int location = getUniformLocation(programId, uniform);

        if (location == -1) return;

        matrixBuffer.clear();
        mat4.get(matrixBuffer);

       GL20.glUniformMatrix4(location, TRANSPOSE, matrixBuffer);
    }

    public static void clearUniformLocation() {
        uniformLocationCache.clear();
    }
}