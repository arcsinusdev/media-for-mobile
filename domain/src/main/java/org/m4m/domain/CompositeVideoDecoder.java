/* Copyright (c) 2019 Arcsinus. All rights reserved
 * Author: Andrei Kulpin
 */

package org.m4m.domain;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class CompositeVideoDecoder implements IOutputRaw, Closeable {
    private final long OVERLAY_INTERVAL_ADJUSTMENT = 40 * 1000L;
    
    private VideoDecoder primaryDecoder;
    private VideoDecoder overlayDecoder;
    private List<Pair<Long, Long>> overlayIntervals;

    private Frame deferredOverlayFrame;
    private long deferredOverlayFrameIntervalStartTime;

    public CompositeVideoDecoder(VideoDecoder primaryDecoder,
                                 VideoDecoder overlayDecoder,
                                 List<Pair<Long, Long>> overlayIntervals)
    {
        this.primaryDecoder = primaryDecoder;
        this.overlayDecoder = overlayDecoder;
        this.overlayIntervals = overlayIntervals;
    }

    public VideoDecoder getPrimaryDecoder() {
        return primaryDecoder;
    }

    public VideoDecoder getOverlayDecoder() {
        return overlayDecoder;
    }

    @Override
    public boolean canConnectFirst(IInputRaw connector) {
        return true;
    }

    @Override
    public CommandQueue getOutputCommandQueue() {
        return primaryDecoder.getOutputCommandQueue();
    }

    @Override
    public void fillCommandQueues() {
        primaryDecoder.fillCommandQueues();
    }

    @Override
    public void close() throws IOException {
        primaryDecoder.close();

        if (overlayDecoder != null)
            overlayDecoder.close();
    }

    /**
    * Fetches next frame from primary media source queue.
    * */
    public Frame getPrimaryFrame() {
        Frame primaryFrame = primaryDecoder.getFrame();
        primaryDecoder.releaseOutputBuffer(primaryFrame.getBufferIndex());
        return primaryFrame;
    }

    /*
    * Tries to fetch next frame from overlay video queue according to specified time.
    * */
    public Frame getOverlayFrame(long primaryTime) {
        Frame overlayFrame = null;
        if (overlayDecoder != null && isOverlayActive(primaryTime)) {
            if (deferredOverlayFrame != null) {
                if (deferredOverlayFrameIntervalStartTime <= primaryTime) {
                    overlayDecoder.releaseOutputBuffer(deferredOverlayFrame.getBufferIndex());
                    overlayFrame = deferredOverlayFrame;
                    deferredOverlayFrame = null;
                }

            } else {
                Frame nextOverlayFrame = overlayDecoder.getFrame();

                Pair<Long, Long> interval = getOverlayIntervalByOverlayTime(nextOverlayFrame.getSampleTime());
                if (interval != null && nextOverlayFrame.getBufferIndex() != 0) {
                    if (interval.left <= primaryTime) {
                        overlayDecoder.releaseOutputBuffer(nextOverlayFrame.getBufferIndex());
                        overlayFrame = nextOverlayFrame;

                    } else {
                        deferredOverlayFrame = nextOverlayFrame;
                        deferredOverlayFrameIntervalStartTime = interval.left;
                    }
                }
            }
        }

        return overlayFrame;
    }

    private boolean isOverlayActive(long time) {
        return overlayIntervals.isEmpty() || getOverlayIntervalByPrimaryTime(time) != null;
    }

    private Pair<Long, Long> getOverlayIntervalByPrimaryTime(long timeProgress) {
        for (Pair<Long, Long> interval : overlayIntervals) {
            if (timeProgress >= interval.left && timeProgress <= interval.right) {
                return interval;
            }
        }
        return null;
    }

    private Pair<Long, Long> getOverlayIntervalByOverlayTime(long timeProgress) {
        long duration = 0;
        for (Pair<Long, Long> interval : overlayIntervals) {
            duration += interval.right - interval.left;

            /*
             * First frame of each next overlay video becomes with sample time
             * that is in previous overlay interval, so for such frame next interval
             * is returned.
             * */
            if (timeProgress <= duration - OVERLAY_INTERVAL_ADJUSTMENT) return interval;
        }
        return null;
    }
}
