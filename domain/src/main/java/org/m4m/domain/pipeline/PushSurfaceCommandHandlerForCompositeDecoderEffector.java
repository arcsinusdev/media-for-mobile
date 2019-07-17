/* Copyright (c) 2019 Arcsinus. All rights reserved
 * Author: Andrei Kulpin
 */

package org.m4m.domain.pipeline;

import org.m4m.IVideoEffect;
import org.m4m.domain.CompositeVideoDecoder;
import org.m4m.domain.Frame;
import org.m4m.domain.ICommandHandler;
import org.m4m.domain.VideoEffector;

public class PushSurfaceCommandHandlerForCompositeDecoderEffector implements ICommandHandler {
    private final CompositeVideoDecoder compositeVideoDecoder;
    protected final VideoEffector videoEffector;

    PushSurfaceCommandHandlerForCompositeDecoderEffector(
            CompositeVideoDecoder output,
            VideoEffector videoEffector)
    {
        this.compositeVideoDecoder = output;
        this.videoEffector = videoEffector;
    }

    @Override
    public void handle() {
        Frame primaryFrame = compositeVideoDecoder.getPrimaryFrame();
        Frame overlayFrame = compositeVideoDecoder.getOverlayFrame(primaryFrame.getSampleTime());

        boolean isOverlaying = overlayFrame != null;
        for (IVideoEffect effect : videoEffector.getVideoEffects()) {
            effect.setOverlayActive(isOverlaying);
        }

        videoEffector.push(primaryFrame);
        videoEffector.checkIfOutputQueueHasData();
    }
}
