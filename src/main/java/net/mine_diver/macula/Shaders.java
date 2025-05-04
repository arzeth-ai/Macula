// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package net.mine_diver.macula;

import net.mine_diver.macula.util.MatrixUtil;
import net.mine_diver.macula.util.MinecraftInstance;
import net.minecraft.block.BlockBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Living;
import net.minecraft.item.ItemInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.ARBFragmentShader.GL_FRAGMENT_SHADER_ARB;
import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.ARBTextureFloat.GL_RGB32F_ARB;
import static org.lwjgl.opengl.ARBVertexShader.GL_VERTEX_SHADER_ARB;
import static org.lwjgl.opengl.ARBVertexShader.glBindAttribLocationARB;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public class Shaders {

    public static final Minecraft MINECRAFT = MinecraftInstance.get();

    public static boolean isIsInitialized() {
        return isInitialized;
    }

    public static void setIsInitialized(boolean isInitialized) {
        Shaders.isInitialized = isInitialized;
    }

    private static boolean isInitialized = false;

    private static int renderWidth = 0;
    private static int renderHeight = 0;

    private static final float[] sunPosition = new float[3];
    private static final float[] moonPosition = new float[3];

    private static final float[] clearColor = new float[3];

    private static float rainStrength = 0.0f;

    private static boolean fogEnabled = true;

    public static int entityAttrib = -1;

    private static final FloatBuffer previousProjection = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projectionInverse = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer previousModelView = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer modelViewInverse = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer modelViewCelestial = BufferUtils.createFloatBuffer(16);

    private static final double[] previousCameraPosition = new double[3];
    private static final double[] cameraPosition = new double[3];

    // Shadow stuff

    // configuration
    private static int shadowPassInterval = 0;
    private static int shadowMapWidth = 1024;
    private static int shadowMapHeight = 1024;
    private static float shadowMapFOV = 25.0f;
    private static float shadowMapHalfPlane = 30.0f;
    private static boolean shadowMapIsOrtho = true;

    private static int shadowPassCounter = 0;

    private static boolean isShadowPass = false;

    private static int sfb = 0;
    private static int sfbDepthTexture = 0;
    private static int sfbDepthBuffer = 0;

    private static final FloatBuffer shadowProjection = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer shadowProjectionInverse = BufferUtils.createFloatBuffer(16);

    private static final FloatBuffer shadowModelView = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer shadowModelViewInverse = BufferUtils.createFloatBuffer(16);

    // Color attachment stuff

    private static int colorAttachments = 0;

    private static IntBuffer dfbDrawBuffers = null;

    private static IntBuffer dfbTextures = null;
    private static IntBuffer dfbRenderBuffers = null;

    private static int dfb = 0;
    private static int dfbDepthBuffer = 0;

    // Program stuff
    public enum Program {
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
        public final Program fallback;

        Program(String fileName, Program fallback) {
            this.fileName = fileName;
            this.fallback = fallback;
        }
    }

    public static final EnumMap<Program, Integer> programs = new EnumMap<>(Program.class);

    public static Program activeProgram = Program.NONE;

    // shaderpack fields

    public static boolean shaderPackLoaded = false;

    // debug info

    public static final String glVersionString = GL11.glGetString(GL11.GL_VERSION);
    public static final String glVendorString = GL11.glGetString(GL11.GL_VENDOR);
    public static final String glRendererString = GL11.glGetString(GL11.GL_RENDERER);

    static {
        if (!ShaderConfig.configDir.exists())
            if (!ShaderConfig.configDir.mkdirs())
                throw new RuntimeException();
        ShaderConfig.loadConfig();
    }

    public static void init() {
        if (!(shaderPackLoaded = !ShaderPack.currentShaderName.equals(ShaderPack.SHADER_DISABLED))) return;

        BufferUtils.zeroBuffer(projection);
        BufferUtils.zeroBuffer(previousProjection);
        BufferUtils.zeroBuffer(modelView);
        BufferUtils.zeroBuffer(previousModelView);
        BufferUtils.zeroBuffer(shadowProjection);
        BufferUtils.zeroBuffer(shadowModelView);
        BufferUtils.zeroBuffer(modelViewCelestial);

        int maxDrawBuffers = glGetInteger(GL_MAX_DRAW_BUFFERS);

        System.out.println("GL_MAX_DRAW_BUFFERS = " + maxDrawBuffers);

        colorAttachments = 4;

        Program[] allPrograms = Program.values();
        for (Program program : allPrograms) {
            if (program.fileName.isEmpty()) {
                programs.put(program, 0);
            } else {
                programs.put(program, setupProgram(program.fileName + ".vsh", program.fileName + ".fsh"));
            }
        }

        if (colorAttachments > maxDrawBuffers) System.out.println("Not enough draw buffers!");

        for (Program program : allPrograms) {
            Program current = program;
            while (programs.get(current) == 0) {
                if (current.fallback == null || current == current.fallback) break;
                current = current.fallback;
            }
            programs.put(program, programs.get(current));
        }

        dfbDrawBuffers = BufferUtils.createIntBuffer(colorAttachments);
        for (int i = 0; i < colorAttachments; ++i) dfbDrawBuffers.put(i, GL_COLOR_ATTACHMENT0_EXT + i);

        dfbTextures = BufferUtils.createIntBuffer(colorAttachments);
        dfbRenderBuffers = BufferUtils.createIntBuffer(colorAttachments);

        resize();
        setupShadowFrameBuffer();
        isInitialized = true;
    }

    public static void destroy() {
        for (Program program : Program.values()) {
            int handle = programs.get(program);
            if (handle != 0) {
                glDeleteObjectARB(handle);
                programs.put(program, 0);
            }
        }
    }

    public static void glEnableWrapper(int cap) {
        glEnable(cap);
        if (cap == GL_TEXTURE_2D) {
            if (activeProgram == Program.BASIC) useProgram(Program.TEXTURED);
        } else if (cap == GL_FOG) {
            fogEnabled = true;
            setProgramUniform1i("fogMode", glGetInteger(GL_FOG_MODE));
        }
    }

    public static void glDisableWrapper(int cap) {
        glDisable(cap);
        if (cap == GL_TEXTURE_2D) {
            if (activeProgram == Program.TEXTURED || activeProgram == Program.TEXTURED_LIT) useProgram(Program.BASIC);
        } else if (cap == GL_FOG) {
            fogEnabled = false;
            setProgramUniform1i("fogMode", 0);
        }
    }

    public static void setClearColor(float red, float green, float blue) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;

        if (isShadowPass) {
            glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            return;
        }

        glDrawBuffers(dfbDrawBuffers);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glDrawBuffers(GL_COLOR_ATTACHMENT0_EXT);
        glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glDrawBuffers(GL_COLOR_ATTACHMENT1_EXT);
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glDrawBuffers(dfbDrawBuffers);
    }

    private static void copyBuffer(FloatBuffer src, FloatBuffer dst) {
        dst.clear();
        dst.put(src);
        dst.flip();
    }

    private static void getMatrixBuffer(int glMatrixType, FloatBuffer buffer) {
        buffer.clear();
        glGetFloat(glMatrixType, buffer);
    }

    public static void setCamera(float f) {
        Living viewEntity = MINECRAFT.viewEntity;

        double x = viewEntity.prevRenderX + (viewEntity.x - viewEntity.prevRenderX) * f;
        double y = viewEntity.prevRenderY + (viewEntity.y - viewEntity.prevRenderY) * f;
        double z = viewEntity.prevRenderZ + (viewEntity.z - viewEntity.prevRenderZ) * f;

        if (isShadowPass) {
            setupShadowViewportAndMatrices(f, (float) x, (float) y, (float) z);
            return;
        }

        copyBuffer(projection, previousProjection);
        getMatrixBuffer(GL_PROJECTION_MATRIX, projection);
        MatrixUtil.invertMat4(projection, projectionInverse);

        copyBuffer(modelView, previousModelView);
        getMatrixBuffer(GL_MODELVIEW_MATRIX, modelView);
        MatrixUtil.invertMat4(modelView, modelViewInverse);

        previousCameraPosition[0] = cameraPosition[0];
        previousCameraPosition[1] = cameraPosition[1];
        previousCameraPosition[2] = cameraPosition[2];

        cameraPosition[0] = x;
        cameraPosition[1] = y;
        cameraPosition[2] = z;
    }

    private static void setupShadowViewportAndMatrices(float f, float x, float y, float z) {
        glViewport(0, 0, shadowMapWidth, shadowMapHeight);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // just backwards compatibility. it's only used when SHADOWFOV is set in the shaders.
        if (shadowMapIsOrtho) {
            glOrtho(-shadowMapHalfPlane, shadowMapHalfPlane, -shadowMapHalfPlane, shadowMapHalfPlane, 0.05f, 256.0f);
        } else {
            gluPerspective(shadowMapFOV, (float) shadowMapWidth / (float) shadowMapHeight, 0.05f, 256.0f);
        }

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glTranslatef(0.0f, 0.0f, -100.0f);
        glRotatef(90.0f, 0.0f, 0.0f, -1.0f);
        float angle = MINECRAFT.level.method_198(f) * 360.0f;
        // night time
        // day time
        if (angle < 90.0 || angle > 270.0) glRotatef(angle - 90.0f, -1.0f, 0.0f, 0.0f);
        else glRotatef(angle + 90.0f, -1.0f, 0.0f, 0.0f);
        // reduces jitter
        if (shadowMapIsOrtho)
            glTranslatef(x % 10.0f - 5.0f, y % 10.0f - 5.0f, z % 10.0f - 5.0f);


        getMatrixBuffer(GL_PROJECTION_MATRIX, shadowProjection);
        MatrixUtil.invertMat4(shadowProjection, shadowProjectionInverse);

        getMatrixBuffer(GL_MODELVIEW_MATRIX, shadowModelView);
        MatrixUtil.invertMat4(shadowModelView, shadowModelViewInverse);
    }

    public static void beginRender(Minecraft minecraft, float f, long l) {
        rainStrength = minecraft.level.getRainGradient(f);

        if (isShadowPass) return;

        if (!isInitialized) init();
        if (!shaderPackLoaded) return;
        if (MINECRAFT.actualWidth != renderWidth || MINECRAFT.actualHeight != renderHeight)
            resize();

        if (shadowPassInterval > 0 && --shadowPassCounter <= 0) {
            // do shadow pass
            boolean preShadowPassThirdPersonView = MINECRAFT.options.thirdPerson;

            MINECRAFT.options.thirdPerson = true;

            isShadowPass = true;
            shadowPassCounter = shadowPassInterval;

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, sfb);

            useProgram(Program.NONE);

            MINECRAFT.gameRenderer.delta(f, l);

            glFlush();

            isShadowPass = false;

            MINECRAFT.options.thirdPerson = preShadowPassThirdPersonView;
        }

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, dfb);

        useProgram(Program.TEXTURED);
    }

    private static void bindTextures() {
        for (byte i = 0; i < colorAttachments; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, dfbTextures.get(i));
        }

        if (shadowPassInterval > 0) {
            glActiveTexture(GL_TEXTURE7);
            glBindTexture(GL_TEXTURE_2D, sfbDepthTexture);
        }

        glActiveTexture(GL_TEXTURE0);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawQuad() {
        glBegin(GL_TRIANGLES);

        // First triangle
        glTexCoord2f(0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);

        glTexCoord2f(1.0f, 0.0f);
        glVertex3f(1.0f, 0.0f, 0.0f);

        glTexCoord2f(1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 0.0f);

        // Second triangle
        glTexCoord2f(0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);

        glTexCoord2f(1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 0.0f);

        glTexCoord2f(0.0f, 1.0f);
        glVertex3f(0.0f, 1.0f, 0.0f);

        glEnd();
    }


    public static void endRender() {
        if (isShadowPass) return;

        glPushMatrix();

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        // composite

        glDisable(GL_BLEND);

        useProgram(Program.COMPOSITE);

        glDrawBuffers(dfbDrawBuffers);

        bindTextures();
        drawQuad();

        // final

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

        useProgram(Program.FINAL);

        glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        bindTextures();
        drawQuad();

        glEnable(GL_BLEND);

        glPopMatrix();
        useProgram(Program.NONE);
    }

    private static void bindTerrainTextures() {
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_nh.png"));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_s.png"));
        glActiveTexture(GL_TEXTURE0);
    }

    public static void beginTerrain() {
        useProgram(Program.TERRAIN);
        bindTerrainTextures();
    }

    public static void endTerrain() {
        useProgram(Program.TEXTURED);
    }

    public static void beginWater() {
        useProgram(Program.WATER);
        bindTerrainTextures();
    }

    public static void endWater() {
        useProgram(Program.TEXTURED);
    }

    public static void beginHand() {
        glEnable(GL_BLEND);
        useProgram(Program.HAND);
    }

    public static void endHand() {
        glDisable(GL_BLEND);
        useProgram(Program.TEXTURED);

        if (isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, sfb); // was set to 0 in beginWeather()
    }

    public static void beginWeather() {
        glEnable(GL_BLEND);
        useProgram(Program.WEATHER);

        if (isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0); // will be set to sbf in endHand()
    }

    public static void endWeather() {
        glDisable(GL_BLEND);
        useProgram(Program.TEXTURED);
    }

    private static void resize() {
        renderWidth = MINECRAFT.actualWidth;
        renderHeight = MINECRAFT.actualHeight;
        setupFrameBuffer();
    }

    private static int setupProgram(String vShaderPath, String fShaderPath) {
        int program = glCreateProgramObjectARB();

        int vShader = 0;
        int fShader = 0;

        if (program != 0) {
            vShader = createVertShader(vShaderPath);
            fShader = createFragShader(fShaderPath);
        }

        if (vShader != 0 || fShader != 0) {
            if (vShader != 0) glAttachObjectARB(program, vShader);
            if (fShader != 0) glAttachObjectARB(program, fShader);
            if (entityAttrib >= 0) glBindAttribLocationARB(program, entityAttrib, "mc_Entity");
            glLinkProgramARB(program);
            glValidateProgramARB(program);
            printLogInfo(program);
        } else if (program != 0) {
            glDeleteObjectARB(program);
            program = 0;
        }

        return program;
    }

    public static void useProgram(Program program) {
        if (activeProgram == program) return;
        else if (isShadowPass) {
            activeProgram = Program.NONE;
            glUseProgramObjectARB(programs.get(Program.NONE));
            return;
        }
        activeProgram = program;
        glUseProgramObjectARB(programs.get(program));
        if (programs.get(program) == 0) return;
        else if (program == Program.TEXTURED) setProgramUniform1i("texture", 0);
        else if (program == Program.TEXTURED_LIT || program == Program.HAND || program == Program.WEATHER) {
            setProgramUniform1i("texture", 0);
            setProgramUniform1i("lightmap", 1);
        } else if (program == Program.TERRAIN || program == Program.WATER) {
            setProgramUniform1i("texture", 0);
            setProgramUniform1i("lightmap", 1);
            setProgramUniform1i("normals", 2);
            setProgramUniform1i("specular", 3);
        } else if (program == Program.COMPOSITE || program == Program.FINAL) {
            setProgramUniform1i("gcolor", 0);
            setProgramUniform1i("gdepth", 1);
            setProgramUniform1i("gnormal", 2);
            setProgramUniform1i("composite", 3);
            setProgramUniform1i("gaux1", 4);
            setProgramUniform1i("gaux2", 5);
            setProgramUniform1i("gaux3", 6);
            setProgramUniform1i("shadow", 7);
            setProgramUniformMatrix4ARB("gbufferPreviousProjection", false, previousProjection);
            setProgramUniformMatrix4ARB("gbufferProjection", false, projection);
            setProgramUniformMatrix4ARB("gbufferProjectionInverse", false, projectionInverse);
            setProgramUniformMatrix4ARB("gbufferPreviousModelView", false, previousModelView);
            if (shadowPassInterval > 0) {
                setProgramUniformMatrix4ARB("shadowProjection", false, shadowProjection);
                setProgramUniformMatrix4ARB("shadowProjectionInverse", false, shadowProjectionInverse);
                setProgramUniformMatrix4ARB("shadowModelView", false, shadowModelView);
                setProgramUniformMatrix4ARB("shadowModelViewInverse", false, shadowModelViewInverse);
            }
        }
        ItemInstance stack = MINECRAFT.player.inventory.getHeldItem();
        setProgramUniform1i("heldItemId", (stack == null ? -1 : stack.itemId));
        setProgramUniform1i("heldBlockLightValue",
                (stack == null || stack.itemId >= BlockBase.BY_ID.length ? 0 : BlockBase.EMITTANCE[stack.itemId]));
        setProgramUniform1i("fogMode", (fogEnabled ? glGetInteger(GL_FOG_MODE) : 0));
        setProgramUniform1f("rainStrength", rainStrength);
        setProgramUniform1i("worldTime", (int) (MINECRAFT.level.getLevelTime() % 24000L));
        setProgramUniform1f("aspectRatio", (float) renderWidth / (float) renderHeight);
        setProgramUniform1f("viewWidth", (float) renderWidth);
        setProgramUniform1f("viewHeight", (float) renderHeight);
        setProgramUniform1f("near", 0.05F);
        setProgramUniform1f("far", 256 >> MINECRAFT.options.viewDistance);
        setProgramUniform3f("sunPosition", sunPosition[0], sunPosition[1], sunPosition[2]);
        setProgramUniform3f("moonPosition", moonPosition[0], moonPosition[1], moonPosition[2]);
        setProgramUniform3f("previousCameraPosition", (float) previousCameraPosition[0],
                (float) previousCameraPosition[1], (float) previousCameraPosition[2]);
        setProgramUniform3f("cameraPosition", (float) cameraPosition[0], (float) cameraPosition[1],
                (float) cameraPosition[2]);
        setProgramUniformMatrix4ARB("gbufferModelView", false, modelView);
        setProgramUniformMatrix4ARB("gbufferModelViewInverse", false, modelViewInverse);
    }

    private static int getUniformLocation(String name) {
        if (activeProgram == Program.NONE) {
            return -1;
        }
        return glGetUniformLocationARB(programs.get(activeProgram), name);
    }

    public static void setProgramUniform1i(String name, int x) {
        int uniform = getUniformLocation(name);
        if(uniform != -1) glUniform1iARB(uniform, x);
    }

    public static void setProgramUniform1f(String name, float x) {
        int uniform = getUniformLocation(name);
        if(uniform != -1) glUniform1fARB(uniform, x);
    }

    public static void setProgramUniform3f(String name, float x, float y, float z) {
        int uniform = getUniformLocation(name);
        if(uniform != -1) glUniform3fARB(uniform, x, y, z);
    }

    public static void setProgramUniformMatrix4ARB(String name, boolean transpose, FloatBuffer matrix) {
        int uniform = getUniformLocation(name);
        if(uniform != -1) glUniformMatrix4ARB(uniform, transpose, matrix);
    }

    private static final float SUN_HEIGHT = 100.0F;

    public static void setCelestialPosition() {
        // This is called when the current matrix is the model view matrix based on the celestial angle.
        // The sun is at (0, 100, 0); the moon at (0, -100, 0).

        getMatrixBuffer(GL_MODELVIEW_MATRIX, modelViewCelestial);

        float[] matrixMV = new float[16];
        modelViewCelestial.get(0, matrixMV, 0, 16);

        // Equivalent to multiplying the matrix by (0, 100, 0, 0).
        sunPosition[0] = matrixMV[4] * SUN_HEIGHT;
        sunPosition[1] = matrixMV[5] * SUN_HEIGHT;
        sunPosition[2] = matrixMV[6] * SUN_HEIGHT;

        // The moon is opposite the sun.
        moonPosition[0] = -sunPosition[0];
        moonPosition[1] = -sunPosition[1];
        moonPosition[2] = -sunPosition[2];
    }

    private static int createShader(int shaderType, String filename, Consumer<String> lineProcessor) {
        int shader = glCreateShaderObjectARB(shaderType);
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
            glDeleteObjectARB(shader);
            return 0;
        }

        glShaderSourceARB(shader, shaderCode.toString());
        glCompileShaderARB(shader);
        printLogInfo(shader);
        return shader;
    }

    private static final Pattern MC_ENTITY = Pattern.compile("attribute [_a-zA-Z0-9]+ mc_Entity.*");

    private static void vertPattern(String line) {
        if (MC_ENTITY.matcher(line).matches()) entityAttrib = 10;
    }

    private static int createVertShader(String filename) {
        return createShader(GL_VERTEX_SHADER_ARB, filename, Shaders::vertPattern);
    }

    private static final Pattern PATTERN_GAUX1 = Pattern.compile("uniform [ _a-zA-Z0-9]+ gaux1;.*");
    private static final Pattern PATTERN_GAUX2 = Pattern.compile("uniform [ _a-zA-Z0-9]+ gaux2;.*");
    private static final Pattern PATTERN_GAUX3 = Pattern.compile("uniform [ _a-zA-Z0-9]+ gaux3;.*");
    private static final Pattern PATTERN_SHADOW = Pattern.compile("uniform [ _a-zA-Z0-9]+ shadow;.*");
    private static final Pattern PATTERN_SHADOWRES = Pattern.compile("/\\* SHADOWRES:([0-9]+) \\*/.*");
    private static final Pattern PATTERN_SHADOWFOV = Pattern.compile("/\\* SHADOWFOV:([0-9.]+) \\*/.*");
    private static final Pattern PATTERN_SHADOWHPL = Pattern.compile("/\\* SHADOWHPL:([0-9.]+) \\*/.*");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[: ]");

    private static void fragPattern(String line) {
        if (colorAttachments < 5 && PATTERN_GAUX1.matcher(line).matches()) colorAttachments = 5;
        else if (colorAttachments < 6 && PATTERN_GAUX2.matcher(line).matches())
            colorAttachments = 6;
        else if (colorAttachments < 7 && PATTERN_GAUX3.matcher(line).matches())
            colorAttachments = 7;
        else if (colorAttachments < 8 && PATTERN_SHADOW.matcher(line).matches()) {
            shadowPassInterval = 1;
            colorAttachments = 8;
        } else if (PATTERN_SHADOWRES.matcher(line).matches()) {
            String[] parts = SPLIT_PATTERN.split(line, 4);
            shadowMapWidth = shadowMapHeight = Math.round(Integer.parseInt(parts[2]) * ShaderConfig.configShadowResMul);
            System.out.println("Shadow map resolution: " + shadowMapWidth);
        } else if (PATTERN_SHADOWFOV.matcher(line).matches()) {
            String[] parts = SPLIT_PATTERN.split(line, 4);
            System.out.println("Shadow map field of view: " + parts[2]);
            shadowMapFOV = Float.parseFloat(parts[2]);
            shadowMapIsOrtho = false;
        } else if (PATTERN_SHADOWHPL.matcher(line).matches()) {
            String[] parts = SPLIT_PATTERN.split(line, 4);
            System.out.println("Shadow map half-plane: " + parts[2]);
            shadowMapHalfPlane = Float.parseFloat(parts[2]);
            shadowMapIsOrtho = true;
        }
    }

    private static int createFragShader(String filename) {
        return createShader(GL_FRAGMENT_SHADER_ARB, filename, Shaders::fragPattern);
    }

    private static boolean printLogInfo(int obj) {
        IntBuffer iVal = BufferUtils.createIntBuffer(1);
        glGetObjectParameterARB(obj, GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);

        int length = iVal.get();
        if (length > 1) {
            ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
            iVal.flip();
            glGetInfoLogARB(obj, iVal, infoLog);
            byte[] infoBytes = new byte[length];
            infoLog.get(infoBytes);
            String out = new String(infoBytes);
            System.out.println("Info log:\n" + out);
            return false;
        }
        return true;
    }

    private static void setupFrameBuffer() {
        setupRenderTextures();

        if (dfb != 0) {
            glDeleteFramebuffersEXT(dfb);
            glDeleteRenderbuffersEXT(dfbRenderBuffers);
        }

        dfb = glGenFramebuffersEXT();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, dfb);

        glGenRenderbuffersEXT(dfbRenderBuffers);

        for (int i = 0; i < colorAttachments; ++i) {
            glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, dfbRenderBuffers.get(i));
            // Depth buffer
            if (i == 1) glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_RGB32F_ARB, renderWidth, renderHeight);
            else glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_RGBA, renderWidth, renderHeight);
            glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, dfbDrawBuffers.get(i), GL_RENDERBUFFER_EXT,
                    dfbRenderBuffers.get(i));
            glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, dfbDrawBuffers.get(i), GL_TEXTURE_2D, dfbTextures.get(i), 0);
        }

        glDeleteRenderbuffersEXT(dfbDepthBuffer);
        dfbDepthBuffer = glGenRenderbuffersEXT();
        glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, dfbDepthBuffer);
        glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH_COMPONENT, renderWidth, renderHeight);
        glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, dfbDepthBuffer);

        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        if (status != GL_FRAMEBUFFER_COMPLETE_EXT)
            System.out.println("Failed creating framebuffer! (Status " + status + ")");
    }

    private static void setupShadowFrameBuffer() {
        if (shadowPassInterval <= 0) return;

        setupShadowRenderTexture();

        glDeleteFramebuffersEXT(sfb);

        sfb = glGenFramebuffersEXT();
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, sfb);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        glDeleteRenderbuffersEXT(sfbDepthBuffer);
        sfbDepthBuffer = glGenRenderbuffersEXT();
        glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, sfbDepthBuffer);
        glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH_COMPONENT, shadowMapWidth, shadowMapHeight);
        glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, sfbDepthBuffer);
        glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_TEXTURE_2D, sfbDepthTexture, 0);

        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        if (status != GL_FRAMEBUFFER_COMPLETE_EXT)
            System.out.println("Failed creating shadow framebuffer! (Status " + status + ")");
    }

    private static void setupRenderTextures() {
        glDeleteTextures(dfbTextures);
        glGenTextures(dfbTextures);

        for (int i = 0; i < colorAttachments; ++i) {
            glBindTexture(GL_TEXTURE_2D, dfbTextures.get(i));
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            if (i == 1) { // depth buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4 * 4);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB32F_ARB, renderWidth, renderHeight, 0, GL_RGBA, GL11.GL_FLOAT,
                        buffer);
            } else {
                ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, renderWidth, renderHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                        buffer);
            }
        }
    }

    private static void setupShadowRenderTexture() {
        if (shadowPassInterval <= 0) return;

        // Depth
        glDeleteTextures(sfbDepthTexture);
        sfbDepthTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, sfbDepthTexture);

        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        ByteBuffer buffer = ByteBuffer.allocateDirect(shadowMapWidth * shadowMapHeight * 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowMapWidth, shadowMapHeight, 0, GL_DEPTH_COMPONENT,
                GL11.GL_FLOAT, buffer);
    }
}
