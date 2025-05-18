package net.mine_diver.macula.shader.program;

public enum Uniform {
    TEXTURE("texture"),
    LIGHTMAP("lightmap"),
    NORMALS("normals"),
    SPECULAR("specular"),
    GCOLOR("gcolor"),
    GDEPTH("gdepth"),
    GNORMAL("gnormal"),
    COMPOSITE("composite"),
    GAUX1("gaux1"),
    GAUX2("gaux2"),
    GAUX3("gaux3"),
    SHADOW("shadow"),
    GB_PREVIOUS_PROJECTION("gbufferPreviousProjection"),
    GB_PROJECTION("gbufferProjection"),
    GB_PROJECTION_INVERSE("gbufferProjectionInverse"),
    GB_PREVIOUS_MODELVIEW("gbufferPreviousModelView"),
    SHADOW_PROJECTION("shadowProjection"),
    SHADOW_PROJECTION_INVERSE("shadowProjectionInverse"),
    SHADOW_MODELVIEW("shadowModelView"),
    SHADOW_MODELVIEW_INVERSE("shadowModelViewInverse"),
    HELD_ITEM_ID("heldItemId"),
    HELD_BLOCK_LIGHT_VALUE("heldBlockLightValue"),
    FOG_MODE("fogMode"),
    RAIN_STRENGTH("rainStrength"),
    WORLD_TIME("worldTime"),
    ASPECT_RATIO("aspectRatio"),
    VIEW_WIDTH("viewWidth"),
    VIEW_HEIGHT("viewHeight"),
    NEAR("near"),
    FAR("far"),
    SUN_POSITION("sunPosition"),
    MOON_POSITION("moonPosition"),
    PREVIOUS_CAMERA_POSITION("previousCameraPosition"),
    CAMERA_POSITION("cameraPosition"),
    GB_MODELVIEW("gbufferModelView"),
    GB_MODELVIEW_INVERSE("gbufferModelViewInverse");

    private final String uniformName;

    Uniform(String uniformName) {
        this.uniformName = uniformName;
    }

    public String getName() {
        return uniformName;
    }
}
