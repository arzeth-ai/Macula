package net.mine_diver.macula;

import net.fabricmc.loader.api.FabricLoader;
import net.mine_diver.macula.option.ShaderOption;
import net.mine_diver.macula.util.MinecraftInstance;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ShaderPack {
    public static final File SHADERPACK_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve(
            "shaderpacks").toFile();

    public static final String SHADER_DISABLED = "OFF";
    public static final String BUILT_IN_SHADER = "(internal)";

    public static String currentShaderName = SHADER_DISABLED;

    public static List<String> listShaderPack() {
        ArrayList<String> shaderPackList = new ArrayList<>();
        shaderPackList.add(SHADER_DISABLED);
        shaderPackList.add(BUILT_IN_SHADER);

        if (!SHADERPACK_DIRECTORY.exists() && !SHADERPACK_DIRECTORY.mkdir()) {
            throw new RuntimeException("Error creating the directory: " + SHADERPACK_DIRECTORY.getName());
        }

        File[] files = SHADERPACK_DIRECTORY.listFiles();
        if (files == null) return shaderPackList;

        List<String> packs = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();

            if (file.isDirectory() && !fileName.equals("debug")) {
                File[] children = file.listFiles();
                if (children != null && children.length > 0) {
                    packs.add(fileName);
                }
            } else if (file.isFile() && fileName.toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
                packs.add(fileName);
            }
        }

        packs.sort(String.CASE_INSENSITIVE_ORDER);
        shaderPackList.addAll(packs);
        return shaderPackList;
    }

    public static void setShaderPack(String shaderPack) {
        currentShaderName = shaderPack;
        ShaderConfig.shadersConfig.setProperty(ShaderOption.SHADER_PACK.getPropertyKey(), shaderPack);
        Shaders.setIsInitialized(false);

        Minecraft mcInstance = MinecraftInstance.get();
        if (mcInstance != null && mcInstance.player != null) {
            loadShaderPack();
        }
    }

    public static void loadShaderPack() {
        Shaders.destroy();
        Shaders.setIsInitialized(false);
        Shaders.init();
        MinecraftInstance.get().worldRenderer.method_1537();
    }

    public static BufferedReader openShaderPackFile(String filename) throws IOException {
        final File shaderDir = new File(ShaderPack.SHADERPACK_DIRECTORY, ShaderPack.currentShaderName);
        final IOException fileNotFoundException = new IOException("File not found: " + filename);

        if (shaderDir.isDirectory()) {
            File file = new File(shaderDir, filename);
            if (!file.exists()) {
                throw fileNotFoundException;
            }
            return new BufferedReader(new FileReader(file));
        } else {
            ZipFile zipFile = new ZipFile(shaderDir);
            ZipEntry zipEntry = zipFile.getEntry(filename);
            if (zipEntry == null) {
                zipFile.close();
                throw fileNotFoundException;
            }
            return new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry)));
        }
    }
}