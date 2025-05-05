// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package net.mine_diver.macula;

import net.mine_diver.macula.util.MatrixUtil;
import net.mine_diver.macula.util.MinecraftInstance;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
import static org.lwjgl.opengl.GL20.GL_MAX_DRAW_BUFFERS;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public class Shaders {

    public static final Minecraft MINECRAFT = MinecraftInstance.get();

    public static void setIsInitialized(boolean isInitialized) {
        Shaders.isInitialized = isInitialized;
    }

    private static boolean isInitialized = false;

    public static int renderWidth = 0;
    public static int renderHeight = 0;

    private static final float[] clearColor = new float[3];

    public static float rainStrength = 0.0f;

    public static boolean fogEnabled = true;

    public static int entityAttrib = -1;

    // Shadow stuff

    // configuration
    public static int shadowPassInterval = 0;
    private static int shadowMapWidth = 1024;
    private static int shadowMapHeight = 1024;
    private static float shadowMapFOV = 25.0f;
    private static float shadowMapHalfPlane = 30.0f;
    private static boolean shadowMapIsOrtho = true;

    private static int shadowPassCounter = 0;

    public static boolean isShadowPass = false;

    private static int sfb = 0;
    private static int sfbDepthTexture = 0;
    private static int sfbDepthBuffer = 0;

    // Color attachment stuff

    private static int colorAttachments = 0;

    private static IntBuffer dfbDrawBuffers = null;

    private static IntBuffer dfbTextures = null;
    private static IntBuffer dfbRenderBuffers = null;

    private static int dfb = 0;
    private static int dfbDepthBuffer = 0;

    static {
        if (!ShaderConfig.configDir.exists())
            if (!ShaderConfig.configDir.mkdirs())
                throw new RuntimeException();
        ShaderConfig.loadConfig();
    }

    public static void init() {
        if (!(ShaderPack.shaderPackLoaded = !ShaderPack.currentShaderName.equals(ShaderPack.SHADER_DISABLED))) return;

        MatrixBuffer.initMatrixBuffer();

        int maxDrawBuffers = glGetInteger(GL_MAX_DRAW_BUFFERS);

        System.out.println("GL_MAX_DRAW_BUFFERS = " + maxDrawBuffers);

        colorAttachments = 4;

        ShaderProgram.initializeShaders();

        if (colorAttachments > maxDrawBuffers) System.out.println("Not enough draw buffers!");

        ShaderProgram.resolveFallbacks();

        dfbDrawBuffers = BufferUtils.createIntBuffer(colorAttachments);
        for (int i = 0; i < colorAttachments; ++i) dfbDrawBuffers.put(i, GL_COLOR_ATTACHMENT0_EXT + i);

        dfbTextures = BufferUtils.createIntBuffer(colorAttachments);
        dfbRenderBuffers = BufferUtils.createIntBuffer(colorAttachments);

        resize();
        setupShadowFrameBuffer();
        isInitialized = true;
    }

    public static void destroy() {
        for (ShaderProgram.ShaderProgramType shaderProgramType : ShaderProgram.ShaderProgramType.values()) {
            int handle = ShaderProgram.shaderProgramId.get(shaderProgramType);
            if (handle != 0) {
                glDeleteObjectARB(handle);
                ShaderProgram.shaderProgramId.put(shaderProgramType, 0);
            }
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

    public static void setupShadowViewportAndMatrices(float f, float x, float y, float z) {
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


        MatrixBuffer.getMatrixBuffer(GL_PROJECTION_MATRIX, MatrixBuffer.shadowProjection);
        MatrixUtil.invertMat4(MatrixBuffer.shadowProjection, MatrixBuffer.shadowProjectionInverse);

        MatrixBuffer.getMatrixBuffer(GL_MODELVIEW_MATRIX, MatrixBuffer.shadowModelView);
        MatrixUtil.invertMat4(MatrixBuffer.shadowModelView, MatrixBuffer.shadowModelViewInverse);
    }

    public static void beginRender(Minecraft minecraft, float f, long l) {
        rainStrength = minecraft.level.getRainGradient(f);

        if (isShadowPass) return;

        if (!isInitialized) init();
        if (!ShaderPack.shaderPackLoaded) return;
        if (MINECRAFT.actualWidth != renderWidth || MINECRAFT.actualHeight != renderHeight)
            resize();

        if (shadowPassInterval > 0 && --shadowPassCounter <= 0) {
            // do shadow pass
            boolean preShadowPassThirdPersonView = MINECRAFT.options.thirdPerson;

            MINECRAFT.options.thirdPerson = true;

            isShadowPass = true;
            shadowPassCounter = shadowPassInterval;

            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, sfb);

            ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.NONE);

            MINECRAFT.gameRenderer.delta(f, l);

            glFlush();

            isShadowPass = false;

            MINECRAFT.options.thirdPerson = preShadowPassThirdPersonView;
        }

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, dfb);

        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.TEXTURED);
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

        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.COMPOSITE);

        glDrawBuffers(dfbDrawBuffers);

        bindTextures();
        GLUtils.drawQuad();

        // final

        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.FINAL);

        glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        bindTextures();
        GLUtils.drawQuad();

        glEnable(GL_BLEND);

        glPopMatrix();
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.NONE);
    }

    private static void bindTerrainTextures() {
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_nh.png"));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_s.png"));
        glActiveTexture(GL_TEXTURE0);
    }

    public static void beginTerrain() {
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.TERRAIN);
        bindTerrainTextures();
    }

    public static void endTerrain() {
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.TEXTURED);
    }

    public static void beginWater() {
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.WATER);
        bindTerrainTextures();
    }

    public static void endWater() {
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.TEXTURED);
    }

    public static void beginHand() {
        glEnable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.HAND);
    }

    public static void endHand() {
        glDisable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.TEXTURED);

        if (isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, sfb); // was set to 0 in beginWeather()
    }

    public static void beginWeather() {
        glEnable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.WEATHER);

        if (isShadowPass) glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0); // will be set to sbf in endHand()
    }

    public static void endWeather() {
        glDisable(GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgram.ShaderProgramType.TEXTURED);
    }

    private static void resize() {
        renderWidth = MINECRAFT.actualWidth;
        renderHeight = MINECRAFT.actualHeight;
        setupFrameBuffer();
    }

    private static int getUniformLocation(String name) {
        // TODO: Implement uniform location caching to avoid repeated calls to glGetUniformLocation
        return glGetUniformLocationARB(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram), name);
    }

    public static void setProgramUniform1i(String name, int n) {
        int uniform = getUniformLocation(name);
        if (uniform != -1) glUniform1iARB(uniform, n);
    }

    public static void setProgramUniform1f(String name, float x) {
        int uniform = getUniformLocation(name);
        if (uniform != -1) glUniform1fARB(uniform, x);
    }

    public static void setProgramUniform3f(String name, float[] vec3) {
        int uniform = getUniformLocation(name);
        if (uniform != -1) glUniform3fARB(uniform, vec3[0], vec3[1], vec3[2]);
    }

    public static void setProgramUniformMatrix4(String name, FloatBuffer mat4) {
        int uniform = getUniformLocation(name);
        final boolean TRANSPOSE = false;
        if (uniform != -1) glUniformMatrix4ARB(uniform, TRANSPOSE, mat4);
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
        GLUtils.printLogInfo(shader);
        return shader;
    }

    private static final Pattern MC_ENTITY = Pattern.compile("attribute [_a-zA-Z0-9]+ mc_Entity.*");

    private static void vertPattern(String line) {
        if (MC_ENTITY.matcher(line).matches()) entityAttrib = 10;
    }

    public static int createVertShader(String filename) {
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

    public static int createFragShader(String filename) {
        return createShader(GL_FRAGMENT_SHADER_ARB, filename, Shaders::fragPattern);
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
