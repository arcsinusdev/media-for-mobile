/*
 * Copyright 2014-2016 Media for Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.m4m.android.graphics;

import android.opengl.Matrix;

import org.m4m.IVideoEffect;
import org.m4m.domain.FileSegment;
import org.m4m.domain.IEffectorSurface;
import org.m4m.domain.Pair;
import org.m4m.domain.Resolution;
import org.m4m.domain.graphics.IEglUtil;
import org.m4m.domain.graphics.Program;
import org.m4m.domain.graphics.TextureRenderer;
import org.m4m.domain.graphics.TextureType;
import org.m4m.domain.pipeline.TriangleVerticesCalculator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class VideoEffect implements IVideoEffect {
    protected static final int FLOAT_SIZE_BYTES = 4;
    protected static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    protected static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    protected static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    protected Resolution inputResolution = new Resolution(0, 0);
    private FileSegment segment = new FileSegment(0l, 0l);
    protected IEglUtil eglUtil;
    protected Program eglProgram = new Program();
    // OpenGL handlers
    protected boolean wasStarted;
    protected float[] mvpMatrix = new float[16];
    private FloatBuffer triangleVertices;
    private int angle;
    protected ShaderProgram shaderProgram;
    private TextureRenderer.FillMode fillMode = TextureRenderer.FillMode.PreserveAspectFit;
    private String vertexShader =  IEglUtil.VERTEX_SHADER;
    private String fragmentShader =  IEglUtil.FRAGMENT_SHADER_OES;
    protected TextureType textureType;
    protected boolean isOverlayActive = false;

    protected final List<Pair<Long, Long>> timeIntervals = new ArrayList<>();

    public VideoEffect(int angle, IEglUtil eglUtil) {
        this(angle, eglUtil, TextureType.GL_TEXTURE_EXTERNAL_OES);
    }

    public VideoEffect(int angle, IEglUtil eglUtil, TextureType textureType) {
        this.angle = angle;
        this.eglUtil = eglUtil;
        this.textureType = textureType;
    }

    public void setVertexShader(String verexShader) {
        this.vertexShader = verexShader;
    }

    public void setFragmentShader(String fragmentShader) {
        this.fragmentShader = fragmentShader;
    }

    @Override
    public FileSegment getSegment() {
        return segment;
    }

    @Override
    public void setSegment(FileSegment segment) {
        this.segment = segment;
    }

    protected void addEffectSpecific() {
    }

    /**
     * Initializes GL state.  Call this after the encoder EGL surface has been created and made current.
     */
    @Override
    public void start() {
        triangleVertices = ByteBuffer
                .allocateDirect(TriangleVerticesCalculator.getDefaultTriangleVerticesData().length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        createProgram(vertexShader, fragmentShader);

        eglProgram.programHandle = shaderProgram.getProgramHandle();
        eglProgram.positionHandle = shaderProgram.getAttributeLocation("aPosition");
        eglProgram.textureHandle = shaderProgram.getAttributeLocation("aTextureCoord");
        eglProgram.mvpMatrixHandle = shaderProgram.getAttributeLocation("uMVPMatrix");
        eglProgram.stMatrixHandle = shaderProgram.getAttributeLocation("uSTMatrix");

        wasStarted = true;
    }

    @Override
    public void setInputResolution(Resolution resolution) {
        inputResolution = resolution;
    }

    @Override
    public void applyEffect(int inputTextureId, long timeProgress, float[] transformMatrix) {
        if (!wasStarted) {
            start();
        }
        triangleVertices.clear();
        triangleVertices.put(TriangleVerticesCalculator.getDefaultTriangleVerticesData()).position(0);

        Resolution outputResolution = eglUtil.calculateOutputResolution(inputResolution, fillMode);

        prepareMvpMatrix(outputResolution);

        eglUtil.drawFrameStart(
                eglProgram,
                triangleVertices,
                mvpMatrix,
                transformMatrix,
                angle,
                textureType,
                inputTextureId,
                outputResolution
        );
        addEffectSpecific();
        eglUtil.drawFrameFinish();
    }

    protected void prepareMvpMatrix(Resolution outputResolution) {
        Matrix.setIdentityM(mvpMatrix, 0);
        eglUtil.prepareMvpMatrix(
                angle, inputResolution, outputResolution, fillMode, mvpMatrix);
    }

    @Override
    public void setFillMode(TextureRenderer.FillMode fillMode) {
        this.fillMode = fillMode;
    }

    @Override
    public TextureRenderer.FillMode getFillMode() {
        return fillMode;
    }

    @Override
    public void setAngle(int degrees) {
        angle = degrees;
    }

    @Override
    public int getAngle() {
        return angle;
    }

    protected int createProgram(String vertexSource, String fragmentSource) {
        shaderProgram = new ShaderProgram(eglUtil);
        shaderProgram.create(vertexSource, fragmentSource);
        return shaderProgram.getProgramHandle();
    }

    protected void checkGlError(String component) {
        eglUtil.checkEglError(component);
    }

    protected void checkGlError() {
        eglUtil.checkEglError("VideoEffect");
    }

    protected Pair<Long, Long> getCurrentInterval(long timeProgress) {
        for (Pair<Long, Long> interval : timeIntervals) {
            if (timeProgress >= interval.left && timeProgress <= interval.right) return interval;
        }
        return null;
    }

    @Override
    public boolean isActive(long timeProgress) {
        // Assuming if no intervals added the effect is active
        return timeIntervals.isEmpty() || getCurrentInterval(timeProgress) != null;
    }

    @Override
    public void addTimeInterval(Pair<Long, Long> interval) {
        timeIntervals.add(interval);
    }

    @Override
    public void addTimeInterval(long start, long end) {
        timeIntervals.add(new Pair<>(start, end));
    }


    @Override
    public void setOverlaySurface(IEffectorSurface surface) {
    }

    @Override
    public void setOverlayActive(boolean isOverlayActive) {
        this.isOverlayActive = isOverlayActive;
    }

    @Override
    public void release() {
        if (shaderProgram != null)
            shaderProgram.release();
    }
}
