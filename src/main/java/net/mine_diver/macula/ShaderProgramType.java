package net.mine_diver.macula;

public enum ShaderProgramType {
    NONE("", null),
    BASIC("gbuffers_basic", NONE),
    TEXTURED("gbuffers_textured", BASIC),
    TEXTURED_LIT("gbuffers_textured_lit", TEXTURED),
    TERRAIN("gbuffers_terrain", TEXTURED_LIT),
    WATER("gbuffers_water", TERRAIN),
    HAND("gbuffers_hand", TEXTURED_LIT),
    WEATHER("gbuffers_weather", TEXTURED_LIT),
    COMPOSITE("composite", NONE),
    FINAL("final", NONE);

    public final String fileName;
    public final ShaderProgramType fallback;

    ShaderProgramType(String fileName, ShaderProgramType fallback) {
        this.fileName = fileName;
        this.fallback = fallback;
    }
}
