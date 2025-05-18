package net.mine_diver.macula.option;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.macula.ShaderPack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

public class ShaderConfig {
    // config stuff
    public static final File
            configDir = FabricLoader.getInstance().getConfigDir().resolve("macula").toFile();
    public static final File shaderConfigFile = new File(configDir, "shaders.properties");
    public static final Properties shadersConfig = new Properties();
    public static float configShadowResMul = 1;

    public static void loadConfig() {
        try {
            if (!ShaderPack.SHADERPACK_DIRECTORY.exists()) {
                ShaderPack.SHADERPACK_DIRECTORY.mkdir();
            }
        } catch (Exception ignored) {
        }

        shadersConfig.setProperty(ShaderOption.SHADER_PACK.getPropertyKey(), "");

        if (shaderConfigFile.exists())
            try (FileReader filereader = new FileReader(shaderConfigFile)) {
                shadersConfig.load(filereader);
            } catch (Exception ignored) {
            }

        if (!shaderConfigFile.exists()) try {
            storeConfig();
        } catch (Exception ignored) {
        }

        for (ShaderOption option : ShaderOption.values())
            setEnumShaderOption(option,
                    shadersConfig.getProperty(option.getPropertyKey(), option.getValueDefault()));
    }

    public static void setEnumShaderOption(ShaderOption eso, String str) {
        if (str == null) str = eso.getValueDefault();

        switch (eso) {
            case SHADOW_RES_MUL -> {
                try {
                    configShadowResMul = Float.parseFloat(str);
                } catch (NumberFormatException e) {
                    configShadowResMul = 1;
                }
            }
            case SHADER_PACK -> ShaderPack.currentShaderName = str;
            default -> throw new IllegalArgumentException("Unknown option: " + eso);
        }
    }

    public static void storeConfig() {

        for (ShaderOption enumshaderoption : ShaderOption.values())
            shadersConfig.setProperty(enumshaderoption.getPropertyKey(), getEnumShaderOption(enumshaderoption));

        try (FileWriter filewriter = new FileWriter(shaderConfigFile)) {
            shadersConfig.store(filewriter, null);
        } catch (Exception ignored) {
        }
    }

    public static String getEnumShaderOption(ShaderOption eso) {
        return switch (eso) {
            case SHADOW_RES_MUL -> Float.toString(configShadowResMul);
            case SHADER_PACK -> ShaderPack.currentShaderName;
        };
    }

}