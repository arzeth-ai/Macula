package net.mine_diver.macula;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ShaderCompiler {
    static final Pattern MC_ENTITY = Pattern.compile("attribute [_a-zA-Z0-9]+ mc_Entity.*");

    static final Pattern PATTERN_GAUX1 = Pattern.compile("uniform [ _a-zA-Z0-9]+ gaux1;.*");
    static final Pattern PATTERN_GAUX2 = Pattern.compile("uniform [ _a-zA-Z0-9]+ gaux2;.*");
    static final Pattern PATTERN_GAUX3 = Pattern.compile("uniform [ _a-zA-Z0-9]+ gaux3;.*");
    static final Pattern PATTERN_SHADOW = Pattern.compile("uniform [ _a-zA-Z0-9]+ shadow;.*");
    static final Pattern PATTERN_SHADOWRES = Pattern.compile("/\\* SHADOWRES:([0-9]+) \\*/.*");
    static final Pattern PATTERN_SHADOWHPL = Pattern.compile("/\\* SHADOWHPL:([0-9.]+) \\*/.*");
    static final Pattern SPLIT_PATTERN = Pattern.compile("[: ]");

    static int createShader(int shaderType, String filename, Consumer<String> lineProcessor) {
        int shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
        if (shader == 0) return 0;

        StringBuilder shaderCode = new StringBuilder();
        try (BufferedReader reader = ShaderPack.openShaderPackFile(filename)) {
            String line;
            while ((line = reader.readLine()) != null) {
                shaderCode.append(line).append("\n");
                lineProcessor.accept(line);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            ARBShaderObjects.glDeleteObjectARB(shader);
            return 0;
        }

        ARBShaderObjects.glShaderSourceARB(shader, shaderCode.toString());
        ARBShaderObjects.glCompileShaderARB(shader);
        GLUtils.printLogInfo(shader);
        return shader;
    }

    static void vertPattern(String line) {
        if (MC_ENTITY.matcher(line).matches()) Shaders.entityAttrib = 10;
    }

    public static int createVertShader(String filename) {
        return createShader(ARBVertexShader.GL_VERTEX_SHADER_ARB, filename, ShaderCompiler::vertPattern);
    }

    static void fragPattern(String line) {
        if (ColorBuffer.colorAttachments < 5 && PATTERN_GAUX1.matcher(line).matches()) ColorBuffer.colorAttachments = 5;
        else if (ColorBuffer.colorAttachments < 6 && PATTERN_GAUX2.matcher(line).matches())
            ColorBuffer.colorAttachments = 6;
        else if (ColorBuffer.colorAttachments < 7 && PATTERN_GAUX3.matcher(line).matches())
            ColorBuffer.colorAttachments = 7;
        else if (ColorBuffer.colorAttachments < 8 && PATTERN_SHADOW.matcher(line).matches()) {
            ShadowBuffer.shadowEnabled = true;
            ColorBuffer.colorAttachments = 8;
        } else if (PATTERN_SHADOWRES.matcher(line).matches()) {
            String[] parts = SPLIT_PATTERN.split(line, 4);
            ShadowBuffer.shadowResolution = Math.round(
                    Integer.parseInt(parts[2]) * ShaderConfig.configShadowResMul);
            System.out.println("Shadow map resolution: " + ShadowBuffer.shadowResolution);
        } else if (PATTERN_SHADOWHPL.matcher(line).matches()) {
            String[] parts = SPLIT_PATTERN.split(line, 4);
            System.out.println("Shadow map half-plane: " + parts[2]);
            ShadowBuffer.shadowMapHalfPlane = Float.parseFloat(parts[2]);
        }
    }

    public static int createFragShader(String filename) {
        return createShader(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB, filename, ShaderCompiler::fragPattern);
    }
}