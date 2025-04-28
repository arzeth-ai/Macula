package net.mine_diver.macula;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.macula.option.ShaderOption;
import net.mine_diver.macula.util.MinecraftInstance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ShaderConfig {
    public static final File shaderPacksDir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks").toFile();
    // config stuff
    public static final File
            configDir = FabricLoader.getInstance().getConfigDir().resolve("macula").toFile();
    public static final File shaderConfigFile = new File(configDir, "shaders.properties");
    public static final Properties shadersConfig = new Properties();
    public static String currentShaderName = "OFF";
    public static float configShadowResMul = 1;

    public static List<String> getShaderPackList() {
        List<String> folderShaders = new ArrayList<String>();
        List<String> zipShaders = new ArrayList<String>();

        try {
            if (!shaderPacksDir.exists()) //noinspection ResultOfMethodCallIgnored
                shaderPacksDir.mkdir();

            File[] afile = shaderPacksDir.listFiles();

            assert afile != null;
            for (File file1 : afile) {
                String s = file1.getName();

                if (file1.isDirectory()) {
                    if (!s.equals("debug")) {
                        File file2 = new File(file1, "shaders");

                        if (file2.exists() && file2.isDirectory()) folderShaders.add(s);
                    }
                } else if (file1.isFile() && s.toLowerCase().endsWith(".zip")) zipShaders.add(s);
            }
        } catch (Exception ignored) {
        }

        folderShaders.sort(String.CASE_INSENSITIVE_ORDER);
        zipShaders.sort(String.CASE_INSENSITIVE_ORDER);
        ArrayList<String> arraylist2 = new ArrayList<String>();
        arraylist2.add("OFF");
        arraylist2.add("(internal)");
        arraylist2.addAll(folderShaders);
        arraylist2.addAll(zipShaders);
        return arraylist2;
    }

    public static void loadConfig() {
        try {
            if (!shaderPacksDir.exists()) {
                shaderPacksDir.mkdir();
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
            case SHADER_PACK -> currentShaderName = str;
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
            case SHADER_PACK -> currentShaderName;
        };
    }

    public static void setShaderPack(String shaderPack) {
        if (null != MinecraftInstance.get()) {
            if (null != MinecraftInstance.get().player) {
                currentShaderName = shaderPack;
                shadersConfig.setProperty(ShaderOption.SHADER_PACK.getPropertyKey(), shaderPack);
                loadShaderPack();
            }
        }
    }

    public static void loadShaderPack() {
        Shaders.destroy();
        Shaders.setIsInitialized(false);
        Shaders.init();
        MinecraftInstance.get().worldRenderer.method_1537();
    }
}