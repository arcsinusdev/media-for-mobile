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

package org.m4m;

import org.m4m.domain.IEffectorSurface;
import org.m4m.domain.Pair;
import org.m4m.domain.Resolution;

/**
 * Use this simple interface for implementing video effects for MediaComposer pipeline.
 */

public interface IVideoEffect extends IBaseVideoEffect {

    /**
     * Performs internal initialization. Creates required internal components and allocates buffers if necessary.
     * Called from GL thread.
     */
    void start();

    /**
     * Applies effect. Main function of the effect object.
     * Gets called for each frame for which the affect is to be applied.
     *
     * @param inTextureId     Input texture ID.
     * @param timeProgress    Time in nanoseconds of applying the effect in the target stream.
     * @param transformMatrix Transform matrix.
     */
    void applyEffect(int inTextureId, long timeProgress, float[] transformMatrix);

    /**
     * Sets input resolution. Notifies the effect about input resolution change.
     * Called by the pipeline manager
     *
     * @param resolution Resolution of surfaces for which the effect is to be applied.
     */
    void setInputResolution(Resolution resolution);

    /**
     * Specifies angle of the resulted frame.
     *
     * @param degrees Rotation angle.
     */
    void setAngle(int degrees);

    /**
     * Specifies angle of the resulted frame.
     *
     */
    int getAngle();

    void addTimeInterval(Pair<Long, Long> interval);

    void addTimeInterval(long start, long end);

    /*
    * Whether the effect will affect in the specified time position
    *
    * @param timeProgress Time position in microseconds
    * */
    boolean isActive(long timeProgress);

    void setOverlaySurface(IEffectorSurface surface);

    void setOverlayActive(boolean isOverlayActive);

    void release();
}
