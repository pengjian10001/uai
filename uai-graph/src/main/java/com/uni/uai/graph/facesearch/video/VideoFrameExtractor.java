package com.uni.uai.graph.facesearch.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

/**
 * 使用 JavaCV/FFmpeg 按固定采样率抽帧，避免全量逐帧处理。
 */
public final class VideoFrameExtractor {

    @FunctionalInterface
    public interface FrameConsumer {
        void accept(Frame frame, long frameNum, double timeSecond) throws Exception;
    }

    private VideoFrameExtractor() {
    }

    public static void extractFrames(String videoPath, int sampleFps, FrameConsumer consumer) throws Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();
            double frameRate = grabber.getFrameRate();
            if (frameRate <= 0) {
                frameRate = 25;
            }
            long sampleInterval = Math.max(1, Math.round(frameRate / sampleFps));

            Frame frame;
            long frameIdx = 0;
            while ((frame = grabber.grabImage()) != null) {
                if (frameIdx % sampleInterval == 0 && frame.image != null) {
                    double timeSec = frameIdx / frameRate;
                    consumer.accept(frame, frameIdx, timeSec);
                }
                frameIdx++;
            }
        }
    }
}
