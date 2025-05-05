package net.mine_diver.macula;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class MatrixBuffer {
    static final FloatBuffer previousProjection = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer projectionInverse = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer previousModelView = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer modelViewInverse = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer modelViewCelestial = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer shadowProjection = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer shadowProjectionInverse = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer shadowModelView = BufferUtils.createFloatBuffer(16);
    static final FloatBuffer shadowModelViewInverse = BufferUtils.createFloatBuffer(16);

    public static void initMatrixBuffer() {
        BufferUtils.zeroBuffer(projection);
        BufferUtils.zeroBuffer(previousProjection);
        BufferUtils.zeroBuffer(modelView);
        BufferUtils.zeroBuffer(previousModelView);
        BufferUtils.zeroBuffer(shadowProjection);
        BufferUtils.zeroBuffer(shadowModelView);
        BufferUtils.zeroBuffer(modelViewCelestial);
    }

    public static void copyBuffer(FloatBuffer src, FloatBuffer dst) {
        dst.clear();
        dst.put(src);
        dst.flip();
    }

    public static void getMatrixBuffer(int glMatrixType, FloatBuffer buffer) {
        buffer.clear();
        GL11.glGetFloat(glMatrixType, buffer);
    }
}