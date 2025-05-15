package net.mine_diver.macula;

import net.mine_diver.macula.util.MatrixUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class MatrixBuffer {
    private static final int MATRIX_BUFFER_SIZE = 16;

    public static final FloatBuffer previousModelView = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
    public static final FloatBuffer modelView = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
    public static final FloatBuffer modelViewInverse = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);

    public static final FloatBuffer previousProjection = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
    public static final FloatBuffer projection = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
    public static final FloatBuffer projectionInverse = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);

    public static final FloatBuffer modelViewCelestial = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);

    public static final FloatBuffer shadowModelView = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
    public static final FloatBuffer shadowModelViewInverse = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);

    public static final FloatBuffer shadowProjection = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
    public static final FloatBuffer shadowProjectionInverse = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);

    public static void initMatrixBuffer() {
        BufferUtils.zeroBuffer(previousModelView);
        BufferUtils.zeroBuffer(modelView);
        BufferUtils.zeroBuffer(modelViewInverse);

        BufferUtils.zeroBuffer(previousProjection);
        BufferUtils.zeroBuffer(projection);
        BufferUtils.zeroBuffer(projectionInverse);

        BufferUtils.zeroBuffer(modelViewCelestial);

        BufferUtils.zeroBuffer(shadowModelView);
        BufferUtils.zeroBuffer(shadowModelViewInverse);

        BufferUtils.zeroBuffer(shadowProjection);
        BufferUtils.zeroBuffer(shadowProjectionInverse);
    }

    public static void updateModelView() {
        // Update previous model-view
        previousModelView.clear();
        previousModelView.put(modelView);
        previousModelView.flip();

        // Update current model-view
        modelView.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);

        // Update inverse model-view
        MatrixUtil.invMat4(modelView, modelViewInverse);
    }

    public static void updateProjection() {
        // Update previous projection
        MatrixBuffer.previousProjection.clear();
        MatrixBuffer.previousProjection.put(MatrixBuffer.projection);
        MatrixBuffer.previousProjection.flip();

        // Update current projection
        MatrixBuffer.projection.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, MatrixBuffer.projection);

        // Update inverse projection
        MatrixUtil.invMat4(MatrixBuffer.projection, MatrixBuffer.projectionInverse);
    }

    public static void updateModelViewCelestial() {
        // Update celestial model-view
        modelViewCelestial.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewCelestial);
    }


    public static void updateShadowModelView() {
        // Update shadow model-view
        shadowModelView.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, shadowModelView);

        // Update inverse shadow model-view
        MatrixUtil.invMat4(shadowModelView, shadowModelViewInverse);
    }

    public static void updateShadowProjection() {
        // Update shadow projection
        shadowProjection.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, shadowProjection);

        // Update inverse shadow projection
        MatrixUtil.invMat4(shadowProjection, shadowProjectionInverse);
    }
}