package net.mine_diver.macula.shader.uniform;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class MatrixUniforms {
    private static final int MATRIX_SIZE = 16;

    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(MATRIX_SIZE);

    public static final Matrix4f previousModelView = new Matrix4f();
    public static final Matrix4f modelView = new Matrix4f();
    public static final Matrix4f modelViewInverse = new Matrix4f();

    public static final Matrix4f previousProjection = new Matrix4f();
    public static final Matrix4f projection = new Matrix4f();
    public static final Matrix4f projectionInverse = new Matrix4f();

    public static final Matrix4f modelViewCelestial = new Matrix4f();

    public static final Matrix4f shadowModelView = new Matrix4f();
    public static final Matrix4f shadowModelViewInverse = new Matrix4f();

    public static final Matrix4f shadowProjection = new Matrix4f();
    public static final Matrix4f shadowProjectionInverse = new Matrix4f();

    public static void updateModelView() {
        // Update previous model-view
        previousModelView.set(modelView);

        // Update current model-view
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        modelView.set(matrixBuffer);

        // Update inverse model-view
        modelView.invert(modelViewInverse);
    }

    public static void updateProjection() {
        // Update previous projection
        previousProjection.set(projection);

        // Update current projection
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer);
        projection.set(matrixBuffer);

        // Update inverse projection
        projection.invert(projectionInverse);
    }

    public static void updateModelViewCelestial() {
        // Update celestial model-view
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        modelViewCelestial.set(matrixBuffer);
    }


    public static void updateShadowModelView() {
        // Update shadow model-view
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        shadowModelView.set(matrixBuffer);

        // Update inverse shadow model-view
        shadowModelView.invert(shadowModelViewInverse);
    }

    public static void updateShadowProjection() {
        // Update shadow projection
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrixBuffer);
        shadowProjection.set(matrixBuffer);

        // Update inverse shadow projection
        shadowProjection.invert(shadowProjectionInverse);
    }
}