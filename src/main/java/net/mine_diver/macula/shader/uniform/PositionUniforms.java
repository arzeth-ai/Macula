package net.mine_diver.macula.shader.uniform;

import net.mine_diver.macula.shader.ShaderCore;
import net.mine_diver.macula.shader.ShadowMap;
import net.minecraft.entity.LivingEntity;
import org.joml.Vector3f;

public class PositionUniforms {
    public static final Vector3f sunPosition = new Vector3f();

    public static final Vector3f moonPosition = new Vector3f();

    public static final Vector3f previousCameraPosition = new Vector3f();
    public static final Vector3f cameraPosition = new Vector3f();

    private static final Vector3f tempPrevRender = new Vector3f();
    private static final Vector3f tempRender = new Vector3f();

    public static void updateCamera(float alpha) {
        LivingEntity viewEntity = ShaderCore.MINECRAFT.viewEntity;

        tempPrevRender.set(
                (float) viewEntity.prevRenderX,
                (float) viewEntity.prevRenderY,
                (float) viewEntity.prevRenderZ
        );

        tempRender.set(
                (float) viewEntity.x,
                (float) viewEntity.y,
                (float) viewEntity.z
        );

        Vector3f currentPosition = tempPrevRender.lerp(tempRender, alpha);

        if (ShadowMap.isShadowPass) {
            ShadowMap.setupShadowViewport(alpha, currentPosition);

            MatrixUniforms.updateShadowProjection();
            MatrixUniforms.updateShadowModelView();
            return;
        }

        MatrixUniforms.updateProjection();
        MatrixUniforms.updateModelView();

        // Update previous camera
        previousCameraPosition.set(cameraPosition);

        // Update current camera
        cameraPosition.set(currentPosition);
    }

    public static void updateCelestialPosition() {
        // This is called when the current matrix is the model view matrix based on the celestial angle
        // The sun is at (0, 100, 0); the moon at (0, -100, 0)
        MatrixUniforms.updateModelViewCelestial();

        // Equivalent to multiplying the matrix by (0, 100, 0, 0)
        final float SUN_HEIGHT = 100f;
        sunPosition.set(
                MatrixUniforms.modelViewCelestial.m10() * SUN_HEIGHT,
                MatrixUniforms.modelViewCelestial.m11() * SUN_HEIGHT,
                MatrixUniforms.modelViewCelestial.m12() * SUN_HEIGHT
        );

        // The moon is opposite the sun
        moonPosition.set(sunPosition).negate();
    }
}