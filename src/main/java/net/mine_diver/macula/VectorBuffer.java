package net.mine_diver.macula;

import net.mine_diver.macula.util.MatrixUtil;
import net.minecraft.entity.Living;
import org.lwjgl.opengl.GL11;

public class VectorBuffer {
    public static final float[] sunPosition = new float[3];
    public static final float[] moonPosition = new float[3];
    public static final float[] previousCameraPosition = new float[3];
    public static final float[] cameraPosition = new float[3];

    public static void setCamera(float f) {
        Living viewEntity = Shaders.MINECRAFT.viewEntity;

        float x = (float) (viewEntity.prevRenderX + (viewEntity.x - viewEntity.prevRenderX) * f);
        float y = (float) (viewEntity.prevRenderY + (viewEntity.y - viewEntity.prevRenderY) * f);
        float z = (float) (viewEntity.prevRenderZ + (viewEntity.z - viewEntity.prevRenderZ) * f);

        if (Shaders.isShadowPass) {
            Shaders.setupShadowViewport(f, x, y, z);
            Shaders.setupShadowMatrix();
            return;
        }

        MatrixBuffer.copyBuffer(MatrixBuffer.projection, MatrixBuffer.previousProjection);
        MatrixBuffer.getMatrixBuffer(GL11.GL_PROJECTION_MATRIX, MatrixBuffer.projection);
        MatrixUtil.invertMat4(MatrixBuffer.projection, MatrixBuffer.projectionInverse);

        MatrixBuffer.copyBuffer(MatrixBuffer.modelView, MatrixBuffer.previousModelView);
        MatrixBuffer.getMatrixBuffer(GL11.GL_MODELVIEW_MATRIX, MatrixBuffer.modelView);
        MatrixUtil.invertMat4(MatrixBuffer.modelView, MatrixBuffer.modelViewInverse);

        previousCameraPosition[0] = cameraPosition[0];
        previousCameraPosition[1] = cameraPosition[1];
        previousCameraPosition[2] = cameraPosition[2];

        cameraPosition[0] = x;
        cameraPosition[1] = y;
        cameraPosition[2] = z;
    }

    public static void setCelestialPosition() {
        // This is called when the current matrix is the model view matrix based on the celestial angle.
        // The sun is at (0, 100, 0); the moon at (0, -100, 0).

        MatrixBuffer.getMatrixBuffer(GL11.GL_MODELVIEW_MATRIX, MatrixBuffer.modelViewCelestial);

        float[] matrixMV = new float[16];
        MatrixBuffer.modelViewCelestial.get(0, matrixMV, 0, 16);

        // Equivalent to multiplying the matrix by (0, 100, 0, 0).
        final float SUN_HEIGHT = 100.0F;
        sunPosition[0] = matrixMV[4] * SUN_HEIGHT;
        sunPosition[1] = matrixMV[5] * SUN_HEIGHT;
        sunPosition[2] = matrixMV[6] * SUN_HEIGHT;

        // The moon is opposite the sun.
        moonPosition[0] = -sunPosition[0];
        moonPosition[1] = -sunPosition[1];
        moonPosition[2] = -sunPosition[2];
    }
}