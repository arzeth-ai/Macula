package net.mine_diver.macula.util;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class MatrixUtil {
    public static float[] multiplyMat4xVec4(float[] matrix, float[] vector) {
        float[] result = new float[4];
        result[0] = matrix[0] * vector[0] + matrix[4] * vector[1] + matrix[8] * vector[2] + matrix[12] * vector[3];
        result[1] = matrix[1] * vector[0] + matrix[5] * vector[1] + matrix[9] * vector[2] + matrix[13] * vector[3];
        result[2] = matrix[2] * vector[0] + matrix[6] * vector[1] + matrix[10] * vector[2] + matrix[14] * vector[3];
        result[3] = matrix[3] * vector[0] + matrix[7] * vector[1] + matrix[11] * vector[2] + matrix[15] * vector[3];
        return result;
    }

    /**
     * Efficient 4x4 matrix inverse
     * Based on: https://stackoverflow.com/questions/2624422/efficient-4x4-matrix-inverse-affine-transform
     * Comparison:
     * |       | Optimized | Original | Reduction |
     * | Add   |        19 |       43 |      56%  |
     * | Sub   |        38 |       48 |      21%  |
     * | Mul   |        94 |      196 |      52%  |
     * | Div   |         1 |       16 |      94%  |
     * | Total |       152 |      303 |      50%  |
     */
    public static FloatBuffer invertMat4x(FloatBuffer matrixBuffer) {
        float[] matrix = new float[16];
        matrixBuffer.get(0, matrix);

        float s0 = matrix[0] * matrix[5] - matrix[4] * matrix[1];
        float s1 = matrix[0] * matrix[6] - matrix[4] * matrix[2];
        float s2 = matrix[0] * matrix[7] - matrix[4] * matrix[3];
        float s3 = matrix[1] * matrix[6] - matrix[5] * matrix[2];
        float s4 = matrix[1] * matrix[7] - matrix[5] * matrix[3];
        float s5 = matrix[2] * matrix[7] - matrix[6] * matrix[3];

        float c5 = matrix[10] * matrix[15] - matrix[14] * matrix[11];
        float c4 = matrix[9] * matrix[15] - matrix[13] * matrix[11];
        float c3 = matrix[9] * matrix[14] - matrix[13] * matrix[10];
        float c2 = matrix[8] * matrix[15] - matrix[12] * matrix[11];
        float c1 = matrix[8] * matrix[14] - matrix[12] * matrix[10];
        float c0 = matrix[8] * matrix[13] - matrix[12] * matrix[9];

        float det = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;

        // Handle non-invertible matrix
        if (det == 0.0f) {
            return BufferUtils.createFloatBuffer(16); // Return zero matrix
        }

        float invDet = 1.0f / det;
        FloatBuffer inverse = BufferUtils.createFloatBuffer(16);

        inverse.put(0, ( matrix[5] * c5 - matrix[6] * c4 + matrix[7] * c3) * invDet);
        inverse.put(1, (-matrix[1] * c5 + matrix[2] * c4 - matrix[3] * c3) * invDet);
        inverse.put(2, ( matrix[13] * s5 - matrix[14] * s4 + matrix[15] * s3) * invDet);
        inverse.put(3, (-matrix[9] * s5 + matrix[10] * s4 - matrix[11] * s3) * invDet);

        inverse.put(4, (-matrix[4] * c5 + matrix[6] * c2 - matrix[7] * c1) * invDet);
        inverse.put(5, ( matrix[0] * c5 - matrix[2] * c2 + matrix[3] * c1) * invDet);
        inverse.put(6, (-matrix[12] * s5 + matrix[14] * s2 - matrix[15] * s1) * invDet);
        inverse.put(7, ( matrix[8] * s5 - matrix[10] * s2 + matrix[11] * s1) * invDet);

        inverse.put(8, ( matrix[4] * c4 - matrix[5] * c2 + matrix[7] * c0) * invDet);
        inverse.put(9, (-matrix[0] * c4 + matrix[1] * c2 - matrix[3] * c0) * invDet);
        inverse.put(10, ( matrix[12] * s4 - matrix[13] * s2 + matrix[15] * s0) * invDet);
        inverse.put(11, (-matrix[8] * s4 + matrix[9] * s2 - matrix[11] * s0) * invDet);

        inverse.put(12, (-matrix[4] * c3 + matrix[5] * c1 - matrix[6] * c0) * invDet);
        inverse.put(13, ( matrix[0] * c3 - matrix[1] * c1 + matrix[2] * c0) * invDet);
        inverse.put(14, (-matrix[12] * s3 + matrix[13] * s1 - matrix[14] * s0) * invDet);
        inverse.put(15, ( matrix[8] * s3 - matrix[9] * s1 + matrix[10] * s0) * invDet);

        return inverse;
    }
}