package com.uni.uai.graph.facesearch.model;

/**
 * 单帧内检测到的人脸框。
 */
public record FaceDetection(float x, float y, float width, float height, float score) {

    public int pixelArea() {
        return Math.round(width * height);
    }

    public boolean isLargeEnough(int minPixels) {
        return pixelArea() >= minPixels;
    }
}
