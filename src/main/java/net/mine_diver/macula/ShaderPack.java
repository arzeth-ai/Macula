package net.mine_diver.macula;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.macula.option.ShaderOption;
import net.mine_diver.macula.util.MinecraftInstance;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShaderPack {
    public static final File shaderPacksDir = FabricLoader.getInstance().getGameDir().resolve("shaderpacks").toFile();
    public static String currentShaderName = "OFF";

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

    public static void setShaderPack(String shaderPack) {
        if (null != MinecraftInstance.get()) {
            if (null != MinecraftInstance.get().player) {
                currentShaderName = shaderPack;
                ShaderConfig.shadersConfig.setProperty(ShaderOption.SHADER_PACK.getPropertyKey(), shaderPack);
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